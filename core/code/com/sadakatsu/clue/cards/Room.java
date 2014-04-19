package com.sadakatsu.clue.cards;

import java.util.ArrayList;
import java.util.List;

/**
 * A Room is a type of Clue Card.  A Room is a location where Mr. Boddy could
 * have been murdered.
 * 
 * @author Joseph A. Craig
 */
public class Room extends Card {
	//******************* Protected and Private Interface ********************//
	/**
	 * Builds a Room Card.
	 * @param name
	 * The Card's name.
	 */
	private Room(String name) {
		super(name);
	}
	
	//***************** Protected and Private Static Fields ******************//
	private static List<Card> all;
	
	//*********************** Public Static Interface ************************//
	/**
	 * All Room Cards.
	 * @return
	 * A List of all Room Cards.
	 */
	public static List<Card> getRooms() {
		if (all == null) {
			all = buildRooms();
		}
		return new ArrayList<Card>(all);
	}
	
	//**************** Protected and Private Static Interface ****************//
	/**
	 * Builds all Room Card instances and returns them in a List.
	 * @return
	 * The List of all Room Card instances.
	 */
	private static List<Card> buildRooms() {
		List<Card> rooms = new ArrayList<Card>();
		rooms.add(new Room("Ballroom"));
		rooms.add(new Room("Billiards Room"));
		rooms.add(new Room("Conservatory"));
		rooms.add(new Room("Dining Room"));
		rooms.add(new Room("Hall"));
		rooms.add(new Room("Kitchen"));
		rooms.add(new Room("Library"));
		rooms.add(new Room("Lounge"));
		rooms.add(new Room("Study"));
		return rooms;
	}
}
