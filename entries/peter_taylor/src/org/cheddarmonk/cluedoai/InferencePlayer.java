package org.cheddarmonk.cluedoai;

import java.io.*;
import java.net.UnknownHostException;
import java.util.*;

/**
 * A player which tries to make inferences from partial information.
 */
public class InferencePlayer extends AbstractCluedoPlayer {
	/* Set up a mapping between cards and bits which allows quick set operations. */
	private static final int allCardsMask;
	private static final int suspectsMask;
	private static final int weaponsMask;
	private static final int roomsMask;
	private static final Map<Integer, Card> cardsById;
	private static final Map<Card, Integer> idsByCard;
	static {
		cardsById = new HashMap<Integer, Card>();
		idsByCard = new HashMap<Card, Integer>();
		Card[] cards = Card.class.getEnumConstants();
		int smask = 0, wmask = 0, rmask = 0;
		for (int idx = 0; idx < cards.length; idx++) {
			Card card = cards[idx];
			int cardMask = 1 << idx;
			cardsById.put(cardMask, card);
			idsByCard.put(card, cardMask);

			if (card.type == CardType.SUSPECT) smask |= cardMask;
			else if (card.type == CardType.WEAPON) wmask |= cardMask;
			else rmask |= cardMask;
		}

		allCardsMask = (1 << cards.length) - 1;
		suspectsMask = smask;
		weaponsMask = wmask;
		roomsMask = rmask;
	}

	/** Stores the information about each player's hand and the solution. The symmetry simplifies things slightly. */
	private PlayerInformation[] knowledge;
	/** Shorthand for <code>knowledge[knowledge.length - 1]</code> */
	private PlayerInformation solution;
	/** Suggestions which we haven't yet made; to avoid accidentally repeating one and forfeiting the game. */
	private Set<Suggestion> unusedSuggestions;
	/** To which other players have we shown which cards from our hand? */
	private Map<Card, Integer> shownBitmask;

	public InferencePlayer(String identifier, int serverPort) throws UnknownHostException, IOException {
		super(identifier, serverPort);
	}

	@Override
	protected void handleReset() {
		int myMask = 1 << myIndex();
		int allMask = (1 << playerCount()) - 1;

		shownBitmask = new HashMap<Card, Integer>();
		Set<Card> myHand = myHand();
		for (Card myCard : myHand) {
			shownBitmask.put(myCard, 0);
		}

		int playerCount = playerCount();
		int cardsDealt = cardsById.size() - 3;
		int div = cardsDealt / playerCount;
		int surplus = cardsDealt % playerCount;
		knowledge = new PlayerInformation[playerCount + 1];
		for (int i = 0; i < playerCount; i++) {
			knowledge[i] = new PlayerInformation(knowledge, i, i == myIndex(), div + (i < surplus ? 1 : 0), myHand);
		}
		solution = new PlayerInformation(knowledge, -1, false, 3, myHand);
		knowledge[playerCount] = solution;

		unusedSuggestions = new HashSet<Suggestion>(Suggestion.allSuggestions());
	}

	@Override
	protected Suggestion makeSuggestion() {
		updateInferences(knowledge);

		Suggestion best = null;
		double bestScore = Double.NEGATIVE_INFINITY;

		// Iterate in random order as a simple tie-break.
		List<Suggestion> candidates = new ArrayList<Suggestion>(unusedSuggestions);
		Collections.shuffle(candidates);
		for (Suggestion candidate : candidates) {
			double score = evaluate(candidate);
			if (score >= bestScore) {
				bestScore = score;
				best = candidate;
			}
		}

		unusedSuggestions.remove(best);
		return best;
	}

