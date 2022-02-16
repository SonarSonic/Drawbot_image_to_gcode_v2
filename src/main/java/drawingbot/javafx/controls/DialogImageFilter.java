package drawingbot.javafx.controls;

import drawingbot.DrawingBotV3;
import drawingbot.FXApplication;

import drawingbot.javafx.FXHelper;
import drawingbot.javafx.observables.ObservableImageFilter;
import drawingbot.javafx.GenericSetting;
import drawingbot.javafx.settings.RangedNumberSetting;
import javafx.event.ActionEvent;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

public class DialogImageFilter extends Dialog<ObservableImageFilter> {

    public DialogImageFilter(ObservableImageFilter filter) {
        super();
        ObservableImageFilter original = new ObservableImageFilter(filter);


        VBox hBox = new VBox();
        GridPane gridPane = FXHelper.createSettingsGridPane(filter.filterSettings, s -> DrawingBotV3.INSTANCE.onImageFilterChanged(filter));
        CheckBox checkBox = new CheckBox("Enabled");
        checkBox.selectedProperty().bindBidirectional(filter.enable);
        checkBox.setPadding(new Insets(8, 0, 16, 0));
        hBox.getChildren().add(checkBox);
        hBox.getChildren().add(gridPane);
        setGraphic(hBox);
        setTitle("Image Filter: " + filter.name.getValue());
        getDialogPane().setPrefWidth(400);
        setResultConverter(param -> {
            if(param == ButtonType.APPLY){
                return filter;
            }else{
                original.dirty.set(true);
                return original;
            }
        });

        getDialogPane().getButtonTypes().add(ButtonType.CANCEL);
        getDialogPane().getButtonTypes().add(FXHelper.buttonResetToDefault);
        getDialogPane().lookupButton(FXHelper.buttonResetToDefault).addEventFilter(
                ActionEvent.ACTION,
                event -> {
                    filter.filterSettings.forEach(GenericSetting::resetSetting);
                    event.consume();
                }
        );
        getDialogPane().getButtonTypes().add(ButtonType.APPLY);
        FXApplication.applyDBIcon((Stage)getDialogPane().getScene().getWindow());

    }

}
