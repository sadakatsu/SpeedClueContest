package com.sadakatsu.clue.exception;

import java.util.Set;

import com.sadakatsu.clue.cards.Card;
import com.sadakatsu.clue.cards.Suggestion;
import com.sadakatsu.clue.contestserver.Player;

/**
 * The SuicidalAccusation exception means that an AI chose to make an accusation
 * that used at least one Card that it has seen, either by having been dealt the
 * Card or having been shown the Card when one of its Suggestions was disproved.
 * This error means that the AI is not designed correctly to win Speed Clue
 * games.  A Player that throws this exception is indicating that the connected
 * AI must be disqualified.
 * @author JAC
 *
 */
@SuppressWarnings("serial")
public class SuicidalAccusation extends ClueException {
	/**
	 * Instantiates a new SuicidalAccusation instance.
	 * @param player
	 * The Player whose connected AI made a suicidal accusation.
	 * @param accusation
	 * The accusation in question.
	 * @param seen
	 * The Cards the Player's connected AI must know, either by having been
	 * dealt them or having been shown them when its Suggestions were disproved. 
	 */
	public SuicidalAccusation(
		Player player,
		Suggestion accusation,
		Set<Card> seen
	) {
		super(
			player,
			"%s tried to make accusation %s when it has seen %s.",
				player,
				accusation,
				seen
		);
	}
}
