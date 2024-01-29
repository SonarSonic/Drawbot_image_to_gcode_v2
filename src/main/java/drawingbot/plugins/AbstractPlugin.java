package drawingbot.plugins;

import drawingbot.api.IPlugin;
import drawingbot.javafx.GenericSetting;
import drawingbot.javafx.util.PropertyUtil;
import drawingbot.registry.MasterRegistry;
import javafx.collections.ObservableList;
import org.jetbrains.annotations.MustBeInvokedByOverriders;

import java.util.ArrayList;
import java.util.List;

public abstract class AbstractPlugin implements IPlugin {

    public final List<GenericSetting<?, ?>> settings = new ArrayList<>();
    public final ObservableList<GenericSetting<?, ?>> observableList;

    public AbstractPlugin(){
        observableList = PropertyUtil.createPropertiesList(settings);
    }

    public <T extends GenericSetting<?, ?>> T register(T add){
        settings.add(add);
        add.createDefaultGetterAndSetter();
        return add;
    }

    @Override
    @MustBeInvokedByOverriders
    public void preInit() {
        settings.forEach(setting -> MasterRegistry.INSTANCE.registerApplicationSetting(setting));
    }
}
