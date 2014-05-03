package com.sadakatsu.clue.testserver;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.lang.ProcessBuilder.Redirect;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.sadakatsu.clue.cards.Card;
import com.sadakatsu.clue.cards.Suggestion;
import com.sadakatsu.clue.exception.DuplicateSuggestion;
import com.sadakatsu.clue.exception.InvalidDisprove;
import com.sadakatsu.clue.exception.ProtocolViolation;
import com.sadakatsu.clue.server.Player;

/**
 * The TestServer handles launching the AIs listed in a script, having them play
 * a single match against each other, then closes them.
 * 
 * @author Joseph A. Craig
 */
public class TestServer {
	//********************* Protected and Private Fields *********************//
	private List<Player> players;
	private Random random;
	private ServerSocket accept;
	
	//*************************** Public Interface ***************************//
	/**
	 * Instantiates a new TestServer connected to the Players listed in the
	 * passed script.
	 * @param launchFilename
	 * The script that lists the commands for launching the AIs.
	 * @throws IOException
	 * @throws InterruptedException
	 * @throws ProtocolViolation
	 */
	public TestServer(
		String launchFilename
	) throws IOException, InterruptedException, ProtocolViolation {
		accept = new ServerSocket(0);
		players = new ArrayList<>();
		random = new Random();
		parseLaunchScript(launchFilename);
	}
	
	/**
	 * Runs a single game.
	 * @throws IOException
	 * @throws ProtocolViolation
	 * @throws DuplicateSuggestion
	 * @throws InvalidDisprove
	 */
	public void run()
	throws
		IOException,
		ProtocolViolation,
		DuplicateSuggestion,
		InvalidDisprove
	{
		int playerCount = players.size();
		
		System.out.println();
		for (int i = 0; i < 79; ++i) {
			System.out.print("-");
		}
		System.out.println("\nGame Start.");
		
		Suggestion solution = chooseSolution();
		System.out.format("Chosen Solution: %s\n", solution);
		
		List<List<Card>> hands = buildHands(solution);
		for (int i = 0; i < playerCount; ++i) {
			players.get(i).reset(playerCount, i, hands.get(i));
		}
		
		int active = 0;
		int stillPlaying = playerCount;
		while (stillPlaying > 1) {
			Player current = players.get(active);
			
			System.out.println();
			for (int i = 0; i < 79; ++i) {
				System.out.print("-");
			}
			System.out.format(
				"\nIt is %s's turn.\nWhat does %s suggest?\n",
				current,
				current
			);
			
			Suggestion suggestion = current.suggest();
			System.out.format("\n%s suggests %s.\n", current, suggestion);
			
			Card shown = null;
			int disprover;
			for (
				disprover = getNextPlayerIndex(active);
				disprover != active;
				disprover = getNextPlayerIndex(disprover)
			) {
				Player next = players.get(disprover);
				if (next.canDisprove(suggestion)) {
					System.out.format("  %s can disprove.\n", next);
					shown = next.disprove(active, suggestion);
					System.out.format("%s disproves with %s.\n", next, shown);
					break;
				}
				
				System.out.format("  %s cannot disprove.\n", next);
			}
			
			for (int i = 0; i < playerCount; ++i) {
				players.get(i).suggestion(
					active,
					suggestion,
					shown == null ? null : disprover,
					shown
				);
			}
			
			System.out.format("\nWill %s make an accusation?\n", current);
			Suggestion accusation = current.accuse();
			if (accusation != null) {
				boolean correct = accusation.equals(solution);
				
				System.out.format(
					"%s makes the accusation %s.\nIt is %s\n",
					current,
					accusation,
					(correct ? "right!" : "wrong.")
				);
				
				if (correct) {
					stillPlaying = 0;
				} else {
					current.lose();
					--stillPlaying;
				}
				
				for (int i = 0; i < playerCount; ++i) {
					players.get(i).accusation(active, accusation, correct);
				}
			} else {
				System.out.format("%s makes no accusation\n", current);
			}
			
			if (stillPlaying > 0) {
				do {
					active = getNextPlayerIndex(active);
				} while (players.get(active).isEliminated());
			}
		}
		
		if (stillPlaying == 1) {
			System.out.format("%s wins by default.\n", players.get(active));
		}
		
		System.out.println("\n");
		for (int i = 0; i < 79; ++i) {
			System.out.print("-");
		}
		
		System.out.format(
			"\nThe game is over.\nThe winner is %s.\n",
			players.get(active)
		);
	}
	
	//******************* Protected and Private Interface ********************//
	/**
	 * Returns the index of the next Player in the play order.
	 * @param current
	 * A Player's index.
	 * @return
	 * The index of the next Player.
	 */
	private int getNextPlayerIndex(int current) {
		return (current + 1) % players.size();
	}
	
