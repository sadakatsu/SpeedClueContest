package com.sadakatsu.clue.ai;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.sadakatsu.clue.cards.*;
import com.sadakatsu.clue.exception.DuplicateSuggestion;
import com.sadakatsu.clue.exception.InvalidDisprove;
import com.sadakatsu.clue.exception.InvalidSuggestionString;
import com.sadakatsu.clue.exception.ProtocolViolation;

/**
 * SpeedClueAI is an abstract superclass for Java Speed Clue players.  It
 * encapsulates the behavior necessary for interfacing with the server and
 * provides abstract methods for making the actual decisions.
 * 
 * @author Joseph A. Craig
 */
public abstract class SpeedClueAI {
	//********************* Protected and Private Fields *********************//
	private boolean playing;
	private BufferedReader in;
	private char[] buffer;
	private int index;
	private int playerCount;
	private int playersInGame;
	private List<Card> hand;
	private List<Suggestion> pastSuggestions;
	private PrintWriter out;
	private Socket socket;
	private String identifier;
	
	protected final boolean logMessages;
	
	//*************************** Public Interface ***************************//
	/**
	 * Instantiates a new SpeedClueAI instance with a connection to the server.
	 * @param identifier
	 * The name given to this player by the server.
	 * @param serverPort
	 * The localhost port number for the server.
	 * @param logMessages
	 * Whether to write messages received by and sent from this player to
	 * stdout.
	 * @throws IOException
	 */
	public SpeedClueAI(
		String identifier,
		int serverPort,
		boolean logMessages
	) throws IOException {
		try {
			socket = new Socket("localhost", serverPort);
			in = new BufferedReader(
				new InputStreamReader(socket.getInputStream())
			);
			out = new PrintWriter(socket.getOutputStream(), true);
		} catch (IOException e) {
			closeConnection();
			throw e;
		}
		
		buffer = new char[512];
		hand = new ArrayList<>();
		index = -1;
		this.identifier = identifier;
		this.logMessages = logMessages;
		pastSuggestions = new ArrayList<>();
		playing = false;
		playerCount = -1;
		playersInGame = -1;
		
		sendMessage(String.format("%s alive", identifier));
	}
	
	/**
	 * Whether this player is currently in a game.  This does not represent
	 * whether the player has not yet already lost.
	 * @return
	 * true if the player is playing a game, false otherwise.
	 */
	public boolean inGame() {
		return playing;
	}
	
	/**
	 * This player's position in the current game's player order.
	 * @return
	 * A number in the range [0..playerCount) if the player is currently in a
	 * game, or -1 if it is not.
	 */
	public int getIndex() {
		return index;
	}
	
	/**
	 * The number of players in the current game.
	 * @return
	 * A number in the range [3..6] if the player is currently in a game, or -1
	 * otherwise.
	 */
	public int getPlayerCount() {
		return playerCount;
	}
	
	/**
	 * The number of players that can still play.  Players that have made
	 * incorrect accusations have lost and will not be counted.
	 * @return
	 * The number of players still playing, or -1 if the player is not in a
	 * game.
	 */
	public int getPlayersInGame() {
		return playersInGame;
	}
	
	/**
	 * This player's current hand for the current game.
	 * @return
	 * A List of the Cards dealt to this player if the player is in a game.  If
	 * the List is empty, then the player is currently not in a game.
	 */
	public List<Card> getHand() {
		return new ArrayList<>(hand);
	}
	
	/**
	 * All the Suggestions the player has made this game.
	 * @return
	 * A List of all the Suggestions the player has made this game.  The List
	 * can be empty if the player is not in a game or has yet to make any
	 * Suggestions.
	 */
	public List<Suggestion> getPastSuggestions() {
		return new ArrayList<>(pastSuggestions);
	}
	
	/**
	 * The player's name.
	 * @return
	 * The String passed to the constructor by the server.
	 */
	public String getIdentifier() {
		return identifier;
	}
	
