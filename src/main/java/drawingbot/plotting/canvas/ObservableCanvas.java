package drawingbot.plotting.canvas;

import drawingbot.api.ICanvas;
import drawingbot.api.IProperties;
import drawingbot.javafx.preferences.DBPreferences;
import drawingbot.utils.*;
import drawingbot.javafx.util.PropertyUtil;
import javafx.beans.Observable;
import javafx.beans.property.*;
import javafx.collections.ObservableList;
import javafx.scene.paint.Color;

import java.util.concurrent.atomic.AtomicBoolean;

public class ObservableCanvas implements ICanvas, IProperties {

    private static final float defaultWidth = 210, defaultHeight = 297; //DEFAULT - A4 Paper

    public final SimpleBooleanProperty useOriginalSizing = new SimpleBooleanProperty(true);
    public final SimpleObjectProperty<EnumCroppingMode> croppingMode = new SimpleObjectProperty<>(EnumCroppingMode.CROP_TO_FIT);
    public final SimpleObjectProperty<EnumClippingMode> clippingMode = new SimpleObjectProperty<>(DBPreferences.INSTANCE.defaultClippingMode.get());
    public final SimpleObjectProperty<UnitsLength> inputUnits = new SimpleObjectProperty<>(UnitsLength.MILLIMETRES);

    public final SimpleFloatProperty width = new SimpleFloatProperty(0);
    public final SimpleFloatProperty height = new SimpleFloatProperty(0);
    public final SimpleFloatProperty drawingAreaPaddingLeft = new SimpleFloatProperty(0);
    public final SimpleFloatProperty drawingAreaPaddingRight = new SimpleFloatProperty(0);
    public final SimpleFloatProperty drawingAreaPaddingTop = new SimpleFloatProperty(0);
    public final SimpleFloatProperty drawingAreaPaddingBottom = new SimpleFloatProperty(0);
    public final SimpleFloatProperty drawingAreaPaddingGangedValue = new SimpleFloatProperty(0);
    public final SimpleBooleanProperty drawingAreaGangPadding = new SimpleBooleanProperty(true);
    public final SimpleObjectProperty<EnumOrientation> orientation = new SimpleObjectProperty<>(EnumOrientation.PORTRAIT);

    public final SimpleFloatProperty targetPenWidth = new SimpleFloatProperty(DBPreferences.INSTANCE.defaultPenWidth.get());

    public final SimpleObjectProperty<EnumRescaleMode> rescaleMode = new SimpleObjectProperty<>(DBPreferences.INSTANCE.defaultRescalingMode.get());

    //the default JFX viewport background colours
    public static final Color backgroundColourDefault = new Color(244 / 255F, 244 / 255F, 244 / 255F, 1F);
    public static final Color backgroundColourDark = new Color(65 / 255F, 65 / 255F, 65 / 255F, 1F);

    //not saved
    public final SimpleObjectProperty<Color> canvasColor = new SimpleObjectProperty<>(DBPreferences.INSTANCE.defaultCanvasColour.get());
    public final SimpleObjectProperty<Color> backgroundColor = new SimpleObjectProperty<>(DBPreferences.INSTANCE.defaultBackgroundColour.get());

    public final ObservableList<Observable> observables = PropertyUtil.createPropertiesList(useOriginalSizing, croppingMode, clippingMode, inputUnits, width, height, drawingAreaPaddingLeft, drawingAreaPaddingRight, drawingAreaPaddingTop, drawingAreaPaddingBottom, drawingAreaGangPadding, rescaleMode, targetPenWidth, canvasColor);

    public ObservableCanvas(){

        AtomicBoolean internalChange = new AtomicBoolean(false);

        width.addListener((observable, oldValue, newValue) -> {
            if(internalChange.get()){
                return;
            }
            if(width.get() == height.get()){
                return;
            }
            internalChange.set(true);
            orientation.set(EnumOrientation.getType(width.get(), height.get()));
            internalChange.set(false);
        });

        height.addListener((observable, oldValue, newValue) -> {
            if(internalChange.get()){
                return;
            }
            if(width.get() == height.get()){
                return;
            }
            internalChange.set(true);
            orientation.set(EnumOrientation.getType(width.get(), height.get()));
            internalChange.set(false);
        });

        orientation.addListener(((observable, oldValue, newValue) -> {
            if(internalChange.get()){
                return;
            }
            if(newValue != null){
                internalChange.set(true);
                float newWidth = getHeight();
                float newHeight = getWidth();
                width.set(newWidth);
                height.set(newHeight);
                internalChange.set(false);
            }

        }));

        inputUnits.addListener((observable, oldValue, newValue) -> {
            internalChange.set(true);
            width.set(UnitsLength.convert(width.get(), oldValue, newValue));
            height.set(UnitsLength.convert(height.get(), oldValue, newValue));
            if(drawingAreaGangPadding.get()){
                drawingAreaPaddingGangedValue.set(UnitsLength.convert(drawingAreaPaddingGangedValue.get(), oldValue, newValue));
            }else{
                drawingAreaPaddingLeft.set(UnitsLength.convert(drawingAreaPaddingLeft.get(), oldValue, newValue));
                drawingAreaPaddingRight.set(UnitsLength.convert(drawingAreaPaddingRight.get(), oldValue, newValue));
                drawingAreaPaddingTop.set(UnitsLength.convert(drawingAreaPaddingTop.get(), oldValue, newValue));
                drawingAreaPaddingBottom.set(UnitsLength.convert(drawingAreaPaddingBottom.get(), oldValue, newValue));
            }
            internalChange.set(false);
        });

        updateGangedPadding();
        drawingAreaGangPadding.addListener((observable, oldValue, newValue) -> {
            updateGangedPadding();
        });

        //keep the ganged value updated so it always matches the last entered value
        drawingAreaPaddingLeft.addListener((observable, oldValue, newValue) -> {
            if(!internalChange.get() && !drawingAreaGangPadding.get()){
                drawingAreaPaddingGangedValue.set(newValue.floatValue());
            }
        });
        drawingAreaPaddingRight.addListener((observable, oldValue, newValue) -> {
            if(!internalChange.get() && !drawingAreaGangPadding.get()){
                drawingAreaPaddingGangedValue.set(newValue.floatValue());
            }
        });
        drawingAreaPaddingTop.addListener((observable, oldValue, newValue) -> {
            if(!internalChange.get() && !drawingAreaGangPadding.get()){
                drawingAreaPaddingGangedValue.set(newValue.floatValue());
            }
        });
        drawingAreaPaddingBottom.addListener((observable, oldValue, newValue) -> {
            if(!internalChange.get() && !drawingAreaGangPadding.get()){
                drawingAreaPaddingGangedValue.set(newValue.floatValue());
            }
        });
    }

