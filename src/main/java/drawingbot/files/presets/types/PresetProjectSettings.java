package drawingbot.files.presets.types;

import com.google.gson.JsonElement;
import drawingbot.drawing.ColourSplitterHandler;
import drawingbot.files.presets.AbstractJsonData;
import drawingbot.files.presets.PresetType;
import drawingbot.image.blend.EnumBlendMode;
import drawingbot.javafx.GenericPreset;
import drawingbot.registry.Register;
import drawingbot.utils.*;

import java.util.HashMap;

public class PresetProjectSettings extends AbstractJsonData {

    public String name;
    public String imagePath;
    public String timeStamp;
    public String thumbnailID;

    public GenericPreset<PresetDrawingArea> drawingArea;
    public GenericPreset<PresetImageFilters> imageFilters;
    public GenericPreset<PresetPFMSettings> pfmSettings;
    public GenericPreset<PresetDrawingSet> drawingSet;

    public EnumRotation imageRotation = EnumRotation.R0;
    public boolean imageFlipHorizontal = false;
    public boolean imageFlipVertical = false;

    public boolean optimiseForPrint;
    public float targetPenWidth;

    public ColourSplitterHandler colourSplitter;
    public EnumDistributionType distributionType;
    public EnumDistributionOrder distributionOrder;
    public EnumBlendMode blendMode;

    public float cyanMultiplier = 1F;
    public float magentaMultiplier = 1F;
    public float yellowMultiplier = 1F;
    public float keyMultiplier = 0.75F;

    public PresetProjectSettings(){
        super();
    }

    public PresetProjectSettings(HashMap<String, JsonElement> settingList){
        super(settingList);
    }

    @Override
    public PresetType getPresetType() {
        return Register.PRESET_TYPE_PROJECT;
    }
}
