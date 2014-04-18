package clue.cards;

import java.util.ArrayList;
import java.util.List;

public class Suspect extends Card {
	private Suspect(String name) {
		super(name);
	}
	
	private static List<Card> all = null;
	
	private static List<Card> buildSuspects() {
		List<Card> suspects = new ArrayList<Card>();
		suspects.add(new Suspect("Green"));
		suspects.add(new Suspect("Mustard"));
		suspects.add(new Suspect("Peacock"));
		suspects.add(new Suspect("Plum"));
		suspects.add(new Suspect("Scarlet"));
		suspects.add(new Suspect("White"));
		return suspects;
	}
	
	public static List<Card> getSuspects() {
		if (all == null) {
			all = buildSuspects();
		}
		return new ArrayList<Card>(all);
	}
}