	private double evaluate(Suggestion suggestion) {
		// The rough idea is to estimate how much information we will get from the response.
		// E.g. if we know that the next player has one of the cards, we can assume that we will gain
		// no information at all from the suggestion.
		// Evaluate the suggestion as the worst-case information gain from the response received.
		double value = Double.POSITIVE_INFINITY;

		int playerCount = playerCount();
		int myIndex = myIndex();
		for (int idx = (myIndex + 1) % playerCount; idx != myIndex; idx = (idx + 1) % playerCount) {
			// If that player has one of the cards we know about, assume they show it.
			Set<Card> intersection = new HashSet<Card>(knowledge[idx].knownHand);
			intersection.retainAll(suggestion.cards());
			if (!intersection.isEmpty()) {
				PlayerInformation[] clone = cloneKnowledge();
				recordInferenceData(clone, suggestion, myIndex, idx);
				return Math.min(value, evaluate(clone));
			}
			else {
				// We obviously don't want to set up contradictions, so ignore the possibility that we get shown
				// shown a card which we know the player doesn't have. Treat all non-excluded possibilities as
				// equally likely. (This is a possible area of improvement).
				for (Card maybeShown : suggestion.cards()) {
					if ((knowledge[idx].possibleCards & idsByCard.get(maybeShown)) != 0) {
						PlayerInformation[] clone = cloneKnowledge();
						recordInferenceData(clone, suggestion, myIndex, idx);
						clone[idx].hasCard(maybeShown);
						value = Math.min(value, evaluate(clone));
					}
				}
			}
		}

		// We didn't hit the case of a known shown card, so we consider also the possibility that the suggestion
		// isn't disproven.
		PlayerInformation[] clone = cloneKnowledge();
		recordInferenceData(clone, suggestion, myIndex, myIndex);
		for (Card card : suggestion.cards()) {
			if (!myHand().contains(card)) {
				clone[clone.length - 1].hasCard(card);
			}
		}

		return Math.min(value, evaluate(clone));
	}

	private static double evaluate(PlayerInformation[] knowledge) {
		// The tricky thing is working out how much information a knowledge state contains.
		// Firstly, because we don't care about narrowing down the location of all of the cards: just determining the solution.
		// Secondly, because there are potentially 1.4E11 states, so enumerating them isn't feasible.
		try {
			updateInferences(knowledge);
		} catch (IllegalStateException ise) {
			// Ok, this isn't actually possible, so rank it as extremely informative.
			return Double.POSITIVE_INFINITY;
		}

		// If we end up knowing the solution, our knowledge is effectively perfect.
		PlayerInformation soln = knowledge[knowledge.length - 1];
		if (soln.knownHand.size() == 3) return Double.POSITIVE_INFINITY;

		// Important factors to take into account:
		// 1. The number of possible solutions: the lower, the better.
		// 2. Knowledge of other players' hands. Each known card corresponds to a one-element clause,
		//    so we can base this solely on the clauses and the possibleCards masks.
		// Value a one-element clause as at least \binom{17 - |myHand|}{2} three-element clauses,
		// and a two-element clause at at least 16 - |myHand| three-element clauses.

		int possibleSolutions =
			Integer.bitCount(soln.possibleCards & suspectsMask) *
			Integer.bitCount(soln.possibleCards & weaponsMask) *
			Integer.bitCount(soln.possibleCards & roomsMask);

		int clauseCount = 0;
		int possibleBits = 0;
		int[] clauseWeights = new int[] { 0, 17 * 8, 16, 1 };
		for (PlayerInformation info : knowledge) {
			if (info == soln) continue;

			possibleBits += Integer.bitCount(info.possibleCards);
			for (Integer clause : info.clauses) {
				int clauseSize = Integer.bitCount(clause);
				clauseCount += clauseWeights[clauseSize];
			}
		}

		// TODO These weights could probably be tuned.
		return -1000 * possibleSolutions + 10 * clauseCount - possibleBits;
	}

	private PlayerInformation[] cloneKnowledge() {
		PlayerInformation[] clone = new PlayerInformation[knowledge.length];
		for (int i = 0; i < knowledge.length; i++) {
			clone[i] = new PlayerInformation(clone, knowledge[i]);
		}

		return clone;
	}

	@Override
	protected Card disproveSuggestion(int suggestingPlayerIndex, Suggestion suggestion) {
		// TODO Seek to minimise information leakage by taking into account what can be inferred
		// from my choice of reply.
		Card[] byNumShown = new Card[playerCount()];
		Set<Card> hand = myHand();
		int bit = 1 << suggestingPlayerIndex;
		for (Card candidate : suggestion.cards()) {
			if (!hand.contains(candidate)) continue;

			int bitmask = shownBitmask.get(candidate);
			if ((bitmask & bit) == bit) return candidate;
			byNumShown[Integer.bitCount(bitmask)] = candidate;
		}

		for (int i = byNumShown.length - 1; i >= 0; i--) {
			if (byNumShown[i] != null) return byNumShown[i];
		}

		throw new IllegalStateException("Unreachable");
	}

