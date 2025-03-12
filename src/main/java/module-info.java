module com.example.supermarketsimulation {
    requires javafx.controls;
    requires javafx.fxml;


    opens com.example.supermarketsimulation to javafx.fxml;
    exports com.example.supermarketsimulation;
}