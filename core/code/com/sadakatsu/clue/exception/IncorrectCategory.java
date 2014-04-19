package com.sadakatsu.clue.exception;

import com.sadakatsu.clue.cards.Card;

/**
 * An IncorrectCategory represents that the Suggestion class was passed the
 * wrong type of Card in its constructor.  For example, it might have received
 * Suspect{Plum} for its Weapon argument.
 * 
 * @author Joseph A. Craig
 */
@SuppressWarnings("serial")
public class IncorrectCategory extends Exception {
	/**
	 * Instantiates an IncorrectCategoryException for the passed category and
	 * Card.
	 * @param category
	 * The Category for which the passed Card is incorrect.
	 * @param card
	 * The Card that was passed to Suggestion for the passed Category.
	 */
	public IncorrectCategory(
		Class<? extends Card> category,
		Card card
	) {
		super(
			String.format(
				"Expected %s, got %s",
				category.getSimpleName(),
				card
			)
		);
	}
}
