package clue.cards;

import java.util.ArrayList;
import java.util.List;

public class Room extends Card {
	private Room(String name) {
		super(name);
	}
	
	private static List<Card> all = null;
	
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
	
	public static List<Card> getRooms() {
		if (all == null) {
			all = buildRooms();
		}
		return new ArrayList<Card>(all);
	}
}
