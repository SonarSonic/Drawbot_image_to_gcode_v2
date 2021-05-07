package drawingbot.plotting;

import drawingbot.DrawingBotV3;
import drawingbot.api.*;
import drawingbot.drawing.ObservableDrawingSet;
import drawingbot.geom.PathBuilder;
import drawingbot.geom.basic.IGeometry;
import drawingbot.image.*;
import drawingbot.image.filters.ObservableImageFilter;
import drawingbot.javafx.GenericSetting;
import drawingbot.pfm.PFMFactory;
import drawingbot.registry.MasterRegistry;
import drawingbot.utils.EnumTaskStage;
import drawingbot.utils.Utils;
import javafx.application.Platform;
import javafx.concurrent.Task;
import org.imgscalr.Scalr;

import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.awt.image.BufferedImageOp;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;

public class PlottingTask extends Task<PlottingTask> implements IPlottingTask {

    public PFMFactory<?> pfmFactory;
    public PlottedDrawing plottedDrawing;
    public File originalFile;

    // STATUS \\
    public EnumTaskStage stage = EnumTaskStage.QUEUED;
    public long startTime;
    public long finishTime = -1;
    public List<String> comments = new ArrayList<>();

    // IMAGES \\
    public BufferedImage img_original;              // The original image
    public BufferedImage img_reference;             // After pre_processing, croped, scaled, boarder, etc.  This is what we will try to draw.
    public BufferedImage img_plotting;              // Used during drawing for current brightness levels.  Gets damaged during drawing.

    // PIXEL DATA \\
    public IPixelData reference;
    public IPixelData plotting;

    // PATH FINDING \\
    public IPathFindingModule pfm;
    public boolean plottingFinished = false;
    public PathBuilder pathBuilder = new PathBuilder(this);

    // RENDERING \\\
    public PrintResolution resolution;

    // GCODE \\
    private float gcode_offset_x;
    private float gcode_offset_y;

    public boolean enableImageFiltering = true;
    public boolean isSubTask = false;
    public int defaultPen = 0;

    public PlottingTask(PFMFactory<?> pfmFactory, ObservableDrawingSet drawingPenSet, BufferedImage image, File originalFile){
        updateTitle("Plotting Image (" + pfmFactory.getName() + ")");
        this.pfmFactory = pfmFactory;
        this.plottedDrawing = new PlottedDrawing(drawingPenSet);
        this.img_original = image;
        this.originalFile = originalFile;
        this.resolution = new PrintResolution(image);
    }

    @Override
    protected void setException(Throwable t) {
        super.setException(t);
        DrawingBotV3.logger.log(Level.SEVERE, "Plotting Task Failed", t);
    }

