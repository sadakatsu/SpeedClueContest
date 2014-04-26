package com.sadakatsu.clue.testserver;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import com.sadakatsu.clue.cards.Card;
import com.sadakatsu.clue.cards.Suggestion;
import com.sadakatsu.clue.exception.DuplicateSuggestion;
import com.sadakatsu.clue.exception.InvalidDisprove;
import com.sadakatsu.clue.exception.InvalidSuggestionString;
import com.sadakatsu.clue.exception.ProtocolViolation;

/**
 * The Player represents an AI's server-side state and facilitates the server's
 * communication with the AI.
 * 
 * @author Joseph A. Craig
 */
public class Player {
	//********************* Protected and Private Fields *********************//
	private boolean eliminated;
	private BufferedReader in;
	private BufferedReader programOutput;
	private char[] buffer;
	private int index = -1;
	private List<Card> hand;
	private List<Suggestion> suggestions;
	private PrintWriter out;
	private Socket socket;
	private String identifier;
    private int score;
	
	//*************************** Public Interface ***************************//
	/**
	 * Instantiates a new Player for an AI that has connected to the server.
	 * @param identifier
	 * The identifier the server assigned to the AI.
	 * @param socket
	 * The Socket through which to communicate with the AI.
	 * @param process
	 * The process that was used to launch the AI.
	 * @throws IOException
	 * @throws ProtocolViolation
	 */
	public Player(
		String identifier,
		Socket socket,
		Process process
	) throws IOException, ProtocolViolation {
        score = 0;
		// Wrap the Socket's input and output streams to ease messaging.
		in = new BufferedReader(
			new InputStreamReader(socket.getInputStream())
		);
		out = new PrintWriter(socket.getOutputStream(), true);
		
		// Get the process's output stream.  If the AI writes messages to its
		// stdout for programmers to monitor for debugging, the Player needs to
		// empty this buffer to prevent deadlock.
		InputStream is = process.getInputStream();
		if (is != null) {
			programOutput = new BufferedReader(new InputStreamReader(is));
		} else {
			programOutput = null;
		}
		
		// Initialize all the Player internal state.
		buffer = new char[512];
		eliminated = true;
		hand = new ArrayList<>();
		suggestions = new ArrayList<>();
		this.identifier = identifier;
		this.socket = socket;
		
		// Get the "<identifier> alive" message.
		String response = getResponse();
		if (
			!response.equalsIgnoreCase(String.format("%s alive",  identifier))
		) {
			throw new ProtocolViolation(this, "alive", response);
		}
	}
	
	/**
	 * Determines whether this Player can disprove the passed Suggestion.
	 * @param suggestion
	 * The Suggestion in question.
	 * @return
	 * true if the Player can disprove the passed Suggestion, false otherwise.
	 */
	public boolean canDisprove(Suggestion suggestion) {
		return getDisproveCards(suggestion).size() > 0;
	}
	
	/**
	 * Determines whether the Player has the passed Card in his hand.
	 * @param card
	 * The Card in question.
	 * @return
	 * true if the Player has the passed Card in his hand, false otherwise.
	 */
	public boolean has(Card card) {
		return hand.contains(card);
	}
	
	/**
	 * Whether this Player has made an incorrect accusation and thus lost the
	 * game.
	 * @return
	 * true if the Player has already lost, false otherwise
	 */
	public boolean isEliminated() {
		return eliminated;
	}
	
	/**
	 * Has the Player disprove the passed Suggestion.  If this Player can use
	 * more than one Card to disprove the Suggestion, the connected AI is asked
	 * which one to use.
	 * @param suggesterIndex
	 * The index of the Player that made the Suggestion.
	 * @param suggestion
	 * The Suggestion in question.
	 * @return
	 * The Card the Player is using to disprove the suggestion.
	 * @throws IOException
	 * @throws ProtocolViolation
	 * @throws InvalidDisprove
	 */
	public Card disprove(
		int suggesterIndex,
		Suggestion suggestion
	) throws IOException, ProtocolViolation, InvalidDisprove {
		// If the Player has only one Card in the Suggestion, return that.
		List<Card> candidates = getDisproveCards(suggestion);
		if (candidates.size() == 1) {
			return candidates.get(0);
		}
		
		// Ask the connected AI which Card to use.
		sendMessage(
			String.format(
				"disprove %d %s",
				suggesterIndex,
				suggestion.getAbbreviation()
			)
		);
		
		// Retrieve the AI's response.  If the message is not formatted
		// correctly, throw a ProtocolViolation.
		String response = getResponse();
		if (
			response.length() != 7 ||
			!response.substring(0, 5).equalsIgnoreCase("show ")
		) {
			throw new ProtocolViolation(this, "disprove", response);
		}
		
		// Get the Card specified by the message.  If the Card specifier is
		// incorrect, throw a ProtocolViolation.
		Card card = Card.from(response.substring(5));
		if (card == null) {
			throw new ProtocolViolation(this, "disprove", response);
		}
		
		// If the Card is not a valid Card for disproving the Suggestion, throw
		// an InvalidDisprove.
		if (!has(card) || !suggestion.has(card)) {
			throw new InvalidDisprove(this, card, suggestion);
		}
		
		// Return the Card chosen.
		return card;
	}
	
