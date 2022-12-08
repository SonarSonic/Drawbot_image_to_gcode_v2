package drawingbot.files.json.projects;

import drawingbot.DrawingBotV3;
import drawingbot.api.Hooks;
import drawingbot.api.ICanvas;
import drawingbot.drawing.DrawingSets;
import drawingbot.geom.MaskingSettings;
import drawingbot.image.ImageFilterSettings;
import drawingbot.image.blend.EnumBlendMode;
import drawingbot.image.format.FilteredImageData;
import drawingbot.javafx.FXHelper;
import drawingbot.javafx.GenericSetting;
import drawingbot.javafx.util.UINodeState;
import drawingbot.javafx.observables.ObservableDrawingSet;
import drawingbot.javafx.observables.ObservableImageFilter;
import drawingbot.javafx.observables.ObservableVersion;
import drawingbot.javafx.preferences.DBPreferences;
import drawingbot.javafx.util.PropertyUtil;
import drawingbot.pfm.PFMFactory;
import drawingbot.pfm.PFMSettings;
import drawingbot.plotting.ITaskManager;
import drawingbot.plotting.PFMTask;
import drawingbot.plotting.PlottedDrawing;
import drawingbot.plotting.canvas.ImageCanvas;
import drawingbot.plotting.canvas.ObservableCanvas;
import drawingbot.plotting.canvas.SimpleCanvas;
import drawingbot.registry.MasterRegistry;
import drawingbot.registry.Register;
import drawingbot.render.IDisplayMode;
import drawingbot.utils.EnumTaskStage;
import drawingbot.utils.Metadata;
import drawingbot.utils.MetadataMap;
import drawingbot.utils.UnitsLength;
import drawingbot.utils.flags.Flags;
import javafx.application.Platform;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.geometry.BoundingBox;
import javafx.geometry.Bounds;
import javafx.scene.control.Tab;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;

public class ObservableProject implements ITaskManager {

    public static final String DEFAULT_NAME = "Untitled Project";

    public final SimpleStringProperty name = new SimpleStringProperty();
    public final SimpleObjectProperty<File> file = new SimpleObjectProperty<>(null);

    public final SimpleObjectProperty<Tab> tab = new SimpleObjectProperty<>();

    ////////////////////////////

    public final SimpleBooleanProperty loaded = new SimpleBooleanProperty();

    public boolean isLoaded() {
        return loaded.get();
    }

    public SimpleBooleanProperty loadedProperty() {
        return loaded;
    }

    public void setLoaded(boolean loaded) {
        this.loaded.set(loaded);
    }

    ////////////////////////////

    /**
     * Drawing Area
     */
    public final SimpleObjectProperty<ObservableCanvas> drawingArea = new SimpleObjectProperty<>(new ObservableCanvas());

    public ICanvas targetCanvas;

    public ObservableCanvas getDrawingArea(){
        return drawingArea.get();
    }

    public SimpleObjectProperty<ObservableCanvas> drawingAreaProperty() {
        return drawingArea;
    }

    public void setDrawingArea(ObservableCanvas drawingArea) {
        this.drawingArea.set(drawingArea);
    }

    /**
     * Image Settings
     */
    public final SimpleObjectProperty<ImageFilterSettings> imageSettings = new SimpleObjectProperty<>(new ImageFilterSettings());

    public ImageFilterSettings getImageSettings() {
        return imageSettings.get();
    }

    public SimpleObjectProperty<ImageFilterSettings> imageSettingsProperty() {
        return imageSettings;
    }

    public void setImageSettings(ImageFilterSettings imageSettings) {
        this.imageSettings.set(imageSettings);
    }

    /**
     * PFM Settings
     */
    public final SimpleObjectProperty<PFMSettings> pfmSettings = new SimpleObjectProperty<>(new PFMSettings());

    public PFMSettings getPFMSettings() {
        return pfmSettings.get();
    }

    public SimpleObjectProperty<PFMSettings> pfmSettingsProperty() {
        return pfmSettings;
    }

