/*
  DrawingBotV3 by Ollie Lansdell <ollielansdell@hotmail.co.uk
  Original by Scott Cooper, Dullbits.com, <scottslongemailaddress@gmail.com>
 */
package drawingbot;

import java.awt.image.BufferedImage;
import java.io.File;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.logging.Level;
import java.util.logging.Logger;

import drawingbot.api.*;
import drawingbot.drawing.DrawingSets;
import drawingbot.files.exporters.GCodeSettings;
import drawingbot.files.json.presets.PresetProjectSettings;
import drawingbot.image.ImageFilterSettings;
import drawingbot.integrations.vpype.VpypeSettings;
import drawingbot.javafx.util.PropertyUtil;
import drawingbot.pfm.PFMSettings;
import drawingbot.plotting.IDrawingManager;
import drawingbot.plotting.canvas.ImageCanvas;
import drawingbot.plotting.canvas.ObservableCanvas;
import drawingbot.image.blend.EnumBlendMode;
import drawingbot.javafx.*;
import drawingbot.javafx.observables.ObservableDrawingSet;
import drawingbot.files.*;
import drawingbot.image.BufferedImageLoader;
import drawingbot.image.FilteredBufferedImage;
import drawingbot.javafx.observables.ObservableImageFilter;
import drawingbot.javafx.observables.ObservableProjectSettings;
import drawingbot.pfm.PFMFactory;
import drawingbot.plotting.PFMTaskImage;
import drawingbot.plotting.PlottedDrawing;
import drawingbot.registry.MasterRegistry;
import drawingbot.registry.Register;
import drawingbot.render.IDisplayMode;
import drawingbot.render.IRenderer;
import drawingbot.render.jfx.JavaFXRenderer;
import drawingbot.utils.*;
import drawingbot.plotting.PFMTask;
import drawingbot.utils.flags.Flags;
import javafx.application.Platform;
import javafx.beans.property.*;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.geometry.Point2D;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseEvent;
import javafx.scene.paint.Color;
import org.jetbrains.annotations.Nullable;

public class DrawingBotV3 implements IDrawingManager {

    public static final Logger logger = Logger.getLogger("DrawingBotV3");
    public static DrawingBotV3 INSTANCE;

    public static JavaFXRenderer RENDERER;
    public static IRenderer OPENGL_RENDERER;

    // DRAWING AREA \\
    public final ObservableCanvas drawingArea = new ObservableCanvas();
    public ICanvas targetCanvas = null;

    // PRE-PROCESSING \\
    public final ImageFilterSettings imgFilterSettings = new ImageFilterSettings();

    // PATH FINDING \\
    public final PFMSettings pfmSettings = new PFMSettings();

    public final SimpleFloatProperty cyanMultiplier = new SimpleFloatProperty(1F);
    public final SimpleFloatProperty magentaMultiplier = new SimpleFloatProperty(1F);
    public final SimpleFloatProperty yellowMultiplier = new SimpleFloatProperty(1F);
    public final SimpleFloatProperty keyMultiplier = new SimpleFloatProperty(0.75F);

    // PEN SETS \\
    public DrawingSets drawingSets = new DrawingSets();

    // VERSION CONTROL \\
    public final ObservableList<ObservableProjectSettings> projectVersions = FXCollections.observableArrayList();
    public final SimpleObjectProperty<ObservableProjectSettings> lastRun = new SimpleObjectProperty<>();

    // DISPLAY \\
    public final SimpleObjectProperty<IDisplayMode> displayMode = new SimpleObjectProperty<>();
    public final SimpleObjectProperty<EnumBlendMode> blendMode = new SimpleObjectProperty<>(EnumBlendMode.NORMAL);

    // VIEWPORT SETTINGS \\
    public static int SVG_DPI = 96;
    public static int PDF_DPI = 72;

    public final SimpleBooleanProperty exportRange = new SimpleBooleanProperty(false);
    public final SimpleBooleanProperty displayGrid = new SimpleBooleanProperty(false);

    //VPYPE SETTINGS
    public final VpypeSettings vpypeSettings = new VpypeSettings();

    //GCODE SETTINGS
    public final GCodeSettings gcodeSettings = new GCodeSettings();

