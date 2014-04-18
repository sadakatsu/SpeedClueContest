package clue.cards;

import java.util.ArrayList;
import java.util.List;

public class Weapon extends Card {
	private Weapon(String name) {
		super(name);
	}
	
	private static List<Card> all = null;
	
	private static List<Card> buildWeapons() {
		List<Card> weapons = new ArrayList<Card>();
		weapons.add(new Weapon("Candlestick"));
		weapons.add(new Weapon("Knife"));
		weapons.add(new Weapon("Pipe"));
		weapons.add(new Weapon("Revolver"));
		weapons.add(new Weapon("Rope"));
		weapons.add(new Weapon("Wrench"));
		return weapons;
	}
	
	public static List<Card> getWeapons() {
		if (all == null) {
			all = buildWeapons();
		}
		return new ArrayList<Card>(all);
	}
}
