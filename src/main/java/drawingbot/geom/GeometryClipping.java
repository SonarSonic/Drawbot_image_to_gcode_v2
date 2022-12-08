package drawingbot.geom;

import drawingbot.geom.shapes.*;
import drawingbot.utils.Utils;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

public class GeometryClipping {

    //TODO someone decide what accuracy is needed or make this configurable
    public static float curveSplits = 128;
    public static float accuracy = 0.5F;

    public static boolean shouldClip(Shape shape, IGeometry geometry) {
        //inaccurate contains check using bounds instead of accurate shapes for speed
        return !shape.contains(geometry.getAWTShape().getBounds2D());
    }

    public static List<IGeometry> clip(Shape shape, IGeometry geometry) {
        List<IGeometry> pathElements = new ArrayList<>();
        GeometryUtils.splitGPath(geometry instanceof GPath ? (GPath) geometry : new GPath(geometry.getAWTShape()), pathElements::add);

        List<IGeometry> geometries = new ArrayList<>();
        for (IGeometry element : pathElements) {
            if (element instanceof GLine) {
                recursiveSplitLine(shape, (GLine) element, geometries::add);
            } else if (element instanceof GQuadCurve) {
                recursiveSplitGQuadCurve(shape, (GQuadCurve) element, geometries::add);
            } else if (element instanceof GCubicCurve) {
                recursiveSplitCubicCurve(shape, (GCubicCurve) element, geometries::add);
            } else {
                throw new UnsupportedOperationException("Invalid element type: " + element.getClass().getSimpleName());
            }
        }
        geometries.forEach(g -> GeometryUtils.copyGeometryData(g, geometry));

        //assert geometries.size() != 1 || geometries.get(0).getAWTShape().equals(geometry.getAWTShape());

        return geometries;
    }

    public static void recursiveSplitLine(Shape shape, GLine line, Consumer<IGeometry> consumer) {
        float[] start = new float[]{line.x1, line.y1};
        float[] end = new float[]{line.x2, line.y2};
        double length = Utils.distance(line.x1, line.y1, line.x2, line.y2);
        double position = 0;
        boolean isInside = shape.contains(start[0], start[1]);

        while (position < length) {
            float[] point = pointAlongLineSegment(start, end, position / length);
            if (isInside != shape.contains(point[0], point[1])) {
                if (isInside) consumer.accept(new GLine(start[0], start[1], point[0], point[1]));;
                recursiveSplitLine(shape, new GLine(point[0], point[1], end[0], end[1]), consumer);
                return;
            }
            position += accuracy;
        }
        if (isInside) {
            consumer.accept(line);
        }
    }

    public static void recursiveSplitGQuadCurve(Shape shape, GQuadCurve curve, Consumer<IGeometry> consumer) {
        //splitting a quad curve will results in two cubic curves
        recursiveSplitCubicCurve(shape, new GCubicCurve(curve), consumer);
    }

    /**
     * Note: this method is inaccurate but fast, for a more accurate method, evenly spaced points would need to be used
     */
    public static void recursiveSplitCubicCurve(Shape shape, GCubicCurve curve, Consumer<IGeometry> consumer) {
        float[] start = new float[]{curve.x1, curve.y1};
        float[] control1 = new float[]{curve.ctrlx1, curve.ctrly1};
        float[] control2 = new float[]{curve.ctrlx2, curve.ctrly2};
        float[] end = new float[]{curve.x2, curve.y2};
        double samples = Math.max(curveSplits, Utils.distance(curve.x1, curve.y1, curve.ctrlx1, curve.ctrly1) +
                                       Utils.distance(curve.ctrlx1, curve.ctrly1, curve.ctrlx2, curve.ctrly2) +
                                       Utils.distance(curve.ctrlx2, curve.ctrly2, curve.x2, curve.y2));
        double sample = 0;
        boolean isInside = shape.contains(start[0], start[1]);

        while (sample < samples) {
            float[] point = pointAlongCubicCurve(start, control1, control2, end, (float) (sample / samples));
            if (isInside != shape.contains(point[0], point[1])) {
                float[] outputLeft = new float[8];
                float[] outputRight = new float[8];
                splitCubicCurve(curve.toFloatArray(), outputLeft, outputRight, (float) (sample / samples));
                GCubicCurve curveLeft = new GCubicCurve(outputLeft);
                GCubicCurve curveRight = new GCubicCurve(outputRight);

                //avoids rare rounding errors when the point exists exactly on the boundary of a shape, which could cause infinite loops
                curveLeft.x2 = point[0];
                curveLeft.y2 = point[1];
                curveRight.x1 = point[0];
                curveRight.y1 = point[1];

                if (isInside) consumer.accept(curveLeft);
                recursiveSplitCubicCurve(shape, curveRight, consumer);
                return;
            }
            sample += 1;
        }
        if (isInside) {
            consumer.accept(curve);
        }
    }

