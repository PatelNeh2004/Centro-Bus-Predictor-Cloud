package edu.oswego.csc380.centrobus.data;

//Used for handling JSON data
public class Vehicle {

    public String vid;             //stores vehicle ID
    public String tmstmp;          //stores date and time of this request
    public String rt;              //stores the route the vehicle is on
    public Weather weather;        //stores weather data at the bus's location
    public String lat;             //stores vehicle's latitude
    public String lon;             //stores vehicle's longitude

    public Vehicle (){

    }

    public String getVid() {

        return vid;

    }

    public String getTmstmp() {

        return tmstmp;

    }

    public String getLat() {

        return lat;

    }

    public String getLon() {

        return lon;

    }

    public String getRt() {

        return rt;

    }

    //Fixes the tmstmp so it is in the correct DateTime format for the database by inserting '-' into YYYY-MM-DD
    public void fixTmstmp() {
    	// See https://docs.oracle.com/javase/tutorial/datetime/iso/format.html
        String newTmstmp = tmstmp.substring(0,4) + "-" + tmstmp.substring(4,6) + "-" + tmstmp.substring(6);
        tmstmp = newTmstmp;

    }

}
