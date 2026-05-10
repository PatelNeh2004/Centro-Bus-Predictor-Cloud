package edu.oswego.csc380.centrobus;

import org.springframework.web.bind.annotation.*;
import org.springframework.jdbc.core.JdbcTemplate;
import java.util.*;

@RestController
public class RouteController {
    private final JdbcTemplate db;
    public RouteController(JdbcTemplate db) { this.db = db; }

    @GetMapping("/api/routes")
    public List<Map<String, Object>> routes() {
        return db.queryForList("SELECT * FROM Route");
    }

    @GetMapping("/api/stops")
    public List<Map<String, Object>> stops(@RequestParam String routeId) {
        return db.queryForList(
            "SELECT MIN(s.StopID) as StopID, s.Name " +
            "FROM Stop s JOIN RoutesStops rs ON s.StopID = rs.StopID " +
            "WHERE rs.RouteID = ? GROUP BY s.Name ORDER BY s.Name", routeId);
    }

    @GetMapping("/api/arrivals")
    public List<Map<String, Object>> arrivals(@RequestParam int stopId,
                                               @RequestParam(defaultValue = "all") String tf,
                                               @RequestParam(required = false) String arrTime) {
        String base = "SELECT Difference FROM MatchedArrivals WHERE StopID = ? AND Difference IS NOT NULL";

        if (tf.equals("7")) {
            base += " AND ScheduledDateTime >= DATE_SUB(NOW(), INTERVAL 7 DAY)";
        } else if (tf.equals("30")) {
            base += " AND ScheduledDateTime >= DATE_SUB(NOW(), INTERVAL 30 DAY)";
        }

        if (arrTime != null && !arrTime.isEmpty()) {
            base += " AND DATE_FORMAT(ScheduledDateTime, '%h:%i %p') = ?";
            return db.queryForList(base, stopId, arrTime);
        }

        return db.queryForList(base, stopId);
    }

    @GetMapping("/api/times")
    public List<Map<String, Object>> times(@RequestParam int stopId) {
        return db.queryForList(
            "SELECT DISTINCT DATE_FORMAT(DateTime, '%h:%i %p') as arrTime, TIME(DateTime) as sortTime " +
            "FROM ScheduledArrivals WHERE StopID = ? ORDER BY sortTime", stopId);
    }
}