    //the default JFX viewport background colours
    public Color backgroundColourDefault = new Color(244 / 255F, 244 / 255F, 244 / 255F, 1F);
    public Color backgroundColourDark = new Color(65 / 255F, 65 / 255F, 65 / 255F, 1F);

    // THREADS \\
    public ExecutorService taskService = initTaskService();
    public ExecutorService backgroundService = initBackgroundService();
    public ExecutorService imageFilteringService = initImageFilteringService();
    public ExecutorService parallelPlottingService = initParallelPlottingService();
    public ExecutorService serialConnectionWriteService = initSerialConnectionService();

    public TaskMonitor taskMonitor = new TaskMonitor(taskService);

    // TASKS \\
    public final ObjectProperty<FilteredBufferedImage> openImage = new SimpleObjectProperty<>(null);
    public final ObjectProperty<PFMTask> activeTask = new SimpleObjectProperty<>(null);
    public final ObjectProperty<PFMTask> renderedTask = new SimpleObjectProperty<>(null);
    public final ObjectProperty<PlottedDrawing> currentDrawing = new SimpleObjectProperty<>(null);

    public File openFile = null;

    // GUI \\
    public FXController controller;

    ///////////////////////////////////////////////////////////////////////////////////////////////////////

    public DrawingBotV3() {

        PropertyUtil.addPropertyListListener(drawingArea, (value, changed) -> {
            if(changed.contains(value.canvasColor)){
                reRender();
            }else{
                onCanvasChanged();
            }
        });

        blendMode.addListener((observable, oldValue, newValue) -> reRender());
        imgFilterSettings.currentFilters.get().addListener((ListChangeListener<ObservableImageFilter>) c -> onImageFiltersChanged());

        imgFilterSettings.imageRotation.addListener((observable, oldValue, newValue) -> onCanvasChanged());
        imgFilterSettings.imageFlipHorizontal.addListener((observable, oldValue, newValue) -> onCanvasChanged());
        imgFilterSettings.imageFlipVertical.addListener((observable, oldValue, newValue) -> onCanvasChanged());

        activeTask.addListener((observable, oldValue, newValue) -> setRenderFlag(Flags.ACTIVE_TASK_CHANGED, true));
        renderedTask.addListener((observable, oldValue, newValue) -> setRenderFlag(Flags.ACTIVE_TASK_CHANGED, true));
        currentDrawing.addListener((observable, oldValue, newValue) -> setRenderFlag(Flags.CURRENT_DRAWING_CHANGED, true));
        displayMode.addListener((observable, oldValue, newValue) -> {
            if(oldValue == null || newValue.getRenderer() == oldValue.getRenderer())
                setRenderFlag(Flags.FORCE_REDRAW, true);
        });
        openImage.addListener((observable, oldValue, newValue) -> setRenderFlag(Flags.FORCE_REDRAW, true));

        pfmSettings.factory.addListener((observable, oldValue, newValue) -> {
            pfmSettings.settings.set(MasterRegistry.INSTANCE.getObservablePFMSettingsList(newValue));
        });

        //generate the target canvas, which will always display the correct Plotting Resolution
        targetCanvas = new ImageCanvas(drawingArea, 0, 0, false){
            @Override
            public int getImageWidth() {
                return openImage.get() != null ? openImage.get().getSource().getWidth() : 0;
            }

            @Override
            public int getImageHeight() {
                return openImage.get() != null ? openImage.get().getSource().getHeight() : 0;
            }

            @Override
            public boolean flipAxis() {
                return imgFilterSettings.imageRotation.get().flipAxis;
            }
        };

    }

    ///////////////////////////////////////////////////////////////////////////////////////////////////////

    private final SimpleDoubleProperty localProgress = new SimpleDoubleProperty(0);
    private final SimpleStringProperty localMessage = new SimpleStringProperty("");

    public void updateLocalMessage(String message){
        localMessage.set(message);
    }

    public void updateLocalProgress(double progress){
        localProgress.set(progress);
    }

    ///////////////////////////////////////////////////////////////////////////////////////////////////////

    // RENDER FLAGS \\

    public <T> void setRenderFlag(Flags.BooleanFlag flag){
        for(IDisplayMode displayMode : MasterRegistry.INSTANCE.displayModes){
            displayMode.getRenderFlags().setFlag(flag, true);
        }
    }

