package clue.exception;

public class InvalidSuggestionString extends Exception {
	private static final long serialVersionUID = -4107026288076171608L;

	public InvalidSuggestionString(String offender, String message) {
		super(message + ": \"" + offender + "\"");
	}
}
