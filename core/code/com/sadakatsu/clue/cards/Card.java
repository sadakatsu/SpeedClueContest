package com.sadakatsu.clue.cards;

import java.util.ArrayList;
import java.util.List;

/**
 * The Card class represents a Card from the game of Clue.  There are three
 * types of Cards -- Suspect, Weapon, and Room -- which have their own
 * subclasses.
 * 
 * @author Joseph A. Craig
 */
public abstract class Card {
	//********************* Protected and Private Fields *********************//
	private String abbreviation;
	private String name;
	
	//*************************** Public Interface ***************************//
	/**
	 * The two-letter abbreviation for the Card used in server-player messaging.
	 * @return
	 * The abbreviation String.
	 */
	public String getAbbreviation() {
		return abbreviation;
	}
	
	/**
	 * The full name of the Card.
	 * @return
	 * The name String.
	 */
	public String getName() {
		return name;
	}
	
	/**
	 * Returns a user-friendly representation for this Card.
	 */
	public String toString() {
		return this.getClass().getSimpleName() + "{" + name + "}";
	}
	
	//******************* Protected and Private Interface ********************//
	/**
	 * Instantiates a new Card with the passed name.
	 * @param name
	 * The name of the Card.
	 */
	protected Card(String name) {
		this.name = name;
		abbreviation = name.substring(0, 2);
	}
	
	//***************** Protected and Private Static Fields ******************//
	private static List<Card> all = null;
	
	//*********************** Public Static Interface ************************//
	/**
	 * All the Cards in Clue.
	 * @return
	 * A List of the Cards.
	 */
	public static List<Card> getCards() {
		if (all == null) {
			all = buildCards();
		}
		return new ArrayList<Card>(all);
	}
	
	/**
	 * Finds and returns the Card whose abbreviation matches the passed
	 * abbreviation when ignoring case.
	 * @param abbreviation
	 * The abbreviation of the Card to get.
	 * @return
	 * The Card instance if the abbreviation is valid, null otherwise.
	 */
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
	
	//**************** Protected and Private Static Interface ****************//
	/**
	 * Builds a List of all the Clue Cards by interacting with the subclasses.
	 * @return
	 * The List of Clue Cards.
	 */
	private static List<Card> buildCards() {
		List<Card> cards = Suspect.getSuspects();
		cards.addAll(Weapon.getWeapons());
		cards.addAll(Room.getRooms());
		return cards;
	}
}
