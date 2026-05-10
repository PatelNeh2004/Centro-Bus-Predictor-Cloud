package edu.oswego.csc380.centrobus.controller;

/**
 * Allows for controllers to have the amount of uses for their given key easily counted.
 * @author alex
 */
public abstract class Controller {
	
	private int numUses = 0;
	
	/**
	 * Gets the amount of times the given controller has used its API key
	 * @return The amount of times the controller has used its API key
	 */
	public int getUses() {
		return numUses;
	}
	
	/**
	 * Adds a use to the counter of API key uses
	 */
	public void addUse() {
		numUses++;
	}
	
	/**
	 * Sets the counter of API uses to zero
	 */
	public void clearUses() {
		numUses = 0;
	}

}
