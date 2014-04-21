package org.cheddarmonk.cluedoai;

import java.io.*;
import java.net.*;
import java.util.*;

public abstract class AbstractCluedoPlayer {
	private final Socket socket;
	private final BufferedReader in;
	private final PrintWriter out;
	private final char[] buffer = new char[512];

	private int playerCount;
	private int myIndex = -1;
	private int roundNumber;
	private int currentPlayerIndex;
	private Set<Card> hand = Collections.emptySet();

	// Core I/O handling

	public AbstractCluedoPlayer(String identifier, int serverPort) throws UnknownHostException, IOException {
		socket = new Socket("localhost", serverPort);
		in = new BufferedReader(new InputStreamReader(socket.getInputStream(), "ISO-8859-1"));
		out = new PrintWriter(new OutputStreamWriter(socket.getOutputStream(), "ISO-8859-1"), true);

		formatResponse("%s alive", identifier);
	}

	protected void run() throws IOException {
		while (true) {
			String message = getMessage();
			String[] tokens = message.split("\\s");
			String type = tokens[0];

			if (type.equals("reset")) reset(tokens);
			else if (type.equals("suggest")) suggest();
			else if (type.equals("disprove")) disprove(tokens);
			else if (type.equals("suggestion")) suggestion(tokens);
			else if (type.equals("accuse")) accuse();
			else if (type.equals("accusation")) accusation(tokens);
			else if (type.equals("done")) { done(); return; }
			else System.out.println("Ignoring unrecognised message type " + type);
		}
	}

	private String getMessage() throws IOException {
		int read = in.read(buffer);
		String response = String.copyValueOf(buffer, 0, read);
		System.out.format("<< %s\n", response);
		return response;
	}

	private void formatResponse(String format, Object... args) {
		System.out.print(">> ");
		System.out.format(format, args);
		System.out.println();
		out.format(format, args);
	}

	// Interface for subclasses to use

	protected int playerCount() {
		return playerCount;
	}

	protected int myIndex() {
		return myIndex;
	}

	protected Set<Card> myHand() {
		return hand;
	}

	/**
	 * Called at the start of a new game, after the player count, my index, and my hand have been set up.
	 */
	protected abstract void handleReset();

	/**
	 * Called when it's my turn to make a suggestion.
	 */
	protected abstract Suggestion makeSuggestion();

	/**
	 * Called when I have to make a decision as to how to disprove a suggestion.
	 * @param suggestingPlayerIndex The player who made the suggestion.
	 * @param suggestion The suggestion they made.
	 * @return One of the cards from the suggestion which I possess. May not be null.
	 */
	protected abstract Card disproveSuggestion(int suggestingPlayerIndex, Suggestion suggestion);

	/**
	 * Called when my suggestion has been offered for disproof.
	 * @param suggestion My most recent suggestion.
	 * @param disprovingPlayerIndex The index of the player who disproved my suggestion, or -1 if none of them did.
	 *            Note that this could still be -1 if I could have disproved the suggestion.
	 * @param shown The card which I was shown to disprove the suggestion, if disprovingPlayerIndex is not -1.
	 */
	protected abstract void handleSuggestionResponse(Suggestion suggestion, int disprovingPlayerIndex, Card shown);

	/**
	 * Called when I have disproved someone else's suggestion.
	 * @param suggestingPlayerIndex The player who made the suggestion.
	 * @param suggestion The suggestion which they made.
	 * @param shown The card which I showed them. Note that if I only have one card from the suggestion, disproveSuggestion
	 *            will not have been called, so this is my only opportunity to record the fact that I've shown the card.
	 */
	protected abstract void recordSuggestionResponse(int suggestingPlayerIndex, Suggestion suggestion, Card shown);

	/**
	 * Called when a suggestion is made by someone else and I did not disprove it.
	 * @param suggestingPlayerIndex The player who made the suggestion.
	 * @param suggestion The suggestion which they made.
	 * @param disprovingPlayerIndex The index of the player who disproved the suggestion, or -1 if none of them did.
	 *            (This does not rule out the suggesting player having a card from the suggestion).
	 */
	protected abstract void recordSuggestionResponse(int suggestingPlayerIndex, Suggestion suggestion, int disprovingPlayerIndex);

