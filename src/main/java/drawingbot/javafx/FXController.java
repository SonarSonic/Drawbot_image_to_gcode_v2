package drawingbot.javafx;

import drawingbot.DrawingBotV3;
import drawingbot.api.IDrawingPen;
import drawingbot.api.IDrawingSet;
import drawingbot.files.*;
import drawingbot.drawing.*;
import drawingbot.files.presets.*;
import drawingbot.files.presets.types.*;
import drawingbot.javafx.observables.ObservableImageFilter;
import drawingbot.image.blend.EnumBlendMode;
import drawingbot.integrations.vpype.FXVPypeController;
import drawingbot.integrations.vpype.VpypeHelper;
import drawingbot.javafx.controls.*;
import drawingbot.javafx.observables.ObservableDrawingPen;
import drawingbot.javafx.observables.ObservableProjectSettings;
import drawingbot.pfm.PFMFactory;
import drawingbot.registry.MasterRegistry;
import drawingbot.plotting.PlottingTask;
import drawingbot.utils.*;
import javafx.application.Platform;
import javafx.beans.binding.Bindings;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.scene.Node;
import javafx.scene.control.*;

import javafx.scene.control.cell.CheckBoxTableCell;
import javafx.scene.control.cell.ComboBoxListCell;
import javafx.scene.control.cell.TextFieldTableCell;
import javafx.scene.control.skin.ComboBoxListViewSkin;
import javafx.scene.image.ImageView;
import javafx.scene.input.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.stage.Stage;
import javafx.util.StringConverter;
import javafx.util.converter.DefaultStringConverter;
import javafx.util.converter.FloatStringConverter;
import javafx.util.converter.IntegerStringConverter;

import java.awt.image.BufferedImageOp;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;

public class FXController {

    /**
     * starts the FXController, called internally by JavaFX
     */
    public void initialize(){
        DrawingBotV3.logger.entering("FX Controller", "initialize");

        try{
            initToolbar();
            initViewport();
            initPlottingControls();
            initProgressBar();
            initDrawingAreaPane();
            initPreProcessingPane();
            initPFMControls();
            initPenSettingsPane();
            initVersionControlPane();
            initBatchProcessingPane();


            viewportStackPane.setOnMousePressed(DrawingBotV3.INSTANCE::mousePressedJavaFX);
            viewportStackPane.setOnMouseDragged(DrawingBotV3.INSTANCE::mouseDraggedJavaFX);

            viewportScrollPane.setHvalue(0.5);
            viewportScrollPane.setVvalue(0.5);

            initSeparateStages();

            DrawingBotV3.INSTANCE.currentFilters.addListener((ListChangeListener<ObservableImageFilter>) c -> DrawingBotV3.INSTANCE.onImageFiltersChanged());

        }catch (Exception e){
            DrawingBotV3.logger.log(Level.SEVERE, "Failed to initialize JAVA FX", e);
        }

        DrawingBotV3.logger.exiting("FX Controller", "initialize");
    }

    public Stage exportSettingsStage;
    public FXExportController exportController;


    public Stage vpypeSettingsStage;
    public FXVPypeController vpypeController;


    public Stage mosaicSettingsStage;
    public FXStylesController mosaicController;

    public void initSeparateStages() {
        FXHelper.initSeparateStage("/fxml/exportsettings.fxml", exportSettingsStage = new Stage(), exportController = new FXExportController(), "Export Settings");
        FXHelper.initSeparateStage("/fxml/vpypesettings.fxml", vpypeSettingsStage = new Stage(), vpypeController = new FXVPypeController(), "vpype Settings");
        FXHelper.initSeparateStage("/fxml/mosaicsettings.fxml", mosaicSettingsStage = new Stage(), mosaicController = new FXStylesController(), "Mosaic Settings");
    }


    ///////////////////////////////////////////////////////////////////////////////////////////////////////
    ////GLOBAL CONTAINERS
    public ScrollPane scrollPaneSettings = null;
    public VBox vBoxSettings = null;

    public DialogPresetRename presetEditorDialog = new DialogPresetRename();

    ///////////////////////////////////////////////////////////////////////////////////////////////////////
    //// TOOL BAR

    public Menu menuFile = null;
    public Menu menuView = null;
    public Menu menuFilters = null;
    public Menu menuHelp = null;

    public void initToolbar(){
        //file
        MenuItem menuOpen = new MenuItem("Open Project");
        menuOpen.setOnAction(e -> FXHelper.importPreset(EnumJsonType.PROJECT_PRESET, true));
        menuFile.getItems().add(menuOpen);

        MenuItem menuSave = new MenuItem("Save Project");
        menuSave.disableProperty().bind(DrawingBotV3.INSTANCE.activeTask.isNull());
        menuSave.setOnAction(e -> {
            GenericPreset<PresetProjectSettings> preset = JsonLoaderManager.PROJECT.createNewPreset();
            JsonLoaderManager.PROJECT.updatePreset(preset);
            FXHelper.exportPreset(preset, DrawingBotV3.INSTANCE.activeTask.get().originalFile.getParentFile(), FileUtils.removeExtension(DrawingBotV3.INSTANCE.activeTask.get().originalFile.getName()));
        });
        menuFile.getItems().add(menuSave);

        menuFile.getItems().add(new SeparatorMenuItem());

        MenuItem menuImport = new MenuItem("Import Image");
        menuImport.setOnAction(e -> FXHelper.importImageFile());
        menuFile.getItems().add(menuImport);

        menuFile.getItems().add(new SeparatorMenuItem());

        Menu menuExport = new Menu("Export per/drawing");
        for(ExportFormats format : ExportFormats.values()){
            MenuItem item = new MenuItem(format.displayName);
            item.setOnAction(e -> FXHelper.exportFile(format, false));
            menuExport.getItems().add(item);
        }
        menuExport.disableProperty().bind(DrawingBotV3.INSTANCE.activeTask.isNull());
        menuFile.getItems().add(menuExport);

        Menu menuExportPerPen = new Menu("Export per/pen");
        for(ExportFormats format : ExportFormats.values()){
            MenuItem item = new MenuItem(format.displayName);
            item.setOnAction(e -> FXHelper.exportFile(format, true));
            menuExportPerPen.getItems().add(item);
        }
        menuExportPerPen.disableProperty().bind(DrawingBotV3.INSTANCE.activeTask.isNull());
        menuFile.getItems().add(menuExportPerPen);

        MenuItem menuExportToVPype = new MenuItem("Export to " + VpypeHelper.VPYPE_NAME);
        menuExportToVPype.setOnAction(e -> {
            if(DrawingBotV3.INSTANCE.getActiveTask() != null){
                vpypeSettingsStage.show();
            }
        });
        menuExportToVPype.disableProperty().bind(DrawingBotV3.INSTANCE.activeTask.isNull());
        menuFile.getItems().add(menuExportToVPype);

        menuFile.getItems().add(new SeparatorMenuItem());

        MenuItem menuExportSettings = new MenuItem("Export Settings");
        menuExportSettings.setOnAction(e -> exportSettingsStage.show());
        menuFile.getItems().add(menuExportSettings);

        menuFile.getItems().add(new SeparatorMenuItem());

        MenuItem menuQuit = new MenuItem("Quit");
        menuQuit.setOnAction(e -> Platform.exit());
        menuFile.getItems().add(menuQuit);

        //view
        ArrayList<TitledPane> allPanes = new ArrayList<>();
        for(Node node : vBoxSettings.getChildren()){
            if(node instanceof TitledPane){
                allPanes.add((TitledPane) node);
            }
        }
        for(TitledPane pane : allPanes){
            MenuItem viewButton = new MenuItem(pane.getText());
            viewButton.setOnAction(e -> allPanes.forEach(p -> p.expandedProperty().setValue(p == pane)));
            menuView.getItems().add(viewButton);
        }

        //filters
        for(Map.Entry<EnumFilterTypes, ObservableList<GenericFactory<BufferedImageOp>>> entry : MasterRegistry.INSTANCE.imgFilterFactories.entrySet()){
            Menu type = new Menu(entry.getKey().toString());

            for(GenericFactory<BufferedImageOp> factory : entry.getValue()){
                MenuItem item = new MenuItem(factory.getName());
                item.setOnAction(e -> FXHelper.addImageFilter(factory));
                type.getItems().add(item);
            }

            menuFilters.getItems().add(type);
        }

        //help
        MenuItem documentation = new MenuItem("View Documentation");
        documentation.setOnAction(e -> FXHelper.openURL(DBConstants.URL_READ_THE_DOCS_HOME));
        menuHelp.getItems().add(documentation);

        MenuItem sourceCode = new MenuItem("View Source Code");
        sourceCode.setOnAction(e -> FXHelper.openURL(DBConstants.URL_GITHUB_REPO));
        menuHelp.getItems().add(sourceCode);

        MenuItem configFolder = new MenuItem("Open Configs Folder");
        configFolder.setOnAction(e -> FXHelper.openFolder(new File(FileUtils.getUserDataDirectory())));
        menuHelp.getItems().add(configFolder);
    }

