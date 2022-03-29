package drawingbot.render.jfx;

import drawingbot.DrawingBotV3;
import drawingbot.api.ICanvas;
import drawingbot.files.ConfigFileHandler;
import drawingbot.image.ImageFilteringTask;
import drawingbot.plotting.PFMTask;
import drawingbot.render.IRenderer;
import drawingbot.render.modes.AbstractJFXDisplayMode;
import drawingbot.utils.flags.Flags;
import javafx.geometry.Point2D;
import javafx.geometry.Rectangle2D;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.effect.BlendMode;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.scene.shape.StrokeLineCap;
import javafx.scene.shape.StrokeLineJoin;
import org.jfree.fx.FXGraphics2D;

public class JavaFXRenderer implements IRenderer {

    public final Rectangle2D screenBounds;
    public AbstractJFXDisplayMode displayMode;

    ///

    public static int vertexRenderLimitNormal = 20000;
    public static int vertexRenderLimitBlendMode = 5000;
    public static int defaultMinTextureSize = 1024;
    public static int defaultMaxTextureSize = 4096;

    public double canvasScaling = 1F;

    ///

    public Pane pane;
    public Canvas canvas;
    public GraphicsContext graphicsFX;
    public FXGraphics2D graphicsAWT;

    ///

    public ImageFilteringTask filteringTask;

    public JavaFXRenderer(Rectangle2D screenBounds) {
        this.screenBounds = screenBounds;
    }


    public void init(){
        canvas = new Canvas(500, 500);
        graphicsFX = canvas.getGraphicsContext2D();
        graphicsAWT = new FXGraphics2D(canvas.getGraphicsContext2D());

        pane = new Pane();
        pane.getChildren().add(canvas);
        updateCanvasPosition();

        DrawingBotV3.INSTANCE.controller.viewportScrollPane.init(pane);

    }

    ///////////////////////////////////////////////////////////////////////////////////////////////////////

    //// RENDERING

    public void clear() {
        clearCanvas(getCurrentBackground());
    }

    @Override
    public void draw() {
        preRender();
        doRender();
        postRender();
    }
    ///

    @Override
    public void updateCanvasPosition(){

        pane.setMinWidth(getPaneScaledWidth());
        pane.setMinHeight(getPaneScaledHeight());

        pane.setMaxWidth(getPaneScaledWidth());
        pane.setMaxHeight(getPaneScaledHeight());

        pane.setPrefWidth(getPaneScaledWidth());
        pane.setPrefHeight(getPaneScaledHeight());


        double offsetX = pane.getWidth()/2 - canvas.getWidth()/2;
        double offsetY = pane.getHeight()/2 - canvas.getHeight()/2;
        canvas.relocate(offsetX, offsetY);
    }

    private void preRender(){
        //reset canvas scaling
        graphicsFX.setTransform(1, 0, 0, 1, 0, 0);

        displayMode.preRender(this);
        displayMode.getRenderFlags().applyMarkedChanges();

        //updateCanvasScaling();
        graphicsFX.setImageSmoothing(false);
        graphicsFX.setLineCap(StrokeLineCap.ROUND);
        graphicsFX.setLineJoin(StrokeLineJoin.ROUND);
        graphicsFX.setGlobalBlendMode(BlendMode.SRC_OVER);
        graphicsFX.save();
    }

    private void doRender() {
        displayMode.doRender(this);
        //TODO GRID RENDERER
    }

    private void postRender(){
        displayMode.postRender(this);
        graphicsFX.restore();

        //update the canvas position after it has been resized
        updateCanvasPosition();
    }

    public void clearCanvas(){
        clearCanvas(DrawingBotV3.INSTANCE.canvasColor.getValue());
    }

    public void clearCanvas(Color color){
        canvas.getGraphicsContext2D().clearRect(0, 0, canvas.getWidth(), canvas.getHeight()); //ensures the canva's buffer is always cleared, some blend modes will prevent fillRect from triggering this
        canvas.getGraphicsContext2D().setFill(color);
        canvas.getGraphicsContext2D().fillRect(0, 0, canvas.getWidth(), canvas.getHeight());
    }

