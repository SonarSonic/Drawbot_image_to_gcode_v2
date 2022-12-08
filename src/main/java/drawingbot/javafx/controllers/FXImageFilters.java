package drawingbot.javafx.controllers;

import drawingbot.DrawingBotV3;
import drawingbot.files.json.AbstractPresetManager;
import drawingbot.files.json.presets.PresetImageFilters;
import drawingbot.files.json.presets.PresetImageFiltersManager;
import drawingbot.files.json.projects.DBTaskContext;
import drawingbot.files.json.projects.ObservableProject;
import drawingbot.image.ImageFilterSettings;
import drawingbot.image.format.FilteredImageData;
import drawingbot.javafx.FXHelper;
import drawingbot.javafx.GenericFactory;
import drawingbot.javafx.GenericPreset;
import drawingbot.javafx.controls.ContextMenuObservableFilter;
import drawingbot.javafx.observables.ObservableImageFilter;
import drawingbot.registry.MasterRegistry;
import drawingbot.registry.Register;
import drawingbot.utils.EnumFilterTypes;
import drawingbot.utils.EnumRotation;
import drawingbot.utils.Utils;
import javafx.beans.property.Property;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.css.Styleable;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.CheckBoxTableCell;
import javafx.scene.control.cell.TextFieldTableCell;
import javafx.scene.input.ContextMenuEvent;
import javafx.util.converter.DefaultStringConverter;
import javafx.util.converter.FloatStringConverter;
import javafx.util.converter.NumberStringConverter;
import org.fxmisc.easybind.EasyBind;

import java.awt.image.BufferedImageOp;
import java.util.List;

public class FXImageFilters extends AbstractFXController {

    public final SimpleObjectProperty<ImageFilterSettings> settings = new SimpleObjectProperty<>();
    public final SimpleObjectProperty<FilteredImageData> image = new SimpleObjectProperty<>();
    public final SimpleObjectProperty<GenericPreset<PresetImageFilters>> selectedImagePreset = new SimpleObjectProperty<>();

    ////////////////////////////////////////////////////////

    public ComboBox<GenericPreset<PresetImageFilters>> comboBoxImageFilterPreset = null;

    public MenuButton menuButtonFilterPresets = null;

    public TableView<ObservableImageFilter> tableViewImageFilters = null;
    public TableColumn<ObservableImageFilter, Boolean> columnEnableImageFilter = null;
    public TableColumn<ObservableImageFilter, String> columnImageFilterType = null;
    public TableColumn<ObservableImageFilter, String> columnImageFilterSettings = null;

    public ComboBox<EnumFilterTypes> comboBoxFilterType = null;
    public ComboBox<GenericFactory<BufferedImageOp>> comboBoxImageFilter = null;
    public Button buttonAddFilter = null;
    public Button buttonRemoveFilter = null;
    public Button buttonDuplicateFilter = null;
    public Button buttonMoveUpFilter = null;
    public Button buttonMoveDownFilter = null;
    public Button buttonClearFilters = null;

    public TextField textFieldCropStartX = null;
    public TextField textFieldCropStartY = null;
    public TextField textFieldCropEndX = null;
    public TextField textFieldCropEndY = null;
    public ToggleButton buttonEditCrop = null;
    public Button buttonResetCrop;

    public ChoiceBox<EnumRotation> choiceBoxRotation = null;
    public CheckBox checkBoxFlipX = null;
    public CheckBox checkBoxFlipY = null;

