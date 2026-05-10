package edu.oswego.csc380.centrobus.data;

import java.util.Date;
import java.time.Instant;

/**
 * Allows for the weather conditions at a given stop and a given time to be easily accessed
 * @author alex
 */
public class Weather {

	public Vehicle vehicle;
	public int conditionID = -1;
	public String icon = "";
	public Date time = Date.from(Instant.ofEpochMilli(0));
	public double lat = -1.0D;
	public double lon = -1.0D;
	public float temp = -1.0F; // In Kelvin
	public float precip = 0.0F; // This is expected to actually be 0 most of the time
	
	public Weather(Vehicle vehicle, int conditionID, String icon, Date time, float temp, float precip) {
		this.vehicle = vehicle;
		this.conditionID = conditionID;
		this.icon = icon;
		this.time = time;
		this.lat = Double.parseDouble(vehicle.getLat());
		this.lon = Double.parseDouble(vehicle.getLon());
		this.temp = temp;
		this.precip = precip;
	}
	
	public float getFahrenheit() {
		float f = ((9 * (temp - 273)) / 5) + 32;
		return f;
	}
	
	public float getCelcius() {
		float c = temp - 273;
		return c;
	}
	
}