	@Override
	protected void handleSuggestionResponse(Suggestion suggestion, int disprovingPlayerIndex, Card shown) {
		if (shown != null) {
			recordInferenceData(knowledge, suggestion, myIndex(), disprovingPlayerIndex);
			knowledge[disprovingPlayerIndex].hasCard(shown);
		}
		else {
			// Either I made a suggestion which I can disprove, or after updating solution
			// the round of pre-accusation inferences will reduce it to one possibility.
			recordInferenceData(knowledge, suggestion, myIndex(), myIndex());

			// If I don't have the cards, no-one does.
			for (Card card : suggestion.cards()) {
				if (!myHand().contains(card)) {
					solution.hasCard(card);
				}
			}
		}
	}

	@Override
	protected void recordSuggestionResponse(int suggestingPlayerIndex, Suggestion suggestion, Card shown) {
		shownBitmask.put(shown, shownBitmask.get(shown) | (1 << suggestingPlayerIndex));
		recordInferenceData(knowledge, suggestion, suggestingPlayerIndex, myIndex());
	}

	@Override
	protected void recordSuggestionResponse(int suggestingPlayerIndex, Suggestion suggestion, int disprovingPlayerIndex) {
		if (disprovingPlayerIndex == -1) {
			// Two cases:
			// 1. The suggesting player doesn't have any disproof either. Assume they will make the optimal move of accusing,
			//    so we won't have any more decisions to make and it doesn't matter what we do.
			// 2. The suggesting player has the disproof, and we can exploit that fact.
			disprovingPlayerIndex = suggestingPlayerIndex;
		}
		recordInferenceData(knowledge, suggestion, suggestingPlayerIndex, disprovingPlayerIndex);
	}

	@Override
	protected Suggestion makeAccusation() {
		updateInferences(knowledge);

		// No game theory here: only accuse if we know we're right.
		if (solution.knownHand.size() == 3) {
			return new Suggestion(
				cardsById.get(solution.possibleCards & suspectsMask),
				cardsById.get(solution.possibleCards & weaponsMask),
				cardsById.get(solution.possibleCards & roomsMask));
		}

		return null;
	}

	@Override
	protected void recordAccusation(int accusingPlayer, Suggestion accusation, boolean correct) {
		// If the accusation was correct, there's nothing to do: game over.
		// If it was incorrect then in principle there's a small amount of information to be gained.
		// However, it's in a different form to the information which we currently use to make inferences.
		// I estimate that the odds of someone making a false accusation are low enough not to bother with handling it.
	}

	private static void recordInferenceData(PlayerInformation[] knowledge, Suggestion suggestion,
	                                        int suggestingPlayerIndex, int disprovingPlayerIndex) {
		int playerCount = knowledge.length - 1;
		for (int idx = (suggestingPlayerIndex + 1) % playerCount; idx != disprovingPlayerIndex; idx = (idx + 1) % playerCount) {
			knowledge[idx].passedSuggestion(suggestion);
		}
		knowledge[disprovingPlayerIndex].disprovedSuggestion(suggestion);
	}

	private static void updateInferences(PlayerInformation[] knowledge) {
		// While there are new inferences to be made, make them.
		boolean madeUpdates = true;
		while (madeUpdates) {
			madeUpdates = false;
			for (PlayerInformation information : knowledge) {
				madeUpdates |= information.update();
			}
		}
	}

	public static int mask(Iterable<Card> cards) {
		int mask = 0;
		for (Card card : cards) mask |= idsByCard.get(card);
		return mask;
	}

	private static class PlayerInformation {
		final PlayerInformation[] context;
		final int playerId;
		final int handSize;
		Set<Integer> clauses = new HashSet<Integer>();
		Set<Card> knownHand = new HashSet<Card>();
		int possibleCards;
		boolean needsUpdate = false;

