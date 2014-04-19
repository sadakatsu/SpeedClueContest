package com.sadakatsu.clue.exception;

import com.sadakatsu.clue.cards.Suggestion;
import com.sadakatsu.clue.testserver.Player;

/**
 * An Exception that represents that a Player (server-side) or AI (client-side)
 * tried to make the same Suggestion more than once.  Doing so is a violation of
 * the contest rules.
 * 
 * @author Joseph A. Craig
 */
@SuppressWarnings("serial")
public class DuplicateSuggestion extends Exception {
	/**
	 * Instantiates a DuplicateSuggestion that reports that an AI attempted to
	 * make a Suggestion more than once.
	 * @param suggestion
	 * The Suggestion in question.
	 */
	public DuplicateSuggestion(Suggestion suggestion) {
		super(String.format("Made the duplicate Suggestion %s.", suggestion));
	}
	
	/**
	 * Instantiates a DuplicteSuggestion that reports that a connected Player
	 * attempted to make a Suggestion more than once.
	 * @param player
	 * The Player that violated the rules.
	 * @param suggestion
	 * The Suggestion that the Player tried to make more than once.
	 */
	public DuplicateSuggestion(Player player, Suggestion suggestion) {
		super(
			String.format(
				"%s made the duplicate Suggestion %s.",
				player,
				suggestion
			)
		);
	}	
}
