package drawingbot.javafx.settings;

import com.google.gson.JsonElement;
import com.sun.javafx.binding.BidirectionalBinding;
import drawingbot.javafx.GenericSetting;
import drawingbot.utils.Utils;
import javafx.beans.property.*;
import javafx.util.StringConverter;
import javafx.util.converter.IntegerStringConverter;

import java.util.concurrent.ThreadLocalRandom;
import java.util.function.BiConsumer;

public class IntegerSetting<C> extends AbstractNumberSetting<C, Integer> {

    public static StringConverter<Integer> stringConverter = new IntegerStringConverter();

    protected IntegerSetting(IntegerSetting<C> toCopy) {
        super(toCopy, toCopy.getValue());
    }

    public IntegerSetting(Class<C> clazz, String category, String settingName, Integer defaultValue) {
        super(clazz, Integer.class, category, settingName, defaultValue);
    }

    public IntegerSetting(Class<C> pfmClass, String category, String settingName, int defaultValue, int minValue, int maxValue){
        super(pfmClass, Integer.class, category, settingName, defaultValue, minValue, maxValue);
    }

    @Override
    public Integer fromNumber(Number number) {
        return number.intValue();
    }

    @Override
    protected Integer defaultValidate(Integer value) {
        return !isRanged ? value : Utils.clamp(value, minValue, maxValue);
    }

    @Override
    protected Integer defaultRandomise(ThreadLocalRandom random) {
        return !isRanged ? random.nextInt() : random.nextInt(safeMinValue, safeMaxValue);
    }

    @Override
    protected StringConverter<Integer> defaultStringConverter() {
        return stringConverter;
    }

    @Override
    public GenericSetting<C, Integer> copy() {
        return new IntegerSetting<>(this);
    }

    //////////////////////////

    public Integer getValueFromJsonElement(JsonElement element){
        return element.getAsInt();
    }

    //////////////////////////

    private IntegerProperty property = null;

    public IntegerProperty asIntegerProperty(){
        if(property == null){
            property = new SimpleIntegerProperty(getValue());
            BidirectionalBinding.bindNumber(property, valueProperty());
        }
        return property;
    }

}