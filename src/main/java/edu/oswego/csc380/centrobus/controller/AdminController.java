package edu.oswego.csc380.centrobus.controller;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;

@RestController
public class AdminController {

    private final CentroAPIController busController;
    private final OpenWeatherAPIController weatherController;

    @Value("${admin.username}")
    private String username;

    @Value("${admin.passwordHash}")
    private String passwordHash;

    @Value("${database.password}")
    private String password;

    public AdminController (CentroAPIController busController, OpenWeatherAPIController weatherController) {

        this.busController = busController;
        this.weatherController = weatherController;

    }

    //Returns whether the provided admin username/password are correct. The password must be hashed prior to sending the request.
    @PostMapping("/admin/login")
    public boolean credentialsAreCorrect(@RequestParam String username, @RequestParam String passwordHash) {

        boolean correct = false;

        if(this.username.equals(username) && this.passwordHash.equals(passwordHash)) {
            correct = true;
        }

        return correct;

    }

    //Returns the API calls for each controller. The integer in index 0 is the Centro API usage, and index 1 contains the OpenWeather API usage.
    @GetMapping("/admin/apicalls")
    public ArrayList<Integer> getAPICalls() {

        ArrayList<Integer> apiCalls = new ArrayList<>();
        apiCalls.add(busController.getUses());
        apiCalls.add(weatherController.getUses());
        return apiCalls;

    }

    //Returns the total number of individual stops that have been served by Centro since the moment our project began collecting data.
    @GetMapping("/admin/totalstops")
    public int getTotalStops() {

        int totalStops = 0;

        //Connect to database
        String url = "jdbc:mysql://wolf.cs.oswego.edu:3306/csc380_s26_t4";
        String user = "csc380_s26_t4";

        try (Connection conn = DriverManager.getConnection(url, user, this.password)) {

            if (conn != null) {
                System.out.println("Connected to database");
            }

            //select all tuples from MatchedArrivals (aka every stop recorded)
            String query = "SELECT * FROM MatchedArrivals";

            //create the statement to select every matched arrival
            PreparedStatement ps = conn.prepareStatement(query);

            //retrieve the table and iterate through it
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {

                //increment for every arrival in the table
                totalStops++;

            }

        } catch (Exception e) {
            System.out.println(e.getMessage());
        }

        return totalStops;

    }

    //Returns the average delay across all stops that we have collected data on.
    @GetMapping("/admin/avgdelay")
    public double getAvgDelay() {

        double avgDelay = 0;

        //Connect to database
        String url = "jdbc:mysql://wolf.cs.oswego.edu:3306/csc380_s26_t4";
        String user = "csc380_s26_t4";

        try (Connection conn = DriverManager.getConnection(url, user, this.password)) {

            if (conn != null) {
                System.out.println("Connected to database");
            }

            //calculate the average delay of all arrival data
            String query = "SELECT AVG(Difference) AS AvgDelay FROM MatchedArrivals;";

            //create the statement to select every matched arrival
            PreparedStatement ps = conn.prepareStatement(query);

            //retrieve the table and iterate through it
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {

                //get the average delay from the resulting table
                avgDelay = rs.getDouble("AvgDelay");

            }

        } catch (Exception e) {
            System.out.println(e.getMessage());
        }

        return avgDelay;

    }

    //Returns the active routes that are currently being serviced within the past minute.
    @GetMapping("/admin/activeroutes")
    public ArrayList<String> getActiveRoutes() {

        ArrayList<String> activeRoutes = new ArrayList<>();

        //Connect to database
        String url = "jdbc:mysql://wolf.cs.oswego.edu:3306/csc380_s26_t4";
        String user = "csc380_s26_t4";

        try (Connection conn = DriverManager.getConnection(url, user, this.password)) {

            if (conn != null) {
                System.out.println("Connected to database");
            }

            //select all distinct routes from the Vehicles table within the past minute
            String query = "SELECT DISTINCT RouteID FROM Vehicle WHERE DateTime > NOW() - INTERVAL 1 MINUTE;";

            //create the above statement
            PreparedStatement ps = conn.prepareStatement(query);

            //retrieve the table and iterate through it
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {

                //add each active route to the list
                activeRoutes.add(rs.getString("RouteID"));

            }

        } catch (Exception e) {
            System.out.println(e.getMessage());
        }

        return activeRoutes;

    }

    //Returns the API key that is requested.
    //Request format: http://localhost:8080/admin/getkey?api=text
    //Parameters: api (valid values are either 'centro' or 'openweather')
    @GetMapping("/admin/getkey")
    public String getAPIKey(@RequestParam String api) throws FileNotFoundException {

        try (FileReader reader = new FileReader("src/main/resources/application.properties")) {

            //read in application.properties
            BufferedReader breader = new BufferedReader(reader);
            String line;

            //return the key specified in the parameter
            while ((line = breader.readLine()) != null) {

                String[] tempLine = line.split("=");
                if (tempLine[0].equals("centro.key") && api.equals("centro")) {

                    return tempLine[1];

                }
                else if (tempLine[0].equals("openweather.key") && api.equals("openweather")) {

                    return tempLine[1];

                }

            }

        } catch (Exception e) {
            System.out.println(e.getMessage());
        }

        //if malformed request, return an error message
        return "Error: key not found";

    }

    //Returns the last 50 lines of the log file in order from most to least recent.
    @GetMapping("/admin/systemlog")
    public ArrayList<String> getSystemLog() throws FileNotFoundException {

        ArrayList<String> systemLog = new ArrayList<>();    //stores the log file for processing
        ArrayList<String> last50Lines = new ArrayList<>();  //stores the last 50 lines of the log file

        try (FileReader reader = new FileReader("logs/system.log")) {

            //read in the log file
            BufferedReader breader = new BufferedReader(reader);
            String line;
            while ((line = breader.readLine()) != null) {

                systemLog.add(line);

            }

            //grab the last 50 lines of the log file (or if there aren't 50 lines, as many as possible), and add to a new ArrayList in order from most to least recent
            while (systemLog.size() > 0 && last50Lines.size() < 51) {

                last50Lines.add(systemLog.get(systemLog.size() - 1));
                systemLog.remove(systemLog.size() - 1);

            }

        } catch (Exception e) {
            System.out.println(e.getMessage());
        }


        return last50Lines;

    }

}
