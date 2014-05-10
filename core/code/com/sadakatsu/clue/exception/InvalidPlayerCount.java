package com.sadakatsu.clue.exception;

/**
 * An InvalidPlayerCount exception is thrown by the Match class if fewer than
 * three or more than six Players are passed to participate in the game.  Speed
 * Clue is a game for three to six Players.
 * @author JAC
 *
 */
@SuppressWarnings("serial")
public class InvalidPlayerCount extends Exception {
	/**
	 * Instantiates a new InvalidPlayerCount instance for the passed Player
	 * count.
	 * @param count
	 * The number of Players with which a user attempted to instantiate a Match.
	 */
	public InvalidPlayerCount(int count) {
		super(String.format("Invalid number of players for a game: %d", count));
	}
}