	/**
	 * Returns a String to identify this Player in output.
	 */
	public String toString() {
		return String.format(
			"Player \"%s\"%s",
			identifier,
			(index > -1 ? String.format(" [%d/%d]", index, playerCount) : "")
		);
	}
	
	/**
	 * Makes the player communicate with the server until the server tells it
	 * to stop.  Regardless of how this method exits, the instance's connection
	 * to the server is closed.
	 * @throws IOException
	 * @throws InvalidDisprove 
	 * @throws DuplicateSuggestion 
	 */
	public void run() throws IOException, InvalidDisprove, DuplicateSuggestion {
		try {
			boolean running = true;
			while (running) {
				running = handleMessage(getMessage());
			}
		} finally {
			closeConnection();
		}
	}
	
	//******************* Protected and Private Interface ********************//
	/**
	 * Handles a message received from the server by performing all necessary
	 * internal processing and responding to the message.
	 * @param message
	 * The message received from the server.
	 * @return
	 * Whether the message allows the player to keep running (true) or not
	 * (false).
	 * @throws InvalidDisprove
	 * @throws DuplicateSuggestion
	 */
	private boolean handleMessage(
		String message
	) throws InvalidDisprove, DuplicateSuggestion {
		boolean keepGoing = true;
		
		String type = getMessageType(message);
		
		try {
			String response;
			
			if (type.equals("accusation")) {
				response = handleAccusationMessage(message);
			} else if (type.equals("accuse")) {
				response = handleAccuseMessage();
			} else if (type.equals("disprove")) {
				response = handleDisproveMessage(message);
			} else if (type.equals("done")) {
				response = handleDoneMessage();
				keepGoing = false;
			} else if (type.equals("reset")) {
				response = handleResetMessage(message);
			} else if (type.equals("suggest")) {
				response = handleSuggestMessage();
			} else if (type.equals("suggestion")) {
				response = handleSuggestionMessage(message);
			} else {
				throw new ProtocolViolation(message);
			}
			
			sendMessage(response);
		} catch (ProtocolViolation pv) {
			if (logMessages) {
				System.err.format("???? %s\n", pv.getMessage());
			}
		}
		
		return keepGoing;
	}
	
	/**
	 * Returns the message passed by the server. 
	 * @return
	 * The message the server passed.
	 * @throws IOException
	 */
	private String getMessage() throws IOException {
		int read = in.read(buffer);
		while (read > 0 && buffer[read - 1] == '\0') {
			--read;
		}
		
		String response = String.copyValueOf(buffer, 0, read).toLowerCase();
		
		if (logMessages) {
			System.out.format("    %s <<: \"%s\"\n", this, response);
		}
		
		return response;
	}
	
	/**
	 * Returns the type indicator for a message received from the server.
	 * @param message
	 * @return
	 * A String that can be used to determine how to process the message.
	 */
	private String getMessageType(String message) {
		return message.split(" ", 2)[0].toLowerCase();
	}
	
	/**
	 * Processes an accusation message from the server, calling
	 * processAccusation() to allow subclasses to perform any desired
	 * processing.  If the message means that the game is over, this method also
	 * resets the player to no longer be in a match.
	 * @param message
	 * The accusation message passed by the server.
	 * @return
	 * The response to forward to the server.
	 * @throws ProtocolViolation
	 */
	private String handleAccusationMessage(
		String message
	) throws ProtocolViolation {
		boolean correct;
		int accuser;
		Suggestion accusation;
		
		// Extract the information from the message.
		try {
			Matcher m = accusationMessagePattern.matcher(message);
			m.matches();
			accuser = Integer.parseInt(m.group("accuser"));
			accusation = new Suggestion(m.group("accusation"));
			correct = m.group("correct") == "+";
		} catch (
			InvalidSuggestionString |
			IllegalStateException |
			NumberFormatException e
		) {
			throw new ProtocolViolation("accusation", message);
		}
		
		// If the accusation is incorrect, decrement the count of players still
		// in the game.
		if (!correct) {
			--playersInGame;
		}
		
		// Determine whether the game has ended.  The game ends when either a
		// player's accusation matches the solution or when only one player is
		// still in the game.
		playing = !(correct || playersInGame == 1); 
		
		// Process the accusation.
		processAccusation(accuser, accusation, correct);
		
		// If the game is over, make all necessary state changes for the player
		// to no longer be in a game.
		if (!playing) {
			hand.clear();
			index = -1;
			pastSuggestions.clear();
			playerCount = -1;
			playersInGame = -1;
			
		}
		
		// Return the message the server expects for an "accusation" message.
		return "ok";
	}
	
