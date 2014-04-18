package clue.randomplayer;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

import clue.cards.*;
import clue.exception.InvalidSuggestionString;

public class RandomPlayer {
	//********************* Protected and Private Fields *********************//
	BufferedReader in;
	boolean disproved;
	char[] buffer;
	int index;
	List<Card> hand;
	List<Suggestion> suggestions;
	PrintWriter out;
	Random random;
	Socket socket;
	String identifier;
	Suggestion last;
	
	//*************************** Public Interface ***************************//
	public RandomPlayer(
		String identifier,
		int serverPort
	) throws UnknownHostException, IOException {
		socket = new Socket("localhost", serverPort);
		in = new BufferedReader(
			new InputStreamReader(socket.getInputStream())
		);
		out = new PrintWriter(socket.getOutputStream(), true);
		
		buffer = new char[512];
		disproved = false;
		hand = new ArrayList<>();
		index = -1;
		this.identifier = identifier;
		last = null;
		random = new Random();
		suggestions = new ArrayList<>();
		
		System.out.println("Succesfully instantiated!\n");
		for (int i = 0; i < 1000000000; ++i);
		sendResponse(String.format("%s alive", identifier));
	}
	
	public String toString() {
		return String.format(
			"Player \"%s\"%s",
			identifier,
			(index > -1 ? String.format(" [%d]", index) : "")
		);
	}
	
	public void run() throws IOException, InvalidSuggestionString {
		boolean running = true;
		do {
			String message = getMessage();
			String type = getMessageType(message);
			
			if (type.equals("reset")) {
				reset(message);
			} else if (type.equals("suggest")) {
				suggest();
			} else if (type.equals("disprove")) {
				disprove(message);
			} else if (type.equals("suggestion")) {
				suggestion(message);
			} else if (type.equals("accuse")) {
				accuse();
			} else if (type.equals("accusation")) {
				accusation(message);
			} else if (type.equals("done")) {
				done();
				running = false;
			}
			
		} while (running);
	}
	
	//******************* Protected and Private Interface ********************//
	private List<Card> handAnd(Suggestion suggestion) {
		List<Card> intersection = suggestion.getCards();
		intersection.retainAll(hand);
		return intersection;
	}
	
	private String getMessage() throws IOException {
		int read = in.read(buffer);
		while (read > 0 && buffer[read - 1] == '\0') {
			--read;
		}
		
		String response = String.copyValueOf(buffer, 0, read);
		System.out.format("    %s <<: \"%s\"\n", this, response);
		return response;
	}
	
	private String getMessageType(String message) {
		return message.split(" ", 2)[0].toLowerCase();
	}
	
	private void accusation(String message) {
		sendResponse("ok");
	}
	
	private void accuse() {
		if (disproved && handAnd(last).size() == 0) {
			sendResponse(String.format("accuse %s", last.getAbbreviation()));
		} else {
			sendResponse("-");
		}
	}
	
	private void done() throws IOException {
		sendResponse("dead");
		in.close();
		out.close();
		socket.close();
	}
	
	private void disprove(String message) throws InvalidSuggestionString {
		Suggestion suggestion = new Suggestion(message.substring(11));
		List<Card> candidates = handAnd(suggestion);
		Collections.shuffle(candidates);
		sendResponse(
			String.format("show %s", candidates.get(0).getAbbreviation())
		);
	}
	
	private void reset(String message) {
		String[] tokens = message.split("\\s");
		index = Integer.parseInt(tokens[2]);
		
		hand.clear();
		for (int i = 3; i < tokens.length; ++i) {
			hand.add(Card.from(tokens[i]));
		}
		
		suggestions = Suggestion.getSuggestions();
		Collections.shuffle(suggestions);
		
		sendResponse("ok");
	}
	
	private void sendResponse(String response) {
		System.out.format("    %s :>> \"%s\"\n", this, response);
		out.format(response);
	}
	
	private void suggest() {
		last = suggestions.remove(0);
		sendResponse(String.format("suggest %s", last.getAbbreviation()));
	}
	
	private void suggestion(String message) {
		String[] tokens = message.split("\\s");
		int activePlayer = Integer.parseInt(tokens[1]);
		if (index == activePlayer) {
			disproved = tokens[5].equals("-");
		}
		sendResponse("ok");
	}
	
	//*********************** Public Static Interface ************************//
	public static void main(String[] args) throws Exception {
		try {
			new RandomPlayer(args[0], Integer.parseInt(args[1])).run();
		} catch (Exception e) {
			PrintWriter pw;
			pw = new PrintWriter(new File("error.txt"));
			pw.format("%s\n", e.getMessage());
			for (StackTraceElement ste : e.getStackTrace()) {
				pw.format("  %s\n", ste.toString());
			}
			pw.close();
		}
	}
}
