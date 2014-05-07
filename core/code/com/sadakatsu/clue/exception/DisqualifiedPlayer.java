package com.sadakatsu.clue.exception;

import com.sadakatsu.clue.contestserver.Player;

/**
 * The DisqualifiedPlayer exception is thrown by the Match class if any of the
 * Players passed to it to play the game were disqualified before being passed
 * in to the Match constructor.  Players are not allowed to participate in any
 * more Matches after having been disqualified.
 * 
 * @author Joseph A. Craig
 */
@SuppressWarnings("serial")
public class DisqualifiedPlayer extends Exception {
	/**
	 * Instantiates a new DisqualifiedPlayer instance for the passed Player.
	 * @param player
	 * A disqualified Player that was passed to the Match constructor.
	 */
	public DisqualifiedPlayer(Player player) {
		super(
			String.format(
				"A Match was passed %s, but it is disqualified.",
				player
			)
		);
	}
}
