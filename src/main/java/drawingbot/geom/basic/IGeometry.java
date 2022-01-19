package drawingbot.geom.basic;

import drawingbot.DrawingBotV3;
import drawingbot.api.IGeometryFilter;
import drawingbot.javafx.observables.ObservableDrawingPen;
import drawingbot.pfm.helpers.BresenhamHelper;
import javafx.scene.canvas.GraphicsContext;
import org.locationtech.jts.geom.Coordinate;

import java.awt.*;
import java.awt.geom.AffineTransform;

public interface IGeometry {

    IGeometryFilter DEFAULT_EXPORT_FILTER = (drawing, geometry, pen) -> pen.isEnabled() && (!DrawingBotV3.INSTANCE.exportRange.get() || geometry.getGeometryIndex() >= drawing.getDisplayedShapeMin() && geometry.getGeometryIndex() <= drawing.getDisplayedShapeMax());
    IGeometryFilter DEFAULT_VIEW_FILTER = (drawing, geometry, pen) -> pen.isEnabled() && (geometry.getGeometryIndex() >= drawing.getDisplayedShapeMin() && geometry.getGeometryIndex() <= drawing.getDisplayedShapeMax());
    IGeometryFilter SELECTED_PEN_FILTER = (drawing, geometry, pen) -> DEFAULT_EXPORT_FILTER.filter(drawing, geometry, pen) && (DrawingBotV3.INSTANCE.controller.getSelectedPen() == null || DrawingBotV3.INSTANCE.controller.getSelectedPen().penNumber.get() == pen.penNumber.get());

    /**
     * This is used by the OpenGL Renderer, therefore this must accurately represent the number of vertices required
     */
    default int getVertexCount(){
        return 1;
    }

    /**
     * The AWT version of this geometry, for speed when rendering with Graphics2D
     */
    Shape getAWTShape();

    Integer getGeometryIndex();

    /**
     * @return the pen index, may be null
     */
    Integer getPenIndex();

    /**
     * @return the sampled rgba value, may be null
     */
    Integer getCustomRGBA();

    /**
     * @return the group id, geometries with the same group id are considered to be from the same section of the drawing
     * therefore when optimising the drawing, geometries with matching group ids, will be optimised together and not mixed with other groups
     */
    int getGroupID();

    /**
     * @param index may be null
     */
    void setGeometryIndex(Integer index);

    /**
     * @param index may be null
     */
    void setPenIndex(Integer index);

    /**
     * @param rgba may be null
     */
    void setCustomRGBA(Integer rgba);

    /**
     * @param groupID sets the geometries group id
     */
    void setGroupID(int groupID);

    /**
     * Render the Geometry in JAVAFX
     */
    void renderFX(GraphicsContext graphics, ObservableDrawingPen pen);

    /**
     * Render the Geometry in AWT
     */
    default void renderAWT(Graphics2D graphics, ObservableDrawingPen pen){
        pen.preRenderAWT(graphics, this);
        graphics.draw(getAWTShape());
    }

    /**
     * Geometries should override this method to optimise the bresenham calculations used
     */
    default void renderBresenham(BresenhamHelper helper, BresenhamHelper.IPixelSetter setter){
        helper.plotShape(getAWTShape(), setter);
    }

    void transform(AffineTransform transform);

    /**
     * Used for geometry sorting only
     */
    Coordinate getOriginCoordinate();


}
