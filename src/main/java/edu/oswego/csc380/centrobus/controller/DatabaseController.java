package edu.oswego.csc380.centrobus.controller;

import java.sql.*;

import edu.oswego.csc380.centrobus.data.Vehicle;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class DatabaseController extends Controller {

    private final CentroAPIController busController;
    private final OpenWeatherAPIController weatherController;

    @Value("${database.password}")
    private String password;

    public DatabaseController(CentroAPIController busController, OpenWeatherAPIController weatherController) {

        this.busController = busController;
        this.weatherController = weatherController;

    }

    //Every 15 seconds from 4:45AM to 9PM, execute the following method to write bus and weather data to the database
    @Scheduled(cron = "*/15 45-59 4 * * *")
    @Scheduled(cron = "*/15 * 5-20 * * *")
    public void writeData() {

        //Connect to database
        String url = "jdbc:mysql://wolf.cs.oswego.edu:3306/csc380_s26_t4";
        String user = "csc380_s26_t4";

        try (Connection conn = DriverManager.getConnection(url, user, this.password)) {
            if (conn != null) {
                System.out.println("Connected to database");
            }

            //Poll the Centro API and store the vehicles into an array
            Vehicle[] vehicleList = busController.getVehicles("OSW46,OSW10,OSW11,SY84,SY88");

            //Make sure the array is not null (sometimes the API will fail and return an error object w/out any vehicles)
            if(vehicleList != null) {

                //Iterate through the array of Vehicles and insert them into the database
                for (Vehicle v : vehicleList) {

                    //Fix the vehicle's tmstmp String so it is in proper DateTime format for the database
                    v.fixTmstmp();

                    //Get the weather data for the vehicle and store it inside the vehicle
                    v.weather = weatherController.getWeather(v);

                    //Insert vehicle data into database
                    //Use INSERT IGNORE because occasionally the Centro API will return the same exact vehicle data in back-to-back calls, leading to a duplicate key error
                    PreparedStatement ps = conn.prepareStatement(
                            "INSERT IGNORE INTO Vehicle (VehicleID, RouteID, DateTime, Latitude, Longitude)\n" +
                                    "VALUES (?, ?, ?, ?, ?)"
                    );

                    //insert the bus variables into the statement
                    ps.setInt(1, Integer.parseInt(v.getVid()));
                    ps.setString(2, v.getRt());
                    ps.setString(3, v.getTmstmp());
                    ps.setDouble(4, Double.parseDouble(v.getLat()));
                    ps.setDouble(5, Double.parseDouble(v.getLon()));

                    //Execute the above statement (printing below is not really needed, it's there for testing purposes)
                    int rowsAffected = ps.executeUpdate();
                    System.out.println("Vehicle rows changed: " + rowsAffected);

                    //Insert each vehicle's weather data into database
                    //Use INSERT IGNORE to avoid same issue as above, in case the same exact vehicle data was returned by the Centro API
                    PreparedStatement ps2 = conn.prepareStatement(
                            "INSERT IGNORE INTO Weather (DateTime, Latitude, Longitude, ConditionID, Temperature, Precipitation)\n" +
                                    "VALUES (?, ?, ?, ?, ?, ?)"
                    );

                    //insert the correct variables into the statement
                    ps2.setString(1, v.getTmstmp());
                    ps2.setDouble(2, Double.parseDouble(v.getLat()));
                    ps2.setDouble(3, Double.parseDouble(v.getLon()));
                    ps2.setInt(4, v.weather.conditionID);
                    ps2.setFloat(5, v.weather.getFahrenheit());
                    ps2.setFloat(6, v.weather.precip);                  //currently in mm; should we divide by 25.4 to convert to inches?

                    //Execute the above statement (printing below is not really needed, it's there for testing purposes)
                    int rowsAffected2 = ps2.executeUpdate();
                    System.out.println("Weather rows changed: " + rowsAffected2);

                }
            }

        } catch (Exception e) {
            System.out.println(e.getMessage());
        }



    }

    //Once a day at 9:30PM, after all routes have concluded service, infer the actual arrival times and pair them up with their scheduled times, and clear API usage from controllers
    @Scheduled(cron = "0 30 21 * * *")
    public void pairArrivals() {

        //Connect to database
        String url = "jdbc:mysql://wolf.cs.oswego.edu:3306/csc380_s26_t4";
        String user = "csc380_s26_t4";

        try (Connection conn = DriverManager.getConnection(url, user, this.password)) {
            if (conn != null) {
                System.out.println("Connected to database");
            }

            //Determine when a bus actually arrived at a stop today by comparing its lat/lon with those of the stops on its route (within 0.001 degrees is considered an arrival)
            //Use INSERT IGNORE because in rare circumstances, two buses on the same route, going in opposite directions, can pass each other at the same time and create duplicate arrivals at the same stop ID
            PreparedStatement ps = conn.prepareStatement(
                    "INSERT IGNORE INTO testing.ActualArrivals (StopID, DateTime, Latitude, Longitude) " +
                    "SELECT " +
                        "s.StopID, " +
                        "v.DateTime, " +
                        "v.Latitude, " +
                        "v.Longitude " +
                    "FROM Stop s " +
                    "JOIN RoutesStops rs ON rs.StopID = s.StopID " +
                    "JOIN Vehicle v ON v.RouteID = rs.RouteID " +
                    "WHERE " +
                        "DATE(v.DateTime) = CURDATE() AND " +
                        "ABS(s.Latitude - v.Latitude) <= 0.001 " +
                        "AND ABS(s.Longitude - v.Longitude) <= 0.001;"
            );

            //Execute the above statement (printing below is not really needed, it's there for testing purposes)
            int rowsAffected = ps.executeUpdate();
            System.out.println("ActualArrival rows changed: " + rowsAffected);

            //Using the arrivals generated above, now pair them up with their corresponding ScheduledArrival and insert into the MatchedArrivals table
            PreparedStatement ps2 = conn.prepareStatement(
                    "INSERT INTO MatchedArrivals (StopID, ScheduledDateTime, ActualDateTime, Latitude, Longitude, Difference) " +
                    "SELECT StopID, ScheduledTime, ActualTime, Latitude, Longitude, " +
                           "TIMESTAMPDIFF(MINUTE, ScheduledTime, ActualTime) AS Difference " +
                    "FROM (" +
                        "SELECT " +
                            "s.StopID, " +
                            "s.DateTime AS ScheduledTime, " +
                            "a.DateTime AS ActualTime, " +
                            "a.Latitude AS Latitude, " +
                            "a.Longitude AS Longitude, " +
                            "ROW_NUMBER() OVER (" +
                                "PARTITION BY s.StopID, s.DateTime " +
                                "ORDER BY a.DateTime ASC" +
                            ") AS RowNumber " +
                        "FROM ScheduledArrivals s " +
                        "JOIN ActualArrivals a " +
                            "ON a.StopID = s.StopID " +
                           "AND a.DateTime BETWEEN s.DateTime - INTERVAL 8 MINUTE " +
                                             "AND s.DateTime + INTERVAL 8 MINUTE " +
                        "WHERE DATE(s.DateTime) = CURDATE()" +
                    ") closestMatch " +
                    "WHERE RowNumber = 1;"
            );

            //Execute the above statement (printing below is not really needed, it's there for testing purposes)
            int rowsAffected2 = ps2.executeUpdate();
            System.out.println("MatchedArrival rows changed: " + rowsAffected2);

            //Reset API controller calls
            busController.clearUses();
            weatherController.clearUses();

        } catch (Exception e) {
            System.out.println(e.getMessage());
        }

    }

}
