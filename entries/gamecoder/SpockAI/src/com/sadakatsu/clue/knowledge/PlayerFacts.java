package com.sadakatsu.clue.knowledge;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import com.sadakatsu.clue.cards.Card;
import com.sadakatsu.clue.cards.Hand;
import com.sadakatsu.clue.cards.Suggestion;
import com.sadakatsu.clue.knowledge.exceptions.Contradiction;
import com.sadakatsu.util.Combinations;

/**
 * The PlayerFacts class is an immutable class designed to store information
 * about what can be determined about a Player's Hand given assertions.  Making
 * assertions using the ifHas() and ifHasNone() methods will return new
 * PlayerFacts instances that use the passed information to reduce the potential
 * Hands the represented Player can have.  A PlayerFacts instance can also be
 * queried to determine what has been learned and what remains to be learned.
 * 
 * @author Joseph A. Craig
 */
public class PlayerFacts {
	//********************* Protected and Private Fields *********************//
	private int handSize;
	private Map<Card, PlayerHasCard> cards;
	private Set<Hand> possibleHands;
	
	//*************************** Public Interface ***************************//
	/**
	 * Instantiates a new PlayerFacts instance that means that a given Player
	 * can only have the passed possible Hands.
	 * @param possibleHands
	 * The only Hands the represented Player can have.
	 */
	public PlayerFacts(Collection<Hand> possibleHands) {
		handSize = ((Hand) possibleHands.toArray()[0]).getCards().size();
		possibleHands = new HashSet<>(possibleHands);
		cards = new HashMap<>();
		for (Card c : Card.getCards()) {
			cards.put(c, PlayerHasCard.MAYBE);
		}
		learnFromPossibleHands();
	}
	
	/**
	 * Instantiates a new PlayerFacts instance that means that a given Player
	 * has the passed Hand.
	 * @param hand
	 * The Hand the represented Player has.
	 */
	public PlayerFacts(Hand hand) {
		handSize = hand.getCards().size();
		
		cards = new HashMap<>();
		for (Card c : Card.getCards()) {
			cards.put(c, hand.has(c) ? PlayerHasCard.YES : PlayerHasCard.NO);
		}
		
		possibleHands = new HashSet<>();
		possibleHands.add(hand);
	}
	
	/**
	 * Instantiates a new PlayerFacts instance that means that a given Player
	 * can only have Hands of the passed size using only the passed Cards.
	 * @param handSize
	 * The size of the represented Player's Hand.
	 * @param possibleCards
	 * The only Cards that the represented Player may have in his Hand.
	 */
	public PlayerFacts(int handSize, Collection<Card> possibleCards) {
		this.handSize = handSize;
		
		cards = new HashMap<>();
		for (Card c : Card.getCards()) {
			cards.put(
				c,
				possibleCards.contains(c) ?
					PlayerHasCard.MAYBE :
					PlayerHasCard.NO
			);
		}
		
		possibleHands = new HashSet<>();
		for (Collection<Card> c : Combinations.get(possibleCards, handSize)) {
			possibleHands.add(new Hand(c));
		}
	}
	
	/**
	 * Determines whether the passed Object is a PlayerFacts instance that
	 * represents the exact same knowledge state as this PlayerFacts.
	 */
	@Override
	public boolean equals(Object other) {
		boolean result = false;
		if (other instanceof PlayerFacts) {
			PlayerFacts that = (PlayerFacts) other;
			return (
				this == other || (
					this.handSize == that.handSize &&
					this.cards.equals(that.cards) &&
					this.possibleHands.equals(that.possibleHands)
				)
			);
		}
		return result;
	}
	
	/**
	 * @return
	 * A Collection with the Cards the represented Player is known to have.
	 */
	public Collection<Card> getKnownToHaveCards() {
		return getSubset(PlayerHasCard.YES);
	}
	
	/**
	 * @return
	 * A Collection with the Cards the represented Player is known to not have.
	 */
	public Collection<Card> getKnownToNotHaveCards() {
		return getSubset(PlayerHasCard.NO);
	}
	
	/**
	 * @return
	 * A Collection with the Cards for which it is unknown whether the
	 * represented Player holds them or not.
	 */
	public Collection<Card> getUnknownCards() {
		return getSubset(PlayerHasCard.MAYBE);
	}
	
	/**
	 * @return
	 * The number of Hands the represented Player can hold given the assertions
	 * that have been made.
	 */
	public int getPossibleHandCount() {
		return possibleHands.size();
	}
	
