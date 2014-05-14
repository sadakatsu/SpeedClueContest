package com.sadakatsu.clue.cards;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import com.sadakatsu.clue.cards.Card;

/**
 * The Hand is a collection of Cards.  It is primarily intended to be used as a
 * class for a Match to deliver a Player its hand at the start of the game.
 * 
 * @author Joseph A. Craig
 */
public class Hand {
	//********************* Protected and Private Fields *********************//
	private Set<Card> cards;
	
	//*************************** Public Interface ***************************//
	/**
	 * Instantiates a new Hand holding the passed Cards.
	 * @param cards
	 * The Cards in the hand.
	 */
	public Hand(Collection<? extends Card> cards) {
		this.cards = new HashSet<>(cards);
	}
	
	/**
	 * Determines whether this Hand has the passed Card in it.
	 * @param card
	 * The Card in question.
	 * @return
	 * true if the Hand has the passed Card in it, false otherwise.
	 */
	public boolean has(Card card) {
		return cards.contains(card);
	}
	
	/**
	 * Determines whether this Hand does not have the passed Card in it.
	 * @param card
	 * The Card in question.
	 * @return
	 * true if the Hand does not have the passed Card in it, false otherwise.
	 */
	public boolean doesNotHave(Card card) {
		return !cards.contains(card);
	}

	/**
	 * Determines whether the passed Object is a Collection of Cards that
	 * contains the same Cards in this Hand (no more and no less).
	 */
	@Override
	public boolean equals(Object obj) {
		boolean equal = false;
		
		if (obj instanceof Collection<?>) {
			Collection<?> c = (Collection<?>) obj;
			equal = (
				cards.size() == c.size() &&
				cards.containsAll(c) &&
				c.containsAll(cards)
			);
		}
		
		return equal;
	}

	@Override
	public int hashCode() {
		return cards.hashCode();
	}
	
	/**
	 * Returns an unmodifiable Collection of the Cards in this Hand.
	 * @return
	 */
	public Collection<Card> getCards() {
		return Collections.unmodifiableSet(cards);
	}
	
	/**
	 * Returns the Cards that are in both this Hand and in the Suggestion.
	 * These are the Cards that this Hand's Player could use to disprove the
	 * Suggestion.
	 * @param suggestion
	 * The Suggestion in question.
	 * @return
	 * A Collection of the Cards that are in both the Hand and the Suggestion.
	 * This Collection may be empty.
	 */
	public Collection<Card> getDisproveCards(Suggestion suggestion) {
		Set<Card> intersection = new HashSet<>(cards);
		intersection.retainAll(suggestion.getCards());
		return intersection;
	}
	
	/**
	 * Returns a String that lists this Hand's Cards' Abbreviations separated by
	 * spaces.
	 * @return
	 * A String formatted as described above.
	 */
	public String getAbbreviation() {
		StringBuilder sb = new StringBuilder();
		for (Card c : cards) {
			if (sb.length() > 0) {
				sb.append(" ");
			}
			sb.append(c.getAbbreviation());
		}
		return sb.toString();
	}
	
	/**
	 * Returns a String describing how many of each Card type are contained in
	 * this Hand.  The order is Suspect count, Weapon count, and Room count.
	 * Each of these numbers is separated by a comma.  For example, a return
	 * value of "1,2,0" means that this Hand holds one Suspect, two Weapons, and
	 * zero Rooms.
	 * @return
	 * A String describing this Hand's Card distribution.
	 */
	public String getDistributionString() {
		int suspects = 0;
		int weapons = 0;
		int rooms = 0;
		
		for (Card c : cards) {
			if (c instanceof Suspect) {
				++suspects;
			} else if (c instanceof Weapon) {
				++weapons;
			} else if (c instanceof Room) {
				++rooms;
			}
		}
		
		return String.format("%d,%d,%d", suspects, weapons, rooms);
	}
	
	/**
	 * Returns a user-friendly description of the Cards in this Hand.
	 */
	@Override
	public String toString() {
		boolean listStarted = false;
		StringBuilder sb = new StringBuilder("Hand{");
		for (Card c : cards) {
			if (listStarted) {
				sb.append(", ");
			} else {
				listStarted = true;
			}
			sb.append(c);
		}
		sb.append("}");
		return sb.toString();
	}
}