    public <T> void setRenderFlag(Flags.Flag<T> flag, T value){
        for(IDisplayMode displayMode : MasterRegistry.INSTANCE.displayModes){
            displayMode.getRenderFlags().setFlag(flag, value);
        }
    }

    public void reRender(){
        setRenderFlag(Flags.FORCE_REDRAW);
    }

    public void onCanvasChanged(){
        setRenderFlag(Flags.CANVAS_CHANGED, true);
    }

    public void onDrawingCleared(){
        setRenderFlag(Flags.CLEAR_DRAWING, true);
    }

    public void onImageFiltersChanged(){
        setRenderFlag(Flags.IMAGE_FILTERS_FULL_UPDATE, true);
    }

    public void onImageFilterDirty(){
        setRenderFlag(Flags.IMAGE_FILTERS_PARTIAL_UPDATE, true);
    }

    ///////////////////////////////////////////////////////////////////////////////////////////////////////

    public void updateUI(){

        taskMonitor.tick();

        //TODO FIX THIS updatinmg on every tick

        //update the latest shapes/vertices counts from the active task
        if(getActiveTask() != null){
            if(getActiveTask().isRunning()){
                int geometryCount = getActiveTask().getCurrentGeometryCount();
                long vertexCount = getActiveTask().getCurrentVertexCount();

                controller.labelPlottedShapes.setText(Utils.defaultNF.format(geometryCount));
                controller.labelPlottedVertices.setText(Utils.defaultNF.format(vertexCount));

                long minutes = (getActiveTask().getElapsedTime() / 1000) / 60;
                long seconds = (getActiveTask().getElapsedTime() / 1000) % 60;
                controller.labelElapsedTime.setText(minutes + " m " + seconds + " s");
            }
        }
        /*
        else {
            controller.labelElapsedTime.setText("0 s");
            controller.labelPlottedShapes.setText("0");
            controller.labelPlottedVertices.setText("0");
        }
         */

        controller.labelPlottingResolution.setText((int)(targetCanvas.getScaledWidth()) + " x " + (int)(targetCanvas.getScaledHeight()));


        if(openImage.get() != null){
            controller.labelImageResolution.setText(openImage.get().getSource().getWidth() + " x " + openImage.get().getSource().getHeight());
        }else{
            controller.labelImageResolution.setText("0 x 0");
        }

        //tick all plugins
        MasterRegistry.PLUGINS.forEach(IPlugin::tick);
    }


    ///////////////////////////////////////////////////////////////////////////////////////////////////////

    ////// EVENTS

    public void onDisplayModeChanged(IDisplayMode oldValue, IDisplayMode newValue){
        if(oldValue != null){
            oldValue.resetSettings();
        }

        if(oldValue == null || newValue.getRenderer() != oldValue.getRenderer()){
            newValue.getRenderer().switchToRenderer();
        }

        newValue.applySettings();
    }

    public void onDrawingPenChanged(){
        updatePenDistribution();
    }

    public void onDrawingSetChanged(){
        if(drawingSets.activeDrawingSet.get() == null || drawingSets.activeDrawingSet.get().loadingDrawingSet){
            //prevents events being fired for every pen addition
            return;
        }
        updatePenDistribution();
        if(controller != null && controller.drawingSetsController != null){ //may not be initilized yet
            controller.drawingSetsController.onDrawingSetChanged(); //TODO REMOVE ME
        }
    }

    public void onImageFilterChanged(ObservableImageFilter filter){
        filter.dirty.set(true);
        onImageFilterDirty();
    }

    public void updatePenDistribution(){
        if(currentDrawing.get() != null){
            currentDrawing.get().updatePenDistribution();
            reRender();
        }
    }

    //// PLOTTING TASKS

    @Override
    public PlottedDrawing createNewPlottedDrawing() {
        return new PlottedDrawing(drawingArea, drawingSets);
    }

    @Override
    public PFMTask initPFMTask(ICanvas canvas, PFMFactory<?> pfmFactory, @Nullable List<GenericSetting<?, ?>> pfmSettings, ObservableDrawingSet drawingPenSet, @Nullable BufferedImage image, @Nullable File originalFile, boolean isSubTask) {
        if(image != null){
            canvas = new ImageCanvas(canvas, image, imgFilterSettings.imageRotation.get().flipAxis);
        }
        return initPFMTask(new PlottedDrawing(canvas, drawingSets), pfmFactory, pfmSettings, drawingPenSet, image, originalFile, isSubTask);
    }

