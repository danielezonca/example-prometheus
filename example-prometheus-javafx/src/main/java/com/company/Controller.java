package com.company;

import java.util.Optional;

import javafx.event.ActionEvent;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;

import static java.util.Optional.of;

public class Controller {

    public Label label;
    public TextArea parallelism;
    private Optional<ApplicationClient> applicationClient = Optional.empty();

    public void startService(ActionEvent actionEvent) throws Exception {
        int numberOfThreads = Integer.parseInt(parallelism.getText());
        applicationClient.ifPresent(ApplicationClient::stop);
        ApplicationClient reference = new ApplicationClient(numberOfThreads, this);
        reference.run();
        applicationClient = of(reference);
        label.setText("Started service");
    }

    public void stopService(ActionEvent actionEvent) {
        applicationClient.ifPresent(ApplicationClient::stop);
        label.setText("Stopped service");
    }
}
