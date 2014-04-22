package org.cheddarmonk.cluedoai;

import java.io.*;
import java.net.UnknownHostException;
import java.util.*;

/**
 * A simple player which doesn't try to make inferences from partial information.
 * It merely tries to maximise the information gain by always making suggestions involving cards which
 * it does not know to be possessed by a player, and to minimise information leakage by recording who
 * has seen which of its own cards.
 */
public class SimpleCluedoPlayer extends AbstractCluedoPlayer {
	private Map<CardType, Set<Card>> unseenCards;
	private Map<Card, Integer> shownBitmask;
	private Random rnd = new Random();

	public SimpleCluedoPlayer(String identifier, int serverPort) throws UnknownHostException, IOException {
		super(identifier, serverPort);
	}

	@Override
	protected void handleReset() {
		unseenCards = new HashMap<CardType, Set<Card>>();
		for (Map.Entry<CardType, Set<Card>> e : Card.byType.entrySet()) {
			unseenCards.put(e.getKey(), new HashSet<Card>(e.getValue()));
		}

		shownBitmask = new HashMap<Card, Integer>();
		for (Card myCard : myHand()) {
			shownBitmask.put(myCard, 0);
			unseenCards.get(myCard.type).remove(myCard);
		}
	}

	@Override
	protected Suggestion makeSuggestion() {
		return new Suggestion(
			selectRandomUnseen(CardType.SUSPECT),
			selectRandomUnseen(CardType.WEAPON),
			selectRandomUnseen(CardType.ROOM));
	}

	private Card selectRandomUnseen(CardType type) {
		Set<Card> candidates = unseenCards.get(type);
		Iterator<Card> it = candidates.iterator();
		for (int idx = rnd.nextInt(candidates.size()); idx > 0; idx--) {
			it.next();
		}
		return it.next();
	}

	@Override
	protected Card disproveSuggestion(int suggestingPlayerIndex, Suggestion suggestion) {
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
		if (shown != null) unseenCards.get(shown.type).remove(shown);
		else {
			// This player never makes a suggestion with cards from its own hand, so we're ready to accuse.
			unseenCards.put(CardType.SUSPECT, Collections.singleton(suggestion.suspect));
			unseenCards.put(CardType.WEAPON, Collections.singleton(suggestion.weapon));
			unseenCards.put(CardType.ROOM, Collections.singleton(suggestion.room));
		}
	}

	@Override
	protected void recordSuggestionResponse(int suggestingPlayerIndex, Suggestion suggestion, Card shown) {
		shownBitmask.put(shown, shownBitmask.get(shown) | (1 << suggestingPlayerIndex));
	}

	@Override
	protected void recordSuggestionResponse(int suggestingPlayerIndex, Suggestion suggestion, int disprovingPlayerIndex) {
		// Do nothing.
	}

	@Override
	protected Suggestion makeAccusation() {
		Set<Card> suspects = unseenCards.get(CardType.SUSPECT);
		Set<Card> weapons = unseenCards.get(CardType.WEAPON);
		Set<Card> rooms = unseenCards.get(CardType.ROOM);
		if (suspects.size() * weapons.size() * rooms.size()  == 1) {
			return new Suggestion(suspects.iterator().next(), weapons.iterator().next(), rooms.iterator().next());
		}

		return null;
	}

	@Override
	protected void recordAccusation(int accusingPlayer, Suggestion accusation, boolean correct) {
		// Do nothing.
	}

	//*********************** Public Static Interface ************************//
	public static void main(String[] args) throws Exception {
		try {
			File logFile = new File(System.getProperty("java.io.tmpdir"), "speed-cluedo-player" + args[0]+".log");
			System.setOut(new PrintStream(logFile));
			new SimpleCluedoPlayer(args[0], Integer.parseInt(args[1])).run();
		} catch (Throwable th) {
			th.printStackTrace(System.out);
		}
	}
}
