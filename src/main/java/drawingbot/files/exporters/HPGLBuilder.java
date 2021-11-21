package drawingbot.files.exporters;

import drawingbot.DrawingBotV3;
import drawingbot.plotting.PlottingTask;
import drawingbot.utils.DBConstants;
import drawingbot.utils.Limit;
import drawingbot.utils.Utils;

import java.awt.geom.PathIterator;
import java.awt.geom.Point2D;
import java.io.PrintWriter;

/// W.I.P
public class HPGLBuilder {

    public final PlottingTask task;
    private final PrintWriter output;

    public boolean isPenDown;

    ///tallies
    public float distanceMoved;
    public float distanceDown;
    public float distanceUp;
    public int pointsDrawn;
    public int penLifts;
    public int penDrops;

    public float lastX = 0, lastY = 0;
    public float lastMoveX = 0, lastMoveY = 0;
    public Limit dx = new Limit(), dy = new Limit();

    public HPGLBuilder(PlottingTask task, PrintWriter output) {
        this.task = task;
        this.output = output;
    }

    /**
     * Note comments can still be called before this, if needed
     */
    public void open() {
        command(DrawingBotV3.INSTANCE.gcodeStartCode.getValue());

        isPenDown = true; //forces the first pen up command
        movePenUp();
    }

    /**
     * Must be called to save the file
     */
    public void close() {
        movePenUp();
        linearMoveG1(0, 0);
        command(DrawingBotV3.INSTANCE.gcodeEndCode.getValue());

        output.flush();
        output.close();
    }

    public void movePenUp() {
        if (isPenDown) {
            output.println(DrawingBotV3.INSTANCE.gcodePenUpCode.getValue());
            isPenDown = false;
            penLifts++;
        }
    }

    public void movePenDown() {
        if (!isPenDown) {
            output.println(DrawingBotV3.INSTANCE.gcodePenDownCode.getValue());
            isPenDown = true;
            penDrops++;
        }
    }

    public void startLayer() {
        output.println(DrawingBotV3.INSTANCE.gcodeStartLayerCode.getValue());
    }


    public void endLayer() {
        output.println(DrawingBotV3.INSTANCE.gcodeEndLayerCode.getValue());
    }

    public void move(float[] coords, int type) {
        switch (type) {
            case PathIterator.SEG_MOVETO:
                movePenUp();
                linearMoveG1(coords[0], coords[1]);
                movePenDown();
                break;
            case PathIterator.SEG_LINETO:
                linearMoveG1(coords[0], coords[1]);
                break;
            case PathIterator.SEG_QUADTO:
                quadCurveG5(coords[0], coords[1], coords[2], coords[3]);
                break;
            case PathIterator.SEG_CUBICTO:
                bezierCurveG5(coords[0], coords[1], coords[2], coords[3], coords[4], coords[5]);
                break;
            case PathIterator.SEG_CLOSE:
                linearMoveG1(lastMoveX, lastMoveY);
                movePenUp();
                break;
        }
    }

    public void linearMoveG1(float xValue, float yValue) {
        output.println("G1 X" + Utils.gcodeFloat(xValue) + " Y" + Utils.gcodeFloat(yValue));
        logMove(xValue, yValue);
        lastMoveX = xValue;
        lastMoveY = yValue;
    }

    public void quadCurveG5(float controlPX, float controlPY, float endX, float endY){
        output.println("G5 P" + Utils.gcodeFloat(controlPX - lastX) + " Q" + Utils.gcodeFloat(controlPY - lastY) + " X" + Utils.gcodeFloat(endX) + " Y" + Utils.gcodeFloat(endY));
        logMove(controlPX, controlPY);
        logMove(endX, endY);
    }

    public void bezierCurveG5(float controlP1X, float controlP1Y, float controlP2X, float controlP2Y, float endX, float endY){
        output.println("G5 I" + Utils.gcodeFloat(controlP1X - lastX) + " J" + Utils.gcodeFloat(controlP1Y - lastY) + " P" + Utils.gcodeFloat(controlP2X - lastX) + " Q" + Utils.gcodeFloat(controlP2Y - lastY) + " X" + Utils.gcodeFloat(endX) + " Y" + Utils.gcodeFloat(endY));
        logMove(controlP1X, controlP1Y);
        logMove(controlP2X, controlP2Y);
        logMove(endX, endY);
    }

    public void logMove(float xValue, float yValue){
        dx.update_limit(xValue);
        dy.update_limit(yValue);

        double distance = Point2D.distance(lastX, lastY, xValue, yValue);

        distanceMoved += distance;
        distanceUp += !isPenDown ? distance : 0;
        distanceDown += isPenDown ? distance : 0;

        if (isPenDown) {
            pointsDrawn++;
        }

        lastX = xValue;
        lastY = yValue;
    }

    public void command(String command) {
        output.println(command);
    }

}
