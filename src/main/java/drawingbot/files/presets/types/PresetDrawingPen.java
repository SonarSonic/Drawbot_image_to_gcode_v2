package drawingbot.files.presets.types;

import drawingbot.api.IDrawingPen;
import drawingbot.drawing.DrawingPen;
import drawingbot.files.presets.IJsonData;
import drawingbot.files.presets.PresetType;
import drawingbot.javafx.GenericPreset;
import drawingbot.registry.Register;

public class PresetDrawingPen extends DrawingPen implements IJsonData {

    public transient GenericPreset<PresetDrawingPen> preset;

    public PresetDrawingPen(){}

    public PresetDrawingPen(GenericPreset<PresetDrawingPen> preset) {
        this.preset = preset;
        this.preset.data = this;
    }

    public PresetDrawingPen(IDrawingPen source, GenericPreset<PresetDrawingPen> preset) {
        super(source);
        this.preset = preset;
        this.preset.data = this;
    }

    @Override
    public void update(String type, String name, int argb, int distributionWeight, float strokeSize, boolean active) {
        super.update(type, name, argb, distributionWeight, strokeSize, active);
        this.type = type;
    }

    @Override
    public void update(IDrawingPen pen) {
        super.update(pen);
        this.type = pen.getType();
    }

    @Override
    public PresetType getPresetType() {
        return Register.PRESET_TYPE_DRAWING_PENS;
    }
}