		public PlayerInformation(PlayerInformation[] context, int playerId, boolean isMe, int handSize, Set<Card> myHand) {
			this.context = context;
			this.playerId = playerId;
			this.handSize = handSize;
			if (isMe) {
				knownHand.addAll(myHand);
				possibleCards = 0;
				for (Card card : knownHand) {
					int cardMask = idsByCard.get(card);
					clauses.add(cardMask);
					possibleCards |= cardMask;
				}
			}
			else {
				possibleCards = allCardsMask;
				for (Card card : myHand) {
					possibleCards &= ~idsByCard.get(card);
				}

				if (playerId == -1) {
					// Not really a player: this represents knowledge about the solution.
					// The solution contains one of each type of card.
					clauses.add(suspectsMask & possibleCards);
					clauses.add(weaponsMask & possibleCards);
					clauses.add(roomsMask & possibleCards);
				}
			}
		}

		/** Copy-constructor */
		public PlayerInformation(PlayerInformation[] context, PlayerInformation source) {
			this.context = context;
			playerId = source.playerId;
			handSize = source.handSize;
			clauses = new HashSet<Integer>(source.clauses);
			knownHand = new HashSet<Card>(source.knownHand);
			possibleCards = source.possibleCards;
		}

		public void hasCard(Card card) {
			if (knownHand.add(card)) {
				// This is new information.
				needsUpdate = true;
				clauses.add(idsByCard.get(card));

				// Inform the other PlayerInformation instances that their player doesn't have the card.
				int mask = idsByCard.get(card);
				for (PlayerInformation pi : context) {
					if (pi != this) pi.excludeMask(mask);
				}

				if (knownHand.size() == handSize) {
					possibleCards = mask(knownHand);
				}
			}
		}

		public void excludeMask(int mask) {
			if (knownHand.size() == handSize) return; // We can't benefit from any new information.

			if ((mask & possibleCards) != 0) {
				// The fact that we have none of the cards in the mask contains some new information.
				needsUpdate = true;
				possibleCards &= ~mask;
			}
		}

		public void disprovedSuggestion(Suggestion suggestion) {
			if (knownHand.size() == handSize) return; // We can't benefit from any new information.

			// Exclude cards which we know the player doesn't have.
			needsUpdate = clauses.add(mask(suggestion.cards()) & possibleCards);
		}

		public void passedSuggestion(Suggestion suggestion) {
			if (knownHand.size() == handSize) return; // We can't benefit from any new information.

			excludeMask(mask(suggestion.cards()));
		}

		public boolean update() {
			if (!needsUpdate) return false;

			needsUpdate = false;

			// Minimise the clauses, step 1: exclude cards which the player definitely doesn't have.
			Set<Integer> newClauses = new HashSet<Integer>();
			for (int clause : clauses) {
				newClauses.add(clause & possibleCards);
			}
			clauses = newClauses;

			if (clauses.contains(0)) throw new IllegalStateException();

			// Minimise the clauses, step 2: where one clause is a superset of another, discard the less specific one.
			Set<Integer> toEliminate = new HashSet<Integer>();
			for (int clause1 : clauses) {
				for (int clause2 : clauses) {
					if (clause1 != clause2 && (clause1 & clause2) == clause1) {
						toEliminate.add(clause2);
					}
				}
			}
			clauses.removeAll(toEliminate);

			// Every single-card clause is a known card: update knownHand if necessary.
			for (int clause : clauses) {
				if (((clause - 1) & clause) == 0) {
					Card singleCard = cardsById.get(clause);
					hasCard(cardsById.get(clause));
				}
			}

			// Every disjoint set of clauses of size equal to handSize excludes all cards not in the union of that set.
			Set<Integer> disjoint = new HashSet<Integer>(clauses);
			for (int n = 2; n <= handSize; n++) {
				Set<Integer> nextDisjoint = new HashSet<Integer>();
				for (int clause : clauses) {
					for (int set : disjoint) {
						if ((set & clause) == 0) nextDisjoint.add(set | clause);
					}
				}
				disjoint = nextDisjoint;
			}

			for (int set : disjoint) excludeMask(~set);

			return true;
		}
	}

	public static void main(String[] args) throws Exception {
		try {
			System.setOut(new PrintStream("/tmp/speed-cluedo-player" + args[0]+".log"));
			new InferencePlayer(args[0], Integer.parseInt(args[1])).run();
		} catch (Throwable th) {
			th.printStackTrace(System.out);
		}
	}
}
