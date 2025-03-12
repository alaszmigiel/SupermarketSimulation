package com.example.supermarketsimulation;

import javafx.animation.TranslateTransition;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.TextField;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.layout.AnchorPane;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import javafx.scene.shape.Rectangle;
import javafx.scene.shape.Circle;
import javafx.scene.paint.Color;
import javafx.scene.text.Text;
import javafx.util.Duration;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.*;
import java.util.concurrent.ThreadLocalRandom;

public class SupermarketController {
    @FXML
    private TextField clientCountField;
    @FXML
    private TextField cashRegisterCountField;
    @FXML
    private AnchorPane rootPane;
    @FXML
    private Button startButton;

    private int m; // Liczba klientów
    private int n; // Liczba kas
    private int clientMinTime; // Minimalny czas przybycia klienta
    private int clientMaxTime; // Maksymalny czas przybycia klienta
    private int cashierMinTime; // Minimalny czas obsługi przez kasjera
    private int cashierMaxTime; // Maksymalny czas obsługi przez kasjera
    private int allCashiers; // Całkowita liczba kasjerów
    private int simulationTime; // Czas trwania symulacji
    private CashRegistersMonitor monitor;
    private List<Thread> clientThreads; // Lista wątków klientów
    private List<Thread> cashierThreads; // Lista wątków kasjerów
    private List<Cashier> cashiers; // Lista kasjerów
    private Map<Integer, Rectangle> cashRegisterRectangles; // Prostokąty reprezentujące kasy
    private Map<Integer, Circle> clientCircles; // Koła reprezentujące klientów
    private Map<Integer, Circle> cashierCircles; // Koła reprezentujące kasjerów
    private Thread clientCreationThread; // Wątek do tworzenia klientów

    @FXML
    public void initialize() {
        clientCountField.textProperty().addListener((observable, oldValue, newValue) -> validateInputs());
        cashRegisterCountField.textProperty().addListener((observable, oldValue, newValue) -> validateInputs());
        startButton.setDisable(true); // Początkowo przycisk startu wyłączony
    }

    // Sprawdzanie danych wejściowych
    private void validateInputs() {
        try {
            int clients = Integer.parseInt(clientCountField.getText());
            int registers = Integer.parseInt(cashRegisterCountField.getText());
            startButton.setDisable(clients < 20 || clients > 100 || registers < 2 || registers > 5);
        } catch (NumberFormatException e) {
            startButton.setDisable(true);
        }
    }