    public boolean doTask(){
        switch (stage){
            case QUEUED:
                startTime = System.currentTimeMillis();
                finishStage();
                break;
            case PRE_PROCESSING:
                updateMessage("Pre-Processing - Copying Original Image");
                plottingFinished = false;

                DrawingBotV3.logger.fine("PFM - Pre-Processing - Started");

                DrawingBotV3.logger.fine("PFM - Create Instance");
                pfm = pfmFactory.instance();
                DrawingBotV3.logger.fine("PFM - Apply Settings");
                GenericSetting.applySettingsToInstance(MasterRegistry.INSTANCE.getObservablePFMSettingsList(pfmFactory), pfm);

                DrawingBotV3.logger.fine("Copying Original Image");
                img_plotting = ImageTools.deepCopy(img_original);

                DrawingBotV3.logger.fine("Updating Resolution");
                resolution.plottingResolution = pfm.getPlottingResolution();
                resolution.updateAll();

                if(enableImageFiltering){
                    DrawingBotV3.logger.fine("Applying Cropping");
                    updateMessage("Pre-Processing - Cropping");
                    img_plotting = FilteredBufferedImage.applyCropping(img_plotting, resolution);

                    DrawingBotV3.logger.fine("Applying Filters");
                    for(ObservableImageFilter filter : DrawingBotV3.INSTANCE.currentFilters){
                        if(filter.enable.get()){
                            BufferedImageOp instance = filter.filterFactory.instance();
                            filter.filterSettings.forEach(setting -> setting.applySetting(instance));

                            updateMessage("Pre-Processing - " + filter.name.getValue());
                            img_plotting = instance.filter(img_plotting, null);
                        }
                    }

                    updateMessage("Pre-Processing - Resize");
                    img_plotting = Scalr.resize(img_plotting, Scalr.Method.ULTRA_QUALITY, (int)(img_plotting.getWidth() * pfm.getPlottingResolution()), (int)(img_plotting.getHeight()* pfm.getPlottingResolution()));
                }
                img_plotting = pfm.preFilter(img_plotting);

                DrawingBotV3.logger.fine("Creating Pixel Data");
                reference = ImageTools.newPixelData(img_plotting.getWidth(), img_plotting.getHeight(), pfm.getColourMode());
                plotting = ImageTools.newPixelData(img_plotting.getWidth(), img_plotting.getHeight(), pfm.getColourMode());
                plotting.setTransparentARGB(pfm.getTransparentARGB());

                DrawingBotV3.logger.fine("Setting Pixel Data");
                ImageTools.copyToPixelData(img_plotting, reference);
                ImageTools.copyToPixelData(img_plotting, plotting);

                DrawingBotV3.logger.fine("PFM - Init");
                pfm.init(this);

                DrawingBotV3.logger.fine("PFM - Pre-Process");
                updateMessage("Pre-Processing - PFM");
                pfm.preProcess();
                finishStage();
                updateMessage("Processing"); //here to avoid excessive task updates
                break;
            case DO_PROCESS:
                if(plottingFinished || isFinished()){
                    finishStage();
                    break;
                }
                pfm.doProcess();
                break;
            case POST_PROCESSING:
                updateMessage("Post-Processing - PFM");
                pfm.postProcess();

                DrawingBotV3.logger.fine("Plotting Task - Distributing Pens - Started");
                updateMessage("Post-Processing - Distributing Pens");
                plottedDrawing.updatePenDistribution();
                DrawingBotV3.logger.fine("Plotting Task - Distributing Pens - Finished");

                updateMessage("Post-Processing - Converting Reference Images");
                img_reference = ImageTools.getBufferedImage(reference);
                img_plotting = ImageTools.getBufferedImage(plotting);

                finishStage();
                break;
            case FINISHING:
                finishTime = (System.currentTimeMillis() - startTime);
                updateMessage("Finished - Elapsed Time: " + finishTime/1000 + " s");
                updateProgress(1, 1);
                finishStage();
                break;
            case FINISHED:
                break;
        }
        return true;
    }

    public void comment(String comment){
        comments.add(comment);
        DrawingBotV3.logger.info("Task Comment: " + comment);
    }

    public long getElapsedTime(){
        if(finishTime != -1){
            return finishTime;
        }
        return System.currentTimeMillis() - startTime;
    }

    public void finishStage(){
        if(!isSubTask){
            DrawingBotV3.INSTANCE.onPlottingTaskStageFinished(this, stage);
        }
        stage = EnumTaskStage.values()[stage.ordinal()+1];
    }

    public boolean isTaskFinished(){
        return stage == EnumTaskStage.FINISHED;
    }

    public BufferedImage getOriginalImage() {
        return img_original;
    }

    public BufferedImage getReferenceImage() {
        return img_reference;
    }

    public BufferedImage getPlottingImage() {
        return img_plotting;
    }

    public PrintResolution getResolution(){
        return resolution;
    }

    public void stopElegantly(){
        if(stage.ordinal() < EnumTaskStage.DO_PROCESS.ordinal()){
            cancel();
        }else if(stage == EnumTaskStage.DO_PROCESS){
            finishProcess();
        }
    }

    public AffineTransform createPrintTransform(){
        AffineTransform transform = new AffineTransform();
        transform.scale(resolution.getPrintScale(), resolution.getPrintScale());
        return transform;
    }

