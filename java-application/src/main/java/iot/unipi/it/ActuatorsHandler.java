package iot.unipi.it;

import org.eclipse.californium.core.CoapClient;
import org.eclipse.californium.core.CoapResponse;
import org.eclipse.californium.core.coap.CoAP;
import org.eclipse.californium.core.coap.MediaTypeRegistry;
import org.eclipse.californium.core.coap.Request;

import java.util.*; 

/**
 * The CoAPHandler class provides methods to send CoAP messages to actuators.
 */
public class ActuatorsHandler {
    private final String in_bar_mac = "f4ce3605c3a3";
    private final String out_bar_mac = "f4ce368efc62";

    private final int parkingId;

    private String fan;
    private String[] bars;

    private int max_cars_number;
    private int cars_number;

    private int vibration_threshold;
    private int[] air_thresholds;

    private DatabaseHandler databaseHandler;

    public ActuatorsHandler(int parkingId, int max_cars, DatabaseHandler databaseHandler){

        this.databaseHandler = databaseHandler;
        this.parkingId = parkingId;
        this.max_cars_number = max_cars;

        this.cars_number = 0;
        this.vibration_threshold = 10;
        this.air_thresholds = new int[] {50, 100, 150};

        this.fan = "0";
        this.bars = new String[]{"up", "down"};

    }


    public void handleValue(String topic, int value, String mac, int parkingID){
        if (value < 0){
            return;
        }
        if(topic.equals("vibration")){

            if(value > vibration_threshold && mac.equals(in_bar_mac) && cars_number != max_cars_number){ //entrance sensor
                databaseHandler.addData(topic, value, mac, parkingID);
                cars_number ++;
                if (cars_number == max_cars_number){
                    sendMessage("bar?bar=0", "down", databaseHandler.findResourcesIPs(parkingId, "bar"));
                    bars[0] = "down";
                    System.out.println("[ENTRANCE BAR DOWN]");
                }
            } else if (value > vibration_threshold && mac.equals(out_bar_mac) && cars_number != 0){ //out sensor
                databaseHandler.addData(topic, value, mac, parkingID);
                cars_number --;
                sendMessage("bar?bar=1", "up", databaseHandler.findResourcesIPs(parkingId, "bar"));
                bars[1] = "up";
                System.out.println("[OUT BAR UP]");

                if(cars_number == max_cars_number-1){
                    sendMessage("bar?bar=0", "up", databaseHandler.findResourcesIPs(parkingId, "bar"));
                    bars[0] = "up";
                    System.out.println("[ENTRANCE BAR UP]");
                }
            }

        } else if (topic.equals("nox")){

            /*
            //not coherent values
            if ((value < air_thresholds[0] && cars_number > 0.25 * max_cars_number) ||
                (value >= air_thresholds[0] && value < air_thresholds[1] && (cars_number > 0.5 * max_cars_number || cars_number < 0.25 * max_cars_number)) ||
                (value >= air_thresholds[1] && value < air_thresholds[2] && (cars_number > 0.75 * max_cars_number || cars_number < 0.5 * max_cars_number)) ||
                (value >= air_thresholds[2] && cars_number < 0.75 * max_cars_number))
            {
                return;
            }
            */
            
            databaseHandler.addData(topic, value, mac, parkingID);

            if(value <air_thresholds[0] && !this.fan.equals("0")){
                sendMessage("fan", "0", databaseHandler.findResourcesIPs(parkingId, "fan"));
                this.fan = "0";
                System.out.println("[FAN OFF]");

            } else if (value >= air_thresholds[0] && value < air_thresholds[1] && !this.fan.equals("1")){
                sendMessage("fan", "1", databaseHandler.findResourcesIPs(parkingId, "fan"));
                this.fan = "1";
                System.out.println("[FAN SPEED 1]");

            } else if (value >= air_thresholds[1] && value < air_thresholds[2] && !this.fan.equals("2")){
                sendMessage("fan", "2", databaseHandler.findResourcesIPs(parkingId, "fan"));
                this.fan = "2";
                System.out.println("[FAN SPEED 2]");
            } else if(value >= air_thresholds[2] && !this.fan.equals("3")){
                sendMessage("fan", "3", databaseHandler.findResourcesIPs(parkingId, "fan"));
                this.fan = "3";
                System.out.println("[FAN SPEED 3]");
            }
            
        } else {
            System.out.printf("Unknown topic: %s\n", topic);
        }
    }

    /**
     * Sends a CoAP message to a list of actuators.
     *
     * @param resource     the resource to which the CoAP message should be sent
     * @param command      the command to be included in the CoAP message
     * @param actuatorsIPs the list of IP addresses of the actuators
     */
    public void sendMessage(String resource, String command, List<String> actuatorsIPs){
        for(String s: actuatorsIPs){
            String url = "coap://["+s+"]/"+resource;
            CoapClient client = new CoapClient(url);
            Request req = new Request(CoAP.Code.PUT);
            req.setPayload(command);
            req.getOptions().setAccept(MediaTypeRegistry.TEXT_PLAIN);
            CoapResponse response = client.advanced(req); //send request and wait for response
            if(response!=null) {
                CoAP.ResponseCode code = response.getCode();
                switch (code) {
                    case CHANGED:
                        // caso 204
                        System.out.println("[RESPONSE: 204 OK]");
                        break;
                    case BAD_REQUEST:
                        System.out.println("[RESPONSE: INTERNAL APPLICATION ERROR]");
                        break;
                    case BAD_OPTION:
                        System.out.println("[RESPONSE: BAD OPTION ERROR]");
                        break;
                    default:
                        System.out.println("[RESPONSE: ACTUATOR ERROR]");
                        break;
                }
            }
        }
    }
}