    ///////////////////////////////////////////////////////////////////////////////////////////////////////
    //// VIEWPORT PANE

    ////VIEWPORT WINDOW
    public VBox vBoxViewportContainer = null;
    public ScrollPane viewportScrollPane = null;
    public StackPane viewportStackPane = null;

    ////VIEWPORT SETTINGS
    public Slider sliderDisplayedLines = null;
    public TextField textFieldDisplayedLines = null;

    public ChoiceBox<EnumDisplayMode> choiceBoxDisplayMode = null;
    public CheckBox checkBoxShowGrid = null;
    public Button buttonZoomIn = null;
    public Button buttonZoomOut = null;
    public Button buttonResetView = null;

    ////PLOT DETAILS
    public Label labelElapsedTime = null;
    public Label labelPlottedShapes = null;
    public Label labelPlottedVertices = null;
    public Label labelImageResolution = null;
    public Label labelPlottingResolution = null;

    public void initViewport(){

        ////VIEWPORT SETTINGS
        sliderDisplayedLines.setMax(1);
        sliderDisplayedLines.valueProperty().addListener((observable, oldValue, newValue) -> {
            PlottingTask task = DrawingBotV3.INSTANCE.getActiveTask();
            if(task != null){
                int lines = (int)Utils.mapDouble(newValue.doubleValue(), 0, 1, 0, task.plottedDrawing.getGeometryCount());
                task.plottedDrawing.displayedLineCount.setValue(lines);
                textFieldDisplayedLines.setText(String.valueOf(lines));
                DrawingBotV3.INSTANCE.reRender();
            }
        });

        textFieldDisplayedLines.setOnAction(e -> {
            PlottingTask task = DrawingBotV3.INSTANCE.getActiveTask();
            if(task != null){
                int lines = (int)Math.max(0, Math.min(task.plottedDrawing.getGeometryCount(), Double.parseDouble(textFieldDisplayedLines.getText())));
                task.plottedDrawing.displayedLineCount.setValue(lines);
                textFieldDisplayedLines.setText(String.valueOf(lines));
                sliderDisplayedLines.setValue((double)lines / task.plottedDrawing.getGeometryCount());
                DrawingBotV3.INSTANCE.reRender();
            }
        });

        choiceBoxDisplayMode.getItems().addAll(EnumDisplayMode.values());
        choiceBoxDisplayMode.setValue(EnumDisplayMode.IMAGE);
        DrawingBotV3.INSTANCE.display_mode.bindBidirectional(choiceBoxDisplayMode.valueProperty());

        DrawingBotV3.INSTANCE.displayGrid.bind(checkBoxShowGrid.selectedProperty());
        DrawingBotV3.INSTANCE.displayGrid.addListener((observable, oldValue, newValue) -> DrawingBotV3.INSTANCE.reRender());

        buttonZoomIn.setOnAction(e -> DrawingBotV3.INSTANCE.scaleMultiplier.set(DrawingBotV3.INSTANCE.scaleMultiplier.getValue() + 0.1));
        buttonZoomOut.setOnAction(e -> {
            if(DrawingBotV3.INSTANCE.scaleMultiplier.getValue() > DrawingBotV3.minScale){
                DrawingBotV3.INSTANCE.scaleMultiplier.set(DrawingBotV3.INSTANCE.scaleMultiplier.getValue() - 0.1);
            }
        });

        buttonResetView.setOnAction(e -> {
            viewportScrollPane.setHvalue(0.5);
            viewportScrollPane.setVvalue(0.5);
            DrawingBotV3.INSTANCE.scaleMultiplier.set(1.0);
        });

        viewportScrollPane.setOnDragOver(event -> {

            if (event.getDragboard().hasFiles()) {
                event.acceptTransferModes(TransferMode.LINK);
            }

            event.consume();
        });

        viewportScrollPane.setOnDragDropped(event -> {

            Dragboard db = event.getDragboard();
            boolean success = false;
            if(db.hasContent(DataFormat.FILES)){
                List<File> files = (List<File>) db.getContent(DataFormat.FILES);
                DrawingBotV3.INSTANCE.openImage(files.get(0), false);
                success = true;
            }
            event.setDropCompleted(success);

            event.consume();
        });

        labelElapsedTime.setText("0 s");
        labelPlottedShapes.setText("0");
        labelPlottedVertices.setText("0");
        labelImageResolution.setText("0 x 0");
        labelPlottingResolution.setText("0 x 0");
    }


    ///////////////////////////////////////////////////////////////////////////////////////////////////////
    //// PLOTTING CONTROLS

    public Button buttonStartPlotting = null;
    public Button buttonStopPlotting = null;
    public Button buttonResetPlotting = null;
    public Button buttonSaveVersion = null;