    public AffineTransform createGCodeTransform(){
        AffineTransform transform = new AffineTransform();

        ///translate by the gcode offset
        transform.translate(getGCodeXOffset(), getGCodeYOffset());

        ///move into print scale
        transform.scale(resolution.getPrintScale(), resolution.getPrintScale());

        //g-code y numbers go the other way
        transform.translate(0, resolution.getScaledHeight());

        //move with pre-scaled offsets
        transform.translate(resolution.getScaledOffsetX(), -resolution.getScaledOffsetY());

        //flip y coordinates
        transform.scale(1, -1);
        return transform;
    }

    public float getGCodeXOffset(){
        return gcode_offset_x + DrawingBotV3.INSTANCE.gcodeOffsetX.get();
    }

    public float getGCodeYOffset(){
        return gcode_offset_y + DrawingBotV3.INSTANCE.gcodeOffsetY.get();
    }

    @Override
    public PlottingTask call() {
        Platform.runLater(() -> DrawingBotV3.INSTANCE.setActivePlottingTask(this));
        while(!isTaskFinished() && !isCancelled()){
            if(!doTask()){
                cancel();
            }
        }
        return this;
    }

    ///////////////////////////////////////////////////////////////////////////////////////////////////////
    //// IPlottingTask
    ///////////////////////////////////////////////////////////////////////////////////////////////////////

    @Override
    public boolean isFinished() {
        return isCancelled() || plottingFinished;
    }

    @Override
    public PathBuilder getPathBuilder() {
        return pathBuilder;
    }

    @Override
    public IGeometry getLastGeometry() {
        return plottedDrawing.geometries.isEmpty() ? null : plottedDrawing.geometries.get(plottedDrawing.geometries.size()-1);
    }

    @Override
    public void addGeometry(IGeometry geometry) {
        addGeometry(geometry, null, null);
    }

    @Override
    public void addGeometry(IGeometry geometry, Integer penIndex, Integer rgba) {
        if(geometry.getCustomRGBA() == null){
            geometry.setCustomRGBA(rgba);
        }
        if(geometry.getPenIndex() == null){
            geometry.setPenIndex(penIndex == null ? defaultPen : penIndex);
        }
        //transform geometry back to the images size
        if(resolution.plottingResolution != 1F){
            geometry.transform(resolution.plottingTransform);
        }
        plottedDrawing.addGeometry(geometry);
    }

    @Override
    public IPixelData getPixelData() {
        return plotting;
    }

    @Override
    public IPixelData getReferencePixelData() {
        return reference;
    }

    @Override
    public int getTotalPens() {
        return plottedDrawing.drawingPenSet.getPens().size();
    }

    @Override
    public ObservableDrawingSet getDrawingSet() {
        return plottedDrawing.drawingPenSet;
    }

    @Override
    public void updatePlottingProgress(double progress, double max) {
        double actual = Math.min(Utils.roundToPrecision(progress / max, 3), 1D);
        updateProgress(actual, 1D);
    }

    @Override
    public void finishProcess() {
        plottingFinished = true;
    }


    public void reset(){
        pfmFactory = null;
        plottedDrawing.reset();
        originalFile = null;

        startTime = 0;
        finishTime = -1;
        comments.clear();

        img_original = null;
        img_reference = null;
        img_plotting = null;

        reference = null;
        plotting = null;

        pfm = null;
        plottingFinished = false;

        resolution = null;

        gcode_offset_x = 0;
        gcode_offset_y = 0;
    }


    ///MAKE UPDATE METHODS ACCESSIBLE TO PFMS

    @Override
    public void updateProgress(long workDone, long max) {
        super.updateProgress(workDone, max);
    }

    @Override
    public void updateProgress(double workDone, double max) {
        super.updateProgress(workDone, max);
    }

    @Override
    public void updateMessage(String message) {
        super.updateMessage(message);
    }

    @Override
    public void updateTitle(String title) {
        super.updateTitle(title);
    }
}
