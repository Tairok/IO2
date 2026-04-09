module com.app.io {
    requires javafx.controls;
    requires javafx.fxml;
    requires java.sql;
    requires bcrypt;
    requires org.slf4j;

    exports com.app.client;
    exports com.app.client.controller;
    opens com.app.client.controller to javafx.fxml;

}