	/**
	 * Processes an accuse message from the server, deciding whether to make an
	 * accusation as requested and returning the appropriate message to forward
	 * to the server.
	 * @return
	 * The response to forward to the server.
	 */
	private String handleAccuseMessage() {
		Suggestion accusation = accuse();
		return (
			accusation == null ?
			"-" :
			String.format("accuse %s", accusation.getAbbreviation())
		);
	}
	
	/**
	 * Processes a disprove message from the server, selecting the Card to show
	 * to the suggesting player and returning the appropriate message to forward
	 * to the server. 
	 * @param message
	 * The disprove message passed by the server.
	 * @return
	 * The response to forward to the server.
	 * @throws ProtocolViolation
	 * @throws InvalidDisprove
	 */
	private String handleDisproveMessage(
		String message
	) throws ProtocolViolation, InvalidDisprove {
		int suggester;
		Suggestion suggestion;
		
		// Extract the information from the message.
		try {
			Matcher m = disproveMessagePattern.matcher(message);
			m.matches();
			suggester = Integer.parseInt(m.group("suggester"));
			suggestion = new Suggestion(m.group("suggestion"));
		} catch (
			InvalidSuggestionString |
			IllegalStateException |
			NumberFormatException e
		) {
			throw new ProtocolViolation("suggestion", message);
		}
		
		// Select a Card to disprove the suggestion.
		Card card = disprove(suggester, suggestion);
		
		// If the passed Card is not in both the player's hand and the
		// Suggestion, the player is trying to cheat.  Throw an exception.
		if (!hand.contains(card) || !suggestion.has(card)) {
			throw new InvalidDisprove(card, suggestion);
		}
		
		// Return a message to tell the server to show the selected Card.
		return String.format("show %s", card.getAbbreviation());
	}
	
	/**
	 * Processes a done message from the server, performing any necessary work
	 * to stop playing.
	 * @return
	 * The response to forward to the server.
	 */
	private String handleDoneMessage() {
		stopPlaying();
		return "dead";
	}
	
	private String handleResetMessage(String message) throws ProtocolViolation {
		int pc;
		int i;
		List<Card> h;
		
		// Extract the settings from the message.  If there are any flaws with
		// the message, throw a ProtocolViolation.
		try {
			// Fit the message to the reset message's syntax.  Extract the
			// number of players and the player's index.
			Matcher m = resetMessagePattern.matcher(message);
			m.matches();
			pc = Integer.parseInt(m.group("playerCount"));
			i = Integer.parseInt(m.group("playerIndex"));
			
			// Carefully examine the Cards passed in the message.  If the wrong
			// number of Cards was passed, or any of the abbreviations does not
			// refer to a valid Card, throw an IllegalStateException (since that
			// type is already watched because of the calls to Matcher.group()).
			// If there are no problems with this part, we should get a List of
			// Cards to use for a hand.
			String[] abbr = m.group("cards").split(" ");
			if (abbr.length != getHandSize(pc, i)) {
				throw new IllegalStateException();
			}
			h = new ArrayList<>();
			for (String a : abbr) {
				Card c = Card.from(a);
				if (c == null) {
					throw new IllegalStateException();
				}
				h.add(c);
			}
		} catch (IllegalStateException | NumberFormatException e) {
			System.err.println(e.getMessage());
			for (StackTraceElement s : e.getStackTrace()) {
				System.err.println(s);
			}
			throw new ProtocolViolation("reset", message);
		}
		
		// Overwrite the current state of this player with the passed
		// information.  Ensure that any related state information is properly
		// reset (just in case the server somehow passes this instance a reset
		// message without the game having been ended by an accusation message).
		hand = h;
		index = i;
		pastSuggestions.clear();
		playerCount = pc;
		playersInGame = pc;
		playing = true;
		
		// Perform any subclass match preparations.
		prepareForGame();
		
		// Return the response to a reset message.
		return "ok";
	}
	