    public void initPlottingControls(){
        buttonStartPlotting.setOnAction(param -> DrawingBotV3.INSTANCE.startPlotting());
        buttonStartPlotting.disableProperty().bind(DrawingBotV3.INSTANCE.taskMonitor.isPlotting);
        buttonStopPlotting.setOnAction(param -> DrawingBotV3.INSTANCE.stopPlotting());
        buttonStopPlotting.disableProperty().bind(DrawingBotV3.INSTANCE.taskMonitor.isPlotting.not());
        buttonResetPlotting.setOnAction(param -> DrawingBotV3.INSTANCE.resetPlotting());

        buttonSaveVersion.setOnAction(param -> saveVersion());
        buttonSaveVersion.disableProperty().bind(Bindings.createBooleanBinding(() -> DrawingBotV3.INSTANCE.taskMonitor.isPlotting.get() || DrawingBotV3.INSTANCE.activeTask.get() == null, DrawingBotV3.INSTANCE.taskMonitor.isPlotting, DrawingBotV3.INSTANCE.activeTask));
    }

    public void saveVersion(){
        GenericPreset<PresetProjectSettings> preset = JsonLoaderManager.PROJECT.createNewPreset();
        JsonLoaderManager.PROJECT.updatePreset(preset);
        DrawingBotV3.INSTANCE.projectVersions.add(new ObservableProjectSettings(preset));
    }

    ///////////////////////////////////////////////////////////////////////////////////////////////////////

    ////PROGRESS BAR PANE

    public Pane paneProgressBar = null;
    public ProgressBar progressBarGeneral = null;
    public Label progressBarLabel = null;
    public Label labelCancelExport = null;
    public Label labelOpenDestinationFolder = null;

    public void initProgressBar(){
        progressBarGeneral.prefWidthProperty().bind(paneProgressBar.widthProperty());
        progressBarLabel.setText("");

        progressBarGeneral.progressProperty().bind(DrawingBotV3.INSTANCE.taskMonitor.progressProperty);
        progressBarLabel.textProperty().bind(Bindings.createStringBinding(() -> DrawingBotV3.INSTANCE.taskMonitor.getCurrentTaskStatus(), DrawingBotV3.INSTANCE.taskMonitor.messageProperty, DrawingBotV3.INSTANCE.taskMonitor.titleProperty, DrawingBotV3.INSTANCE.taskMonitor.exceptionProperty));

        labelCancelExport.setOnMouseEntered(event -> labelCancelExport.textFillProperty().setValue(Color.BLANCHEDALMOND));
        labelCancelExport.setOnMouseExited(event -> labelCancelExport.textFillProperty().setValue(Color.BLACK));
        labelCancelExport.setOnMouseClicked(event -> {
            Task<?> task = DrawingBotV3.INSTANCE.taskMonitor.currentTask;
            if(task instanceof ExportTask){
                task.cancel(true);
            }
        });
        labelCancelExport.visibleProperty().bind(DrawingBotV3.INSTANCE.taskMonitor.isExporting);

        labelOpenDestinationFolder.setOnMouseEntered(event -> labelOpenDestinationFolder.textFillProperty().setValue(Color.BLANCHEDALMOND));
        labelOpenDestinationFolder.setOnMouseExited(event -> labelOpenDestinationFolder.textFillProperty().setValue(Color.BLACK));
        labelOpenDestinationFolder.setOnMouseClicked(event -> {
            ExportTask task = DrawingBotV3.INSTANCE.taskMonitor.getDisplayedExportTask();
            if(task != null){
                FXHelper.openFolder(task.saveLocation.getParentFile());
            }
        });
        labelOpenDestinationFolder.visibleProperty().bind(Bindings.createBooleanBinding(() -> DrawingBotV3.INSTANCE.taskMonitor.wasExporting.get() || DrawingBotV3.INSTANCE.taskMonitor.isExporting.get(), DrawingBotV3.INSTANCE.taskMonitor.isExporting, DrawingBotV3.INSTANCE.taskMonitor.wasExporting));
    }

    ///////////////////////////////////////////////////////////////////////////////////////////////////////
    ////DRAWING AREA PANE
    public ComboBox<GenericPreset<PresetDrawingArea>> comboBoxDrawingAreaPreset = null;
    public MenuButton menuButtonDrawingAreaPresets = null;

    /////SIZING OPTIONS
    public CheckBox checkBoxOriginalSizing = null;
    public ChoiceBox<UnitsLength> choiceBoxDrawingUnits = null;
    public Pane paneDrawingAreaCustom = null;
    public TextField textFieldDrawingWidth = null;
    public TextField textFieldDrawingHeight = null;
    public Button buttonRotate = null;
    public TextField textFieldPaddingLeft = null;
    public TextField textFieldPaddingRight = null;
    public TextField textFieldPaddingTop = null;
    public TextField textFieldPaddingBottom = null;
    public CheckBox checkBoxGangPadding = null;

    public ChoiceBox<EnumScalingMode> choiceBoxScalingMode = null;

    public CheckBox checkBoxOptimiseForPrint = null;
    public TextField textFieldPenWidth = null;

