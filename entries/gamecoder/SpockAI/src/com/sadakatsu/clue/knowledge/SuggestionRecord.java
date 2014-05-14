package com.sadakatsu.clue.knowledge;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import com.sadakatsu.clue.cards.Card;
import com.sadakatsu.clue.cards.Suggestion;

public class SuggestionRecord implements Record {
	public final int suggester;
	public final Integer disprover;
	public final Set<Card> shown;
	public final Suggestion suggestion;
	
	public SuggestionRecord(
		int suggester,
		Suggestion suggestion,
		Integer disprover,
		Card shown,
		PlayerFacts disproverFacts
	) {
		this.suggester = suggester;
		this.suggestion = suggestion;
		this.disprover = disprover;
		
		if (disprover == null) {
			this.shown = null;
		} else {
			this.shown = Collections.unmodifiableSet(
				getPossiblyShownCards(
					disproverFacts,
					shown
				)
			);
		}
	}
	
	public Card getShownCard() {
		if (shown == null || shown.size() > 1) {
			return null;
		}
		
		return shown.iterator().next();
	}
	
	public SuggestionRecord refine(PlayerFacts disprover) {
		if (shown == null || shown.size() == 1) {
			return this;
		}
		
		Set<Card> possiblyShown = getPossiblyShownCards(disprover, null);
		if (possiblyShown.equals(shown)) {
			return this;
		}
		
		return new SuggestionRecord(this, possiblyShown);
	}
	
	private SuggestionRecord(
		SuggestionRecord previous,
		Set<Card> possiblyShown
	) {
		this.disprover = previous.disprover;
		this.shown = Collections.unmodifiableSet(possiblyShown);
		this.suggester = previous.suggester;
		this.suggestion = previous.suggestion;
	}
	
	private Set<Card> getPossiblyShownCards(
		PlayerFacts disprover,
		Card shown
	) {
		Set<Card> possible = new HashSet<>();
		if (shown == null) {
			for (Card c : suggestion.getCards()) {
				if (disprover.has(c) != PlayerHasCard.NO) {
					possible.add(c);
				}
			}
		} else {
			possible.add(shown);
		}
		return possible;
	}
}