	/**
	 * The Player's position in the play order.
	 * @return
	 * A number in the range [0..numberOfPlayers) if the Player is in a game.
	 * If the Player is not in a game, this was the Player's index in the last
	 * game he played (or -1 if he has not yet played a game).
	 */
	public int getIndex() {
		return index;
	}
	
	/**
	 * The String used by the server to uniquely identify this Player.
	 * @return
	 * The String passed to the constructor.
	 */
	public String getIndentifier() {
		return identifier;
	}
	
	/**
	 * Returns a user-friendly String for identifying this Player.
	 */
	public String toString() {
		StringBuilder sb = new StringBuilder("Player \"");
		sb.append(identifier);
		sb.append("\"");
		if (index > -1) {
			sb.append(" [");
			sb.append(index);
			sb.append("]");
		}
		return sb.toString();
	}

    public void printStatics() {
        System.out.format(" [%s]: %d\n", identifier, score);
    }
	
	/**
	 * Handles sending the specified accusation message to the connected AI and
	 * the AI's response.
	 * @param playerIndex
	 * The index of the Player that made the accusation.
	 * @param accusation
	 * The accusation that that Player made.
	 * @param correct
	 * Whether the accusation was correct.
	 * @throws IOException
	 * @throws ProtocolViolation
	 */
	public void accusation(
		int playerIndex,
		Suggestion accusation,
		boolean correct
	) throws IOException, ProtocolViolation {
		sendMessage(
			String.format(
				"accusation %d %s %c",
				playerIndex,
				accusation.getAbbreviation(),
				(correct ? '+' : '-')
			)
		);
		
		String response = getResponse();
		if (!response.equalsIgnoreCase("ok")) {
			throw new ProtocolViolation(this, "accusation", response);
		}
	}
	
	/**
	 * Asks the connected AI whether it wants to make an accusation. 
	 * @return
	 * Returns a Suggestion representing the AI's chosen accusation, or null if
	 * the AI chose not to make an accusation.
	 * @throws IOException
	 * @throws ProtocolViolation
	 */
	public Suggestion accuse() throws IOException, ProtocolViolation {
		Suggestion accusation = null;
		sendMessage("accuse");
		String response = getResponse();
		if (!response.equals("-")) {
			if (
				response.length() != 15 ||
				!response.substring(0, 7).equalsIgnoreCase("accuse ")
			) {
				throw new ProtocolViolation(this, "accuse", response);
			}
			
			try {
				accusation = new Suggestion(response.substring(7));
			} catch (InvalidSuggestionString e) {
				throw new ProtocolViolation(this, "accuse", response);
			}
		}
		
		return accusation;
	}
	
	/**
	 * Asks the connected AI to make a Suggestion.
	 * @return
	 * The Suggestion the AI chose to make.
	 * @throws IOException
	 * @throws ProtocolViolation
	 * @throws DuplicateSuggestion
	 */
	public Suggestion suggest()
	throws IOException, ProtocolViolation, DuplicateSuggestion {
		sendMessage("suggest");
		String response = getResponse();
		if (
			response.length() != 16 ||
			!response.substring(0, 8).equalsIgnoreCase("suggest ")
		) {
			throw new ProtocolViolation(this, "suggest", response);
		}
		
		Suggestion suggestion;
		try {
			suggestion = new Suggestion(response.substring(8));
			
			if (suggestions.contains(suggestion)) {
				throw new DuplicateSuggestion(this, suggestion);
			}
			
			suggestions.add(suggestion);
		} catch (InvalidSuggestionString e) {
			throw new ProtocolViolation(this, "suggest", response);
		}
		
		return suggestion;
	}
	
