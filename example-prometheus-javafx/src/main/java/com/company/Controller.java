package com.company;

import java.net.URL;
import java.util.Optional;
import java.util.ResourceBundle;

import javafx.event.ActionEvent;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.Background;
import javafx.scene.layout.BackgroundFill;
import javafx.scene.layout.CornerRadii;
import javafx.scene.layout.GridPane;
import javafx.scene.paint.Color;

import static java.util.Optional.of;

public class Controller implements Initializable  {

    public Label label;
    public TextArea parallelism;
    public ImageView spinner;
    public GridPane gridpane;
    public Label label2;
    private Optional<ApplicationClient> applicationClient = Optional.empty();

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        spinner.setImage(new Image("spinner.gif"));
        gridpane.setBackground(new Background(new BackgroundFill(Color.WHITE, CornerRadii.EMPTY, Insets.EMPTY)));
        parallelism.setText("1");
    }

    public void startService(ActionEvent actionEvent) throws Exception {
        int numberOfThreads;
        try {
            numberOfThreads = Integer.parseInt(parallelism.getText());
        } catch (NumberFormatException e) {
            numberOfThreads = 1;
            parallelism.setText("1");
        }
        applicationClient.ifPresent(ApplicationClient::stop);
        ApplicationClient reference = new ApplicationClient(numberOfThreads, this);
        reference.run();
        applicationClient = of(reference);
        String threadLabel = numberOfThreads == 1 ? "thread" : " threads";
        label.setText("Client started: " + numberOfThreads + " "+ threadLabel);
        spinner.setVisible(true);
    }

    public void stopService(ActionEvent actionEvent) {
        applicationClient.ifPresent(ApplicationClient::stop);
        label.setText("Client stopped");
        spinner.setVisible(false);
        label2.setText("0 request sent");
    }
}