    public void initDrawingAreaPane(){

        comboBoxDrawingAreaPreset.setItems(JsonLoaderManager.DRAWING_AREA.presets);
        comboBoxDrawingAreaPreset.setValue(JsonLoaderManager.DRAWING_AREA.getDefaultPreset());
        comboBoxDrawingAreaPreset.valueProperty().addListener((observable, oldValue, newValue) -> {
            if(newValue != null){
                JsonLoaderManager.DRAWING_AREA.applyPreset(newValue);
            }
        });

        FXHelper.setupPresetMenuButton(JsonLoaderManager.DRAWING_AREA, menuButtonDrawingAreaPresets, comboBoxDrawingAreaPreset::getValue, (preset) -> {
            comboBoxDrawingAreaPreset.setValue(preset);

            ///force update rendering
            comboBoxDrawingAreaPreset.setItems(JsonLoaderManager.DRAWING_AREA.presets);
            comboBoxDrawingAreaPreset.setButtonCell(new ComboBoxListCell<>());
        });


        /////SIZING OPTIONS
        DrawingBotV3.INSTANCE.useOriginalSizing.bindBidirectional(checkBoxOriginalSizing.selectedProperty());

        paneDrawingAreaCustom.disableProperty().bind(checkBoxOriginalSizing.selectedProperty());
        choiceBoxDrawingUnits.disableProperty().bind(checkBoxOriginalSizing.selectedProperty());

        choiceBoxDrawingUnits.getItems().addAll(UnitsLength.values());
        choiceBoxDrawingUnits.setValue(UnitsLength.MILLIMETRES);
        DrawingBotV3.INSTANCE.inputUnits.bindBidirectional(choiceBoxDrawingUnits.valueProperty());

        DrawingBotV3.INSTANCE.drawingAreaWidth.bind(Bindings.createFloatBinding(() -> textFieldDrawingWidth.textProperty().get().isEmpty() ? 0F : Float.parseFloat(textFieldDrawingWidth.textProperty().get()), textFieldDrawingWidth.textProperty()));
        textFieldDrawingWidth.textFormatterProperty().setValue(new TextFormatter<>(new FloatStringConverter(), 0F));

        DrawingBotV3.INSTANCE.drawingAreaHeight.bind(Bindings.createFloatBinding(() -> textFieldDrawingHeight.textProperty().get().isEmpty() ? 0F : Float.parseFloat(textFieldDrawingHeight.textProperty().get()), textFieldDrawingHeight.textProperty()));
        textFieldDrawingHeight.textFormatterProperty().setValue(new TextFormatter<>(new FloatStringConverter(), 0F));

        buttonRotate.setOnAction(e -> {
            String width = textFieldDrawingWidth.getText();
            String height = textFieldDrawingHeight.getText();
            textFieldDrawingWidth.setText(height);
            textFieldDrawingHeight.setText(width);
        });

        DrawingBotV3.INSTANCE.drawingAreaPaddingLeft.bind(Bindings.createFloatBinding(() -> textFieldPaddingLeft.textProperty().get().isEmpty() ? 0F : Float.parseFloat(textFieldPaddingLeft.textProperty().get()), textFieldPaddingLeft.textProperty()));
        textFieldPaddingLeft.textFormatterProperty().setValue(new TextFormatter<>(new FloatStringConverter(), 0F));

        DrawingBotV3.INSTANCE.drawingAreaPaddingRight.bind(Bindings.createFloatBinding(() -> textFieldPaddingRight.textProperty().get().isEmpty() ? 0F : Float.parseFloat(textFieldPaddingRight.textProperty().get()), textFieldPaddingRight.textProperty()));
        textFieldPaddingRight.textFormatterProperty().setValue(new TextFormatter<>(new FloatStringConverter(), 0F));

        DrawingBotV3.INSTANCE.drawingAreaPaddingTop.bind(Bindings.createFloatBinding(() -> textFieldPaddingTop.textProperty().get().isEmpty() ? 0F : Float.parseFloat(textFieldPaddingTop.textProperty().get()), textFieldPaddingTop.textProperty()));
        textFieldPaddingTop.textFormatterProperty().setValue(new TextFormatter<>(new FloatStringConverter(), 0F));

        DrawingBotV3.INSTANCE.drawingAreaPaddingBottom.bind(Bindings.createFloatBinding(() -> textFieldPaddingBottom.textProperty().get().isEmpty() ? 0F : Float.parseFloat(textFieldPaddingBottom.textProperty().get()), textFieldPaddingBottom.textProperty()));
        textFieldPaddingBottom.textFormatterProperty().setValue(new TextFormatter<>(new FloatStringConverter(), 0F));

        checkBoxGangPadding.setSelected(true);
        checkBoxGangPadding.selectedProperty().addListener((observable, oldValue, newValue) -> updatePaddingBindings(newValue));
        updatePaddingBindings(checkBoxGangPadding.isSelected());

        choiceBoxScalingMode.getItems().addAll(EnumScalingMode.values());
        choiceBoxScalingMode.setValue(EnumScalingMode.CROP_TO_FIT);
        DrawingBotV3.INSTANCE.scalingMode.bindBidirectional(choiceBoxScalingMode.valueProperty());

        DrawingBotV3.INSTANCE.optimiseForPrint.bindBidirectional(checkBoxOptimiseForPrint.selectedProperty());

        DrawingBotV3.INSTANCE.targetPenWidth.bind(Bindings.createFloatBinding(() -> textFieldPenWidth.textProperty().get().isEmpty() ? 0.5F : Float.parseFloat(textFieldPenWidth.textProperty().get()), textFieldPenWidth.textProperty()));
        textFieldPenWidth.textFormatterProperty().setValue(new TextFormatter<>(new FloatStringConverter(), 0.5F));
        textFieldPenWidth.disableProperty().bind(checkBoxOptimiseForPrint.selectedProperty().not());


        ///generic listeners
        DrawingBotV3.INSTANCE.useOriginalSizing.addListener((observable, oldValue, newValue) -> DrawingBotV3.INSTANCE.onDrawingAreaChanged());
        DrawingBotV3.INSTANCE.scalingMode.addListener((observable, oldValue, newValue) -> DrawingBotV3.INSTANCE.onDrawingAreaChanged());
        DrawingBotV3.INSTANCE.inputUnits.addListener((observable, oldValue, newValue) -> DrawingBotV3.INSTANCE.onDrawingAreaChanged());
        DrawingBotV3.INSTANCE.drawingAreaHeight.addListener((observable, oldValue, newValue) -> DrawingBotV3.INSTANCE.onDrawingAreaChanged());
        DrawingBotV3.INSTANCE.drawingAreaWidth.addListener((observable, oldValue, newValue) -> DrawingBotV3.INSTANCE.onDrawingAreaChanged());
        DrawingBotV3.INSTANCE.drawingAreaPaddingLeft.addListener((observable, oldValue, newValue) -> DrawingBotV3.INSTANCE.onDrawingAreaChanged());
        DrawingBotV3.INSTANCE.drawingAreaPaddingRight.addListener((observable, oldValue, newValue) -> DrawingBotV3.INSTANCE.onDrawingAreaChanged());
        DrawingBotV3.INSTANCE.drawingAreaPaddingTop.addListener((observable, oldValue, newValue) -> DrawingBotV3.INSTANCE.onDrawingAreaChanged());
        DrawingBotV3.INSTANCE.drawingAreaPaddingBottom.addListener((observable, oldValue, newValue) -> DrawingBotV3.INSTANCE.onDrawingAreaChanged());
        DrawingBotV3.INSTANCE.optimiseForPrint.addListener((observable, oldValue, newValue) -> DrawingBotV3.INSTANCE.onDrawingAreaChanged());
        DrawingBotV3.INSTANCE.targetPenWidth.addListener((observable, oldValue, newValue) -> DrawingBotV3.INSTANCE.onDrawingAreaChanged());

    }

    public void updatePaddingBindings(boolean ganged){
        if(ganged){
            DrawingBotV3.INSTANCE.drawingAreaPaddingGang.set("0");
            textFieldPaddingLeft.textProperty().bindBidirectional(DrawingBotV3.INSTANCE.drawingAreaPaddingGang);
            textFieldPaddingRight.textProperty().bindBidirectional(DrawingBotV3.INSTANCE.drawingAreaPaddingGang);
            textFieldPaddingTop.textProperty().bindBidirectional(DrawingBotV3.INSTANCE.drawingAreaPaddingGang);
            textFieldPaddingBottom.textProperty().bindBidirectional(DrawingBotV3.INSTANCE.drawingAreaPaddingGang);
        }else{
            textFieldPaddingLeft.textProperty().unbindBidirectional(DrawingBotV3.INSTANCE.drawingAreaPaddingGang);
            textFieldPaddingRight.textProperty().unbindBidirectional(DrawingBotV3.INSTANCE.drawingAreaPaddingGang);
            textFieldPaddingTop.textProperty().unbindBidirectional(DrawingBotV3.INSTANCE.drawingAreaPaddingGang);
            textFieldPaddingBottom.textProperty().unbindBidirectional(DrawingBotV3.INSTANCE.drawingAreaPaddingGang);
        }
    }


