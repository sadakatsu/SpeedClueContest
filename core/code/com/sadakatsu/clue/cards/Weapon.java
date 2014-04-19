package com.sadakatsu.clue.cards;

import java.util.ArrayList;
import java.util.List;

/**
 * A Weapon is a type of Clue Card.  A Weapon is an item that might have been
 * used to murder Mr. Boddy.
 * 
 * @author Joseph A. Craig
 */
public class Weapon extends Card {
	//******************* Protected and Private Interface ********************//
	/**
	 * Builds a Weapon Card.
	 * @param name
	 * The Weapon's name.
	 */
	private Weapon(String name) {
		super(name);
	}
	
	//***************** Protected and Private Static Fields ******************//
	private static List<Card> all;
	
	//*********************** Public Static Interface ************************//
	/**
	 * All Weapon Cards.
	 * @return
	 * A List of all Weapon Cards.
	 */
	public static List<Card> getWeapons() {
		if (all == null) {
			all = buildWeapons();
		}
		return new ArrayList<Card>(all);
	}
	
	//**************** Protected and Private Static Interface ****************//
	/**
	 * Builds all Weapon instances and returns them in a List.
	 * @return
	 * The List of all Weapon Card instances.
	 */
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
}
