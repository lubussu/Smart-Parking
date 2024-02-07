package iot.unipi.it;

import java.io.FileInputStream;
import java.io.IOException;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

/**
 * The DatabaseHandler class handles database operations for actuator and sensor data.
 */
public class DatabaseHandler {
    private String db_IP;
    private String port;
    private String db_name;
    private String user;
    private String password;

    /**
     * Constructs a new DatabaseHandler object by loading the database configuration from a file.
     *
     * @param config_path  the path to the configuration file
     */
    public DatabaseHandler(String config_path) {
        Properties config = new Properties();
        try (FileInputStream input = new FileInputStream(config_path)) {
            config.load(input);
        } catch (IOException e) {
            System.err.println("Failed to load configuration file!");
            e.printStackTrace();
        }

        this.db_IP = config.getProperty("db.IP");
        this.db_name = config.getProperty("db.name");
        this.port = config.getProperty("db.port");
        this.user = config.getProperty("db.username");
        this.password = config.getProperty("db.password");
    }

    /**
     * Adds an actuator to the database.
     *
     * @param ip            the IP address of the actuator
     * @param parkingID  the ID of the associated greenhouse
     * @param role          the role of the actuator
     * @return true if the actuator is successfully added, false otherwise
     */
    public boolean addActuator(String ip, int parkingID, String role){
        String url = "jdbc:mysql://"+db_IP+":"+port+"/"+db_name;
        String sql = "INSERT INTO Actuators(Actuator_IP, Parking_ID, Role) values (?,?,?)";
        try (Connection co = DriverManager.getConnection(url, user, password);
             PreparedStatement pr = co.prepareStatement(sql)){
            pr.setString(1, ip);
            pr.setInt(2,parkingID);
            pr.setString(3,role);
            int rowsInserted = pr.executeUpdate();
            if(rowsInserted <=0){
                System.out.println("[ACTUATOR ALREADY REGISTERED]");
                return false;
            }
        } catch(SQLIntegrityConstraintViolationException e){
            System.out.println("[ACTUATOR ALREADY REGISTERED]");
            return true;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
        System.out.println("[ACTUATOR REGISTERED]:\tParkingID: " + parkingID + "\tRole: " + role);

        return true;
    }

    /**
     * Adds a data to the database.
     *
     * @param data          the data value
     * @param mac           the MAC address of the sensor
     * @param parkingID  the ID of the associated greenhouse
     * @return true if data is successfully added, false otherwise
     */
    public boolean addData(String topic, double data, String mac, int parkingID){
        String url = "jdbc:mysql://"+db_IP+":"+port+"/"+db_name;
        String sql = "INSERT INTO SensorData(Sensor_MAC, Topic, Value, Parking_ID) values (?,?,?,?)";
        try (Connection co = DriverManager.getConnection(url, user, password);
             PreparedStatement pr = co.prepareStatement(sql)){
            pr.setString(1, mac);
            pr.setString(2, topic);  
            pr.setDouble(3, data);
            pr.setInt(4,parkingID);
            int rowsInserted = pr.executeUpdate();
            if(rowsInserted <=0){
                System.out.println("[SENSORDATA]: No rows inserted.");
                return false;
            }
        } catch (SQLException e) {
            System.out.println("[RELEVATION NOT INSERTED]");
            e.printStackTrace();
        
            return false;
        }
        System.out.println("[RELEVATION INSERTED]:\t Topic: " + topic + " From " + mac + " Value: " + data);
        return true;
    }

    /**
     * Retrieves the IP addresses of tents associated with a specific greenhouse from the database.
     *
     * @param parkingId the ID of the greenhouse
     * @return a list of IP addresses of tents
     */
    public List<String> findResourcesIPs(int parkingId, String resource){

        List<String> ips = new ArrayList<>();
        String url = "jdbc:mysql://"+db_IP+":"+port+"/"+db_name;
        String sql = "SELECT Actuator_IP FROM Actuators WHERE Parking_ID=? AND Role=?";
        try (Connection co = DriverManager.getConnection(url, user, password);
        PreparedStatement pr = co.prepareStatement(sql)){
            pr.setInt(1,parkingId);
            pr.setString(2,resource);
            ResultSet r = pr.executeQuery();
            while(r.next()){
                ips.add(r.getString(1));
            }
        } catch (SQLException e){
            e.printStackTrace();
        }
        return ips;
    }
   
}
