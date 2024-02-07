package iot.unipi.it;

import java.io.FileInputStream;
import java.io.IOException;
import java.sql.*;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Properties;
import java.util.Random;

/**
 * The GenerateRandomData class handles database operations for actuator and sensor data.
 */
public class GenerateRandomData {
    private String db_IP;
    private String port;
    private String db_name;
    private String user;
    private String password;

    /**
     * Constructs a new GenerateRandomData object by loading the database configuration from a file.
     *
     * @param config_path  the path to the configuration file
     */
    public GenerateRandomData(String config_path) {
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

    public void generateVibration(String inputDate) {
        String in_bar_mac = "f4ce3605c3a3";
        String out_bar_mac = "f4ce368efc62";
    
        int max_cars = 50;
        int cars = 0;
        int num_rows = 45;

        // Creazione di un oggetto Random per generare dati casuali
        Random random = new Random();

        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
        LocalDateTime dateTime;
        try {
            dateTime = LocalDateTime.ofInstant(dateFormat.parse(inputDate).toInstant(), ZoneId.systemDefault());
        } catch (ParseException e) {
            System.out.println("Invalid date format. Please use yyyy-MM-dd.");
            return;
        }

        // Query SQL per l'inserimento dei dati
        String sql = "INSERT INTO SensorData (Sensor_MAC, Topic, Value, Parking_ID, Timestamp) VALUES (?, ?, ?, ?, ?)";


        // Ciclo sulle ore da 8 a 19
        for (int hour = 8; hour < 20; hour++) {
            dateTime = dateTime.withHour(hour).withMinute(0).withSecond(0);
            for (int i = 0; i < num_rows; i++) {
                // Generazione di dati casuali
                String topic = "vibration";
                String sensorMac = (random.nextInt(100) < 60) ? in_bar_mac : out_bar_mac;

                if ((sensorMac.equals(in_bar_mac) && cars == max_cars) || (sensorMac.equals(out_bar_mac) && cars == 0)) {
                    continue;
                }
                int randomSeconds = random.nextInt(176) + 5; // (3 * 60) - 5 + 1
                dateTime = dateTime.plusSeconds(randomSeconds);
                if (dateTime.getHour() >= 20) {
                    break;
                }

                Timestamp randomTimestamp = Timestamp.valueOf(dateTime);

                double value = 15 + (random.nextInt(11)); // Valore compreso tra 15 e 25

                String url = "jdbc:mysql://"+db_IP+":"+port+"/"+db_name;
                try (Connection co = DriverManager.getConnection(url, user, password);
                        PreparedStatement pr = co.prepareStatement(sql)){
                            
                    // Impostazione dei parametri della query
                    pr.setString(1, sensorMac);
                    pr.setString(2, topic);
                    pr.setDouble(3, value);
                    pr.setInt(4, 1);
                    pr.setTimestamp(5, randomTimestamp);

                    // Esecuzione della query di inserimento
                    pr.executeUpdate();

                } catch (SQLIntegrityConstraintViolationException e) {
                    // Gestisci l'eccezione di chiave primaria duplicata
                    System.out.println("Duplicate primary key. Skipping to the next iteration.");
                } catch (SQLException e) {
                    System.out.println("[error]");
                    e.printStackTrace();
                    return;
                }
                cars = (sensorMac.equals(in_bar_mac)) ? cars+1 : cars-1;
            }
        }

        if(cars > 0){
            sql =   "DELETE FROM SensorData " +
                    "WHERE Sensor_MAC = ? " +
                    "  AND DATE(Timestamp) = ? " +
                    "ORDER BY Timestamp DESC " +
                    "LIMIT ?;";

            String url = "jdbc:mysql://"+db_IP+":"+port+"/"+db_name; 
            try (Connection co = DriverManager.getConnection(url, user, password); 
                    PreparedStatement pr = co.prepareStatement(sql)){ 

                // Impostazione dei parametri della query 
                pr.setString(1, "f4ce3605c3a3"); 
                pr.setString(2, inputDate); 
                pr.setInt(3, cars);
                pr.executeUpdate(); 

            } catch (SQLException e) { 
                System.out.println("[error]"); 
                e.printStackTrace(); 
                return; 
            } 
        }

    }

    public void generateNox(String inputDate) {

        String sql = "INSERT INTO SensorData (Sensor_MAC, Topic, Value, Parking_ID, Timestamp) VALUES (?, ?, ?, ?, ?)";

        String topic = "nox";
        String sensorMac = "f4ce3663b772";

        int[] air_thresholds = new int[] {50, 100, 150, 200};
        int max_cars = 50;
        Random random = new Random();

        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
        LocalDateTime dateTime;
        try {
            dateTime = LocalDateTime.ofInstant(dateFormat.parse(inputDate).toInstant(), ZoneId.systemDefault());
        } catch (ParseException e) {
            System.out.println("Invalid date format. Please use yyyy-MM-dd.");
            return;
        }
       
        dateTime = dateTime.withHour(8).withMinute(0).withSecond(0);

        while(dateTime.getHour() < 20){
            dateTime = dateTime.plusSeconds(600);

            Timestamp ts = Timestamp.valueOf(dateTime);
            int num_auto = getNumAuto(inputDate, ts);
            int value;

            if (num_auto >= 0 && num_auto <= (max_cars * 0.25)) {
                value = random.nextInt(air_thresholds[0] + 1);
            } else if (num_auto > (max_cars * 0.25) && num_auto <= (max_cars * 0.5)) {
                value = random.nextInt(air_thresholds[1] - air_thresholds[0] + 1) + air_thresholds[0];
            } else if (num_auto > (max_cars * 0.5) && num_auto <= (max_cars * 0.75)) {
                value = random.nextInt(air_thresholds[2] - air_thresholds[1] + 1) + air_thresholds[1];
            } else if (num_auto > (max_cars * 0.75)) {
                value = random.nextInt(air_thresholds[3] - air_thresholds[2] + 1) + air_thresholds[2];
            } else {
                continue;
            }

            String url = "jdbc:mysql://"+db_IP+":"+port+"/"+db_name;
            try (Connection co = DriverManager.getConnection(url, user, password);
                    PreparedStatement pr = co.prepareStatement(sql)){
                        
                // Impostazione dei parametri della query
                pr.setString(1, sensorMac);
                pr.setString(2, topic);
                pr.setDouble(3, value);
                pr.setInt(4, 1);
                pr.setTimestamp(5, ts);

                // Esecuzione della query di inserimento
                pr.executeUpdate();

            } catch (SQLIntegrityConstraintViolationException e) {
                // Gestisci l'eccezione di chiave primaria duplicata
                System.out.println("Duplicate primary key. Skipping to the next iteration.");
            } catch (SQLException e) {
                System.out.println("[error]");
                e.printStackTrace();
                return;
            }    
            
        }
            
    }

    public int getNumAuto(String inputDate, Timestamp ts){
        String sql_get = "SELECT " +
        "                ( SELECT COUNT(*) " +
        "                  FROM SensorData " + 
        "                  WHERE Sensor_MAC = 'f4ce3605c3a3' " +
        "                  AND DATE(timestamp) = ? AND Timestamp < ?) - " +
        "                  (SELECT COUNT(*) " +
        "                  FROM SensorData " +
        "                  WHERE Sensor_MAC = 'f4ce368efc62' " +
        "                  AND DATE(timestamp) = ? AND Timestamp < ?) AS NumeroAuto;";


        String url = "jdbc:mysql://"+db_IP+":"+port+"/"+db_name;
        try (Connection co = DriverManager.getConnection(url, user, password);
                PreparedStatement pr = co.prepareStatement(sql_get)){
                    
            // Impostazione dei parametri della query
            pr.setString(1, inputDate);
            pr.setTimestamp(2, ts);
            pr.setString(3, inputDate);
            pr.setTimestamp(4, ts);;

            ResultSet rs = pr.executeQuery();
            
            return rs.next()? rs.getInt("NumeroAuto") : 0;

        } catch (SQLException e) {
            System.out.println("[error]");
            e.printStackTrace();
            return -1;
        }

    }

    public void showData(String inputDate) {
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
        LocalDateTime dateTime;
        try {
            dateTime = LocalDateTime.ofInstant(dateFormat.parse(inputDate).toInstant(), ZoneId.systemDefault());
        } catch (ParseException e) {
            System.out.println("Invalid date format. Please use yyyy-MM-dd.");
            return;
        }

        for(int i=8; i<20; i++){
            dateTime = dateTime.withHour(i+1).withMinute(0).withSecond(0);
            Timestamp ts = Timestamp.valueOf(dateTime);

            int num_auto = getNumAuto(inputDate, ts);
            System.out.printf("Numero di auto alle %s: %d\n", String.format("%02d", i+1), num_auto);
        }

    }
   
}