	/**
	 * Determines whether the represented Player holds a given Card.
	 * @param card
	 * The Card in question.
	 * @return
	 * YES if the Player holds the Card, NO if the Player does not hold the
	 * Card, or MAYBE if it is currently unknown.
	 */
	public PlayerHasCard has(Card card) {
		return cards.get(card);
	}
	
	/**
	 * Returns a new PlayerFacts instance representing how what is known about
	 * the represented Player changes if it is assumed that the Player must hold
	 * at least one of the passed Cards.
	 * @param cards
	 * The Cards in question.
	 * @return
	 * A new PlayerFacts instance representing this PlayerFacts plus the
	 * additional assertion.
	 * @throws Contradiction
	 * If the assertion contradicts a past assertion or would result in the
	 * represented Player having no possible hands, a Contradiction is thrown.
	 */
	public PlayerFacts ifHas(Card...cards) throws Contradiction {
		return addAssertion(true, cards);
	}
	
	/**
	 * Returns a new PlayerFacts instance representing how what is known about
	 * the represented Player changes if it is assumed that the Player must hold
	 * at least one of the Cards in the passed Collection.
	 * @param cards
	 * The Collection of Cards in question.
	 * @return
	 * A new PlayerFacts instance representing this PlayerFacts plus the
	 * additional assertion.
	 * @throws Contradiction
	 * If the assertion contradicts a past assertion or would result in the
	 * represented Player having no possible hands, a Contradiction is thrown.
	 */
	public PlayerFacts ifHas(Collection<Card> cards) throws Contradiction {
		return ifHas(getArray(cards));
	}
	
	/**
	 * Returns a new PlayerFacts instance representing how what is known about
	 * the represented Player changes if it is assumed that the Player must hold
	 * at least one of the Cards in the passed Suggestion.
	 * @param suggestion
	 * The Suggestion in question.
	 * @return
	 * A new PlayerFacts instance representing this PlayerFacts plus the
	 * additional assertion.
	 * @throws Contradiction
	 * If the assertion contradicts a past assertion or would result in the
	 * represented Player having no possible hands, a Contradiction is thrown.
	 */
	public PlayerFacts ifHas(Suggestion suggestion) throws Contradiction {
		return ifHas(getArray(suggestion.getCards()));
	}
	
	/**
	 * Returns a new PlayerFacts instance representing how what is known about
	 * the represented Player changes if it is assumed that the Player must not
	 * hold any of the passed Cards.
	 * @param cards
	 * The Cards in question.
	 * @return
	 * A new PlayerFacts instance representing this PlayerFacts plus the
	 * additional assertion.
	 * @throws Contradiction
	 * If the assertion contradicts a past assertion or would result in the
	 * represented Player having no possible hands, a Contradiction is thrown.
	 */
	public PlayerFacts ifHasNone(Card...cards) throws Contradiction {
		return addAssertion(false, cards);
	}
	
	/**
	 * Returns a new PlayerFacts instance representing how what is known about
	 * the represented Player changes if it is assumed that the Player must not
	 * hold any of the Cards in the passed Collection.
	 * @param cards
	 * The Collection of Cards in question.
	 * @return
	 * A new PlayerFacts instance representing this PlayerFacts plus the
	 * additional assertion.
	 * @throws Contradiction
	 * If the assertion contradicts a past assertion or would result in the
	 * represented Player having no possible hands, a Contradiction is thrown.
	 */
	public PlayerFacts ifHasNone(Collection<Card> cards) throws Contradiction {
		return ifHasNone(getArray(cards));
	}
	
	/**
	 * Returns a new PlayerFacts instance representing how what is known about
	 * the represented Player changes if it is assumed that the Player must not
	 * hold any of the Cards in the passed Suggestion.
	 * @param suggestion
	 * The Suggestion in question.
	 * @param suggestion
	 * @return
	 * A new PlayerFacts instance representing this PlayerFacts plus the
	 * additional assertion.
	 * @throws Contradiction
	 * If the assertion contradicts a past assertion or would result in the
	 * represented Player having no possible hands, a Contradiction is thrown.
	 */
	public PlayerFacts ifHasNone(Suggestion suggestion) throws Contradiction {
		return ifHasNone(getArray(suggestion.getCards()));
	}
	
