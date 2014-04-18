package clue.exception;

import clue.cards.Suggestion;
import clue.testserver.Player;

public class DuplicateSuggestion extends Exception {
	private static final long serialVersionUID = -7138878836454681676L;

	public DuplicateSuggestion(Player player, Suggestion suggestion) {
		super(
			player.toString() +
			" made the duplicate suggestion " +
			suggestion
		);
	}
}
