package com.sadakatsu.clue.cards;

import java.util.ArrayList;
import java.util.List;

import com.sadakatsu.clue.exception.IncorrectCategory;
import com.sadakatsu.clue.exception.InvalidSuggestionString;

/**
 * The Suggestion class represents three important and similar Clue concepts:
 * solution, suggestion, and accusation.   The purpose of Clue is to determine
 * who killed Mr. Boddy with what and where.  This is a tuple of Suspect,
 * Weapon, and Room.  The game's solution, the suggestions the players make each
 * turn, and the accusations raised when players try to win all have this same
 * structure.  Since suggestions are the most commonly referenced concept when
 * playing Clue, "Suggestion" was chosen as the name for the structure for all
 * three.
 * 
 * @author Joseph A. Craig
 */
public class Suggestion {
	//********************* Protected and Private Fields *********************//
	private Suspect s;
	private Weapon w;
	private Room r;
	
	//*************************** Public Interface ***************************//
	/**
	 * Builds a Suggestion from the passed Suspect, Weapon, and Room.
	 * @param suspect
	 * The Suspect for the Suggestion.
	 * @param weapon
	 * The Suspect for the Suggestion.
	 * @param room
	 * The Suspect for the Suggestion.
	 */
	public Suggestion(Suspect suspect, Weapon weapon, Room room) {
		s = suspect;
		w = weapon;
		r = room;
	}
	
	/**
	 * Builds a Suggestion from the passed Cards.
	 * @param suspect
	 * The Suspect for the Suggestion.
	 * @param weapon
	 * The Weapon for the Suggestion.
	 * @param room
	 * The Room for the Suggestion.
	 * @throws IncorrectCategory
	 */
	public Suggestion(
		Card suspect,
		Card weapon,
		Card room
	) throws IncorrectCategory {
		initializeWith(suspect, weapon, room);
	}
	
	/**
	 * Builds a Suggestion from a server-player-style message, which has the
	 * format "Su We Ro".  This is the format for a Suggestion abbreviation.
	 * @param from
	 * The String from which to build the Suggestion.
	 * @throws InvalidSuggestionString
	 */
	public Suggestion(String from) throws InvalidSuggestionString {
		// Split the abbreviation by spaces.  If this does not leave three
		// tokens, the message is incorrectly formatted.
		String[] tokens = from.split("\\s");
		if (tokens.length != 3) {
			throw new InvalidSuggestionString(
				from,
				"Expected only three Card abbreviations, received"
			);
		}
		
		// Get the Card instances from the message tokens.  If any of the Card
		// abbreviations is invalid, the message is incorrectly formatted.
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
		
		// Try to initialize using the Cards from the message.  If any of the
		// Cards is the wrong Category, the message is incorrectly formatted.
		try {
			initializeWith(cards[0], cards[1], cards[2]);
		} catch (IncorrectCategory e) {
			throw new InvalidSuggestionString(from, e.getMessage());
		}
	}
	
	/**
	 * Determines whether two Suggestions are similar.
	 */
	public boolean equals(Object other) {
		if (!(other instanceof Suggestion)) {
			return false;
		}
		Suggestion that = (Suggestion) other;
		return this.s == that.s && this.w == that.w && this.r == that.r;
	}
	
	/**
	 * Determines whether the passed Card is in this Suggestion.
	 * @param card
	 * The Card whose membership to check.
	 * @return
	 * true if the Card is in the Suggestion, false otherwise.
	 */
	public boolean has(Card card) {
		return s == card || w == card || r == card;
	}
	
	/**
	 * This Suggestion's Room.
	 * @return
	 * The Room Card.
	 */
	public Card getRoom() {
		return r;
	}
	
	/**
	 * This Suggestion's Suspect.
	 * @return
	 * The Suspect Card.
	 */
	public Card getSuspect() {
		return s;
	}
	
	/**
	 * This Suggestion's Weapon.
	 * @return
	 * The Weapon Card.
	 */
	public Card getWeapon() {
		return w;
	}
	
	/**
	 * All this Suggestion's Cards in "Suspect Weapon Room" order.
	 * @return
	 * A List of this Suggestion's Cards.
	 */
	public List<Card> getCards() {
		List<Card> cards = new ArrayList<>();
		cards.add(s);
		cards.add(w);
		cards.add(r);
		return cards;
	}
	
	/**
	 * A String abbreviation for the Suggestion, such as is found in server-
	 * player messaging.
	 * @return
	 * A String in the format "Su We Ro" that represents this Suggestion.
	 */
	public String getAbbreviation() {
		return
			s.getAbbreviation() +
			" " +
			w.getAbbreviation() +
			" " +
			r.getAbbreviation();
	}
	
	/**
	 * Returns a user-friendly String representation of this Suggestion.
	 */
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
	
	//******************* Protected and Private Interface ********************//
	/**
	 * Determines that each of the Cards is of the expected category, then
	 * initializes this Suggestion with the passed Cards.
	 * @param suspect
	 * @param weapon
	 * @param room
	 * @throws IncorrectCategory
	 */
	private void initializeWith(
		Card suspect,
		Card weapon,
		Card room
	) throws IncorrectCategory {
		validateCardTypes(suspect, weapon, room);
		s = (Suspect) suspect;
		w = (Weapon) weapon;
		r = (Room) room;
	}
	
	/**
	 * Validates that each of the passed Cards is of the expected category.
	 * @param suspect
	 * A Suspect Card.
	 * @param weapon
	 * A Weapon Card.
	 * @param room
	 * A Room Card.
	 * @throws IncorrectCategory
	 */
	private void validateCardTypes(
		Card suspect,
		Card weapon,
		Card room
	) throws IncorrectCategory {
		if (!(suspect instanceof Suspect)) {
			throw new IncorrectCategory(Suspect.class, suspect);
		}
		
		if (!(weapon instanceof Weapon)) {
			throw new IncorrectCategory(Weapon.class, weapon);
		}
		
		if (!(room instanceof Room)) {
			throw new IncorrectCategory(Room.class, room);
		}
	}
	
	//***************** Protected and Private Static Fields ******************//
	private static List<Suggestion> all;
	
	//*********************** Public Static Interface ************************//
	/**
	 * All 324 possible Suggestions.
	 * @return
	 * A List of all the possible Suggestions.
	 */
	public static List<Suggestion> getSuggestions() {
		if (all == null) {
			all = buildAll();
		}
		return new ArrayList<Suggestion>(all);
	}
	
	//**************** Protected and Private Static Interface ****************//
	/**
	 * Instantiates every possible Suggestion.
	 * @return
	 * Returns a List of every possible Suggestion.
	 */
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
}
