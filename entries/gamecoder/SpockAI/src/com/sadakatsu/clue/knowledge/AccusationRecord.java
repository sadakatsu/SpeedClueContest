package com.sadakatsu.clue.knowledge;

import com.sadakatsu.clue.cards.Suggestion;

public class AccusationRecord implements Record {
	public final boolean correct;
	public final int accuser;
	public final Suggestion accusation;
	
	public AccusationRecord(
		int accuser,
		Suggestion accusation,
		boolean correct
	) {
		this.accusation = accusation;
		this.accuser = accuser;
		this.correct = correct;
	}
}
