package com.sadakatsu.clue.knowledge.exceptions;

public class Contradiction extends Exception {
	private static final long serialVersionUID = -4170180848719188923L;

	public Contradiction(String format, Object...args) {
		super(String.format(format, args));
	}
}