	/**
	 * Called when it's my turn to make an accusation.
	 * @return The accusation I wish to make, or null if I don't wish to make one.
	 */
	protected abstract Suggestion makeAccusation();

	/**
	 * Called when any player makes an accusation.
	 * @param accusingPlayer The player making the accusation.
	 * @param accusation The accusation made.
	 * @param correct Whether or not the accusation was correct.
	 */
	protected abstract void recordAccusation(int accusingPlayer, Suggestion accusation, boolean correct);

	// Message parsing and construction.

	private void reset(String[] tokens) {
		playerCount = Integer.parseInt(tokens[1]);
		myIndex = Integer.parseInt(tokens[2]);
		currentPlayerIndex = 0;
		roundNumber = 0;

		Set<Card> newHand = new HashSet<Card>();
		for (int i = 3; i < tokens.length; ++i) {
			newHand.add(Card.parse(tokens[i]));
		}
		this.hand = Collections.unmodifiableSet(newHand);

		handleReset();

		formatResponse("ok");
	}

	private void suggest() {
		Suggestion suggestion = makeSuggestion();
		formatResponse("suggest %s", suggestion.abbreviation());
	}

	private void disprove(String[] tokens) {
		int suggestingPlayerIndex, cardOff;
		if (tokens.length == 4) {
			suggestingPlayerIndex = currentPlayerIndex;
			cardOff = 1;
		}
		else if (tokens.length == 5) {
			suggestingPlayerIndex = Integer.parseInt(tokens[1]);
			cardOff = 2;
		}
		else throw new IllegalArgumentException("Wrong number of tokens in 'disprove' message");

		Card card = disproveSuggestion(suggestingPlayerIndex, Suggestion.parse(tokens, cardOff));
		formatResponse("show %s", card.abbreviation());
	}

	private void suggestion(String[] tokens) {
		if (currentPlayerIndex == 0) roundNumber++;

		int suggestingPlayerIndex = Integer.parseInt(tokens[1]);
		Suggestion suggestion = Suggestion.parse(tokens, 2);
		int disprovingPlayerIndex = tokens[5].equals("-") ? -1 : Integer.parseInt(tokens[5]);
		Card shown = tokens.length == 7 ? Card.parse(tokens[6]) : null;
		if (myIndex == suggestingPlayerIndex) {
			handleSuggestionResponse(suggestion, disprovingPlayerIndex, shown);
		}
		else if (myIndex == disprovingPlayerIndex) {
			recordSuggestionResponse(suggestingPlayerIndex, suggestion, shown);
		}
		else {
			recordSuggestionResponse(suggestingPlayerIndex, suggestion, disprovingPlayerIndex);
		}

		currentPlayerIndex++;
		if (currentPlayerIndex == playerCount) currentPlayerIndex = 0;

 		formatResponse("ok");
	}

	private void accuse() {
		Suggestion accusation = makeAccusation();
		if (accusation == null) formatResponse("-");
		else formatResponse("accuse %s", accusation.abbreviation());
	}

	private void accusation(String[] tokens) {
		int accusingPlayer = Integer.parseInt(tokens[1]);
		Suggestion accusation = Suggestion.parse(tokens, 2);
		boolean correct = tokens[5].equals("+");
		recordAccusation(accusingPlayer, accusation, correct);

		if (correct) System.out.format("Player %d won on round %d\n", accusingPlayer, roundNumber);

		formatResponse("ok");
	}

	private void done() throws IOException {
		formatResponse("dead");
		in.close();
		out.close();
		socket.close();
	}

	// Support types

	public static enum CardType {
		SUSPECT,
		WEAPON,
		ROOM;
	}

