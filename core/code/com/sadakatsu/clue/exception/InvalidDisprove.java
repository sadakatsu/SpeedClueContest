package com.sadakatsu.clue.exception;

import com.sadakatsu.clue.cards.Card;
import com.sadakatsu.clue.cards.Suggestion;
import com.sadakatsu.clue.server.Player;

/**
 * The InvalidDisprove represents that a Player (server-side) or AI (client-
 * side) tried to disprove a Suggestion with a Card that is not in the
 * intersection between the Suggestion's Cards and the player's/AI's hand.  This
 * is against the rules.
 * 
 * @author Joseph A. Craig
 */
@SuppressWarnings("serial")
public class InvalidDisprove extends Exception {
	/**
	 * Instantiates an InvalidDisprove for the passed Card and Suggestion.
	 * @param card
	 * The Card the AI tried to show.
	 * @param suggestion
	 * The Suggestion that the AI tried to disprove.
	 */
	public InvalidDisprove(Card card, Suggestion suggestion) {
		super(
			String.format(
				"Tried to disprove %s with %s, %s.",
				suggestion,
				card, (
					suggestion.has(card) ?
						" which he does not have" :
						" which was not suggested"
				)
			)
		);
	}
	
	/**
	 * Instantiates an InvalidDisprove for the passed Player, Card, and
	 * Suggestion.
	 * @param player
	 * The Player who improperly tried to disprove the passed Suggestion.
	 * @param card
	 * The Card the Player tried to show.
	 * @param suggestion
	 * The Suggestion in question.
	 */
	public InvalidDisprove(Player player, Card card, Suggestion suggestion) {
		super(
			String.format(
				"%s tried to disprove %s with %s, %s",
				player,
				suggestion,
				card, (
					suggestion.has(card) ?
						" which he does not have" :
						" which was not suggested"
				)
			)
		);
	}
}