    public void updateGangedPadding(){
        if(drawingAreaGangPadding.get()){
            drawingAreaPaddingLeft.bindBidirectional(drawingAreaPaddingGangedValue);
            drawingAreaPaddingRight.bindBidirectional(drawingAreaPaddingGangedValue);
            drawingAreaPaddingTop.bindBidirectional(drawingAreaPaddingGangedValue);
            drawingAreaPaddingBottom.bindBidirectional(drawingAreaPaddingGangedValue);
        }else{
            drawingAreaPaddingLeft.unbindBidirectional(drawingAreaPaddingGangedValue);
            drawingAreaPaddingRight.unbindBidirectional(drawingAreaPaddingGangedValue);
            drawingAreaPaddingTop.unbindBidirectional(drawingAreaPaddingGangedValue);
            drawingAreaPaddingBottom.unbindBidirectional(drawingAreaPaddingGangedValue);
        }
    }


    @Override
    public UnitsLength getUnits() {
        return inputUnits.get();
    }

    @Override
    public EnumCroppingMode getCroppingMode() {
        return croppingMode.get();
    }

    @Override
    public EnumClippingMode getClippingMode() {
        return clippingMode.get();
    }

    @Override
    public EnumRescaleMode getRescaleMode() {
        return rescaleMode.get();
    }

    @Override
    public float getPlottingScale(){
        return getRescaleMode().shouldRescale() ? 1F / targetPenWidth.get() : 1F;
    }

    @Override
    public float getTargetPenWidth() {
        return targetPenWidth.get();
    }

    @Override
    public float getWidth(){
        if(width.getValue() > 0){
            return width.getValue();
        }
        return defaultWidth;
    }

    @Override
    public float getHeight(){
        if(height.getValue() > 0){
            return height.getValue();
        }
        return defaultHeight;
    }

    @Override
    public float getDrawingWidth(){
        return getWidth() - drawingAreaPaddingLeft.get() - drawingAreaPaddingRight.get();
    }

    @Override
    public float getDrawingHeight(){
        return getHeight() - drawingAreaPaddingTop.get() - drawingAreaPaddingBottom.get();
    }

    @Override
    public float getDrawingOffsetX(){
        return drawingAreaPaddingLeft.get();
    }

    @Override
    public float getDrawingOffsetY(){
        return drawingAreaPaddingTop.get();
    }

    @Override
    public boolean useOriginalSizing(){
        return useOriginalSizing.get();
    }

    public ObservableCanvas copy(){
        ObservableCanvas copy = new ObservableCanvas();
        copy.useOriginalSizing.set(useOriginalSizing.get());
        copy.croppingMode.set(croppingMode.get());
        copy.clippingMode.set(clippingMode.get());
        copy.inputUnits.set(inputUnits.get());

        copy.width.set(width.get());
        copy.height.set(height.get());
        copy.drawingAreaPaddingLeft.set(drawingAreaPaddingLeft.get());
        copy.drawingAreaPaddingRight.set(drawingAreaPaddingRight.get());
        copy.drawingAreaPaddingTop.set(drawingAreaPaddingTop.get());
        copy.drawingAreaPaddingBottom.set(drawingAreaPaddingBottom.get());
        copy.drawingAreaGangPadding.set(drawingAreaGangPadding.get());

        copy.rescaleMode.set(rescaleMode.get());
        copy.targetPenWidth.set(targetPenWidth.get());
        copy.canvasColor.set(canvasColor.get());
        return copy;
    }

    @Override
    public ObservableList<Observable> getObservables() {
        return observables;
    }
}
