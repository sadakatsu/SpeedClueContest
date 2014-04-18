package clue.cards;

import java.util.ArrayList;
import java.util.List;

public abstract class Card {
	private String name = "";
	
	protected Card(String name) {
		this.name = name;
	}
	
	public String toString() {
		return this.getClass().getSimpleName() + "{" + name + "}";
	}
	
	public String getName() {
		return name;
	}
	
	public String getAbbreviation() {
		return name.substring(0, 2);
	}
	
	private static List<Card> all = null;
	
	private static List<Card> buildCards() {
		List<Card> cards = Suspect.getSuspects();
		cards.addAll(Weapon.getWeapons());
		cards.addAll(Room.getRooms());
		return cards;
	}
	
	public static List<Card> getCards() {
		if (all == null) {
			all = buildCards();
		}
		return new ArrayList<Card>(all);
	}
	
	public static Card from(String abbreviation) {
		Card card = null;
		String abbr = abbreviation.substring(0, 2);
		
		if (all == null) {
			all = buildCards();
		}
		for (Card c : all) {
			if (c.getAbbreviation().equalsIgnoreCase(abbr)) {
				card = c;
				break;
			}
		}
		
		return card;
	}
}
