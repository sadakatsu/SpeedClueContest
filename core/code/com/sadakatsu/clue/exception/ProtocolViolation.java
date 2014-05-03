package com.sadakatsu.clue.exception;

import com.sadakatsu.clue.server.Player;

/**
 * A ProtocolViolation means that a server/AI received a message that does not
 * match the contest's specified protocol.
 * 
 * @author Joseph A. Craig
 */
@SuppressWarnings("serial")
public class ProtocolViolation extends Exception {
	/**
	 * Instantiates a ProtocolViolation for an unintelligible message.
	 * @param message
	 * The message that was received.
	 */
	public ProtocolViolation(String message) {
		super(String.format("Received an unknown message: \"%s\"", message));
	}
	
	/**
	 * Instantiates a ProtocolViolation for a violation of a specific message
	 * format in the protocol. 
	 * @param messageType
	 * The type of message that was supposed to be received.
	 * @param message
	 * The message that was received.
	 */
	public ProtocolViolation(String messageType, String message) {
		super(
			String.format(
				"Received an invalid %s message: \"%s\"",
				messageType,
				message
			)
		);
	}
	
	/**
	 * Instantiates a ProtocolViolation for a violation of a specific message
	 * format in the protocol. 
	 * @param player
	 * The Player that issued the bad message.
	 * @param messageType
	 * The type of message that was supposed to be received.
	 * @param received
	 * The message that was received.
	 */
	public ProtocolViolation(
		Player player,
		String messageType,
		String received
	) {
		super(
			String.format(
				"%s sent an invalid %s message: \"%s\"",
				player,
				messageType,
				received
			)
		);
	}
	
	
	
	
}