    @Override
    public PFMTask initPFMTask(PlottedDrawing drawing, PFMFactory<?> pfmFactory, @Nullable List<GenericSetting<?, ?>> settings, ObservableDrawingSet drawingPenSet, @Nullable BufferedImage image, @Nullable File originalFile, boolean isSubTask){
        if(settings == null){
            settings = MasterRegistry.INSTANCE.getObservablePFMSettingsList(pfmFactory);
        }

        //only update the distribution type the first time the PFM is changed, also only trigger the update when Start Plotting is hit again, so the current drawing doesn't get re-rendered
        if(!isSubTask){
            Platform.runLater(() -> {
                if(drawingPenSet.colourSeperator.get().isDefault()){
                    if(pfmSettings.nextDistributionType.get() != null){
                        drawingPenSet.distributionType.set(pfmSettings.nextDistributionType.get());
                        pfmSettings.nextDistributionType.set(null);
                    }
                }else{
                    drawingPenSet.distributionType.set(EnumDistributionType.getRecommendedType(drawingPenSet, pfmFactory));
                }
            });
        }

        PFMTask task;
        if(!pfmFactory.isGenerativePFM()){
            task = new PFMTaskImage(this, drawing, pfmFactory, drawingPenSet, settings, imgFilterSettings, image, originalFile);
        }else{
            task = new PFMTask(this, drawing, pfmFactory, drawingPenSet, settings);
        }

        Object[] hookReturn = Hooks.runHook(Hooks.NEW_PLOTTING_TASK, task);
        task = (PFMTask) hookReturn[0];
        task.isSubTask = isSubTask;
        return task;
    }

    public void startPlotting(){
        if(activeTask.get() != null){
            activeTask.get().cancel();
        }
        if(openImage.get() != null){
            taskMonitor.queueTask(initPFMTask(drawingArea.copy(), pfmSettings.factory.get(), null, drawingSets.activeDrawingSet.get(), openImage.get().getSource(), openFile, false));
        }
    }

    public void stopPlotting(){
        if(activeTask.get() != null){
            activeTask.get().stopElegantly();
        }
    }

    public void saveLastRun(PFMTask plottingTask){
        backgroundService.submit(() -> {
            GenericPreset<PresetProjectSettings> preset = Register.PRESET_LOADER_PROJECT.createNewPreset();
            Register.PRESET_LOADER_PROJECT.getDefaultManager().updatePreset(preset); //TODO FIXME! - is last run even used ?
            lastRun.set(new ObservableProjectSettings(preset, true));
        });
    }

    public void resetPlotting(){
        resetTaskService();
        setActiveTask(null);
    }

    public void resetTaskService(){
        taskService.shutdownNow();
        taskService = initTaskService();
        taskMonitor.resetMonitor(taskService);
    }

    ///////////////////////////////////////////////////////////////////////////////////////////////////////


    public void openFile(File file, boolean internal) {
        BufferedImageLoader.Filtered loadingTask = getImageLoaderTask(file, internal);
        if(loadingTask != null){
            taskMonitor.queueTask(loadingTask);
        }
    }

    public BufferedImageLoader.Filtered getImageLoaderTask(File file, boolean internal){
        String extension = FileUtils.getExtension(file.toString());
        if(extension.equalsIgnoreCase(".drawingbotv3")){
            FXHelper.loadPresetFile(Register.PRESET_TYPE_PROJECT, file, true);
            return null;
        }

        if(activeTask.get() != null){
            activeTask.get().cancel();
            setActiveTask(null);
            openImage.set(null);
        }

        openFile = file;
        BufferedImageLoader.Filtered loadingImage = new BufferedImageLoader.Filtered(file.getPath(), internal);
        loadingImage.setOnSucceeded(e -> {
            openImage.set((FilteredBufferedImage) e.getSource().getValue());
            Platform.runLater(() -> displayMode.set(Register.INSTANCE.DISPLAY_MODE_IMAGE));
            FXApplication.primaryStage.setTitle(DBConstants.versionName + ", Version: " + DBConstants.appVersion + ", '" + file.getName() + "'");
        });
        return loadingImage;
    }

