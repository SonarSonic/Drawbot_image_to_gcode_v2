package drawingbot.javafx.controllers;

import drawingbot.DrawingBotV3;
import drawingbot.FXApplication;
import drawingbot.files.json.presets.PresetImageFilters;
import drawingbot.files.json.presets.PresetPFMSettings;
import drawingbot.files.json.presets.PresetPFMSettingsManager;
import drawingbot.files.json.projects.DBTaskContext;
import drawingbot.javafx.FXController;
import drawingbot.javafx.FXHelper;
import drawingbot.javafx.GenericPreset;
import drawingbot.javafx.GenericSetting;
import drawingbot.javafx.controls.*;
import drawingbot.javafx.settings.CategorySetting;
import drawingbot.pfm.PFMFactory;
import drawingbot.pfm.PFMSettings;
import drawingbot.registry.MasterRegistry;
import drawingbot.registry.Register;
import drawingbot.utils.DBConstants;
import javafx.beans.property.Property;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.*;

//TODO PRESETS MAY STILL APPLY TO THE MASTER REGISTRY LISTS
public class FXPFMControls extends AbstractFXController {

    public final SimpleObjectProperty<PFMSettings> pfmSettings = new SimpleObjectProperty<>();
    public final SimpleObjectProperty<GenericPreset<PresetPFMSettings>> selectedPFMPreset = new SimpleObjectProperty<>();

    ////////////////////////////////////////////////////////

    public ComboBox<PFMFactory<?>> comboBoxPFM = null;

    public ComboBox<GenericPreset<PresetPFMSettings>> comboBoxPFMPreset = null;
    public MenuButton menuButtonPFMPresets = null;

    public TableView<GenericSetting<?,?>> tableViewAdvancedPFMSettings = null;
    public TableColumn<GenericSetting<?, ?>, Boolean> tableColumnLock = null;
    public TableColumn<GenericSetting<?, ?>, String> tableColumnSetting = null;
    public TableColumn<GenericSetting<?, ?>, Object> tableColumnValue = null;
    public TableColumn<GenericSetting<?, ?>, Object> tableColumnControl = null;

    public TreeTableView<GenericSetting<?, ?>> treeTableViewPFMSettings = null;
    public TreeTableColumn<GenericSetting<?, ?>, Boolean> treeTableColumnLock = null;
    public TreeTableColumn<GenericSetting<?, ?>, String> treeTableColumnSetting = null;
    public TreeTableColumn<GenericSetting<?, ?>, Object> treeTableColumnValue = null;
    public TreeTableColumn<GenericSetting<?, ?>, Object> treeTableColumnControl = null;

    public Button buttonPFMSettingReset = null;
    public Button buttonPFMSettingRandom = null;
    public Button buttonPFMSettingHelp = null;

