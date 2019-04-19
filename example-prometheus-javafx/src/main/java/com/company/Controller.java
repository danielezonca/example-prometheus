package com.company;


import javafx.event.ActionEvent;
import javafx.scene.control.Label;

public class Controller {

    public Label label;
    private ApplicationClient applicationClient;

    public void startService(ActionEvent actionEvent) throws Exception {
        applicationClient = new ApplicationClient(1, this);
        applicationClient.run();
        label.setText("Started service");
    }

    public void stopService(ActionEvent actionEvent) {
        applicationClient.stop();
        label.setText("Stopped service");

    }
}
