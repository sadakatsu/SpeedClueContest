package com.sadakatsu.clue.exception;

import com.sadakatsu.clue.contestserver.Player;

/**
 * The TimeoutViolation is a ClueException for disqualifying Players that take
 * too long to respond to the server.  It provides the contest's timeout value
 * in a public static final field so that other classes can refer to it.
 * @author JAC
 */
@SuppressWarnings("serial")
public class TimeoutViolation extends ClueException {
	/**
	 * Instantiates a TimeoutViolation object.
	 * @param player
	 * The Player that timed out.
	 */
	public TimeoutViolation(Player player) {
		super(
			player,
			"%s exceeded the contest time limit for responding to a server " +
			"message.",
			player
		);
	}
	
	/**
	 * The number of milliseconds an AI may take to respond to any server
	 * message.
	 */
	public static final int TIMEOUT = 10000;
}
