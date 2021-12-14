package drawingbot.integrations.vpype;

import drawingbot.DrawingBotV3;
import drawingbot.files.ConfigFileHandler;
import drawingbot.files.presets.JsonLoaderManager;
import drawingbot.javafx.FXHelper;
import drawingbot.javafx.GenericPreset;
import javafx.scene.control.*;
import javafx.scene.control.cell.ComboBoxListCell;

import java.io.File;
import java.nio.file.Files;

public class FXVPypeController {


    public void initialize(){
        initVPypeSettingsPane();

        DrawingBotV3.INSTANCE.controller.vpypeSettingsStage.setOnHidden(e -> ConfigFileHandler.getApplicationSettings().markDirty());
    }


    ///////////////////////////////////////////////////////////////////////////////////////////////////////
    ////VPYPE OPTIMISATION

    public ComboBox<GenericPreset<PresetVpypeSettings>> comboBoxVPypePreset = null;
    public MenuButton menuButtonVPypePresets = null;
    public TextArea textAreaVPypeCommand = null;
    public CheckBox checkBoxBypassPathOptimisation = null;
    public TextField textBoxVPypeExecutablePath = null;
    public Button buttonVPypeExecutablePath = null;

    public Label labelWildcard = null;
    public Button buttonCancel = null;
    public Button buttonSendCommand = null;


    public void initVPypeSettingsPane(){

        comboBoxVPypePreset.setItems(JsonLoaderManager.VPYPE_SETTINGS.presets);
        comboBoxVPypePreset.valueProperty().addListener((observable, oldValue, newValue) -> {
            if(newValue != null){
                JsonLoaderManager.VPYPE_SETTINGS.applyPreset(newValue);
                if(!ConfigFileHandler.getApplicationSettings().vPypePresetName.equals(newValue.presetName)){
                    ConfigFileHandler.getApplicationSettings().vPypePresetName = newValue.presetName;
                    ConfigFileHandler.getApplicationSettings().markDirty();
                }
            }
        });
        comboBoxVPypePreset.setValue(PresetVpypeSettingsLoader.getPresetOrDefault(ConfigFileHandler.getApplicationSettings().vPypePresetName));

        FXHelper.setupPresetMenuButton(JsonLoaderManager.VPYPE_SETTINGS, menuButtonVPypePresets, false, comboBoxVPypePreset::getValue, (preset) -> {
            comboBoxVPypePreset.setValue(preset);

            ///force update rendering
            comboBoxVPypePreset.setItems(JsonLoaderManager.VPYPE_SETTINGS.presets);
            comboBoxVPypePreset.setButtonCell(new ComboBoxListCell<>());
        });

        textAreaVPypeCommand.textProperty().bindBidirectional(DrawingBotV3.INSTANCE.vPypeCommand);

        labelWildcard.setText(VpypeHelper.OUTPUT_FILE_WILDCARD);

        checkBoxBypassPathOptimisation.selectedProperty().bindBidirectional(DrawingBotV3.INSTANCE.vPypeBypassOptimisation);

        textBoxVPypeExecutablePath.textProperty().bindBidirectional(DrawingBotV3.INSTANCE.vPypeExecutable);
        textBoxVPypeExecutablePath.setText(ConfigFileHandler.getApplicationSettings().pathToVPypeExecutable);
        textBoxVPypeExecutablePath.textProperty().addListener((observable, oldValue, newValue) -> {
            ConfigFileHandler.getApplicationSettings().pathToVPypeExecutable = textBoxVPypeExecutablePath.getText();
            ConfigFileHandler.getApplicationSettings().markDirty();
        });


        buttonVPypeExecutablePath.setOnAction(e -> VpypeHelper.choosePathToExecutable());

        buttonCancel.setOnAction(e -> DrawingBotV3.INSTANCE.controller.vpypeSettingsStage.hide());
        buttonSendCommand.setOnAction(e -> {
            if(DrawingBotV3.INSTANCE.vPypeExecutable.getValue().isEmpty() || Files.notExists(new File(DrawingBotV3.INSTANCE.vPypeExecutable.getValue()).toPath())){
                textBoxVPypeExecutablePath.requestFocus();
            }else{
                DrawingBotV3.INSTANCE.controller.vpypeSettingsStage.hide();
                VpypeHelper.exportToVpype();
            }
        });
    }
}
