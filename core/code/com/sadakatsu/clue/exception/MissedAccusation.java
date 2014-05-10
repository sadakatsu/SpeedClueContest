package com.sadakatsu.clue.exception;

import com.sadakatsu.clue.contestserver.Player;

/**
 * The MissedAccusation exception means that a connected AI failed to repeat an
 * undisproved Suggestion it made when it holds none of the Cards from the
 * Suggestion in its Hand.  This means that the undisproved Suggestion is the
 * solution, and the AI is not designed to make this obvious deduction.  This
 * error disqualifies an AI. 
 * @author Joseph A. Craig
 *
 */
@SuppressWarnings("serial")
public class MissedAccusation extends ClueException {
	/**
	 * Instantiates a new MissedAccusation exception as having been caused by
	 * the passed Player
	 * @param player
	 * The Player that missed an accusation.
	 */
	public MissedAccusation(Player player) {
		super(
			player,
			"%s did not repeat an undisproved suggestion that contained no " +
			"cards from its hand.",
			player
		);
	}
}
