module hr.algebra.client {
    // JavaFX
    requires javafx.controls;
    requires javafx.fxml;

    // HTTP + JSON
    requires java.net.http;
    requires com.fasterxml.jackson.databind;

    // JAXB (API + runtime na module-pathu)
    requires jakarta.xml.bind;
    requires org.glassfish.jaxb.runtime; // može i bez ovoga ako si na classpathu

    // XML-RPC  (ostavi ova dva ako ti tako radi; inače 1 linija: requires org.apache.xmlrpc;)
    requires xmlrpc.common;
    requires xmlrpc.client;

    // Lombok (samo compile-time)
    requires static lombok;

    // --- OPENS (refleksija) ---
    // FXML loaderi
    opens hr.algebra.client to javafx.fxml;
    opens hr.algebra.client.controller to javafx.fxml;

    // Jackson serializacija u AuthService-u
    opens hr.algebra.client.service to com.fasterxml.jackson.databind;

    // KLJUČNO: da JAXB i JavaFX PropertyValueFactory smiju čitati gettere iz XmlItem/XmlResponse
    opens hr.algebra.client.xml to jakarta.xml.bind, javafx.base;

    // --- EXPORTS (javne API pakete) ---
    exports hr.algebra.client;
    exports hr.algebra.client.controller;
}