	public static enum Card {
		Green(CardType.SUSPECT),
		Mustard(CardType.SUSPECT),
		Peacock(CardType.SUSPECT),
		Plum(CardType.SUSPECT),
		Scarlet(CardType.SUSPECT),
		White(CardType.SUSPECT),
		Candlestick(CardType.WEAPON),
		Knife(CardType.WEAPON),
		Pipe(CardType.WEAPON),
		Revolver(CardType.WEAPON),
		Rope(CardType.WEAPON),
		Wrench(CardType.WEAPON),
		Ballroom(CardType.ROOM),
		BilliardsRoom(CardType.ROOM),
		Conservatory(CardType.ROOM),
		DiningRoom(CardType.ROOM),
		Hall(CardType.ROOM),
		Kitchen(CardType.ROOM),
		Library(CardType.ROOM),
		Lounge(CardType.ROOM),
		Study(CardType.ROOM);

		public static final Map<String, Card> byAbbreviation;
		public static final Map<CardType, Set<Card>> byType;

		static {
			Map<String, Card> _byAbbrev = new HashMap<String, Card>();
			Map<CardType, Set<Card>> _byType = new HashMap<CardType, Set<Card>>();

			for (CardType type : CardType.class.getEnumConstants()) {
				_byType.put(type, new HashSet<Card>());
			}

			for (Card card : Card.class.getEnumConstants()) {
				if (_byAbbrev.put(card.abbreviation(), card) != null) {
					throw new IllegalStateException("Collision for abbreviation " + card.abbreviation());
				}
				_byType.get(card.type).add(card);
			}

			for (CardType type : CardType.class.getEnumConstants()) {
				_byType.put(type, Collections.unmodifiableSet(_byType.get(type)));
			}

			byAbbreviation = Collections.unmodifiableMap(_byAbbrev);
			byType = Collections.unmodifiableMap(_byType);
		}

		public final CardType type;

		Card(CardType type) {
			this.type = type;
		}

		public String abbreviation() {
			return name().substring(0, 2);
		}

		public static Card parse(String str) {
			Card card = byAbbreviation.get(str);
			if (card == null) throw new IllegalArgumentException("str");
			return card;
		}
	}

	public static class Suggestion {
		public final Card suspect;
		public final Card weapon;
		public final Card room;

		private final Set<Card> cards;

		public Suggestion(Card suspect, Card weapon, Card room) {
			if (suspect.type != CardType.SUSPECT) throw new IllegalArgumentException("suspect");
			if (weapon.type != CardType.WEAPON) throw new IllegalArgumentException("weapon");
			if (room.type != CardType.ROOM) throw new IllegalArgumentException("room");

			this.suspect = suspect;
			this.weapon = weapon;
			this.room = room;

			Set<Card> cards = new HashSet<Card>(3);
			cards.add(suspect);
			cards.add(weapon);
			cards.add(room);
			this.cards = Collections.unmodifiableSet(cards);
		}

		public Set<Card> cards() {
			return cards;
		}

		public String abbreviation() {
			return String.format("%s %s %s", suspect.abbreviation(), weapon.abbreviation(), room.abbreviation());
		}

		public static Suggestion parse(String str) {
			String[] tokens = str.split("\\s");
			if (tokens.length != 3) throw new IllegalArgumentException("str");
			return parse(tokens, 0);
		}

		public static Suggestion parse(String[] tokens, int off) {
			return new Suggestion(Card.parse(tokens[off]), Card.parse(tokens[off + 1]), Card.parse(tokens[off + 2]));
		}

		public static Set<Suggestion> allSuggestions() {
			Set<Suggestion> all = new HashSet<Suggestion>(9 * 6 * 6);
			for (Card suspect : Card.byType.get(CardType.SUSPECT)) {
				for (Card weapon : Card.byType.get(CardType.WEAPON)) {
					for (Card room : Card.byType.get(CardType.ROOM)) {
						all.add(new Suggestion(suspect, weapon, room));
					}
				}
			}
			return all;
		}

		@Override
		public boolean equals(Object obj) {
			if (!(obj instanceof Suggestion)) return false;
			Suggestion other = (Suggestion)obj;
			return this.suspect == other.suspect && this.weapon == other.weapon && this.room == other.room;
		}

		@Override
		public int hashCode() {
			return (suspect.hashCode() * 37 + weapon.hashCode()) * 37 + room.hashCode();
		}
	}
}