    public static void splitCubicCurve(float[] inputCurve, float[] outputLeft, float[] outputRight, float t) {
        float startX = inputCurve[0];
        float startY = inputCurve[1];
        float ctrlX1 = inputCurve[2];
        float ctrlY1 = inputCurve[3];
        float ctrlX2 = inputCurve[4];
        float ctrlY2 = inputCurve[5];
        float endX = inputCurve[6];
        float endY = inputCurve[7];

        float midPointX = (ctrlX2 - ctrlX1) * t + ctrlX1;
        float midPointY = (ctrlY2 - ctrlY1) * t + ctrlY1;

        //1st control point of the 1st curve
        float ctrl1X = (ctrlX1 - startX) * t + startX;
        float ctrl1Y = (ctrlY1 - startY) * t + startY;

        //2nd control point of the 2nd curve
        float ctrl4X = (endX - ctrlX2) * t + ctrlX2;
        float ctrl4Y = (endY - ctrlY2) * t + ctrlY2;

        //2nd control point of the 1st curve
        float ctrl2X = (midPointX - ctrl1X) * t + ctrl1X;
        float ctrl2Y = (midPointY - ctrl1Y) * t + ctrl1Y;

        //1st control point of the 2nd curve
        float ctrl3X = (ctrl4X - midPointX) * t + midPointX;
        float ctrl3Y = (ctrl4Y - midPointY) * t + midPointY;

        //end point of the first curve / start point of the second curve
        float x2 = (ctrl3X - ctrl2X) * t + ctrl2X;
        float y2 = (ctrl3Y - ctrl2Y) * t + ctrl2Y;

        outputLeft[0] = startX;
        outputLeft[1] = startY;
        outputLeft[2] = ctrl1X;
        outputLeft[3] = ctrl1Y;
        outputLeft[4] = ctrl2X;
        outputLeft[5] = ctrl2Y;
        outputLeft[6] = x2;
        outputLeft[7] = y2;

        outputRight[0] = x2;
        outputRight[1] = y2;
        outputRight[2] = ctrl3X;
        outputRight[3] = ctrl3Y;
        outputRight[4] = ctrl4X;
        outputRight[5] = ctrl4Y;
        outputRight[6] = endX;
        outputRight[7] = endY;
    }


    public static float[] pointAlongLineSegment(float[] start, float[] end, double index){
        if (index <= 0.0){
            return start;
        }
        if (index >= 1.0){
            return end;
        }
        float x = (float) ((end[0] - start[0]) * index + start[0]);
        float y = (float) ((end[1] - start[1]) * index + start[1]);
        return new float[]{x, y};
    }

    public static float[] pointAlongQuadraticCurve(float[] start, float[] control, float[] end, double index){
        if (index <= 0.0){
            return start;
        }
        if (index >= 1.0){
            return end;
        }
        float x = (float) ((1F - index) * (1F - index) * start[0] + 2F * (1F - index) * index * control[0] + index * index * end[0]);
        float y = (float) ((1F - index) * (1 - index) * start[1] + 2 * (1 - index) * index * control[1] + index * index * end[1]);
        return new float[]{x, y};
    }

    public static float[] pointAlongCubicCurve(float[] start, float[] control1, float[] control2, float[] end, double index){
        if (index <= 0.0){
            return start;
        }
        if (index >= 1.0){
            return end;
        }
        float x = (float) (((1 - index) * (1 - index) * (1 - index)) * start[0] + 3 * ((1 - index) * (1 - index)) * index * control1[0] + 3 * (1 - index) * (index * index) * control2[0] + (index * index * index) * end[0]);
        float y = (float) (((1 - index) * (1 - index) * (1 - index)) * start[1] + 3 * ((1 - index) * (1 - index)) * index * control1[1] + 3 * (1 - index) * (index * index) * control2[1] + (index * index * index) * end[1]);
        return new float[]{x, y};
    }

}