	/**
	 * Processes a suggest message from the server, choosing a new Suggestion to
	 * make.  This Suggestion is added to the List of past Suggestions to ensure
	 * that the player does not make the same Suggestion twice.
	 * @return
	 * The message to forward to the server.
	 * @throws DuplicateSuggestion
	 */
	private String handleSuggestMessage() throws DuplicateSuggestion {
		Suggestion suggestion = suggest();
		
		if (pastSuggestions.contains(suggestion)) {
			throw new DuplicateSuggestion(suggestion);
		}
		
		pastSuggestions.add(suggestion);
		
		return String.format("suggest %s", suggestion.getAbbreviation());
	}
	
	/**
	 * Processes a suggestion message from the server.
	 * @param message
	 * The suggestion message received from the server.
	 * @return
	 * The message to forward to the server.
	 * @throws ProtocolViolation
	 */
	private String handleSuggestionMessage(
		String message
	) throws ProtocolViolation {
		Card shown;
		Integer disprover;
		int suggester;
		Suggestion suggestion;

		try {
			Matcher m = suggestionMessagePattern.matcher(message);
			m.matches();
			suggester = Integer.parseInt(m.group("suggester"));
			suggestion = new Suggestion(m.group("suggestion"));
			
			String d = m.group("disprover");
			disprover = (d.equals("-") ? null : new Integer(d));
			
			String c = m.group("shown");
			shown = (c == null ? null : Card.from(c));
		} catch (
			InvalidSuggestionString |
			IllegalStateException |
			NumberFormatException e
		) {
			throw new ProtocolViolation("accusation", message);
		}
		
		processSuggestion(suggester, suggestion, disprover, shown);
		
		return "ok";
	}
	
