package edu.oswego.csc380.centrobus.controller;


import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

import com.google.gson.Gson;

import edu.oswego.csc380.centrobus.data.Root;
import edu.oswego.csc380.centrobus.data.BustimeResponse;
import edu.oswego.csc380.centrobus.data.Vehicle;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class CentroAPIController extends Controller {

    @Value("${centro.key}")
    private String key;

    public CentroAPIController() {

    }

    //Polls the Centro API for bus data along our routes, and returns it as an array of Vehicle objects
    public Vehicle[] getVehicles(String rt) {

        //Create the URL for the request
        String url = "https://bus-time.centro.org/bustime/api/v3/getvehicles?key=" +
                key + "&rt=" +
                rt + "&format=json" +
                "&tmres=s";

        //Create the necessary HTTP objects to facilitate sending the request and receiving the JSON
        try {

            HttpClient client = HttpClient.newHttpClient();
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .GET()
                    .build();

            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            String body = response.body();

            //Increment number of API calls used today
            super.addUse();

            //Parse the JSON and store the Vehicles from it into an array of Vehicles
            Gson gson = new Gson();
            Root root = gson.fromJson(body, Root.class);
            Vehicle[] busArray = root.bustimeResponse.vehicle;

            return busArray;

        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