	/**
	 * Handles sending the command to stop to the connected AI and its response.
	 * @throws IOException
	 * @throws ProtocolViolation
	 */
	public void done() throws IOException, ProtocolViolation {
		try {
			sendMessage("done");
			String response = getResponse();
			if (!response.equalsIgnoreCase("dead")) {
				throw new ProtocolViolation(this, "dead", response);
			}
		} finally {
			socket.close();
		}
	}
	
	/**
	 * Flags this Player as having lost.
	 */
	public void lose() {
		eliminated = true;
	}
	
	/**
	 * Handles starting this Player in a new game.
	 * @param playerCount
	 * The number of Players in this game.
	 * @param playerIndex
	 * This Player's position in the play order.
	 * @param hand
	 * The Cards in this Player's hand.
	 * @throws IOException
	 * @throws ProtocolViolation
	 */
	public void reset(
		int playerCount,
		int playerIndex,
		Collection<Card> hand
	) throws IOException, ProtocolViolation {
		eliminated = false;
		index = playerIndex;
		suggestions.clear();
		this.hand.clear();
		this.hand.addAll(hand);
		
		StringBuilder message = new StringBuilder("reset ");
		message.append(playerCount);
		message.append(" ");
		message.append(playerIndex);
		for (Card c : hand) {
			message.append(" ");
			message.append(c.getAbbreviation());
		}
		
		sendMessage(message.toString());
		String response = getResponse();
		if (!response.equalsIgnoreCase("ok")) {
			throw new ProtocolViolation(this, "reset", response);
		}
	}
	
	/**
	 * Handles sending an update regarding the last Suggestion made to the
	 * connected AI.
	 * @param playerIndex
	 * The index of the Player that made the Suggestion.
	 * @param suggestion
	 * The Suggestion in question.
	 * @param disproverIndex
	 * The index of the Player that disproved the Suggestion.
	 * @param card
	 * The Card that was shown to the suggesting Player.  This should be null if
	 * this Player is neither the suggester nor the disprover.
	 * @throws IOException
	 * @throws ProtocolViolation
	 */
	public void suggestion(
		int playerIndex,
		Suggestion suggestion,
		Integer disproverIndex,
		Card card
	) throws IOException, ProtocolViolation {
		StringBuilder message = new StringBuilder(
			String.format(
				"suggestion %d %s ",
				playerIndex,
				suggestion.getAbbreviation()
			)
		);
		if (disproverIndex != null) {
			message.append(disproverIndex);
			if (index == playerIndex || index == disproverIndex.intValue()) {
				message.append(String.format(" %s", card.getAbbreviation()));
			}
		} else {
			message.append("-");
		}
		sendMessage(message.toString());
		
		String response = getResponse();
		if (!response.equalsIgnoreCase("ok")) {
			throw new ProtocolViolation(this, "suggestion", response);
		}
	}
	
	//******************* Protected and Private Interface ********************//
	/**
	 * Returns the Cards common to this Player's hand and the Suggestion.
	 * @param suggestion
	 * The Suggestion in question.
	 * @return
	 * A List of Cards.
	 */
	private List<Card> getDisproveCards(Suggestion suggestion) {
		List<Card> cards = suggestion.getCards();
		cards.retainAll(hand);
		return cards;
	}
	
	/**
	 * Waits for and cleans up the connected AI's response to a message. 
	 * @return
	 * The AI's response.
	 * @throws IOException
	 */
	private String getResponse() throws IOException {
		if (programOutput != null) {
			while (!in.ready()) {
				while (programOutput.ready()) {
					programOutput.readLine();
				}
			}
		}
		
		int read = in.read(buffer);
		while (read > 0 && buffer[read - 1] == '\0') {
			--read;
		}
		
		String response = String.copyValueOf(buffer, 0, read);
		System.out.format("    %s :>> \"%s\"\n", this, response);
		return response;
	}
	
	/**
	 * Sends a message to the connected AI.
	 * @param message
	 * The message to send to the AI.
	 * @throws IOException
	 */
	private void sendMessage(String message) throws IOException {
		System.out.format("    %s <<: \"%s\"\n", this, message);
		out.format(message);
	}

    public void win() {
        score += 1;
    }
}
