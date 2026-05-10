package edu.oswego.csc380.centrobus.data;

import java.time.LocalTime;

/**
 * A unique point along a given pattern. Alongside what is typically found for a stop, this also contains
 * when the expected arrival time is and how far along the stop is on the given pattern.
 * @author alex
 */
public class UniqueStop extends Stop {

	private Pattern pattern;
	private LocalTime arrivalTime = LocalTime.MIN;
	private int stopDistance = -1;
	
	public UniqueStop(Stop s, LocalTime arrivalTime, int stopDistance) {
		super(s);
		this.arrivalTime = arrivalTime;
		this.stopDistance = stopDistance;
	}
	
	/**
	 * Gets the pattern of the given unique stop
	 * @return The pattern the unique stop is on
	 */
	public Pattern getPattern() {
		return pattern;
	}
	
	/**
	 * Sets the pattern of the given unique stop
	 * @param pattern The pattern the unique stop is to be on
	 */
	public void setPattern(Pattern pattern) {
		this.pattern = pattern;
	}
	
	/**
	 * Gets the time the bus is scheduled to arrive at the given unique stop
	 * @return The time the bus is scheduled to arrive at the unique stop
	 */
	public LocalTime getArrivalTime() {
		return arrivalTime;
	}
	
	/**
	 * Sets the time the bus is scheduled to arrive at the given unique stop
	 * @param arrivalTime The time the bus is to be scheduled to arrive at the unique stop
	 */
	public void setArrivalTime(LocalTime arrivalTime) {
		this.arrivalTime = arrivalTime;
	}
	
	/**
	 * Gets how far along the unique stop is in the pattern
	 * @return How far along the unique stop is from the start of its pattern
	 */
	public int getStopDistance() {
		return stopDistance;
	}
	
	/**
	 * Sets how far along the unique stop is in the pattern
	 * @param stopDistance How far along the unique stop is to be from the start of its pattern
	 */
	public void setStopDistance(int stopDistance) {
		this.stopDistance = stopDistance;
	}


}
