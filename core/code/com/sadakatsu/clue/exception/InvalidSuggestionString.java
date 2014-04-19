package com.sadakatsu.clue.exception;

/**
 * An InvalidSuggestionString means that the String passed to the
 * Suggestion(String) constructor was not of the format "Su We Ro".
 * 
 * @author Joseph A. Craig
 */
@SuppressWarnings("serial")
public class InvalidSuggestionString extends Exception {
	/**
	 * Instantiates an InvalidSuggestionString.
	 * @param badString
	 * The String that was passed to the Suggestion(String) constructor.
	 * @param problem
	 * A String explaining the problem with badString.
	 */
	public InvalidSuggestionString(String badString, String problem) {
		super(String.format("%s: \"%s\"", problem, badString));
	}
}
