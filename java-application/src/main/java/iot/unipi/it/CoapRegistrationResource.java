package iot.unipi.it;

import org.eclipse.californium.core.CoapResource;
import org.eclipse.californium.core.coap.CoAP;
import org.eclipse.californium.core.coap.MediaTypeRegistry;
import org.eclipse.californium.core.coap.Response;
import org.eclipse.californium.core.server.resources.CoapExchange;
import org.json.simple.JSONObject;
import org.json.simple.JSONValue;
import org.json.simple.parser.ParseException;

/**
 * The CoapRegistrationResource class represents a CoAP resource for actuator registration.
 * It handles GET and POST requests related to actuator registration.
 */
public class CoapRegistrationResource extends CoapResource {

    DatabaseHandler databaseHandler;

    /**
     * Constructs a new CoapRegistrationResource object.
     *
     * @param name             the name of the CoAP resource
     * @param databaseHandler  the instance of DatabaseHandler to interact with the database
     */
    public CoapRegistrationResource(String name, DatabaseHandler databaseHandler) {
        super(name);
        this.databaseHandler = databaseHandler;
    }

    /**
     * Handles a GET request received by the CoAP resource.
     *
     * @param exchange  the CoAP exchange for handling the request and response
     */
    public void handleGET(CoapExchange exchange) {
        exchange.respond("smart_parking!");
    }

    /**
     * Handles a POST request received by the CoAP resource.
     *
     * @param exchange  the CoAP exchange for handling the request and response
     */
    public void handlePOST(CoapExchange exchange) {
        Response response = new Response(CoAP.ResponseCode.CONTENT);
        if(exchange.getRequestOptions().getContentFormat() == MediaTypeRegistry.APPLICATION_JSON) {
            String payload = exchange.getRequestText();
            try {
                // request unpacking
                JSONObject requestJson = (JSONObject) JSONValue.parseWithException(payload);
                String appName = (String) requestJson.get("app");
                String role = (String) requestJson.get("role");
                int parkingID= Integer.parseInt(requestJson.get("parking_id").toString());
                System.out.println("[ACTUATOR REGISTRATION]:\tParking_ID: " + parkingID + "\tRole: " + role);
                if(appName.equals("smart_parking")) {
                    databaseHandler.addActuator(exchange.getSourceAddress().toString().substring(1), parkingID, role);
                    response.setPayload("200"); // 200 is the code for success
                }
            } catch (ParseException e) {
                throw new RuntimeException(e);
            }
        }
        else{
            response.setPayload("ERROR: Request expected in JSON format.");
        }
        exchange.respond(response);
    }
}