    @FXML
    public void initialize(){
        settings.addListener((observable, oldValue, newValue) -> {
            if(oldValue != null){
                tableViewImageFilters.itemsProperty().unbind();
            }
            if(newValue != null){
                tableViewImageFilters.itemsProperty().bind(newValue.currentFilters);
            }
        });

        ///////////////////////////////////////////////////////////////////////////////////////////////////////
        selectedImagePreset.setValue(Register.PRESET_LOADER_FILTERS.getDefaultPreset());
        selectedImagePreset.addListener((observable, oldValue, newValue) -> {
            if(newValue != null){
                getImageFiltersPresetManager().applyPreset(DrawingBotV3.context(), newValue);
            }
        });

        comboBoxImageFilterPreset.setItems(Register.PRESET_LOADER_FILTERS.presets);
        comboBoxImageFilterPreset.valueProperty().bindBidirectional(selectedImagePreset);

        FXHelper.setupPresetMenuButton(menuButtonFilterPresets, Register.PRESET_LOADER_FILTERS, this::getImageFiltersPresetManager, false, selectedImagePreset);

        tableViewImageFilters.setRowFactory(param -> {
            TableRow<ObservableImageFilter> row = new TableRow<>();
            row.addEventFilter(ContextMenuEvent.CONTEXT_MENU_REQUESTED, event -> {
                if(row.getItem() == null){
                    event.consume();
                }
            });
            row.setOnMouseClicked(e -> {
                if(e.getClickCount() > 1){
                    FXHelper.openImageFilterDialog(row.getItem());
                }
            });
            row.setContextMenu(new ContextMenuObservableFilter(row, settings));
            row.setPrefHeight(30);
            return row;
        });

        columnEnableImageFilter.setCellFactory(param -> new CheckBoxTableCell<>(index -> columnEnableImageFilter.getCellObservableValue(index)));
        columnEnableImageFilter.setCellValueFactory(param -> param.getValue().enable);

        columnImageFilterType.setCellFactory(param -> new TextFieldTableCell<>(new DefaultStringConverter()));
        columnImageFilterType.setCellValueFactory(param -> param.getValue().name);


        columnImageFilterSettings.setCellValueFactory(param -> param.getValue().settingsString);

        comboBoxFilterType.setItems(FXCollections.observableArrayList(MasterRegistry.INSTANCE.imgFilterFactories.keySet()));
        comboBoxFilterType.setValue(MasterRegistry.INSTANCE.getDefaultImageFilterType());
        comboBoxFilterType.valueProperty().addListener((observable, oldValue, newValue) -> {
            comboBoxImageFilter.setItems(MasterRegistry.INSTANCE.imgFilterFactories.get(newValue));
            comboBoxImageFilter.setValue(MasterRegistry.INSTANCE.getDefaultImageFilter(newValue));
        });

        comboBoxImageFilter.setItems(MasterRegistry.INSTANCE.imgFilterFactories.get(MasterRegistry.INSTANCE.getDefaultImageFilterType()));
        comboBoxImageFilter.setValue(MasterRegistry.INSTANCE.getDefaultImageFilter(MasterRegistry.INSTANCE.getDefaultImageFilterType()));
        buttonAddFilter.setOnAction(e -> {
            if(comboBoxImageFilter.getValue() != null){
                FXHelper.addImageFilter(comboBoxImageFilter.getValue(), settings.get());
            }
        });
        buttonRemoveFilter.setOnAction(e -> FXHelper.deleteItem(tableViewImageFilters.getSelectionModel(), settings.get().currentFilters.get()));
        buttonRemoveFilter.setTooltip(new Tooltip("Remove Selected Filter"));
        buttonRemoveFilter.disableProperty().bind(tableViewImageFilters.getSelectionModel().selectedItemProperty().isNull());

        buttonDuplicateFilter.setOnAction(e -> FXHelper.duplicateItem(tableViewImageFilters.getSelectionModel(), settings.get().currentFilters.get(), ObservableImageFilter::new));
        buttonDuplicateFilter.setTooltip(new Tooltip("Duplicate Selected Filter"));
        buttonDuplicateFilter.disableProperty().bind(tableViewImageFilters.getSelectionModel().selectedItemProperty().isNull());

        buttonMoveUpFilter.setOnAction(e -> FXHelper.moveItemUp(tableViewImageFilters.getSelectionModel(), settings.get().currentFilters.get()));
        buttonMoveUpFilter.setTooltip(new Tooltip("Move Selected Filter Up"));
        buttonMoveUpFilter.disableProperty().bind(tableViewImageFilters.getSelectionModel().selectedItemProperty().isNull());

        buttonMoveDownFilter.setOnAction(e -> FXHelper.moveItemDown(tableViewImageFilters.getSelectionModel(), settings.get().currentFilters.get()));
        buttonMoveDownFilter.setTooltip(new Tooltip("Move Selected Filter Down"));
        buttonMoveDownFilter.disableProperty().bind(tableViewImageFilters.getSelectionModel().selectedItemProperty().isNull());

        buttonClearFilters.setOnAction(e -> settings.get().currentFilters.get().clear());
        buttonClearFilters.setTooltip(new Tooltip("Clear Filters"));

        image.addListener((observable, oldValue, newValue) -> {
            if(oldValue != null){
                textFieldCropStartX.textProperty().unbindBidirectional(oldValue.cropStartX);
                textFieldCropStartY.textProperty().unbindBidirectional(oldValue.cropStartY);
                textFieldCropEndX.textProperty().unbindBidirectional(oldValue.cropEndX);
                textFieldCropEndY.textProperty().unbindBidirectional(oldValue.cropEndY);
                choiceBoxRotation.valueProperty().unbindBidirectional(oldValue.imageRotation);
                checkBoxFlipX.selectedProperty().unbindBidirectional(oldValue.imageFlipHorizontal);
                checkBoxFlipY.selectedProperty().unbindBidirectional(oldValue.imageFlipVertical);
            }
            if(newValue != null){
                textFieldCropStartX.textProperty().bindBidirectional(newValue.cropStartX, new NumberStringConverter(Utils.oneDecimal));
                textFieldCropStartY.textProperty().bindBidirectional(newValue.cropStartY, new NumberStringConverter(Utils.oneDecimal));
                textFieldCropEndX.textProperty().bindBidirectional(newValue.cropEndX, new NumberStringConverter(Utils.oneDecimal));
                textFieldCropEndY.textProperty().bindBidirectional(newValue.cropEndY, new NumberStringConverter(Utils.oneDecimal));
                choiceBoxRotation.valueProperty().bindBidirectional(newValue.imageRotation);
                checkBoxFlipX.selectedProperty().bindBidirectional(newValue.imageFlipHorizontal);
                checkBoxFlipY.selectedProperty().bindBidirectional(newValue.imageFlipVertical);
            }
        });

        textFieldCropStartX.textFormatterProperty().setValue(new TextFormatter<>(new FloatStringConverter(), 0F));
        textFieldCropStartY.textFormatterProperty().setValue(new TextFormatter<>(new FloatStringConverter(), 0F));
        textFieldCropEndX.textFormatterProperty().setValue(new TextFormatter<>(new FloatStringConverter(), 0F));
        textFieldCropEndY.textFormatterProperty().setValue(new TextFormatter<>(new FloatStringConverter(), 0F));

        EasyBind.select(DrawingBotV3.INSTANCE.activeProject).selectObject(ObservableProject::displayModeProperty).addListener((observable, oldValue, newValue) -> {
            buttonEditCrop.setSelected(newValue == Register.INSTANCE.DISPLAY_MODE_IMAGE_CROPPING);
        });

        buttonEditCrop.selectedProperty().addListener((observable, oldValue, newValue) -> {
            if(newValue){
                DrawingBotV3.project().displayMode.set(Register.INSTANCE.DISPLAY_MODE_IMAGE_CROPPING);
            }else if(DrawingBotV3.project().displayMode.get() == Register.INSTANCE.DISPLAY_MODE_IMAGE_CROPPING){
                DrawingBotV3.project().displayMode.set(Register.INSTANCE.DISPLAY_MODE_IMAGE);
            }
        });

        buttonResetCrop.setOnAction(e -> {
            if(DrawingBotV3.project().openImage.get() != null){
                DrawingBotV3.project().openImage.get().resetCrop();
            }
        });

        choiceBoxRotation.setItems(FXCollections.observableArrayList(EnumRotation.DEFAULTS));
        choiceBoxRotation.setValue(EnumRotation.R0);

        checkBoxFlipX.setSelected(false);

        checkBoxFlipY.setSelected(false);

    }

    ////////////////////////////////////////////////////////

    public final AbstractPresetManager<PresetImageFilters> imageFiltersPresetManager = new PresetImageFiltersManager(Register.PRESET_LOADER_FILTERS) {
        @Override
        public Property<ObservableList<ObservableImageFilter>> imageFiltersProperty(DBTaskContext context) {
            return settings.get().currentFilters;
        }
    };

    public AbstractPresetManager<PresetImageFilters> getImageFiltersPresetManager(){
        return imageFiltersPresetManager;
    }


    ////////////////////////////////////////////////////////

    @Override
    public List<Styleable> getPersistentNodes(){
        return List.of(tableViewImageFilters, columnEnableImageFilter, columnImageFilterType, columnImageFilterSettings);
    }
}
