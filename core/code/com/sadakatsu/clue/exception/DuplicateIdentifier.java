package com.sadakatsu.clue.exception;

/**
 * The DuplicateIdentifier exception is thrown when the EntryScript.process()
 * method finds that AIs in the entry script have been assigned the same
 * identifier.  An identifier has to be unique.
 * 
 * @author Joseph A. Craig
 */
@SuppressWarnings("serial")
public class DuplicateIdentifier extends Exception {
	/**
	 * Instantiates a new DuplicateIdentifier instance with the passed
	 * identifier.
	 * @param identifier
	 * The identifier that was duplicated in an entry script.
	 */
	public DuplicateIdentifier(String identifier) {
		super(
			String.format(
				"The agent launch script duplicates the identifier \"%s\".",
					identifier
			)
		);
	}
}
