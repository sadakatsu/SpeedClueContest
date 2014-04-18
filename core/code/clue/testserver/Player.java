package clue.testserver;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import clue.cards.Card;
import clue.cards.Suggestion;
import clue.exception.DuplicateSuggestion;
import clue.exception.InvalidDisprove;
import clue.exception.InvalidSuggestionString;
import clue.exception.ProtocolViolation;

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
	
	//*************************** Public Interface ***************************//
	public Player(
		String identifier,
		Socket socket,
		Process process
	) throws IOException, ProtocolViolation {
		in = new BufferedReader(
			new InputStreamReader(socket.getInputStream())
		);
		out = new PrintWriter(socket.getOutputStream(), true);
		
		InputStream is = process.getInputStream();
		if (is != null) {
			programOutput = new BufferedReader(new InputStreamReader(is));
		} else {
			programOutput = null;
		}
		
		buffer = new char[512];
		eliminated = true;
		hand = new ArrayList<>();
		suggestions = new ArrayList<>();
		this.identifier = identifier;
		this.socket = socket;
		
		String response = getResponse();
		if (!response.equalsIgnoreCase(identifier + " alive")) {
			throw new ProtocolViolation(this, "alive", response);
		}
	}
	
	public boolean canDisprove(Suggestion suggestion) {
		return (
			has(suggestion.getSuspect()) ||
			has(suggestion.getWeapon()) ||
			has(suggestion.getRoom())
		);
	}
	
	public boolean has(Card card) {
		return hand.contains(card);
	}
	
	public boolean isEliminated() {
		return eliminated;
	}
	
	public Card disprove(
		int suggesterIndex,
		Suggestion suggestion
	) throws IOException, ProtocolViolation, InvalidDisprove {
		List<Card> candidates = handAnd(suggestion);
		if (candidates.size() == 1) {
			return candidates.get(0);
		}
		
		sendMessage(
			String.format(
				"disprove %d %s",
				suggesterIndex,
				suggestion.getAbbreviation()
			)
		);
		String response = getResponse();
		if (
			response.length() != 7 ||
			!response.substring(0, 5).equalsIgnoreCase("show ")
		) {
			throw new ProtocolViolation(this, "disprove", response);
		}
		
		Card card = Card.from(response.substring(5));
		if (card == null) {
			throw new ProtocolViolation(this, "disprove", response);
		}
		
		if (!has(card) || !suggestion.has(card)) {
			throw new InvalidDisprove(this, card, suggestion);
		}
		
		return card;
	}
	
	public int getIndex() {
		return index;
	}
	
	public List<Card> handAnd(Suggestion suggestion) {
		List<Card> intersection = suggestion.getCards();
		intersection.retainAll(hand);
		return intersection;
	}
	
	public String getIndentifier() {
		return identifier;
	}
	
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
	
	public void lose() {
		eliminated = true;
	}
	
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
	
	private void sendMessage(String message) throws IOException {
		System.out.format("    %s <<: \"%s\"\n", this, message);
		out.format(message);
	}
}
