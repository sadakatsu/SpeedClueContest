package clue.exception;

import clue.cards.Card;

public class IncorrectCategoryException extends Exception {
	private static final long serialVersionUID = -2876478606857224825L;
	
	public IncorrectCategoryException(Class<? extends Card> a, Card b) {
		super("Expected " + a.getSimpleName() + ", got " + b);
	}
}