    private double refWidth = -1, refHeight = -1;

    public void setupCanvasSize(ICanvas canvas){
        if(canvas != null){
            setupCanvasSize((int)canvas.getScaledWidth(), (int)canvas.getScaledHeight());
        }
    }

    public void setupCanvasSize(double width, double height){
        if(refWidth == width && refHeight == height){
            return;
        }
        refWidth = width;
        refHeight = height;
        if(width > getMaxTextureSize() || height > getMaxTextureSize()){
            double max = Math.max(width, height);
            canvasScaling = getMaxTextureSize() / max;
            width = Math.floor(width*canvasScaling);
            height = Math.floor(height*canvasScaling);
        }else if(width < getMinTextureSize() || height < getMinTextureSize()){
            double max = Math.max(width, height);
            double newScaling = getMinTextureSize() / max;
            double newWidth = Math.floor(width*newScaling);
            double newHeight = Math.floor(height*newScaling);
            if(newWidth > width && newHeight > height){ //sanity check, prevents scaling down images where one side is under and one is over the limit
                canvasScaling = newScaling;
                width = newWidth;
                height = newHeight;
            }else{
                canvasScaling = 1;
            }
        }else{
            canvasScaling = 1;
        }

        if(canvas.getWidth() == width && canvas.getHeight() == height){
            return;
        }
        canvas.widthProperty().setValue(width);
        canvas.heightProperty().setValue(height);

        updateCanvasScaling();

        DrawingBotV3.INSTANCE.resetView();
        clearCanvas();//wipe the canvas
        DrawingBotV3.INSTANCE.setRenderFlag(Flags.FORCE_REDRAW, true);
    }

    public void updateCanvasScaling(){
        double screen_scale_x = DrawingBotV3.INSTANCE.controller.viewportScrollPane.getWidth() / ((float) canvas.getWidth());
        double screen_scale_y = DrawingBotV3.INSTANCE.controller.viewportScrollPane.getHeight() / ((float) canvas.getHeight());
        double screen_scale = Math.min(screen_scale_x, screen_scale_y);
        if(canvas.getScaleX() != screen_scale){
            canvas.setScaleX(screen_scale);
            canvas.setScaleY(screen_scale);
        }
    }

    public int getMinTextureSize(){
        return defaultMinTextureSize;
    }

    public int getMaxTextureSize(){
        if(ConfigFileHandler.getApplicationSettings().maxTextureSize != -1){
            return ConfigFileHandler.getApplicationSettings().maxTextureSize;
        }
        return defaultMaxTextureSize;
    }

    public int getVertexRenderLimit(){
        return graphicsFX.getGlobalBlendMode() == BlendMode.SRC_OVER ? vertexRenderLimitNormal : vertexRenderLimitBlendMode;
    }

    //// IRENDERER \\\\

    @Override
    public Rectangle2D getScreenBounds() {
        return screenBounds;
    }

    @Override
    public Pane getPane() {
        return pane;
    }

    @Override
    public Point2D sceneToRenderer(Point2D point2D) {
        Point2D dst = canvas.sceneToLocal(point2D);
        dst = new Point2D(dst.getX()/canvasScaling, dst.getY()/ canvasScaling);
        return dst;
    }

    @Override
    public Point2D rendererToScene(Point2D point2D) {
        Point2D dst = new Point2D(point2D.getX()*canvasScaling, point2D.getY() * canvasScaling);
        dst = canvas.localToScene(dst);
        return dst;
    }

    @Override
    public void switchToRenderer() {
        DrawingBotV3.INSTANCE.controller.viewportScrollPane.init(DrawingBotV3.RENDERER.pane);
        DrawingBotV3.INSTANCE.controller.viewportScrollPane.requestLayout();
    }

    @Override
    public boolean isDefaultRenderer() {
        return true;
    }

    @Override
    public boolean isOpenGL() {
        return false;
    }
}