    public void setPfmSettings(PFMSettings pfmSettings) {
        this.pfmSettings.set(pfmSettings);
    }

    /**
     * Drawing Sets
     */
    public SimpleObjectProperty<DrawingSets> drawingSets = new SimpleObjectProperty<>(new DrawingSets());

    public DrawingSets getDrawingSets(){
        return drawingSets.get();
    }

    public SimpleObjectProperty<DrawingSets> drawingSetsProperty() {
        return drawingSets;
    }

    public void setDrawingSets(DrawingSets drawingSets) {
        this.drawingSets.set(drawingSets);
    }

    /**
     * Version Control
     */
    public final SimpleObjectProperty<ObservableList<ObservableVersion>> projectVersions = new SimpleObjectProperty<>(FXCollections.observableArrayList());

    public ObservableList<ObservableVersion> getProjectVersions() {
        return projectVersions.get();
    }

    public SimpleObjectProperty<ObservableList<ObservableVersion>> projectVersionsProperty() {
        return projectVersions;
    }

    public void setProjectVersions(ObservableList<ObservableVersion> projectVersions) {
        this.projectVersions.set(projectVersions);
    }

    public final SimpleObjectProperty<ObservableVersion> lastRun = new SimpleObjectProperty<>();

    /**
     * Masking Settings
     */
    public final SimpleObjectProperty<MaskingSettings> maskingSettings = new SimpleObjectProperty<>(new MaskingSettings());

    public MaskingSettings getMaskingSettings() {
        return maskingSettings.get();
    }

    public SimpleObjectProperty<MaskingSettings> maskingSettingsProperty() {
        return maskingSettings;
    }

    public void setMaskingSettings(MaskingSettings maskingSettings) {
        this.maskingSettings.set(maskingSettings);
    }

    // DISPLAY \\
    public final SimpleObjectProperty<IDisplayMode> displayMode = new SimpleObjectProperty<>();

    public IDisplayMode getDisplayMode() {
        return displayMode.get();
    }

    public SimpleObjectProperty<IDisplayMode> displayModeProperty() {
        return displayMode;
    }

    public void setDisplayMode(IDisplayMode displayMode) {
        this.displayMode.set(displayMode);
    }

    public final SimpleBooleanProperty dpiScaling = new SimpleBooleanProperty(false);
    public final SimpleObjectProperty<EnumBlendMode> blendMode = new SimpleObjectProperty<>(EnumBlendMode.NORMAL);
    public final SimpleObjectProperty<Bounds> canvasBoundsInScene = new SimpleObjectProperty<>(new BoundingBox(0, 0, 0, 0));

    public final SimpleBooleanProperty exportRange = new SimpleBooleanProperty(DBPreferences.INSTANCE.defaultRangeExport.get());
    public final SimpleBooleanProperty displayGrid = new SimpleBooleanProperty(false);

    //VPYPE SETTINGS
    //public final VpypeSettings vpypeSettings = new VpypeSettings();

    //GCODE SETTINGS
    //public final GCodeSettings gcodeSettings = new GCodeSettings();

    // TASKS \\
    public final ObjectProperty<FilteredImageData> openImage = new SimpleObjectProperty<>(null);
    public final ObjectProperty<PFMTask> activeTask = new SimpleObjectProperty<>(null);
    public final ObjectProperty<PFMTask> renderedTask = new SimpleObjectProperty<>(null);
    public final ObjectProperty<PlottedDrawing> currentDrawing = new SimpleObjectProperty<>(null);

    // ADDITIONAL DATA \\
    public MetadataMap metadata = new MetadataMap(new LinkedHashMap<>());

    public final DBTaskContext context = new DBTaskContext(this, this);

    public ObservableProject(){
        this(DEFAULT_NAME, null);
    }

    public ObservableProject(ObservableProject project){
        this(project.name.get(), project.file.get());
        copy(project);
    }

    public ObservableProject(ObservableVersion version){
        this(version.name.get(), new File(version.file.get()));
    }

