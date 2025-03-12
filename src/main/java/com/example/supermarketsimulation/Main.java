package com.example.supermarketsimulation;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;
import java.io.IOException;

public class Main extends Application {

    private static Stage primaryStage;
    private static SupermarketController controller;

    @Override
    public void start(Stage stage) throws IOException {
        primaryStage = stage;
        FXMLLoader fxmlLoader = new FXMLLoader(Main.class.getResource("supermarket_layout.fxml"));
        Scene scene = new Scene(fxmlLoader.load(), 300, 250);
        controller = fxmlLoader.getController();
        primaryStage.setTitle("Supermarket Simulation");
        primaryStage.setScene(scene);
        // Zamykanie okna
        primaryStage.setOnCloseRequest(event -> {
            controller.stopSimulation();
        });
        primaryStage.show();
    }

    public static Stage getPrimaryStage() {
        return primaryStage;
    }

    public static void main(String[] args) {
        launch();
    }
}