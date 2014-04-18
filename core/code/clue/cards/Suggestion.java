package clue.cards;

import java.util.ArrayList;
import java.util.List;

import clue.exception.*;

public class Suggestion {
	private Suspect s;
	private Weapon w;
	private Room r;
	
	public Suggestion(Suspect suspect, Weapon weapon, Room room) {
		s = suspect;
		w = weapon;
		r = room;
	}
	
	public Suggestion(
		Card suspect,
		Card weapon,
		Card room
	) throws IncorrectCategoryException {
		initializeWith(suspect, weapon, room);
	}
	
	public Suggestion(String from) throws InvalidSuggestionString {
		String[] tokens = from.split("\\s");
		if (tokens.length != 3) {
			throw new InvalidSuggestionString(
				from,
				"Expected only three Card abbreviations, received"
			);
		}
		
		Card[] cards = new Card[3];
		for (int i = 0; i < 3; i++) {
			String t = tokens[i];
			if (t.length() != 2 || (cards[i] = Card.from(t)) == null) {
				throw new InvalidSuggestionString(
					from,
					"Found invalid Card abbreviation \"" + t + "\""
				);
			}
		}
		
		try {
			initializeWith(cards[0], cards[1], cards[2]);
		} catch (IncorrectCategoryException e) {
			throw new InvalidSuggestionString(from, e.getMessage());
		}
	}
	
	public boolean equals(Object other) {
		if (!(other instanceof Suggestion)) {
			return false;
		}
		Suggestion that = (Suggestion) other;
		return this.s == that.s && this.w == that.w && this.r == that.r;
	}
	
	public boolean has(Card card) {
		return s == card || w == card || r == card;
	}
	
	public Card getRoom() {
		return r;
	}
	
	public Card getSuspect() {
		return s;
	}
	
	public Card getWeapon() {
		return w;
	}
	
	public List<Card> getCards() {
		List<Card> cards = new ArrayList<>();
		cards.add(s);
		cards.add(w);
		cards.add(r);
		return cards;
	}
	
	public String getAbbreviation() {
		return
			s.getAbbreviation() +
			" " +
			w.getAbbreviation() +
			" " +
			r.getAbbreviation();
	}
	
	public String toString() {
		StringBuilder sb = new StringBuilder("Suggestion{");
		sb.append(s.getName());
		sb.append(", ");
		sb.append(w.getName());
		sb.append(", ");
		sb.append(r.getName());
		sb.append("}");
		return sb.toString();
	}
	
	private void initializeWith(
		Card suspect,
		Card weapon,
		Card room
	) throws IncorrectCategoryException {
		validateCardTypes(suspect, weapon, room);
		s = (Suspect) suspect;
		w = (Weapon) weapon;
		r = (Room) room;
	}
	
	private void validateCardTypes(
		Card suspect,
		Card weapon,
		Card room
	) throws IncorrectCategoryException {
		if (!(suspect instanceof Suspect)) {
			throw new IncorrectCategoryException(Suspect.class, suspect);
		}
		
		if (!(weapon instanceof Weapon)) {
			throw new IncorrectCategoryException(Weapon.class, weapon);
		}
		
		if (!(room instanceof Room)) {
			throw new IncorrectCategoryException(Room.class, room);
		}
	}
	
	private static List<Suggestion> all = buildAll();
	
	private static List<Suggestion> buildAll() {
		List<Suggestion> suggestions = new ArrayList<Suggestion>();
		for (Card s : Suspect.getSuspects()) {
			for (Card w : Weapon.getWeapons()) {
				for (Card r : Room.getRooms()) {
					suggestions.add(
						new Suggestion(
							(Suspect) s,
							(Weapon) w,
							(Room) r
						)
					);
				}
			}
		}
		return suggestions;
	}
	
	public static List<Suggestion> getSuggestions() {
		return new ArrayList<Suggestion>(all);
	}
}