    public ObservableProject(String name, File file){
        this.name.set(name);
        this.file.set(file);
        init();
    }

    public void copy(ObservableProject project){
        drawingArea.set(project.getDrawingArea().copy());
        imageSettings.set(project.getImageSettings().copy());
        pfmSettings.set(project.getPFMSettings().copy());
        drawingSets.set(project.getDrawingSets().copy());

        displayMode.set(project.getDisplayMode());
        dpiScaling.set(project.dpiScaling.get());
        blendMode.set(project.blendMode.get());
        canvasBoundsInScene.set(project.canvasBoundsInScene.get());
        exportRange.set(project.exportRange.get());
        displayGrid.set(project.displayGrid.get());

        project.projectVersions.get().forEach(version -> projectVersions.get().add(version.copy()));

        if(lastRun.get() != null){
            lastRun.set(project.lastRun.get().copy());
        }
        if(openImage.get() != null){
            DrawingBotV3.INSTANCE.openFile(context, openImage.get().getSourceFile(), false, false);
        }
        if(currentDrawing.get() != null) {
            currentDrawing.set(project.currentDrawing.get().copy());
        }
        Hooks.runHook(Hooks.COPY_OBSERVABLE_PROJECT, this, project);
    }

    public void init(){
        PropertyUtil.addPropertyListListener(drawingArea, (value, changed) -> {
            if(!isLoaded()){
                return;
            }
            if(changed.contains(value.canvasColor)){
                DrawingBotV3.INSTANCE.reRender();
            }else{
                DrawingBotV3.INSTANCE.onCanvasChanged();
            }
        });

        imageSettings.get().currentFilters.get().addListener((ListChangeListener<ObservableImageFilter>) c -> { if(isLoaded()) DrawingBotV3.INSTANCE.onImageFiltersChanged();});

        blendMode.addListener((observable, oldValue, newValue) -> reRender());
        dpiScaling.addListener((observable, oldValue, newValue) -> resetView());
        displayMode.set(Register.INSTANCE.DISPLAY_MODE_IMAGE);

        //generate the target canvas, which will always display the correct Plotting Resolution
        targetCanvas = new ImageCanvas(drawingArea.get(), new SimpleCanvas(0, 0){
            @Override
            public float getWidth() {
                return openImage.get() != null ? openImage.get().getSourceCanvas().getWidth() : 0;
            }

            @Override
            public float getHeight() {
                return openImage.get() != null ? openImage.get().getSourceCanvas().getHeight() : 0;
            }

            @Override
            public UnitsLength getUnits() {
                return openImage.get() != null ? openImage.get().getSourceCanvas().getUnits() : UnitsLength.PIXELS;
            }
        }, false){

            @Override
            public boolean flipAxis() {
                return openImage.get() != null && openImage.get().imageRotation.get().flipAxis;
            }
        };

        pfmSettings.get().factory.addListener((observable, oldValue, newValue) -> {
            pfmSettings.get().settings.set(MasterRegistry.INSTANCE.getObservablePFMSettingsList(newValue));
        });


        activeTask.addListener((observable, oldValue, newValue) -> setRenderFlag(Flags.ACTIVE_TASK_CHANGED, true));
        renderedTask.addListener((observable, oldValue, newValue) -> setRenderFlag(Flags.ACTIVE_TASK_CHANGED, true));
        currentDrawing.addListener((observable, oldValue, newValue) -> setRenderFlag(Flags.CURRENT_DRAWING_CHANGED, true));
        openImage.addListener((observable, oldValue, newValue) -> {
            onImageChanged();
            if(newValue != null && (this.name.get().equals(DEFAULT_NAME) || oldValue != null && oldValue.getSourceFile().getName().equals(this.name.get()))){
                this.name.set(newValue.getSourceFile().getName());
            }
        });

        drawingSets.get().drawingSetSlots.get().add(new ObservableDrawingSet(MasterRegistry.INSTANCE.getDefaultDrawingSet()));
        drawingSets.get().activeDrawingSet.set(drawingSets.get().drawingSetSlots.get().get(0));
        drawingSets.get().activeDrawingSet.get().name.set("Default");
        pfmSettings.get().factory.set(MasterRegistry.INSTANCE.getDefaultPFM());

        loaded.addListener((observable, oldValue, newValue) -> {
            if(oldValue && !newValue){
                unload();
            }
            if(!oldValue && newValue){
                load();
            }
        });

        Hooks.runHook(Hooks.INIT_OBSERVABLE_PROJECT, this);
    }

