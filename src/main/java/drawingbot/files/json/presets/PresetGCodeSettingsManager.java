package drawingbot.files.json.presets;

import drawingbot.files.exporters.GCodeBuilder;
import drawingbot.files.exporters.GCodeExporter;
import drawingbot.files.exporters.GCodeSettings;
import drawingbot.files.json.DefaultPresetManager;
import drawingbot.files.json.projects.DBTaskContext;
import drawingbot.javafx.GenericSetting;
import drawingbot.javafx.preferences.DBPreferences;
import drawingbot.utils.UnitsLength;
import javafx.collections.FXCollections;

public class PresetGCodeSettingsManager extends DefaultPresetManager<PresetGCodeSettings, GCodeSettings> {

    public PresetGCodeSettingsManager(PresetGCodeSettingsLoader presetLoader) {
        super(presetLoader);
    }

    @Override
    public void registerDataLoaders() {
        registerSetting(GenericSetting.createRangedFloatSetting(GCodeSettings.class, "gcodeOffsetX", 0F, 0F, Float.MAX_VALUE, i -> i.gcodeOffsetX));
        registerSetting(GenericSetting.createRangedFloatSetting(GCodeSettings.class, "gcodeOffsetY", 0F, 0F, Float.MAX_VALUE, i -> i.gcodeOffsetY));
        registerSetting(GenericSetting.createOptionSetting(GCodeSettings.class, UnitsLength.class, "gcodeUnits", FXCollections.observableArrayList(UnitsLength.values()), UnitsLength.MILLIMETRES, i -> i.gcodeUnits));
        registerSetting(GenericSetting.createRangedFloatSetting(GCodeSettings.class, "gcodeCurveFlatness", 0.1F, 0F, Float.MAX_VALUE, i -> i.gcodeCurveFlatness));
        registerSetting(GenericSetting.createBooleanSetting(GCodeSettings.class, "gcodeEnableFlattening", true, i -> i.gcodeEnableFlattening));
        registerSetting(GenericSetting.createBooleanSetting(GCodeSettings.class, "gcodeCenterZeroPoint", false, i -> i.gcodeCenterZeroPoint));
        registerSetting(GenericSetting.createOptionSetting(GCodeSettings.class, GCodeBuilder.CommentType.class, "gcodeCommentType", FXCollections.observableArrayList(GCodeBuilder.CommentType.values()), GCodeBuilder.CommentType.BRACKETS, i -> i.gcodeCommentType));
        registerSetting(GenericSetting.createStringSetting(GCodeSettings.class, "gcodeStartCode", GCodeExporter.defaultStartCode, i -> i.gcodeStartCode));
        registerSetting(GenericSetting.createStringSetting(GCodeSettings.class, "gcodeEndCode", GCodeExporter.defaultEndCode, i -> i.gcodeEndCode));
        registerSetting(GenericSetting.createStringSetting(GCodeSettings.class, "gcodePenDownCode", GCodeExporter.defaultPenDownCode, i -> i.gcodePenDownCode));
        registerSetting(GenericSetting.createStringSetting(GCodeSettings.class, "gcodePenUpCode", GCodeExporter.defaultPenUpCode, i -> i.gcodePenUpCode));
        registerSetting(GenericSetting.createStringSetting(GCodeSettings.class, "gcodeStartLayerCode", GCodeExporter.defaultStartLayerCode, i -> i.gcodeStartLayerCode));
        registerSetting(GenericSetting.createStringSetting(GCodeSettings.class, "gcodeEndLayerCode", GCodeExporter.defaultEndLayerCode, i -> i.gcodeEndLayerCode));
    }

    @Override
    public GCodeSettings getInstance(DBTaskContext context) {
        return DBPreferences.INSTANCE.gcodeSettings;
    }
}
