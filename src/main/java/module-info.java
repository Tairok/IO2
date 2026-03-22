module com.app.io2 {
    requires javafx.controls;
    requires javafx.fxml;


    opens com.app.io2 to javafx.fxml;
    exports com.app.io2;
}