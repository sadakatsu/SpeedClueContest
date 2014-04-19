package com.sadakatsu.clue.cards;

import java.util.ArrayList;
import java.util.List;

/**
 * A Suspect is a type of Clue Card.  A Suspect is a person who might have
 * murdered Mr. Boddy.
 * 
 * @author Joseph A. Craig
 */
public class Suspect extends Card {
	//******************* Protected and Private Interface ********************//
	/**
	 * Builds a Suspect Card.
	 * @param name
	 * The Card's name.
	 */
	private Suspect(String name) {
		super(name);
	}
	
	//***************** Protected and Private Static Fields ******************//
	private static List<Card> all;
	
	//*********************** Public Static Interface ************************//
	/**
	 * All Suspect Cards.
	 * @return
	 * A List of all Suspect Cards.
	 */
	public static List<Card> getSuspects() {
		if (all == null) {
			all = buildSuspects();
		}
		return new ArrayList<Card>(all);
	}
	
	//**************** Protected and Private Static Interface ****************//
	/**
	 * Builds all Suspect Card instances and returns them in a List.
	 * @return
	 * The List of all Suspect Card instances.
	 */
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
}
