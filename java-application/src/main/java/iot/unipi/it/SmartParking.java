package iot.unipi.it;

import org.eclipse.californium.core.CoapServer;

public class SmartParking{
    public static void main( String[] args ){

        // MySQL Connection
        DatabaseHandler databaseHandler = new DatabaseHandler("db_config.properties");
        ActuatorsHandler actuatorsHandler = new ActuatorsHandler(1, 5, databaseHandler);

        // CoAP Server that stores actuators info in a MySQL DB
        CoapServer server = new CoapServer();
        server.add(new CoapRegistrationResource("registration", databaseHandler));
        server.start();

        // MQTT subscriber launch
        MqttSubscriber myMQTTSubscriber = new MqttSubscriber(actuatorsHandler);
    }
}