    @FXML
    public void initialize(){

        final ChangeListener<PFMFactory<?>> FACTORY_CHANGE_LISTENER = (observable, oldValue, newValue) -> {
            comboBoxPFMPreset.setItems(MasterRegistry.INSTANCE.getObservablePFMPresetList(newValue));
            comboBoxPFMPreset.setValue(Register.PRESET_LOADER_PFM.getDefaultPresetForSubType(newValue.getName()));
        };

        pfmSettings.addListener((observable, oldValue, newValue) -> {

            if(oldValue != null){
                comboBoxPFM.valueProperty().unbindBidirectional(oldValue.factory);
                //tableViewAdvancedPFMSettings.itemsProperty().unbind();
                treeTableViewPFMSettings.rootProperty().unbind();
                oldValue.factory.removeListener(FACTORY_CHANGE_LISTENER);
            }

            if(newValue != null){
                comboBoxPFM.valueProperty().bindBidirectional(newValue.factory);
                comboBoxPFMPreset.setItems(MasterRegistry.INSTANCE.getObservablePFMPresetList(newValue.factory.get()));

                //tableViewAdvancedPFMSettings.itemsProperty().bind(newValue.settings);
                treeTableViewPFMSettings.rootProperty().bind(newValue.treeRoot);
                newValue.factory.addListener(FACTORY_CHANGE_LISTENER);
            }
        });

        ///////////////////////////////////////////////////////////////////////////////////////////////////////

        ////PATH FINDING CONTROLS
        comboBoxPFM.setCellFactory(param -> new ComboCellNamedSetting<>());
        comboBoxPFM.setItems(MasterRegistry.INSTANCE.getObservablePFMLoaderList());
        comboBoxPFM.setValue(MasterRegistry.INSTANCE.getDefaultPFM());
        comboBoxPFM.valueProperty().addListener((observable, oldValue, newValue) -> changePathFinderModule(newValue));

        selectedPFMPreset.setValue(Register.PRESET_LOADER_PFM.getDefaultPreset());
        selectedPFMPreset.addListener((observable, oldValue, newValue) -> {
            if(newValue != null){
                pfmSettingsPresetManager.applyPreset(DrawingBotV3.context(), newValue);
            }
        });

        comboBoxPFMPreset.setItems(Register.PRESET_LOADER_PFM.presets);
        comboBoxPFMPreset.valueProperty().bindBidirectional(selectedPFMPreset);

        FXHelper.setupPresetMenuButton(menuButtonPFMPresets, Register.PRESET_LOADER_PFM, () -> pfmSettingsPresetManager, false, selectedPFMPreset);

        treeTableViewPFMSettings.setRowFactory(param -> {
            TreeTableRow<GenericSetting<?, ?>> row = new TreeTableRow<>();
            row.setContextMenu(new ContextMenuPFMSetting(row));
            row.treeItemProperty().addListener((observable, oldValue, newValue) -> {
                row.disableProperty().unbind();
                if(newValue != null){
                    row.disableProperty().bind(newValue.getValue().disabledProperty());
                }
            });
            return row;
        });
        treeTableViewPFMSettings.setShowRoot(false);

        treeTableColumnLock.setCellFactory(param ->
             new CheckBoxTreeTableCell<>(index -> treeTableColumnLock.getCellObservableValue(index))
        );
        treeTableColumnLock.setCellValueFactory(param -> {
            GenericSetting<?, ? > setting = param.getValue().getValue();
            return setting instanceof CategorySetting ? null : setting.randomiseExcludeProperty();
        });

        treeTableColumnSetting.setCellValueFactory(param -> param.getValue().getValue().displayNameProperty());

        treeTableColumnValue.setCellFactory(param -> {
            TextFieldTreeTableCell<GenericSetting<?, ?>, Object> cell = new TextFieldTreeTableCell<>();

            cell.setConverter(new StringConverterGenericSetting(() -> {
                if(cell.getTableRow() != null && cell.getTableRow().getTreeItem() != null){
                    GenericSetting<?, ? > setting = cell.getTableRow().getTreeItem().getValue();
                    return setting instanceof CategorySetting ? null : setting;
                }
                return null;
            }));

            return cell;
        });
        treeTableColumnValue.setCellValueFactory(param -> {
            GenericSetting<?, ? > setting = param.getValue().getValue();
            return setting instanceof CategorySetting ? null : (ObservableValue<Object>) setting.valueProperty();
        });

        treeTableColumnControl.setCellFactory(param -> new TreeTableCellSettingControl());
        treeTableColumnControl.setCellValueFactory(param -> {
            GenericSetting<?, ? > setting = param.getValue().getValue();
            return setting instanceof CategorySetting ? null : (ObservableValue<Object>) setting.valueProperty();
        });

        /*
        tableViewAdvancedPFMSettings.setRowFactory(param -> {
            TableRow<GenericSetting<?, ?>> row = new TableRow<>();
            row.setContextMenu(new ContextMenuPFMSetting(row));
            return row;
        });

        tableColumnLock.setCellFactory(param -> new CheckBoxTableCell<>(index -> tableColumnLock.getCellObservableValue(index)));
        tableColumnLock.setCellValueFactory(param -> param.getValue().randomiseExclude);

        tableColumnSetting.setCellValueFactory(param -> param.getValue().displayNameProperty());

        tableColumnValue.setCellFactory(param -> {
            TextFieldTableCell<GenericSetting<?, ?>, Object> cell = new TextFieldTableCell<>();
            cell.setConverter(new StringConverterGenericSetting(() -> cell.tableViewProperty().get().getItems().get(cell.getIndex())));
            return cell;
        });
        tableColumnValue.setCellValueFactory(param -> (ObservableValue<Object>)param.getValue().value);

        tableColumnControl.setCellFactory(param -> new TableCellSettingControl());
        tableColumnControl.setCellValueFactory(param -> (ObservableValue<Object>)param.getValue().value);

         */

        buttonPFMSettingReset.setOnAction(e -> {
            if(comboBoxPFMPreset.getValue() == null){
                GenericSetting.resetSettings(pfmSettings.get().settings.get());
            }else{
                pfmSettingsPresetManager.applyPreset(DrawingBotV3.context(), comboBoxPFMPreset.getValue());
            }

        });

        buttonPFMSettingRandom.setOnAction(e -> GenericSetting.randomiseSettings(pfmSettings.get().settings.get()));
        buttonPFMSettingHelp.setOnAction(e -> FXHelper.openURL(DBConstants.URL_READ_THE_DOCS_PFMS));
    }

    public void changePathFinderModule(PFMFactory<?> pfm){
        if(pfm.isPremiumFeature() && !FXApplication.isPremiumEnabled){
            comboBoxPFM.setValue(MasterRegistry.INSTANCE.getDefaultPFM());
            FXController.showPremiumFeatureDialog();
        }else{
            pfmSettings.get().factory.set(pfm);
            pfmSettings.get().nextDistributionType.set(pfm.getDistributionType());
        }
    }

    ////////////////////////////////////////////////////////

    private final PresetPFMSettingsManager pfmSettingsPresetManager = new PresetPFMSettingsManager(Register.PRESET_LOADER_PFM) {

        @Override
        public Property<PFMFactory<?>> pfmProperty(DBTaskContext context) {
            return pfmSettings.get().factory;
        }

        @Override
        public Property<ObservableList<GenericSetting<?, ?>>> settingProperty(DBTaskContext context) {
            return pfmSettings.get().settings;
        }
    };

    ////////////////////////////////////////////////////////

}
