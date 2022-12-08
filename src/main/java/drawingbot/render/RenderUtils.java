package drawingbot.render;

import drawingbot.api.IGeometryFilter;
import drawingbot.geom.shapes.IGeometry;
import drawingbot.javafx.observables.ObservableDrawingPen;
import drawingbot.plotting.AbstractGeometryIterator;
import drawingbot.plotting.PlottedDrawing;
import drawingbot.plotting.PlottedGroup;
import javafx.scene.canvas.GraphicsContext;

import java.awt.*;
import java.awt.geom.PathIterator;

public class RenderUtils {

    public interface IRenderFunction<R> {
        void renderGeometry(R renderer, IGeometry geometry, PlottedDrawing drawing, PlottedGroup group, ObservableDrawingPen pen);
    }

    public static void renderDrawingFX(GraphicsContext graphics, AbstractGeometryIterator geometryIterator, IGeometryFilter geometryFilter, int vertexLimit) {
        renderDrawing(graphics, geometryIterator, geometryFilter, vertexLimit, RenderUtils::renderGeometryFX);
    }

    public static void renderDrawingAWT(Graphics2D graphics, AbstractGeometryIterator geometryIterator, IGeometryFilter geometryFilter, int vertexLimit) {
        renderDrawing(graphics, geometryIterator, geometryFilter, vertexLimit, RenderUtils::renderGeometryAWT);
    }

    /**
     * Renders all of the geometries provided by the geometry iterator which pass the geometry filter, the iterator will stop when the vertex limit is reached
     * @param geometryIterator the geometry iterator
     * @param geometryFilter the geometry filter
     * @param vertexLimit a vertex limit of 0 will render every vertex
     * @param renderFunction the render function, to pass the geometry for JavaFX, AWT, OpenGL for rendering
     */
    public static <R> void renderDrawing(R renderer, AbstractGeometryIterator geometryIterator, IGeometryFilter geometryFilter, int vertexLimit, IRenderFunction<R> renderFunction) {
        geometryIterator.setGeometryFilter(geometryFilter);
        geometryIterator.setVertexLimit(vertexLimit);
        while(geometryIterator.hasNext()){
            IGeometry next = geometryIterator.next();
            if(geometryIterator.currentFilterResult){
                renderFunction.renderGeometry(renderer, next, geometryIterator.currentDrawing, geometryIterator.currentGroup, geometryIterator.currentPen);
            }
        }
    }


    ///////////////////////////////////////////////////////////////////////////////////////////////////////

    public static void preRenderGeometryFX(GraphicsContext graphics, IGeometry geometry, PlottedDrawing drawing, PlottedGroup group, ObservableDrawingPen pen){
        graphics.setLineWidth(drawing.getCanvas().getRenderedPenWidth(pen.getStrokeSize()));
        graphics.setStroke(pen.getFXColor(geometry.getSampledRGBA()));
        graphics.setFill(pen.getFXColor(geometry.getSampledRGBA()));
    }

    public static void renderGeometryFX(GraphicsContext graphics, IGeometry geometry, PlottedDrawing drawing, PlottedGroup group, ObservableDrawingPen pen){
        preRenderGeometryFX(graphics, geometry, drawing, group, pen);
        geometry.renderFX(graphics);
    }

    ///////////////////////////////////////////////////////////////////////////////////////////////////////

    public static void preRenderGeometryAWT(Graphics2D graphics, IGeometry geometry, PlottedDrawing drawing, PlottedGroup group, ObservableDrawingPen pen){
        graphics.setStroke(pen.getAWTStroke(drawing.getCanvas().getRenderedPenWidth(pen.getStrokeSize())));
        graphics.setColor(pen.getAWTColor(geometry.getSampledRGBA()));
    }

    public static void renderGeometryAWT(Graphics2D graphics, IGeometry geometry, PlottedDrawing drawing, PlottedGroup group, ObservableDrawingPen pen){
        preRenderGeometryAWT(graphics, geometry, drawing, group, pen);
        geometry.renderAWT(graphics);
    }

    ///////////////////////////////////////////////////////////////////////////////////////////////////////

    private static final double[] coords = new double[6];

    public static void renderAWTShapeToFX(GraphicsContext graphics, Shape s) {
        graphics.beginPath();
        PathIterator iterator = s.getPathIterator(null);
        while (!iterator.isDone()) {
            int segType = iterator.currentSegment(coords);
            switch (segType) {
                case PathIterator.SEG_MOVETO:
                    graphics.moveTo(coords[0], coords[1]);
                    break;
                case PathIterator.SEG_LINETO:
                    graphics.lineTo(coords[0], coords[1]);
                    break;
                case PathIterator.SEG_QUADTO:
                    graphics.quadraticCurveTo(coords[0], coords[1], coords[2], coords[3]);
                    break;
                case PathIterator.SEG_CUBICTO:
                    graphics.bezierCurveTo(coords[0], coords[1], coords[2], coords[3], coords[4], coords[5]);
                    break;
                case PathIterator.SEG_CLOSE:
                    graphics.closePath();
                    break;
                default:
                    throw new RuntimeException("Unrecognised segment type " + segType);
            }
            iterator.next();
        }
        graphics.stroke();
    }

}
