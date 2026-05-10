package edu.oswego.csc380.centrobus.controller;


import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.sql.*;
import java.util.ArrayList;

@RestController
public class AnalysisController {

    @Value("${database.password}")
    private String password;

    //takes a request from the front-end and returns an int[] via JSON, detailing the early/late/on-time arrivals for a given stop ID and time (with optional filters)
    //formatted as: http://localhost:8080/analysis?stopID=###&time=##:##:##&dateRange=text&condition=text
    //filters: dateRange can be either the past week or the past month ('week' and 'month', respectively).
    //         condition can be either 'clear', 'rain', or 'snow'
    //         if no filters are desired, just omit one or both of them from the request
    @GetMapping("/analysis")
    public ArrayList<Integer> returnAnalysis(@RequestParam int stopID, @RequestParam String time,
                                             @RequestParam(required = false) String dateRange,
                                             @RequestParam(required = false) String condition) {

        //will store the early/late/on-time arrival data from the database
        ArrayList<Integer> histogramData = new ArrayList<>();

        //Connect to database
        String url = "jdbc:mysql://wolf.cs.oswego.edu:3306/csc380_s26_t4";
        String user = "csc380_s26_t4";

        try (Connection conn = DriverManager.getConnection(url, user, this.password)) {

            if (conn != null) {
                System.out.println("Connected to database");
            }

            //the query differs based on condition being present or not
            if (condition == null) {

                //select all tuples from MatchedArrivals that match the given stop ID and scheduled arrival time
                String query = "SELECT * FROM MatchedArrivals WHERE StopID = ? AND TIME(ScheduledDateTime) = ?";

                //now restrict by the specified date range, if any
                if (dateRange != null && dateRange.equals("week")) {

                    query = query + " AND ScheduledDateTime >= CURDATE() - INTERVAL 7 DAY";

                }
                if (dateRange != null && dateRange.equals("month")) {

                    query = query + " AND ScheduledDateTime >= CURDATE() - INTERVAL 30 DAY";

                }

                //create the statement to select the corresponding data from the MatchedArrivals table
                PreparedStatement ps = conn.prepareStatement(query);

                //insert the requested values into the statement
                ps.setInt(1, stopID);
                ps.setString(2, time);

                //retrieve the table and iterate through it
                ResultSet rs = ps.executeQuery();
                while (rs.next()) {

                    //add each time difference to the ArrayList
                    histogramData.add(rs.getInt("Difference"));

                }

            }
            //else, the condition has been specified
            else {

                //join Weather table with MatchedArrivals to make weather data accessible
                String query = "SELECT * FROM MatchedArrivals m " +
                                "JOIN Weather w ON m.ActualDateTime = w.DateTime " +
                                "AND ABS(m.Latitude - w.Latitude) <= 0.0000001 " +
                                "AND ABS(m.Longitude - w.Longitude) <= 0.0000001 WHERE StopID = ? AND TIME(ScheduledDateTime) = ?";

                //finish the query based on the condition selected
                switch(condition) {

                    case "clear":
                        query = query + " AND w.ConditionID BETWEEN 800 AND 899";
                        break;

                    case "rain":
                        query = query + " AND w.ConditionID BETWEEN 200 AND 599";
                        break;

                    case "snow":
                        query = query + " AND w.ConditionID BETWEEN 600 AND 699";
                        break;

                }

                //add date range filter if specified
                if (dateRange != null && dateRange.equals("week")) {

                    query = query + " AND ScheduledDateTime >= CURDATE() - INTERVAL 7 DAY";

                }
                if (dateRange != null && dateRange.equals("month")) {

                    query = query + " AND ScheduledDateTime >= CURDATE() - INTERVAL 30 DAY";

                }

                //create the statement to select the corresponding data from the MatchedArrivals table
                PreparedStatement ps = conn.prepareStatement(query);

                //insert the requested values into the statement
                ps.setInt(1, stopID);
                ps.setString(2, time);

                //retrieve the table and iterate through it
                ResultSet rs = ps.executeQuery();
                while (rs.next()) {

                    //add each time difference to the ArrayList
                    histogramData.add(rs.getInt("Difference"));

                }

            }

        } catch (Exception e) {
            System.out.println(e.getMessage());
        }

        //return the ArrayList as an int[] via JSON
        return histogramData;

    }


}
