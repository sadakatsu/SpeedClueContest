package clue.exception;

import clue.testserver.Player;

public class ProtocolViolation extends Exception {
	private static final long serialVersionUID = 5488387073592841355L;

	public ProtocolViolation(
		Player player,
		String messageType,
		String received
	) {
		super(
			player.toString() +
			" sent an invalid " +
			messageType +
			" message: \"" +
			received +
			"\""
		);
	}
}