    @Override
    public void onPlottingTaskStageFinished(PFMTask task, EnumTaskStage stage){
        switch (stage){
            case QUEUED:
                break;
            case PRE_PROCESSING:
                Platform.runLater(() -> {
                    if(displayMode.get().getRenderer() != OPENGL_RENDERER || !FXApplication.isPremiumEnabled){
                        displayMode.setValue(Register.INSTANCE.DISPLAY_MODE_DRAWING);
                    }
                });
                break;
            case DO_PROCESS:
                break;
            case POST_PROCESSING:
            case FINISHING:
                break;
            case FINISHED:
                saveLastRun(task);
                break;
        }
        if(task == getRenderedTask()){
            setRenderFlag(Flags.ACTIVE_TASK_CHANGED_STATE, true);
        }
        logger.info("Plotting Task: Finished Stage " + stage.name());
    }

    ///////////////////////////////////////////////////////////////////////////////////////////////////////

    //// DRAWING MANAGER

    @Override
    public PFMTask getActiveTask(){
        return activeTask.get();
    }

    @Override
    public void setActiveTask(PFMTask task) {
        if(activeTask.get() == task){
            return;
        }
        if(activeTask.get() != null){
            final PFMTask toReset = activeTask.get();
            backgroundService.submit(toReset::reset); //help GC by removing references to Geometries, run after other queue tasks have finished
        }
        activeTask.set(task);
        renderedTask.set(null);
    }

    @Override
    public void setRenderedTask(PFMTask task) {
        renderedTask.set(task);
    }

    @Override
    public PFMTask getRenderedTask(){
        return renderedTask.get() == null ? activeTask.get() : renderedTask.get();
    }

    @Override
    public void setCurrentDrawing(PlottedDrawing drawing) {
        currentDrawing.set(drawing);
    }

    @Override
    public PlottedDrawing getCurrentDrawing() {
        return currentDrawing.get();
    }

    @Override
    public void clearDrawingRender(){
        onDrawingCleared();
    }

    ///////////////////////////////////////////////////////////////////////////////////////////////////////

    //// EXPORT TASKS

    public Task<?> createExportTask(DrawingExportHandler exportHandler, ExportTask.Mode exportMode, PlottedDrawing plottedDrawing, IGeometryFilter pointFilter, String extension, File saveLocation, boolean forceBypassOptimisation){
        ExportTask task = new ExportTask(exportHandler, exportMode, plottedDrawing, pointFilter, extension, saveLocation, true, forceBypassOptimisation, false);
        Object[] hookReturn = Hooks.runHook(Hooks.NEW_EXPORT_TASK, task);
        taskMonitor.queueTask((Task<?>) hookReturn[0]);
        return task;
    }

    ///////////////////////////////////////////////////////////////////////////////////////////////////////

    //// INTERACTION EVENTS

    private boolean ctrl_down = false;

    public void keyReleased(KeyEvent event) {
        if (event.getCode() == KeyCode.CONTROL) { ctrl_down = false; }
    }

    public void keyPressed(KeyEvent event) {
        if (event.getCode() == KeyCode.CONTROL) { ctrl_down = true; }

        if (event.getCode().isArrowKey()) {
            double delta = 0.01;
            double currentH = controller.viewportScrollPane.getHvalue();
            double currentV = controller.viewportScrollPane.getVvalue();
            if (event.getCode() == KeyCode.UP)    {
                controller.viewportScrollPane.setVvalue(currentV - delta);
            }
            if (event.getCode() == KeyCode.DOWN)  {
                controller.viewportScrollPane.setVvalue(currentV + delta);
            }
            if (event.getCode() == KeyCode.RIGHT) {
                controller.viewportScrollPane.setHvalue(currentH - delta);
            }
            if (event.getCode() == KeyCode.LEFT)  {
                controller.viewportScrollPane.setHvalue(currentH + delta);
            }
        }
    }

    ///////////////////////////////////////////////////////////////////////////////////////////////////////

    //// MOUSE EVENTS

