module deliverysystem {
    requires javafx.controls;
    requires javafx.fxml;
    requires java.sql;

    opens controllers to javafx.fxml;
    opens application to javafx.graphics, javafx.fxml;

    exports application;
}