    ///////////////////////////////////////////////////////////////////////////////////////////////////////
    ////PRE PROCESSING PANE

    public ComboBox<GenericPreset<PresetImageFilters>> comboBoxImageFilterPreset = null;

    public MenuButton menuButtonFilterPresets = null;

    public TableView<ObservableImageFilter> tableViewImageFilters = null;
    public TableColumn<ObservableImageFilter, Boolean> columnEnableImageFilter = null;
    public TableColumn<ObservableImageFilter, String> columnImageFilterType = null;
    public TableColumn<ObservableImageFilter, String> columnImageFilterSettings = null;

    public ComboBox<EnumFilterTypes> comboBoxFilterType = null;
    public ComboBox<GenericFactory<BufferedImageOp>> comboBoxImageFilter = null;
    public Button buttonAddFilter = null;

    public ChoiceBox<EnumImageRotate> choiceBoxRotation = null;
    public CheckBox checkBoxFlipX = null;
    public CheckBox checkBoxFlipY = null;

    public void initPreProcessingPane(){
        comboBoxImageFilterPreset.setItems(MasterRegistry.INSTANCE.imgFilterPresets);
        comboBoxImageFilterPreset.setValue(MasterRegistry.INSTANCE.getDefaultImageFilterPreset());
        comboBoxImageFilterPreset.valueProperty().addListener((observable, oldValue, newValue) -> {
            if(newValue != null){
                JsonLoaderManager.FILTERS.applyPreset(newValue);
            }
        });

        FXHelper.setupPresetMenuButton(JsonLoaderManager.FILTERS, menuButtonFilterPresets, comboBoxImageFilterPreset::getValue, (preset) -> {
            comboBoxImageFilterPreset.setValue(preset);

            ///force update rendering
            comboBoxImageFilterPreset.setItems(MasterRegistry.INSTANCE.imgFilterPresets);
            comboBoxImageFilterPreset.setButtonCell(new ComboBoxListCell<>());
        });

        tableViewImageFilters.setItems(DrawingBotV3.INSTANCE.currentFilters);
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
            row.setContextMenu(new ContextMenuObservableFilter(row));
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
                FXHelper.addImageFilter(comboBoxImageFilter.getValue());
            }
        });

        choiceBoxRotation.setItems(FXCollections.observableArrayList(EnumImageRotate.values()));
        choiceBoxRotation.setValue(EnumImageRotate.R0);
        choiceBoxRotation.valueProperty().bindBidirectional(DrawingBotV3.INSTANCE.imageRotation);

        checkBoxFlipX.setSelected(false);
        checkBoxFlipX.selectedProperty().bindBidirectional(DrawingBotV3.INSTANCE.imageFlipHorizontal);

        checkBoxFlipY.setSelected(false);
        checkBoxFlipY.selectedProperty().bindBidirectional(DrawingBotV3.INSTANCE.imageFlipVertical);

        DrawingBotV3.INSTANCE.imageRotation.addListener((observable, oldValue, newValue) -> DrawingBotV3.INSTANCE.onDrawingAreaChanged());
        DrawingBotV3.INSTANCE.imageFlipHorizontal.addListener((observable, oldValue, newValue) -> DrawingBotV3.INSTANCE.onDrawingAreaChanged());
        DrawingBotV3.INSTANCE.imageFlipVertical.addListener((observable, oldValue, newValue) -> DrawingBotV3.INSTANCE.onDrawingAreaChanged());
    }

    ///////////////////////////////////////////////////////////////////////////////////////////////////////
    ////PATH FINDING CONTROLS
    public ChoiceBox<PFMFactory<?>> choiceBoxPFM = null;

    public ComboBox<GenericPreset<PresetPFMSettings>> comboBoxPFMPreset = null;
    public MenuButton menuButtonPFMPresets = null;

    public TableView<GenericSetting<?,?>> tableViewAdvancedPFMSettings = null;
    public TableColumn<GenericSetting<?, ?>, Boolean> tableColumnLock = null;
    public TableColumn<GenericSetting<?, ?>, String> tableColumnSetting = null;
    public TableColumn<GenericSetting<?, ?>, Object> tableColumnValue = null;
    public TableColumn<GenericSetting<?, ?>, Object> tableColumnControl = null;

    public Button buttonPFMSettingReset = null;
    public Button buttonPFMSettingRandom = null;
    public Button buttonPFMSettingHelp = null;

    public ChoiceBox<EnumColourSplitter> choiceBoxColourSeperation = null;
    public Button buttonConfigureSplitter = null;

    public void initPFMControls(){

        ////PATH FINDING CONTROLS
        DrawingBotV3.INSTANCE.pfmFactory.bindBidirectional(choiceBoxPFM.valueProperty());
        choiceBoxPFM.setItems(MasterRegistry.INSTANCE.getObservablePFMLoaderList());
        choiceBoxPFM.setValue(MasterRegistry.INSTANCE.getDefaultPFM());
        choiceBoxPFM.valueProperty().addListener((observable, oldValue, newValue) -> changePathFinderModule(newValue));

        comboBoxPFMPreset.setItems(MasterRegistry.INSTANCE.getObservablePFMPresetList());
        comboBoxPFMPreset.setValue(MasterRegistry.INSTANCE.getDefaultPFMPreset());
        comboBoxPFMPreset.valueProperty().addListener((observable, oldValue, newValue) -> {
            if(newValue != null){
                JsonLoaderManager.PFM.applyPreset(newValue);
            }
        });

        FXHelper.setupPresetMenuButton(JsonLoaderManager.PFM, menuButtonPFMPresets, comboBoxPFMPreset::getValue, (preset) -> {
            comboBoxPFMPreset.setValue(preset);

            ///force update rendering
            comboBoxPFMPreset.setItems(MasterRegistry.INSTANCE.getObservablePFMPresetList());
            comboBoxPFMPreset.setButtonCell(new ComboBoxListCell<>());
        });

        DrawingBotV3.INSTANCE.pfmFactory.addListener((observable, oldValue, newValue) -> {
            comboBoxPFMPreset.setItems(MasterRegistry.INSTANCE.getObservablePFMPresetList(newValue));
            comboBoxPFMPreset.setValue(MasterRegistry.INSTANCE.getDefaultPFMPreset(newValue));
        });

        tableViewAdvancedPFMSettings.setItems(MasterRegistry.INSTANCE.getObservablePFMSettingsList());
        tableViewAdvancedPFMSettings.setRowFactory(param -> {
            TableRow<GenericSetting<?, ?>> row = new TableRow<>();
            row.setContextMenu(new ContextMenuPFMSetting(row));
            return row;
        });
        DrawingBotV3.INSTANCE.pfmFactory.addListener((observable, oldValue, newValue) -> tableViewAdvancedPFMSettings.setItems(MasterRegistry.INSTANCE.getObservablePFMSettingsList()));

        tableColumnLock.setCellFactory(param -> new CheckBoxTableCell<>(index -> tableColumnLock.getCellObservableValue(index)));
        tableColumnLock.setCellValueFactory(param -> param.getValue().lock);

        tableColumnSetting.setCellValueFactory(param -> param.getValue().settingName);

        tableColumnValue.setCellFactory(param -> {
            TextFieldTableCell<GenericSetting<?, ?>, Object> cell = new TextFieldTableCell<>();
            cell.setConverter(new StringConverterGenericSetting(() -> cell.tableViewProperty().get().getItems().get(cell.getIndex())));
            return cell;
        });
        tableColumnValue.setCellValueFactory(param -> (ObservableValue<Object>)param.getValue().value);

        tableColumnControl.setCellFactory(param -> new TableCellSettingControl());
        tableColumnControl.setCellValueFactory(param -> (ObservableValue<Object>)param.getValue().value);

        buttonPFMSettingReset.setOnAction(e -> JsonLoaderManager.PFM.applyPreset(comboBoxPFMPreset.getValue()));

        buttonPFMSettingRandom.setOnAction(e -> GenericSetting.randomiseSettings(tableViewAdvancedPFMSettings.getItems()));
        buttonPFMSettingHelp.setOnAction(e -> FXHelper.openURL(DBConstants.URL_READ_THE_DOCS_PFMS));


        DrawingBotV3.INSTANCE.colourSplitter.bindBidirectional(choiceBoxColourSeperation.valueProperty());
        choiceBoxColourSeperation.setItems(FXCollections.observableArrayList(EnumColourSplitter.values()));
        choiceBoxColourSeperation.setValue(EnumColourSplitter.DEFAULT);
        choiceBoxColourSeperation.setOnAction(e -> FXHelper.openColourSeperationDialog(choiceBoxColourSeperation.getValue()));

        buttonConfigureSplitter.setOnAction(e -> FXHelper.openColourSeperationConfigureDialog(choiceBoxColourSeperation.getValue()));
        buttonConfigureSplitter.disableProperty().bind(Bindings.createBooleanBinding(() -> DrawingBotV3.INSTANCE.colourSplitter.get() != EnumColourSplitter.CMYK, DrawingBotV3.INSTANCE.colourSplitter));
    }


    ///////////////////////////////////////////////////////////////////////////////////////////////////////
    ////PEN SETTINGS

    public ComboBox<String> comboBoxSetType = null;
    public ComboBox<IDrawingSet<IDrawingPen>> comboBoxDrawingSet = null;
    public MenuButton menuButtonDrawingSetPresets = null;

    public TableView<ObservableDrawingPen> penTableView = null;
    public TableColumn<ObservableDrawingPen, Boolean> penEnableColumn = null;
    public TableColumn<ObservableDrawingPen, String> penTypeColumn = null;
    public TableColumn<ObservableDrawingPen, String> penNameColumn = null;
    public TableColumn<ObservableDrawingPen, Color> penColourColumn = null;
    public TableColumn<ObservableDrawingPen, Float> penStrokeColumn = null;
    public TableColumn<ObservableDrawingPen, String> penPercentageColumn = null;
    public TableColumn<ObservableDrawingPen, Integer> penWeightColumn = null;
    public TableColumn<ObservableDrawingPen, Integer> penLinesColumn = null;

    public ComboBox<String> comboBoxPenType = null;
    public ComboBox<DrawingPen> comboBoxDrawingPen = null;
    public ComboBoxListViewSkin<DrawingPen> comboBoxDrawingPenSkin = null;
    public MenuButton menuButtonDrawingPenPresets = null;

    public Button buttonAddPen = null;
    public Button buttonRemovePen = null;
    public Button buttonDuplicatePen = null;
    public Button buttonMoveUpPen = null;
    public Button buttonMoveDownPen = null;

    public ComboBox<EnumDistributionType> comboBoxDistributionType = null;
    public ComboBox<EnumDistributionOrder> comboBoxDistributionOrder = null;
    public ComboBox<EnumBlendMode> comboBoxBlendMode = null;

    public void initPenSettingsPane(){

        ///////////////////////////////////////////////////////////////////////////////////////////////////////

        comboBoxSetType.setItems(FXCollections.observableArrayList(MasterRegistry.INSTANCE.registeredSets.keySet()));
        comboBoxSetType.setValue(MasterRegistry.INSTANCE.getDefaultSetType());
        comboBoxSetType.valueProperty().addListener((observable, oldValue, newValue) -> {
            comboBoxDrawingSet.setItems(MasterRegistry.INSTANCE.registeredSets.get(newValue));
            comboBoxDrawingSet.setValue(null);
        });

        comboBoxDrawingSet.setItems(MasterRegistry.INSTANCE.registeredSets.get(comboBoxSetType.getValue()));
        comboBoxDrawingSet.setValue(null);
        comboBoxDrawingSet.valueProperty().addListener((observable, oldValue, newValue) -> changeDrawingSet(newValue));
        comboBoxDrawingSet.setCellFactory(param -> new ComboCellDrawingSet());
        comboBoxDrawingSet.setButtonCell(new ComboCellDrawingSet());
        comboBoxDrawingSet.setPromptText("Select a Drawing Set");

        FXHelper.setupPresetMenuButton(JsonLoaderManager.DRAWING_SET, menuButtonDrawingSetPresets,
            () -> {
            if(comboBoxDrawingSet.getValue() instanceof PresetDrawingSet){
                PresetDrawingSet set = (PresetDrawingSet) comboBoxDrawingSet.getValue();
                return set.preset;
            }
            return null;
        }, (preset) -> {
            //force update rendering
            comboBoxSetType.setItems(FXCollections.observableArrayList(MasterRegistry.INSTANCE.registeredSets.keySet()));
            comboBoxDrawingSet.setItems(MasterRegistry.INSTANCE.registeredSets.get(comboBoxSetType.getValue()));
            comboBoxDrawingSet.setButtonCell(new ComboCellDrawingSet());
            if(preset != null){
                comboBoxSetType.setValue(preset.presetSubType);
                comboBoxDrawingSet.setValue(preset.data);
            }
            /*
            else{
                //don't set to avoid overwriting the users configured pens
                //comboBoxSetType.setValue(DrawingRegistry.INSTANCE.getDefaultSetType());
                //comboBoxDrawingSet.setValue(DrawingRegistry.INSTANCE.getDefaultSet(comboBoxSetType.getValue()));
            }
             */
        });

        ///////////////////////////////////////////////////////////////////////////////////////////////////////

        penTableView.setRowFactory(param -> {
            TableRow<ObservableDrawingPen> row = new TableRow<>();
            row.addEventFilter(ContextMenuEvent.CONTEXT_MENU_REQUESTED, event -> {
                if(row.getItem() == null){
                    event.consume();
                }
            });
            row.setContextMenu(new ContextMenuObservablePen(row));
            return row;
        });


        penTableView.setItems(DrawingBotV3.INSTANCE.observableDrawingSet.pens);
        penTableView.getSelectionModel().selectedItemProperty().addListener((observable, oldValue, newValue) -> {
            if(DrawingBotV3.INSTANCE.display_mode.get() == EnumDisplayMode.SELECTED_PEN){
                DrawingBotV3.INSTANCE.reRender();
            }
        });

        ///////////////////////////////////////////////////////////////////////////////////////////////////////

        penNameColumn.setCellFactory(param -> new TextFieldTableCell<>(new DefaultStringConverter()));
        penNameColumn.setCellValueFactory(param -> param.getValue().name);

        penTypeColumn.setCellFactory(param -> new TextFieldTableCell<>(new DefaultStringConverter()));
        penTypeColumn.setCellValueFactory(param -> param.getValue().type);

        penColourColumn.setCellFactory(TableCellColorPicker::new);
        penColourColumn.setCellValueFactory(param -> param.getValue().javaFXColour);

        penStrokeColumn.setCellFactory(param -> new TextFieldTableCell<>(new FloatStringConverter()));
        penStrokeColumn.setCellValueFactory(param -> param.getValue().strokeSize.asObject());

        penEnableColumn.setCellFactory(param -> new CheckBoxTableCell<>(index -> penEnableColumn.getCellObservableValue(index)));
        penEnableColumn.setCellValueFactory(param -> param.getValue().enable);

        penPercentageColumn.setCellValueFactory(param -> param.getValue().currentPercentage);

        penWeightColumn.setCellFactory(param -> new TextFieldTableCell<>(new IntegerStringConverter()));
        penWeightColumn.setCellValueFactory(param -> param.getValue().distributionWeight.asObject());

        penLinesColumn.setCellValueFactory(param -> param.getValue().currentGeometries.asObject());

        ///////////////////////////////////////////////////////////////////////////////////////////////////////

        comboBoxPenType.setItems(FXCollections.observableArrayList(MasterRegistry.INSTANCE.registeredPens.keySet()));
        comboBoxPenType.setValue(MasterRegistry.INSTANCE.getDefaultPenType());

        comboBoxPenType.valueProperty().addListener((observable, oldValue, newValue) -> {
            comboBoxDrawingPen.setItems(MasterRegistry.INSTANCE.registeredPens.get(newValue));
            comboBoxDrawingPen.setValue(MasterRegistry.INSTANCE.getDefaultPen(newValue));
        });

        comboBoxDrawingPenSkin = new ComboBoxListViewSkin<>(comboBoxDrawingPen);
        comboBoxDrawingPenSkin.hideOnClickProperty().set(false);
        comboBoxDrawingPen.setSkin(comboBoxDrawingPenSkin);

        comboBoxDrawingPen.setItems(MasterRegistry.INSTANCE.registeredPens.get(comboBoxPenType.getValue()));
        comboBoxDrawingPen.setValue(MasterRegistry.INSTANCE.getDefaultPen(comboBoxPenType.getValue()));
        comboBoxDrawingPen.setCellFactory(param -> new ComboCellDrawingPen(true));
        comboBoxDrawingPen.setButtonCell(new ComboCellDrawingPen(false));

        FXHelper.setupPresetMenuButton(JsonLoaderManager.DRAWING_PENS, menuButtonDrawingPenPresets,
            () -> {
                if(comboBoxDrawingPen.getValue() instanceof PresetDrawingPen){
                    PresetDrawingPen set = (PresetDrawingPen) comboBoxDrawingPen.getValue();
                    return set.preset;
                }
                return null;
            }, (preset) -> {
                //force update rendering
                comboBoxPenType.setItems(FXCollections.observableArrayList(MasterRegistry.INSTANCE.registeredPens.keySet()));
                comboBoxDrawingPen.setItems(MasterRegistry.INSTANCE.registeredPens.get(comboBoxPenType.getValue()));
                comboBoxDrawingPen.setButtonCell(new ComboCellDrawingPen(false));

                if(preset != null){
                    comboBoxPenType.setValue(preset.presetSubType);
                    comboBoxDrawingPen.setValue(preset.data);
                }else{
                    comboBoxPenType.setValue(MasterRegistry.INSTANCE.getDefaultPenType());
                    comboBoxDrawingPen.setValue(MasterRegistry.INSTANCE.getDefaultPen(comboBoxPenType.getValue()));
                }
            });

        ///////////////////////////////////////////////////////////////////////////////////////////////////////

        buttonAddPen.setOnAction(e -> DrawingBotV3.INSTANCE.observableDrawingSet.addNewPen(comboBoxDrawingPen.getValue()));
        buttonRemovePen.setOnAction(e -> FXHelper.deleteItem(penTableView.getSelectionModel().getSelectedItem(), DrawingBotV3.INSTANCE.observableDrawingSet.pens));
        buttonDuplicatePen.setOnAction(e -> {
            ObservableDrawingPen pen = penTableView.getSelectionModel().getSelectedItem();
            if(pen != null)
                DrawingBotV3.INSTANCE.observableDrawingSet.addNewPen(pen);
        });
        buttonMoveUpPen.setOnAction(e -> FXHelper.moveItemUp(penTableView.getSelectionModel().getSelectedItem(), DrawingBotV3.INSTANCE.observableDrawingSet.pens));
        buttonMoveDownPen.setOnAction(e -> FXHelper.moveItemDown(penTableView.getSelectionModel().getSelectedItem(), DrawingBotV3.INSTANCE.observableDrawingSet.pens));
        buttonMoveDownPen.setOnAction(e -> FXHelper.moveItemDown(penTableView.getSelectionModel().getSelectedItem(), DrawingBotV3.INSTANCE.observableDrawingSet.pens));

        ///////////////////////////////////////////////////////////////////////////////////////////////////////

        comboBoxDistributionOrder.setItems(FXCollections.observableArrayList(EnumDistributionOrder.values()));
        comboBoxDistributionOrder.valueProperty().bindBidirectional(DrawingBotV3.INSTANCE.observableDrawingSet.distributionOrder);

        comboBoxDistributionType.setItems(FXCollections.observableArrayList(EnumDistributionType.values()));
        comboBoxDistributionType.valueProperty().bindBidirectional(DrawingBotV3.INSTANCE.observableDrawingSet.distributionType);

        comboBoxBlendMode.setItems(FXCollections.observableArrayList(EnumBlendMode.values()));
        comboBoxBlendMode.valueProperty().bindBidirectional(DrawingBotV3.INSTANCE.observableDrawingSet.blendMode);


    }

    ///////////////////////////////////////////////////////////////////////////////////////////////////////
    ////VERSION CONTROL

    public TableView<ObservableProjectSettings> tableViewVersions = null;
    public TableColumn<ObservableProjectSettings, ImageView> versionThumbColumn = null;
    public TableColumn<ObservableProjectSettings, String> versionNameColumn = null;
    public TableColumn<ObservableProjectSettings, String> versionDateColumn = null;

    public TableColumn<ObservableProjectSettings, String> versionPFMColumn = null;
    public TableColumn<ObservableProjectSettings, String> versionFileColumn = null;


    public Button buttonAddVersion = null;
    public Button buttonDeleteVersion = null;
    public Button buttonMoveUpVersion = null;
    public Button buttonMoveDownVersion = null;

    public void initVersionControlPane() {
        tableViewVersions.setRowFactory(param -> {
            TableRow<ObservableProjectSettings> row = new TableRow<>();
            row.addEventFilter(ContextMenuEvent.CONTEXT_MENU_REQUESTED, event -> {
                if(row.getItem() == null){
                    event.consume();
                }
            });
            row.setContextMenu(new ContextMenuObservableProjectSettings(row));
            return row;
        });

        tableViewVersions.setItems(DrawingBotV3.INSTANCE.projectVersions);

        versionThumbColumn.setCellValueFactory(param -> param.getValue().imageView);

        versionNameColumn.setCellFactory(param -> new TextFieldTableCell<>(new DefaultStringConverter()));
        versionNameColumn.setCellValueFactory(param -> param.getValue().userDefinedName);

        versionDateColumn.setCellFactory(param -> new TextFieldTableCell<>(new DefaultStringConverter()));
        versionDateColumn.setCellValueFactory(param -> param.getValue().date);
        versionDateColumn.setEditable(false);

        versionFileColumn.setCellFactory(param -> new TextFieldTableCell<>(new DefaultStringConverter()));
        versionFileColumn.setCellValueFactory(param -> param.getValue().file);
        versionFileColumn.setEditable(false);

        versionPFMColumn.setCellFactory(param -> new TextFieldTableCell<>(new DefaultStringConverter()));
        versionPFMColumn.setCellValueFactory(param -> param.getValue().pfm);
        versionPFMColumn.setEditable(false);

        buttonAddVersion.setOnAction(e -> saveVersion());
        buttonDeleteVersion.setOnAction(e -> FXHelper.deleteItem(tableViewVersions.getSelectionModel().getSelectedItem(), DrawingBotV3.INSTANCE.projectVersions));
        buttonMoveUpVersion.setOnAction(e -> FXHelper.moveItemUp(tableViewVersions.getSelectionModel().getSelectedItem(), DrawingBotV3.INSTANCE.projectVersions));
        buttonMoveDownVersion.setOnAction(e -> FXHelper.moveItemDown(tableViewVersions.getSelectionModel().getSelectedItem(), DrawingBotV3.INSTANCE.projectVersions));
    }

    ///////////////////////////////////////////////////////////////////////////////////////////////////////
    ////BATCH PROCESSING

    public Label labelInputFolder = null;
    public Label labelOutputFolder = null;

    public Button buttonSelectInputFolder = null;
    public Button buttonSelectOutputFolder = null;
    public Button buttonStartBatchProcessing = null;
    public Button buttonStopBatchProcessing = null;

    public CheckBox checkBoxOverwrite = null;

    public TableView<BatchProcessing.BatchExportTask> tableViewBatchExport = null;
    public TableColumn<BatchProcessing.BatchExportTask, String> tableColumnFileFormat = null;
    public TableColumn<BatchProcessing.BatchExportTask, Boolean> tableColumnPerDrawing = null;
    public TableColumn<BatchProcessing.BatchExportTask, Boolean> tableColumnPerPen = null;

    public void initBatchProcessingPane(){

        labelInputFolder.textProperty().bindBidirectional(BatchProcessing.inputFolder);
        labelOutputFolder.textProperty().bindBidirectional(BatchProcessing.outputFolder);

        buttonSelectInputFolder.setOnAction(e -> BatchProcessing.selectFolder(true));
        buttonSelectInputFolder.disableProperty().bind(BatchProcessing.isBatchProcessing);

        buttonSelectOutputFolder.setOnAction(e -> BatchProcessing.selectFolder(false));
        buttonSelectOutputFolder.disableProperty().bind(BatchProcessing.isBatchProcessing);

        buttonStartBatchProcessing.setOnAction(e -> BatchProcessing.startProcessing());
        buttonStartBatchProcessing.disableProperty().bind(BatchProcessing.isBatchProcessing);

        buttonStopBatchProcessing.setOnAction(e -> BatchProcessing.finishProcessing());
        buttonStopBatchProcessing.disableProperty().bind(BatchProcessing.isBatchProcessing.not());

        checkBoxOverwrite.selectedProperty().bindBidirectional(BatchProcessing.overwriteExistingFiles);
        checkBoxOverwrite.disableProperty().bind(BatchProcessing.isBatchProcessing);

        tableViewBatchExport.setItems(BatchProcessing.exportTasks);
        tableViewBatchExport.disableProperty().bind(BatchProcessing.isBatchProcessing);

        tableColumnFileFormat.setCellValueFactory(task -> new SimpleStringProperty(task.getValue().formatName()));

        tableColumnPerDrawing.setCellFactory(param -> new CheckBoxTableCell<>(index -> tableColumnPerDrawing.getCellObservableValue(index)));
        tableColumnPerDrawing.setCellValueFactory(param -> param.getValue().enablePerDrawing);

        tableColumnPerPen.setCellFactory(param -> new CheckBoxTableCell<>(index -> tableColumnPerPen.getCellObservableValue(index)));
        tableColumnPerPen.setCellValueFactory(param -> param.getValue().enablePerPen);
    }
    ///////////////////////////////////////////////////////////////////////////////////////////////////////

    public static void changePathFinderModule(PFMFactory<?> pfm){
        DrawingBotV3.INSTANCE.pfmFactory.set(pfm);
        DrawingBotV3.INSTANCE.updateDistributionType = pfm.getDistributionType();
    }

    public static void changeDrawingSet(IDrawingSet<IDrawingPen> set){
        if(set != null){
            DrawingBotV3.INSTANCE.observableDrawingSet.loadDrawingSet(set);
            if(set instanceof EnumColourSplitter.ColourSplitterDrawingSet){
                EnumColourSplitter.ColourSplitterDrawingSet splitterDrawingSet = (EnumColourSplitter.ColourSplitterDrawingSet) set;
                DrawingBotV3.INSTANCE.colourSplitter.set(splitterDrawingSet.splitter);
            }
        }
    }

    public ObservableDrawingPen getSelectedPen(){
        return penTableView.getSelectionModel().getSelectedItem();
    }

    //// PRESET MENU BUTTON \\\\


}
