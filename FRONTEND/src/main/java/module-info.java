module com.netclient.frontend {
    requires javafx.controls;
    requires javafx.fxml;
    requires javafx.web;
    requires com.fasterxml.jackson.databind;
    requires spring.websocket;
    requires spring.messaging;
    requires org.apache.httpcomponents.httpclient;
    requires org.apache.httpcomponents.httpcore;
    requires static lombok;
    exports com.netclient.frontend;
    exports com.netclient.frontend.utils;

    opens com.netclient.frontend to javafx.fxml;
    opens com.netclient.frontend.utils to com.fasterxml.jackson.databind;
}