    public void resetView(){
        controller.viewportScrollPane.scaleValue = 1;
        controller.viewportScrollPane.updateScale();
        controller.viewportScrollPane.setHvalue(0.5);
        controller.viewportScrollPane.setVvalue(0.5);
        FXApplication.drawTimer.resetLayoutTimer = 2;
    }

    /**
     * The viewport centre relative to the scene
     */
    public Point2D getViewportCentre(){

        return controller.viewportScrollPane.localToScene(
                controller.viewportScrollPane.getWidth()/2,
                controller.viewportScrollPane.getHeight()/2);
    }

    public void onMouseMovedViewport(MouseEvent event){
        controller.onMouseMovedColourPicker(event);
        Point2D mouse = new Point2D(event.getSceneX(), event.getSceneY());
        Point2D position = displayMode.get().getRenderer().sceneToRenderer(mouse);

        if(drawingArea.useOriginalSizing.get()){
            controller.labelCurrentPosition.setText(((int)position.getX())  + ", " + ((int)position.getY()) + " px");
        }else{
            double printScale = 1;

            if(displayMode.get() != Register.INSTANCE.DISPLAY_MODE_IMAGE && getCurrentDrawing() != null){
                printScale = getCurrentDrawing().getCanvas().getPlottingScale();
            }
            if(displayMode.get() == Register.INSTANCE.DISPLAY_MODE_IMAGE && openImage.get() != null){
                printScale = openImage.get().getDestCanvas().getPlottingScale();
            }

            position = position.multiply(1F/printScale);

            controller.labelCurrentPosition.setText(((int)position.getX())  + ", " + ((int)position.getY()) + " mm");
        }

    }

    public void onMousePressedViewport(MouseEvent event){
        controller.onMousePressedColourPicker(event);
    }

    public void onKeyPressedViewport(KeyEvent event){
        controller.onKeyPressedColourPicker(event);
    }

    ///////////////////////////////////////////////////////////////////////////////////////////////////////

    //// SERVICES

    public void startTask(ExecutorService service, Task<?> task){
        service.submit(task);
        taskMonitor.logTask(task);
    }

    public final Thread.UncaughtExceptionHandler exceptionHandler = (thread, throwable) -> {
        DrawingBotV3.logger.log(Level.SEVERE, "Thread Exception: " + thread.getName(), throwable);
    };

    public ExecutorService initTaskService(){
        return Executors.newSingleThreadExecutor(r -> {
            Thread t = new Thread(r, "DrawingBotV3 - Task Thread");
            t.setDaemon(true);
            t.setUncaughtExceptionHandler(exceptionHandler);
            return t;
        });
    }

    public ExecutorService initBackgroundService(){
        return Executors.newSingleThreadExecutor(r -> {
            Thread t = new Thread(r, "DrawingBotV3 - Background Thread");
            t.setDaemon(true);
            t.setUncaughtExceptionHandler(exceptionHandler);
            return t ;
        });
    }

    public ExecutorService initImageLoadingService(){
        return Executors.newSingleThreadExecutor(r -> {
            Thread t = new Thread(r, "DrawingBotV3 - Image Loading Thread");
            t.setDaemon(true);
            t.setUncaughtExceptionHandler(exceptionHandler);
            return t;
        });
    }

    public ExecutorService initImageFilteringService(){
        return Executors.newSingleThreadExecutor(r -> {
            Thread t = new Thread(r, "DrawingBotV3 - Image Filtering Thread");
            t.setDaemon(true);
            t.setUncaughtExceptionHandler(exceptionHandler);
            return t;
        });
    }

    public ExecutorService initParallelPlottingService(){
        return Executors.newFixedThreadPool(5, r -> {
            Thread t = new Thread(r, "DrawingBotV3 - Parallel Plotting Service");
            t.setDaemon(true);
            t.setUncaughtExceptionHandler(exceptionHandler);
            return t;
        });
    }

    public ExecutorService initSerialConnectionService(){
        return Executors.newSingleThreadExecutor(r -> {
            Thread t = new Thread(r, "DrawingBotV3 - Serial Connection Writing Service");
            t.setDaemon(true);
            t.setUncaughtExceptionHandler(exceptionHandler);
            return t;
        });
    }
    ///////////////////////////////////////////////////////////////////////////////////////////////////////
}