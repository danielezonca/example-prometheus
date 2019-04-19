package com.company;


import javafx.event.ActionEvent;
import javafx.scene.control.Label;

public class Controller {

    public Label helloWorld;

    public void sayHelloWorld(ActionEvent actionEvent) throws Exception {
        new ApplicationClient(1, this).run();
    }

}
