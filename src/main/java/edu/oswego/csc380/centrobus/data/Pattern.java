package edu.oswego.csc380.centrobus.data;

import java.util.ArrayList;

/**
 * The given pattern a bus will run along a route. The bus will travel along to each UniqueStop present.
 * @author alex
 */
public class Pattern {

	private int id = -1;
	private ArrayList<UniqueStop> stopList = new ArrayList<UniqueStop>();
	// private int numberOfStops = -1; Note that, although this is in the UML diagram, this isn't actually needed, as stopList.size() can be used instead
	private int patternDistance = -1;
	
	public Pattern(int id, int patternDistance) {
		this.id = id;
		this.patternDistance = patternDistance;
	}
	
	/**
	 * Gets the ID of a given pattern
	 * @return The ID of the pattern
	 */
	public int getID() {
		return id;
	}
	
	/**
	 * Sets the ID of a given pattern
	 * @param id The new ID for the pattern
	 */
	public void setID(int id) {
		this.id = id;
	}
	
	/**
	 * Gets the number of stops in the pattern
	 * @return The number of stops in the pattern
	 */
	public int getNumberOfStops() {
		return stopList.size();
	}
	
	// The UML diagram calls for setNumberOfStops() here, however that should be automatically handled by adding stops to the list
	
	/**
	 * Gets the list of all stops for the given pattern
	 * @return The list of all stops in the pattern
	 */
	public ArrayList<UniqueStop> getStopList() {
		return stopList;
	}
	
	/**
	 * Sets the list of all stops for the given pattern. Automatically sets all stops to be of this pattern.
	 * Note that this overwrites any previous stops.
	 * FIXME: This allows for one to add stops of other patterns! Perhaps the get and set stop list functions should be changed to functions that get the various attributes needed...
	 * @param stopList The list of all the stops to be in the pattern
	 */
	public void setStopList(ArrayList<UniqueStop> stopList) {
		this.stopList = stopList;
		setPatternsOfStops();
	}
	
	/**
	 * Gets the distance covered for the given pattern
	 * @return The distance covered by the pattern
	 */
	public int getPatternDistance() {
		return patternDistance;
	}
	
	/**
	 * Sets the distance covered for the given pattern
	 * @param patternDistance The distance to be covered by the pattern
	 */
	public void setPatternDistance(int patternDistance) {
		this.patternDistance = patternDistance;
	}
	
	/**
	 * Makes sure that all of the stops are of the correct pattern
	 */
	private void setPatternsOfStops() {
		for (int i = 0; i < stopList.size(); i++) {
			stopList.get(i).setPattern(this);
		}
	}
}
