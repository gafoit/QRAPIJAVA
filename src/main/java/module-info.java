module com.qrcode.qr {
    requires javafx.controls;
    requires javafx.fxml;
    requires org.slf4j;


    opens com.qrcode.qr to javafx.fxml;
    exports com.qrcode.qr;
}