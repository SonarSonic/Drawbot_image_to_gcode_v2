package drawingbot.files.json.presets;

import drawingbot.files.json.DefaultPresetManager;
import drawingbot.files.json.projects.DBTaskContext;
import drawingbot.javafx.GenericPreset;
import drawingbot.javafx.GenericSetting;
import drawingbot.plotting.canvas.ObservableCanvas;
import drawingbot.registry.Register;
import drawingbot.utils.EnumOrientation;
import drawingbot.utils.EnumCroppingMode;
import drawingbot.utils.EnumRescaleMode;
import drawingbot.utils.UnitsLength;
import javafx.collections.FXCollections;

public abstract class PresetDrawingAreaManager extends DefaultPresetManager<PresetDrawingArea, ObservableCanvas> {

    public PresetDrawingAreaManager(PresetDrawingAreaLoader presetLoader) {
        super(presetLoader);
        changesOnly = true;
    }

    @Override
    public void applyPreset(DBTaskContext context, GenericPreset<PresetDrawingArea> preset) {
        ObservableCanvas canvas = getInstance(context);
        if(canvas != null){
            EnumOrientation orientation = canvas.orientation.get();
            super.applyPreset(context, preset);
            if(canvas.orientation.get() != orientation){
                canvas.orientation.set(orientation);
            }
        }
    }

    @Override
    public void registerDataLoaders() {
        registerSetting(GenericSetting.createBooleanSetting(ObservableCanvas.class, Register.CATEGORY_UNIQUE, "useOriginalSizing", false, i -> i.useOriginalSizing).setDisplayName("Use Original Sizing"));
        registerSetting(GenericSetting.createOptionSetting(ObservableCanvas.class, UnitsLength.class, Register.CATEGORY_UNIQUE, "inputUnits", FXCollections.observableArrayList(UnitsLength.values()), UnitsLength.MILLIMETRES, i -> i.inputUnits).setDisplayName("Input Units"));
        registerSetting(GenericSetting.createOptionSetting(ObservableCanvas.class, EnumCroppingMode.class, Register.CATEGORY_UNIQUE, "scalingMode", FXCollections.observableArrayList(EnumCroppingMode.values()), EnumCroppingMode.CROP_TO_FIT, i -> i.croppingMode).setDisplayName("Cropping Mode"));
        registerSetting(GenericSetting.createOptionSetting(ObservableCanvas.class, EnumRescaleMode.class, Register.CATEGORY_UNIQUE, "rescaleMode", FXCollections.observableArrayList(EnumRescaleMode.values()), EnumRescaleMode.HIGH_QUALITY, i -> i.rescaleMode).setDisplayName("Rescale Mode"));
        registerSetting(GenericSetting.createFloatSetting(ObservableCanvas.class, Register.CATEGORY_UNIQUE, "drawingAreaWidth", 0F, i -> i.width).setValidator(Math::abs).setDisplayName("Width"));
        registerSetting(GenericSetting.createFloatSetting(ObservableCanvas.class, Register.CATEGORY_UNIQUE, "drawingAreaHeight", 0F, i -> i.height).setValidator(Math::abs).setDisplayName("Height"));
        registerSetting(GenericSetting.createBooleanSetting(ObservableCanvas.class, Register.CATEGORY_UNIQUE, "drawingAreaGang", true, i -> i.drawingAreaGangPadding).setDisplayName("Gang Padding"));
        registerSetting(GenericSetting.createFloatSetting(ObservableCanvas.class, Register.CATEGORY_UNIQUE, "drawingAreaPaddingLeft", 0F, i -> i.drawingAreaPaddingLeft).setValidator(Math::abs).setDisplayName("Padding Left"));
        registerSetting(GenericSetting.createFloatSetting(ObservableCanvas.class, Register.CATEGORY_UNIQUE, "drawingAreaPaddingRight", 0F, i -> i.drawingAreaPaddingRight).setValidator(Math::abs).setDisplayName("Padding Right"));
        registerSetting(GenericSetting.createFloatSetting(ObservableCanvas.class, Register.CATEGORY_UNIQUE, "drawingAreaPaddingTop", 0F, i -> i.drawingAreaPaddingTop).setValidator(Math::abs).setDisplayName("Padding Top"));
        registerSetting(GenericSetting.createFloatSetting(ObservableCanvas.class, Register.CATEGORY_UNIQUE, "drawingAreaPaddingBottom", 0F, i -> i.drawingAreaPaddingBottom).setValidator(Math::abs).setDisplayName("Padding Bottom"));
        registerSetting(GenericSetting.createFloatSetting(ObservableCanvas.class, Register.CATEGORY_UNIQUE, "targetPenWidth", 0.3F, i -> i.targetPenWidth).setValidator(Math::abs).setDisplayName("Target pen width"));
    }
}