    // Wybór pliku
    @FXML
    private void loadDefaults() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("Properties Files", "*.properties"),
                new FileChooser.ExtensionFilter("XML Files", "*.xml")
        );
        fileChooser.setTitle("Wybierz plik");
        File file = fileChooser.showOpenDialog(Main.getPrimaryStage());
        if (file != null) {
            try {
                if (file.getName().endsWith(".properties")) {
                    loadPropertiesFile(file);
                } else if (file.getName().endsWith(".xml")) {
                    loadXmlFile(file);
                }
            } catch (IOException e) {
                showAlert("Błąd podczas wczytywania pliku: " + e.getMessage());
            }
        }
    }

    // Wstawianie danych z pliku .properties
    private void loadPropertiesFile(File file) throws IOException {
        Properties properties = new Properties();
        try (InputStream inputStream = new FileInputStream(file)) {
            properties.load(inputStream);
            String clients = properties.getProperty("clients");
            String registers = properties.getProperty("registers");
            if (clients != null && registers != null) {
                clientCountField.setText(clients);
                cashRegisterCountField.setText(registers);
            } else {
                showAlert("Plik properties nie zawiera poprawnych danych.");
            }
        }
    }

    // Wstawianie danych z pliku .xml
    private void loadXmlFile(File file) throws IOException {
        Properties properties = new Properties();
        try (InputStream inputStream = new FileInputStream(file)) {
            properties.loadFromXML(inputStream);
            String clients = properties.getProperty("clients");
            String registers = properties.getProperty("registers");
            if (clients != null && registers != null) {
                clientCountField.setText(clients);
                cashRegisterCountField.setText(registers);
            } else {
                showAlert("Plik XML nie zawiera poprawnych danych.");
            }
        }
    }

    @FXML
    public void startSimulation() {
        try {
            m = Integer.parseInt(clientCountField.getText());
            n = Integer.parseInt(cashRegisterCountField.getText());

            clientMinTime = 900;
            clientMaxTime = 3000;
            cashierMinTime = 5000;
            cashierMaxTime = 6000;
            allCashiers = 0;
            simulationTime = 2;

            monitor = new CashRegistersMonitor(n, m, this);
            clientThreads = new ArrayList<>();
            cashierThreads = new ArrayList<>();
            cashiers = new ArrayList<>();
            cashRegisterRectangles = new HashMap<>();
            clientCircles = new HashMap<>();
            cashierCircles = new HashMap<>();

            Platform.runLater(this::enlargeWindowAndClear);

            for (int i = 0; i < n; i++) {
                createCashRegisterRectangle(i + 1);
                updateCashRegisterColor(i + 1, Color.GREEN);
                createCashierCircle(i + 1);
            }

            // Tworzenie kasjerów
            for (int i = 0; i < n; i++) {
                Cashier cashier = new Cashier(i + 1, i + 1, monitor, cashierMinTime, cashierMaxTime, this);
                Thread cashierThread = new Thread(cashier);
                cashiers.add(cashier);
                allCashiers++;
                cashierThreads.add(cashierThread);
                cashierThread.start();
            }

            // Wątek do tworzenia klientów
            clientCreationThread = new Thread(() -> {
                try {
                    // Tworzenie klientów
                    for (int i = 0; i < m; i++) {
                        Thread.sleep(ThreadLocalRandom.current().nextInt(clientMinTime, clientMaxTime));
                        Client client = new Client(i + 1, monitor);
                        Thread clientThread = new Thread(client);
                        clientThreads.add(clientThread);
                        clientThread.start();
                    }

                    for (Thread thread : clientThreads) {
                        try {
                            thread.join();
                        } catch (InterruptedException e) {
                            Thread.currentThread().interrupt();
                        }
                    }

                    for (Thread thread : cashierThreads) {
                        try {
                            thread.join();
                        } catch (InterruptedException e) {
                            Thread.currentThread().interrupt();
                        }
                    }

                    Platform.runLater(() -> {
                        showAlert("Wszyscy klienci zostali obsłużeni. Program kończy działanie.\nŁączna liczba kasjerów: " + allCashiers);
                    });

                    System.out.println("Wszyscy klienci zostali obsłużeni. Program kończy działanie.");

                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            });
            clientCreationThread.start();

        } catch (NumberFormatException e) {
            showAlert("Nieprawidłowe dane.");
        }
    }

    // Stop symulacji
    public void stopSimulation() {
        if (clientCreationThread != null && clientCreationThread.isAlive()) {
            clientCreationThread.interrupt();
        }
        if (clientThreads != null) {
            for (Thread clientThread : clientThreads) {
                if (clientThread.isAlive()) {
                    clientThread.interrupt();
                }
            }
        }
        if (cashierThreads != null) {
            for (Thread cashierThread : cashierThreads) {
                if (cashierThread.isAlive()) {
                    cashierThread.interrupt();
                }
            }
        }
        Platform.exit();
        System.exit(0);
    }

    // Powiększenie i wyczyszczenie GUI
    private void enlargeWindowAndClear() {
        Stage stage = Main.getPrimaryStage();
        stage.setWidth(800);
        stage.setHeight(600);
        rootPane.getChildren().clear();
    }

    // Tworzenie prostokątów kas
    private void createCashRegisterRectangle(int registerId) {
        double rectangleWidth = 60;
        double rectangleHeight = 30;
        double spacing = 10;
        double xOffset = 10;
        double yOffset = 30;

        Rectangle rectangle = new Rectangle(rectangleWidth, rectangleHeight, Color.LIGHTGRAY);
        rectangle.setStroke(Color.BLACK);
        double x = xOffset + (rectangleWidth + spacing) * (registerId - 1);
        rectangle.setX(x);
        rectangle.setY(yOffset);

        Text text = new Text("Kasa " + registerId);
        text.setX(x + 12);
        text.setY(yOffset + 20);

        Platform.runLater(() -> rootPane.getChildren().addAll(rectangle, text));

        cashRegisterRectangles.put(registerId, rectangle);
    }

    // Tworzenie kółek kasjerów
    private void createCashierCircle(int registerId) {
        double circleRadius = 10;
        double xOffset = 20;
        double yOffset = 15;

        Rectangle rectangle = cashRegisterRectangles.get(registerId);
        double x = rectangle.getX() + xOffset;
        double y = rectangle.getY() - yOffset;

        Circle circle = new Circle(circleRadius, Color.GRAY);
        circle.setStroke(Color.BLACK);
        circle.setCenterX(x);
        circle.setCenterY(y);

        Platform.runLater(() -> rootPane.getChildren().add(circle));
        cashierCircles.put(registerId, circle);
    }

    // Usuwanie kółek kasjerów
    public void removeCashierCircle(int registerId) {
        Platform.runLater(() -> {
            Circle circle = cashierCircles.remove(registerId);
            if (circle != null) {
                rootPane.getChildren().remove(circle);
            }
        });
    }

    // Zmiana koloru prostokątów kas
    public void updateCashRegisterColor(int registerId, Color color) {
        Platform.runLater(() -> {
            Rectangle rectangle = cashRegisterRectangles.get(registerId);
            if (rectangle != null) {
                rectangle.setFill(color);
            }
        });
    }

    // Tworzenie kółek klientów
    public void createClientCircle(int clientId) {
        Platform.runLater(() -> {
            double circleRadius = 10;
            double xOffset = rootPane.getWidth() / 2;
            double yOffset = rootPane.getHeight() - circleRadius - 100;

            Circle circle = new Circle(circleRadius, Color.BLACK);
            circle.setStroke(Color.BLACK);
            circle.setCenterX(xOffset);
            circle.setCenterY(yOffset);

            rootPane.getChildren().add(circle);
            clientCircles.put(clientId, circle);
        });
    }

    // Przesuwanie klientów w kolejce
    public void shiftClientCirclesUp(int registerId) {
        List<Integer> queueClientIds = monitor.getQueueClientIds(registerId);
        for (int i = 0; i < queueClientIds.size(); i++) {
            int clientId = queueClientIds.get(i);
            moveClientToCashRegister(clientId, registerId);
        }
    }

    // Przesuwanie klientów do kolejki
    public void moveClientToCashRegister(int clientId, int registerId) {
        Platform.runLater(() -> {
            Circle circle = clientCircles.get(clientId);
            if (circle != null) {
                Rectangle rectangle = cashRegisterRectangles.get(registerId);
                if (rectangle != null) {
                    List<Integer> queueClientIds = monitor.getQueueClientIds(registerId);
                    int index = queueClientIds.indexOf(clientId);

                    if (index != -1) {
                        double targetX = rectangle.getX() + rectangle.getWidth() / 2 - circle.getCenterX();
                        double targetY = rectangle.getY() + rectangle.getHeight() + circle.getRadius() * (3 * index + 0.5) - circle.getCenterY();

                        TranslateTransition transition = new TranslateTransition(Duration.seconds(simulationTime), circle);
                        transition.setToX(targetX);
                        transition.setToY(targetY);
                        transition.play();
                    }
                }
            }
        });
    }

    // Usuwanie kólek klientów
    public void removeClientCircle(int clientId) {
        Platform.runLater(() -> {
            Circle circle = clientCircles.remove(clientId);
            if (circle != null) {
                rootPane.getChildren().remove(circle);
            }
        });
    }

    // Zmiana koloru kółek klientów
    public void updateClientCircleColor(int clientId, Color color) {
        Platform.runLater(() -> {
            Circle circle = clientCircles.get(clientId);
            if (circle != null) {
                circle.setFill(color);
            }
        });
    }

    // Zmiana kasjera
    public void replaceCashier(int registerId) {
        System.out.printf("Zmiana kasjera przy kasie %d.\n", registerId);

        List<Integer> indexesToRemove = new ArrayList<>(); // Lista kasjerów do usunięcia

        for (int i = 0; i < cashierThreads.size(); i++) {
            Cashier cashier = cashiers.get(i);
            if (cashier.getRegisterId() == registerId && cashierThreads.get(i).isAlive()) {
                cashierThreads.get(i).interrupt(); // Przerwanie wątku kasjera
                indexesToRemove.add(i);
                System.out.printf("Zakończono wątek kasjera %d przy kasie %d.\n", cashier.getCashierId(), registerId);
            }
        }

        // Usunięcie wątków kasjerów z listy
        for (int index : indexesToRemove) {
            cashierThreads.remove(index);
            cashiers.remove(index);
        }

        // Tworzenie nowego kasjera
        Cashier newCashier = new Cashier(registerId, registerId, monitor, cashierMinTime, cashierMaxTime, this);
        Thread newCashierThread = new Thread(newCashier);
        cashiers.add(newCashier);
        cashierThreads.add(newCashierThread);
        allCashiers++;
        createCashierCircle(registerId);
        newCashierThread.start();
        System.out.printf("Do kasy %d przyszedł nowy kasjer.\n", registerId);

        monitor.openRegister(registerId);
        System.out.printf("Kasa %d została otwarta przez nowego kasjera.\n", registerId);
    }

    public int getSimulationTimeMillis() {
        return simulationTime * 1000;
    }

    // Wyświetlanie alertu
    private void showAlert(String message) {
        Alert alert = new Alert(AlertType.INFORMATION);
        alert.setTitle("KONIEC PROGRAMU");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}