package clue.exception;

import clue.cards.Card;
import clue.cards.Suggestion;
import clue.testserver.Player;

public class InvalidDisprove extends Exception {
	private static final long serialVersionUID = 597103057981453595L;

	public InvalidDisprove(Player player, Card card, Suggestion suggestion) {
		super(
			player.toString() +
			" tried to disprove " +
			suggestion +
			" with " +
			card + (
				suggestion.has(card) ?
					" which he does not have" :
					" which was not suggested"
			)
		);
	}
}