    ////////////////////////////

    private final List<UINodeState> nodeStates = new ArrayList<>();

    public void unload(){
        nodeStates.clear();
        FXHelper.saveUIStates(nodeStates);
    }

    public void load(){
        FXHelper.loadUIStates(nodeStates);
    }


    ////////////////////////////

    public <T> void setRenderFlag(Flags.BooleanFlag flag){
        if(!isLoaded()){
            return;
        }
        for(IDisplayMode displayMode : MasterRegistry.INSTANCE.displayModes){
            displayMode.getRenderFlags().setFlag(flag, true);
        }
    }

    public <T> void setRenderFlag(Flags.Flag<T> flag, T value){
        if(!isLoaded()){
            return;
        }
        for(IDisplayMode displayMode : MasterRegistry.INSTANCE.displayModes){
            displayMode.getRenderFlags().setFlag(flag, value);
        }
    }

    public void reRender(){
        setRenderFlag(Flags.FORCE_REDRAW);
    }

    public void onImageChanged(){
        setRenderFlag(Flags.OPEN_IMAGE_UPDATED, true);
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

    public void resetView(){
        if(isLoaded()){
            DrawingBotV3.INSTANCE.resetView();
        }
    }


    ///////////////////////////////////////////////////////////////////////////////////////////////////////

    public <T> void setMetadata(Metadata<T> metadata, T value){
        this.metadata.setMetadata(metadata, value);
    }

    public <T> T getMetadata(Metadata<T> metadata){
        return this.metadata.getMetadata(metadata);
    }

    ///////////////////////////////////////////////////////////////////////////////////////////////////////

    //// DRAWING MANAGER

    @Override
    public PFMTask getActiveTask(){
        return activeTask.get();
    }

    @Override
    public PlottedDrawing createNewPlottedDrawing() {
        return new PlottedDrawing(getDrawingArea(), getDrawingSets());
    }

    @Override
    public PFMTask initPFMTask(DBTaskContext context, ICanvas canvas, PFMFactory<?> pfmFactory, @Nullable List<GenericSetting<?, ?>> pfmSettings, ObservableDrawingSet drawingPenSet, @Nullable FilteredImageData imageData, boolean isSubTask) {
        return DrawingBotV3.INSTANCE.initPFMTask(context, canvas, pfmFactory, pfmSettings, drawingPenSet, imageData, isSubTask);
    }

    @Override
    public PFMTask initPFMTask(DBTaskContext context, PlottedDrawing drawing, PFMFactory<?> pfmFactory, @Nullable List<GenericSetting<?, ?>> settings, ObservableDrawingSet drawingPenSet, @Nullable FilteredImageData imageData, boolean isSubTask) {
        return DrawingBotV3.INSTANCE.initPFMTask(context, drawing, pfmFactory, settings, drawingPenSet, imageData, isSubTask);
    }

    @Override
    public void onPlottingTaskStageFinished(PFMTask task, EnumTaskStage stage) {
        DrawingBotV3.onPlottingTaskStageFinished(context, task, stage);
    }

    @Override
    public void setActiveTask(PFMTask task) {
        if(activeTask.get() == task){
            return;
        }
        if(activeTask.get() != null){
            final PFMTask toReset = activeTask.get();
            DrawingBotV3.INSTANCE.backgroundService.submit(toReset::reset); //help GC by removing references to Geometries, run after other queue tasks have finished
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
        Platform.runLater(this::onDrawingCleared);
    }
}
