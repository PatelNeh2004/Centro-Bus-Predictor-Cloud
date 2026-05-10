package edu.oswego.csc380.centrobus.data;

/**
 * Represents a place in which the bus will stop. This is usually shared in between different routes, and has
 * geographic information.
 * @author alex
 */
public class Stop {

	protected int id = -1;
	protected String name = "";
	protected double latitude = -1.0D;
	protected double longitude = -1.0D;
	
	public Stop(int id, String name, double latitude, double longitude) {
		this.id = id;
		this.name = name;
		this.latitude = latitude;
		this.longitude = longitude;
	}
	
	public Stop(Stop s) {
		this.id = s.id;
		this.name = s.name;
		this.latitude = s.latitude;
		this.longitude = s.longitude;
	}
	
	/**
	 * Gets the ID of the given stop
	 * @return The ID of the stop
	 */
	public int getID() {
		return id;
	}
	
	/**
	 * Sets the ID of the given stop
	 * @param id The ID to be used
	 */
	public void setID(int id) {
		this.id = id;
	}
	
	/**
	 * Gets the name of the given stop
	 * @return The name of the stop
	 */
	public String getName() {
		return name;
	}
	
	/**
	 * Sets the name of the given stop
	 * @param name The name of the stop
	 */
	public void setName(String name) {
		this.name = name;
	}
	
	/**
	 * Gets the latitude, in degrees, of the given stop
	 * @return The latitude in degrees of the stop
	 */
	public double getLatitude() {
		return latitude;
	}
	
	/**
	 * Sets the latitude, in degrees, of the given stop
	 * @param latitude The latitude in degrees of the stop
	 */
	public void setLatitude(double latitude) {
		this.latitude = latitude;
	}
	
	/**
	 * Gets the longitude, in degrees, of the given stop
	 * @return The longitude in degrees of the stop
	 */
	public double getLongitude() {
		return longitude;
	}
	
	/**
	 * Sets the longitude, in degrees, of the given stop
	 * @param longitude The longitude in degrees of the stop
	 */
	public void setLongitude(double longitude) {
		this.longitude = longitude;
	}
	
}