	/**
	 * @return
	 * A Set of the Hands the represented Player can have given the past
	 * assertions.
	 */
	public Set<Hand> getPossibleHands() {
		return new HashSet<>(possibleHands);
	}
	
	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder("{");
		boolean notFirst = false;
		for (Card c : Card.getCards()) {
			if (notFirst) {
				sb.append(",");
			} else {
				notFirst = true;
			}
			sb.append(
				String.format("%s:%s", c.getAbbreviation(), cards.get(c))
			);
		}
		sb.append("}");
		return sb.toString();
	}
	
	//******************* Protected and Private Interface ********************//
	/**
	 * Instantiates a new PlayerFacts instance that is a copy of the passed
	 * PlayerFacts instance.
	 * @param that
	 * The PlayerFacts instance to copy.
	 */
	private PlayerFacts(PlayerFacts that) {
		cards = new HashMap<>(that.cards);
		handSize = that.handSize;
		possibleHands = new HashSet<>(that.possibleHands);
	}
	
	/**
	 * Determines whether the passed Hand satisfies the assertion represented by
	 * the other arguments.  If "has" is true, the assertion is that the passed
	 * Hand has at least one of the passed Cards.  If "has" is false, the
	 * assertion is that the passed Hand has none of the passed Cards. 
	 * @param hand
	 * The Hand in question
	 * @param has
	 * true if at least one of the passed Cards should be present in the passed
	 * Hand, false if none of the passed Cards may be present in the passed
	 * Hand
	 * @param cards
	 * The Cards whose Hand membership is defined by "has"
	 * @return
	 * true if the Hand satisfies the assertion, false otherwise
	 */
	private boolean handSatisfiesAssertion(
		Hand hand,
		boolean has,
		Card...cards
	) {
		boolean result = !has;
		for (Card c : cards) {
			if (hand.has(c)) {
				result = has;
				break;
			}
		}
		return result;
	}
	
	private Card[] getArray(Collection<Card> items) {
		return items.toArray(new Card[items.size()]);
	}
	
	/**
	 * Returns a new PlayerFacts instance representing how what is known about
	 * the represented Player changes if it is assumed that the Player either
	 * has at least one or has none of the passed Cards. 
	 * @param has
	 * true if the assertion is that the Player has at least one of the passed
	 * Cards, false if the assertion is that the Player has none of the passed
	 * Cards.
	 * @param cards
	 * The Cards in question.
	 * @return
	 * A new PlayerFacts instance representing this PlayerFacts plus the
	 * additional assertion.
	 * @throws Contradiction
	 * If the assertion contradicts a past assertion or would result in the
	 * represented Player having no possible hands, a Contradiction is thrown.
	 */
	private PlayerFacts addAssertion(boolean has, Card...cards)
	throws Contradiction {
		PlayerFacts next = validateAssertion(has, cards);
		if (next == null) {
			next = new PlayerFacts(this);
			if (!has || cards.length == 1) {
				next.updateCardStates(has, cards);
			}
			next.filterHands(has, cards);
			next.learnFromPossibleHands();
		}
		return next;
	}
	
	/**
	 * Determines whether it is valid to assert that the represented Player has
	 * the specified relationship with the passed Cards.
	 * @param has
	 * true if the assertion is that the Player has at least one of the Cards,
	 * false if the assertion is that the Player has none of the Cards.
	 * @param cards
	 * The Cards in question.
	 * @return
	 * This PlayerFacts instance if the assertion is already reflected in the
	 * internal state, or null if the assertion is possible.
	 * @throws Contradiction
	 * If the passed assertion contradicts a previous assertion reflected in
	 * this PlayerFacts's internal state, a Contradiction is thrown.
	 */
	private PlayerFacts validateAssertion(boolean has, Card...cards)
	throws Contradiction {
		// If the assertion is that the Player holds at least one of the Cards,
		// this Player may be in one of three states:
		// - This Player does not hold any of the Cards.  This should throw a
		//   Contradiction.
		// - This Player does hold at least one of the Cards.  This should
		//   return this PlayerFacts.
		// - At least one of the Cards is listed as a MAYBE.  This should return
		//   null.
		if (has) {
			boolean valid = false;
			for (Card c : cards) {
				PlayerHasCard state = this.cards.get(c);
				if (state == PlayerHasCard.YES) {
					return this;
				} else if (state == PlayerHasCard.MAYBE) {
					valid = true;
				}
			}
			if (valid) {
				return null;
			} else {
				throw new Contradiction(
					"The assertion that %s holds at least one of %s " +
					"contradicts previous assertions.",
						this,
						Arrays.toString(cards)
				);
			}
		}
		
		// Otherwise, the Player may be in one of three states:
		// - This Player does not hold any of the Cards.  This should return
		//   this PlayerFacts.
		// - This Player holds at least one of the Cards.  This should throw a
		//   Contradiction.
		// - At least one of the Cards is listed as a MAYBE.  This should return
		//   null.
		else {
			boolean alreadyAsserted = true;
			for (Card c : cards) {
				PlayerHasCard state = this.cards.get(c);
				if (state == PlayerHasCard.YES) {
					throw new Contradiction(
						"The assertion that %s does not hold any of %s " +
						"contradicts previous assertions.",
							this,
							Arrays.toString(cards)
					);
				} else if (state == PlayerHasCard.MAYBE) {
					alreadyAsserted = false;
				}
			}
			return (alreadyAsserted ? this : null);
		}
	}
	
	/**
	 * Returns the subset of Cards for which this PlayerFacts has the Card's
	 * state in the possible Hands flagged with the passed state.  In other
	 * words, passing YES to this function returns the Set of Cards that the
	 * represented Player must have due to the past assertions. 
	 * @param desiredState
	 * The possession state in question.
	 * @return
	 * A Set of Cards for which this Player's possession state matches the
	 * desired state.
	 */
	private Set<Card> getSubset(PlayerHasCard desiredState) {
		Set<Card> result = new HashSet<>();
		for (Entry<Card, PlayerHasCard> entry : cards.entrySet()) {
			if (entry.getValue() == desiredState) {
				result.add(entry.getKey());
			}
		}
		return result;
	}
	
	/**
	 * Returns an appropriate String for describing an assertion.
	 * @param has
	 * true if the assertion is that the represented Player has at least one of
	 * the Cards in the assertion, false if the assertion is that the
	 * represented Player has none of the Cards in the assertion.
	 * @return
	 * A descriptive String.
	 */
	private String getAssertionText(boolean has) {
		return (has ? "has" : "does not have"); 
	}
	
	/**
	 * Filters this PlayerFacts's possible Hands so that no Hands that
	 * contradict the passed assertion remain in the set of possible Hands.
	 * @param has
	 * true if the assertion is that the represented Player has at least one of
	 * the Cards in the assertion, false if the assertion is that the
	 * represented Player has none of the Cards in the assertion.
	 * @param cards
	 * The Cards in question.
	 * @throws Contradiction
	 * If the filtering leaves no possible Hands for the Player to have, a
	 * Contradiction is thrown.
	 */
	private void filterHands(boolean has, Card...cards) throws Contradiction {
		Iterator<Hand> ph = possibleHands.iterator();
		while (ph.hasNext()) {
			if (!handSatisfiesAssertion(ph.next(), has, cards)) {
				ph.remove();
			}
		}
		if (possibleHands.size() == 0) {
			throw new Contradiction(
				"Asserting %s %s %s eliminates all possible hands.",
					this,
					getAssertionText(has),
					Arrays.toString(cards)
			);
		}
	}
	
	/**
	 * Infers whether it is possible to determine whether the represented
	 * Player either has or does not have the currently unknown Cards and
	 * updates this PlayerFacts's internal state according to the findings. 
	 */
	private void learnFromPossibleHands() {
		Collection<Card> unknown = getUnknownCards();
		removeMaybeStates();
		
		for (Hand h : possibleHands) {
			Iterator<Card> u = unknown.iterator();
			while (u.hasNext()) {
				Card c = u.next();
				if (!cards.containsKey(c)) {
					cards.put(
						c,
						h.has(c) ?
							PlayerHasCard.YES :
							PlayerHasCard.NO
					);
				} else {
					if (cards.get(c) == PlayerHasCard.YES ^ h.has(c)) {
						cards.put(c, PlayerHasCard.MAYBE);
						u.remove();
					}
				}
			}
			
			if (unknown.size() == 0) {
				break;
			}
		}
	}
	
	/**
	 * Removes all entries from the possession state map for which it is
	 * currently unknown whether the represented Player has them or not.
	 */
	private void removeMaybeStates() {
		Set<Entry<Card, PlayerHasCard>> es = cards.entrySet();
		Iterator<Entry<Card, PlayerHasCard>> iterator = es.iterator();
		while (iterator.hasNext()) {
			if (iterator.next().getValue() == PlayerHasCard.MAYBE) {
				iterator.remove();
			}
		}
	}
	
	/**
	 * Overrides the internal Card possession states to reflect the asserted
	 * state.
	 * @param has
	 * true if the Player is asserted to have the passed Cards, false if the
	 * Player is asserted to not have the passed Cards. 
	 * @param cards
	 * The Cards in question.
	 */
	private void updateCardStates(boolean has, Card...cards) {
		for (Card c : cards) {
			this.cards.put(c, has ? PlayerHasCard.YES : PlayerHasCard.NO);
		}
	}
}
