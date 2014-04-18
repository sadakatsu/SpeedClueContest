package clue.testserver;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import clue.cards.Card;
import clue.cards.Suggestion;
import clue.exception.DuplicateSuggestion;
import clue.exception.InvalidDisprove;
import clue.exception.ProtocolViolation;

public class TestServer {
	//********************* Protected and Private Fields *********************//
	private List<Player> players;
	private Random random;
	private ServerSocket accept;
	
	//*************************** Public Interface ***************************//
	public TestServer(
		String launchFilename
	) throws IOException, InterruptedException, ProtocolViolation {
		accept = new ServerSocket(0);
		players = new ArrayList<>();
		random = new Random();
		parseLaunchScript(launchFilename);
	}
	
	public void run()
	throws
		IOException,
		ProtocolViolation,
		DuplicateSuggestion,
		InvalidDisprove
	{
		int playerCount = players.size();
		
		Suggestion solution = chooseSolution();
		System.out.format("Chosen Solution: %s\n", solution);
		
		List<List<Card>> hands = dealCards(solution);
		for (int i = 0; i < playerCount; ++i) {
			players.get(i).reset(playerCount, i, hands.get(i));
		}
		
		int active = 0;
		int stillPlaying = playerCount;
		while (stillPlaying > 1) {
			Player current = players.get(active);
			Suggestion suggestion = current.suggest();
			System.out.format("%s suggests %s\n", current, suggestion);
			
			Card shown = null;
			int disprover;
			for (
				disprover = getNextPlayerIndex(active);
				disprover != active;
				disprover = getNextPlayerIndex(disprover)
			) {
				Player next = players.get(disprover);
				if (next.canDisprove(suggestion)) {
					System.out.format("%s can disprove.\n", next);
					shown = next.disprove(active, suggestion);
					System.out.format("%s shows %s.\n", next, shown);
					break;
				}
				
				System.out.format("%s cannot disprove...\n", next);
			}
			
			for (int i = 0; i < playerCount; ++i) {
				players.get(i).suggestion(
					active,
					suggestion,
					shown == null ? null : disprover,
					shown
				);
			}
			
			Suggestion accusation = current.accuse();
			if (accusation != null) {
				boolean correct = accusation.equals(solution);
				
				System.out.format(
					"%s makes the accusation %s... it is %s\n",
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
	}
	
	//******************* Protected and Private Interface ********************//
	private int getNextPlayerIndex(int current) {
		return (current + 1) % players.size();
	}
	
	private List<Card> shuffleDeck(Suggestion solution) {
		List<Card> deck = Card.getCards();
		deck.removeAll(solution.getCards());
		return deck;
	}
	
	private List<List<Card>> dealCards(Suggestion solution) {
		List<List<Card>> hands = new ArrayList<>();
		
		List<Card> deck = shuffleDeck(solution);
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
	
	private Suggestion chooseSolution() {
		List<Suggestion> suggestions = Suggestion.getSuggestions();
		Collections.shuffle(suggestions, random);
		return suggestions.get(0);
	}
	
	
	
	private void cleanUp() throws IOException, ProtocolViolation {
		closeConnections();
		accept.close();
	}
	
	private void closeConnections() throws IOException, ProtocolViolation {
		for (Player p : players) {
			p.done();
		}
	}
	
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
		Runtime r = Runtime.getRuntime();
		String portString = String.valueOf(portNumber);
		
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
				
				Process process = r.exec(line);
				Socket socket = accept.accept();
				players.add(new Player(identifier, socket, process));
			}
		}
	}
	
	//*********************** Public Static Interface ************************//
	public static void main(String[] args) throws Exception {
		TestServer ts = new TestServer(args[0]);
		try {
			ts.run();
		} finally {
			ts.cleanUp();
		}
	}
}