	/**
	 * Gets the deck of Cards to deal to the Players.
	 * @param solution
	 * The Cards that have been removed from the deck.
	 * @return
	 * A List of the Cards that remain to deal to Players.
	 */
	private List<Card> getDeck(Suggestion solution) {
		List<Card> deck = Card.getCards();
		deck.removeAll(solution.getCards());
		return deck;
	}
	
	/**
	 * Builds the hands to pass to the Players for a new game.
	 * @param solution
	 * The solution for the game.
	 * @return
	 * A List of Card Lists to pass to the Players.  The List is ordered, so
	 * element 0 goes to Player 0 and so on.
	 */
	private List<List<Card>> buildHands(Suggestion solution) {
		List<List<Card>> hands = new ArrayList<>();
		
		List<Card> deck = getDeck(solution);
		Collections.shuffle(deck, random);
		
		int cardCount = deck.size();
		int playerCount = players.size();
		int minHandSize = cardCount / playerCount;
		int remainder = cardCount % playerCount;
		
		int dealt = 0;
		for (int i = 0; i < playerCount; ++i) {
			int handSize = minHandSize + (i < remainder ? 1 : 0);
			hands.add(deck.subList(dealt, dealt + handSize));
			dealt += handSize;
		}
		
		return hands;
	}
	
	/**
	 * Chooses a solution for a new game.
	 * @return
	 * A Suggestion to be used as a solution.
	 */
	private Suggestion chooseSolution() {
		List<Suggestion> suggestions = Suggestion.getSuggestions();
		Collections.shuffle(suggestions, random);
		return suggestions.get(0);
	}
	
	/**
	 * Closes all the connections with the Players to end execution cleanly.
	 * @throws IOException
	 * @throws ProtocolViolation
	 */
	private void cleanUp() throws IOException, ProtocolViolation {
		closeConnections();
		accept.close();
	}
	
	/**
	 * Closes the connection with each Player. 
	 * @throws IOException
	 * @throws ProtocolViolation
	 */
	private void closeConnections() throws IOException, ProtocolViolation {
		for (Player p : players) {
			p.done();
		}
	}
	
	/**
	 * Parses a script of AI programs with which to play, launching and
	 * connecting to each of them as a new Player.
	 * @param launchFilename
	 * The formatted script containing the commands to run to be Players.  Each
	 * line must be a separate command.  Each line must wrap the intended
	 * identifier with curly braces and placehold the server port number with
	 * two percent signs ("%%").
	 * @throws FileNotFoundException
	 * @throws IOException
	 * @throws InterruptedException
	 * @throws ProtocolViolation
	 */
	private void parseLaunchScript(
		String launchFilename
	) throws
		FileNotFoundException,
		IOException,
		InterruptedException,
		ProtocolViolation
	{
		int portNumber = accept.getLocalPort(); 
		Pattern getIdentifier = Pattern.compile("\\{([^}]+)\\}");
		String portString = String.valueOf(portNumber);
		
		System.out.println("Launching Players...");
		
		try (
			BufferedReader br = new BufferedReader(
				new FileReader(launchFilename)
			)
		) {
			String line;
			while ((line = br.readLine()) != null) {
				Matcher matcher = getIdentifier.matcher(line);
				if (!matcher.find()) {
					throw new IllegalStateException(
						"The identifier must be wrapped with curly braces."
					);
				}
				String identifier = matcher.group(1);
				line = matcher.replaceAll(identifier);
				line = line.replaceAll("%%", portString);
				
				System.out.format("Starting \"%s\": %s\n", identifier, line);
				
				ProcessBuilder pb = new ProcessBuilder(line.split(" "));
				// pb.redirectError(Redirect.INHERIT);
				// pb.redirectOutput(Redirect.INHERIT);
				File f = new File(String.format("output_%s.txt", identifier));
				pb.redirectError(f);
				pb.redirectOutput(f);
				Process process = pb.start();
				
				System.out.println("... started");
				Socket socket = accept.accept();
				System.out.println("... accepted");
				players.add(new Player(identifier, socket, process));
				System.out.println("... running!");
			}
		}
	}
	
	//*********************** Public Static Interface ************************//
	/**
	 * "java -jar testServer.jar script"
	 * @param args
	 * - script: the path and filename for the launch script
	 * @throws Exception
	 */
	public static void main(String[] args) throws Exception {
		TestServer ts = new TestServer(args[0]);
		try {
			ts.run();
		} finally {
			ts.cleanUp();
		}
	}
}