	/**
	 * Closes the Socket and its associated streams.
	 */
	private void closeConnection() {
		try {
			if (out != null) {
				out.close();
			}
			
			if (in != null) {
				in.close();
			}
			
			if (socket != null) {
				socket.close();
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	/**
	 * Sends a message to the server.
	 * @param message
	 * The message to send to the server.
	 */
	private void sendMessage(String message) {
		if (logMessages) {
			System.out.format("    %s :>> \"%s\"\n", this, message);
		}
		out.format(message);
	}
	
	/**
	 * Returns the Cards in the passed Hand that can be used to disprove the
	 * passed Suggestion.
	 * @param hand
	 * The hand that must be used to disprove the Suggestion.
	 * @param suggestion
	 * The Suggestion to disprove.
	 * @return
	 * A list of Cards that can be used to Disprove the passed Suggestion.
	 */
	protected List<Card> getDisproveCards(
		List<Card> hand,
		Suggestion suggestion
	) {
		List<Card> intersection = suggestion.getCards();
		intersection.retainAll(hand);
		return intersection;
	}
	
	/**
	 * Returns the Cards in this player's hand that can be used to disprove the
	 * passed Suggestion.
	 * @param suggestion
	 * The Suggestion to disprove.
	 * @return
	 * A list of Cards that can be used to Disprove the passed Suggestion.
	 */
	protected List<Card> getMyDisproveCards(Suggestion suggestion) {
		return getDisproveCards(hand, suggestion);
	}
	
	/**
	 * Returns the number of Cards a given player should have.
	 * @param numberOfPlayers
	 * The number of players playing a game.
	 * @param playerIndex
	 * The index of the given player in the play order.
	 * @return
	 * The number of Cards the given player should have in his hand.
	 */
	protected int getHandSize(int numberOfPlayers, int playerIndex) {
		int size = 18 / numberOfPlayers;
		if (18 % numberOfPlayers > playerIndex) {
			++size;
		}
		return size;
	}
	
	//********************* Required Subclass Interface **********************//
	/**
	 * Chooses either an accusation to make or not to make an accusation. 
	 * @return
	 * A Suggestion object that represents the accusation if the player chose to
	 * make an accusation or null if the player chose not to make an accusation.
	 */
	protected abstract Suggestion accuse();
	
	/**
	 * Chooses a Card in the intersection between this player's hand and the
	 * Suggestion's Cards to return to disprove the Suggestion to the suggester.
	 * @param suggester
	 * The index of the player who made the Suggestion.
	 * @param suggestion
	 * The Suggestion that this player can and must disprove.
	 * @return
	 * The Card that the player chose to disprove the Suggestion.
	 */
	protected abstract Card disprove(int suggester, Suggestion suggestion);
	
	/**
	 * Chooses a Suggestion to make that this player has not yet made.
	 * @return
	 * The Suggestion that this player chose.
	 */
	protected abstract Suggestion suggest();
	
	/**
	 * Performs any work that is necessary for the player to be ready to play a
	 * new game.
	 */
	protected abstract void prepareForGame();
	
	/**
	 * Performs any work that is necessary for the player to stop playing before
	 * informing the server that it has indeed stopped playing.
	 */
	protected abstract void stopPlaying();
	
	/**
	 * Performs any player-specific processing related to the passed accusation.
	 * If the accusation ends the game (inGame() == false), this method also
	 * cleans up any subclass game-specific instance data. 
	 * @param accuser
	 * The index of the player who made the accusation.
	 * @param accusation
	 * The accusation the player made.
	 * @param correct
	 * Whether the accusation matches the solution (true) or not (false).
	 */
	protected abstract void processAccusation(
		int accuser,
		Suggestion accusation,
		boolean correct
	);
	
	/**
	 * Performs any player-specific processing related to the passed Suggestion.
	 * @param suggester
	 * The index of the player who made the Suggestion.
	 * @param suggestion
	 * The Suggestion that was made.
	 * @param disprover
	 * The index of the player that disproved the Suggestion, or null if the
	 * Suggestion was not disproved.
	 * @param shown
	 * If either this player is neither the suggester nor the disprover, or the
	 * Suggestion was not disproved, this argument is null.  Otherwise, this
	 * argument holds the Card that was shown. 
	 */
	protected abstract void processSuggestion(
		int suggester,
		Suggestion suggestion,
		Integer disprover,
		Card shown
	);
	//***************** Protected and Private Static Fields ******************//
	private static final String cardPattern = "\\p{Alpha}{2}";
	private static final String playerIndexPattern = "[0-5]";
	private static final String suggestionPattern =
		cardPattern + " " + cardPattern + " " + cardPattern; 
	
	private static final Pattern accusationMessagePattern = Pattern.compile(
		String.format(
			"accusation (?<accuser>%s) (?<accusation>%s) (?<correct>\\+|-)",
			playerIndexPattern,
			suggestionPattern
		),
		Pattern.CASE_INSENSITIVE
	);
	
	private static final Pattern disproveMessagePattern = Pattern.compile(
		String.format(
			"disprove (?<suggester>%s) (?<suggestion>%s)",
			playerIndexPattern,
			suggestionPattern
		),
		Pattern.CASE_INSENSITIVE
	);
	
	private static final Pattern resetMessagePattern = Pattern.compile(
		String.format(
			"reset (?<playerCount>[3-6]) (?<playerIndex>%s) " +
				"(?<cards>(?:%s ?){3,6})",
			playerIndexPattern,
			cardPattern
		),
		Pattern.CASE_INSENSITIVE
	);
	
	private static final Pattern suggestionMessagePattern = Pattern.compile(
		String.format(
			"suggestion (?<suggester>%s) (?<suggestion>%s) (?<disprover>-|%s)" +
				"(?: (?<shown>%s))?",
			playerIndexPattern,
			suggestionPattern,
			playerIndexPattern,
			cardPattern
		)
	);
}
