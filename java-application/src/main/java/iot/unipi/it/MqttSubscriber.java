package iot.unipi.it;

import org.eclipse.paho.client.mqttv3.*;
import org.json.simple.JSONObject;
import org.json.simple.JSONValue;
import org.json.simple.parser.ParseException;


/**

 * The MqttSubscriber class implements an MQTT subscriber that listens to a specific topic for temperature messages.

 */

public class MqttSubscriber implements MqttCallback {

    private final String[] topics = {"vibration", "nox"};
    private final String broker = "tcp://127.0.0.1";
    private final String clientId = "SmartParking";

    private ActuatorsHandler actuatorsHandler;



    /**

     * Constructs a new MqttSubscriber object with the specified DatabaseHandler.

     * @param actuatorsHandler  the ActuatorsHandler used to send commands to actuators

     */

    public MqttSubscriber(ActuatorsHandler actuatorsHandler){

        this.actuatorsHandler = actuatorsHandler;

        try {
            MqttClient mqttClient = new MqttClient(broker, clientId);

            mqttClient.setCallback(this);
            mqttClient.connect();
            mqttClient.subscribe(topics);

        } catch (MqttException me){
            me.printStackTrace();
        }

    }

    @Override
    public void connectionLost(Throwable throwable) {

        System.out.println("Connection with broker lost..." + throwable.getMessage());

    }

    @Override
    public void messageArrived(String topic, MqttMessage mqttMessage) throws Exception {

        try {

            JSONObject requestJson = (JSONObject) JSONValue.parseWithException(new String(mqttMessage.getPayload()));
            String app = requestJson.get("app").toString();

            if(app.equals("smart_parking")) {

                int parkingID = Integer.parseInt(requestJson.get("parking_id").toString());
                int value = Integer.parseInt(requestJson.get("value").toString());
                String mac = requestJson.get("MAC").toString();

                // handle value received
                actuatorsHandler.handleValue(topic, value, mac, parkingID);
            }

        } catch (ParseException e) {

            System.out.println("[DATA NOT RECEIVED CORRECTLY]");
            throw new RuntimeException(e);
        }

    }

    @Override
    public void deliveryComplete(IMqttDeliveryToken iMqttDeliveryToken) {

        System.out.println("Delivery completed!");

    }

}