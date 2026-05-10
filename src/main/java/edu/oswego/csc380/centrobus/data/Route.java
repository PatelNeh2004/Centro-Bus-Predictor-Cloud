package edu.oswego.csc380.centrobus.data;

import java.util.ArrayList;

/**
 * A route that the bus runs along. A route can have a variety of different patterns that the bus will run.
 * @author alex
 */
public class Route {

	private String id = "";
	private String name = "";
	private ArrayList<Stop> stopList = new ArrayList<Stop>();
	private ArrayList<Pattern> patternList = new ArrayList<Pattern>();
	
	public Route(String id, String name) {
		this.id = id;
		this.name = name;
	}
	
	/**
	 * Gets the ID of the given route
	 * @return The ID of the route
	 */
	public String getID() {
		return id;
	}
	
	/**
	 * Sets the ID of the given route
	 * @param id What is to be the ID of the route
	 */
	public void setID(String id) {
		this.id = id;
	}
	
	/**
	 * Gets the name of the given route
	 * @return The name of the route
	 */
	public String getName() {
		return name;
	}
	
	/**
	 * Sets the name of the given route
	 * @param name What is to be the name of the route
	 */
	public void setName(String name) {
		this.name = name;
	}
	
	/**
	 * Gets the list of all stops on the given route
	 * @return The list of all stops on the route
	 */
	public ArrayList<Stop> getStopList() {
		return stopList;
	}
	
	/**
	 * Sets the list of all stops on the given route. Note that this overwrites any existing stops.
	 * @param stopList The list of all stops to be on the route
	 */
	public void setStopList(ArrayList<Stop> stopList) {
		this.stopList = stopList;
	}
	
	/**
	 * Gets the list of patterns that can be run on the given route
	 * @return The list of patterns that can be run on the route
	 */
	public ArrayList<Pattern> getPatternList() {
		return patternList;
	}
	
	/**
	 * Sets the list of patterns that can be run on the given route
	 * @param patternList The list of all patterns that will be run on the route
	 */
	public void setPatternList(ArrayList<Pattern> patternList) {
		this.patternList = patternList;
	}
}
