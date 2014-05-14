package com.sadakatsu.clue.ai;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

import com.sadakatsu.clue.cards.Card;
import com.sadakatsu.clue.cards.Suggestion;

/**
 * RandomAI is an AI that randomly makes Suggestions with no duplicates.  It
 * does not use any information other than the last Suggestion it made and
 * whether that Suggestion was disproved.  When asked to make an accusation, it
 * will only raise the last Suggestion it made as an accusation if that
 * Suggestion was not disproved and if it does not hold any of the cards in its
 * Suggestion in its hand.
 * 
 * @author Joseph A. Craig
 */
public class RandomAI extends SpeedClueAI {
	//********************* Protected and Private Fields *********************//
	boolean disproved;
	List<Suggestion> suggestions;
	Random random;
	Suggestion last;
	
	//*************************** Public Interface ***************************//
	/**
	 * Instantiates a RandomAI connected to a localhost Speed Clue server.
	 * @param identifier
	 * The identifier for this RandomAI instance assigned by the server.
	 * @param serverPort
	 * The port number with which this RandomAI should correspond.
	 * @param logMessages
	 * Whether this instance should write the messages it receives and sends to
	 * stdout and report any message problems to stderr.
	 * @throws IOException
	 */
	public RandomAI(
		String identifier,
		int serverPort,
		boolean logMessages
	) throws IOException {
		super(identifier, serverPort, logMessages);
		disproved = false;
		last = null;
		random = new Random();
		suggestions = new ArrayList<>();
	}
	
	//******************* Protected and Private Interface ********************//
	@Override
	protected Suggestion accuse() {
		return (
			disproved || getMyDisproveCards(last).size() > 0 ?
				null :
				last
		);
	}

	@Override
	protected Card disprove(int suggester, Suggestion suggestion) {
		List<Card> candidates = getMyDisproveCards(suggestion);
		Collections.shuffle(candidates, random);
		return candidates.get(0);
	}

	@Override
	protected Suggestion suggest() {
		return suggestions.remove(0);
	}

	@Override
	protected void prepareForGame() {
		suggestions = Suggestion.getSuggestions();
		Collections.shuffle(suggestions, random);
	}

	@Override
	protected void stopPlaying() {}

	@Override
	protected void processAccusation(
		int accuser,
		Suggestion accusation,
		boolean correct
	) {}

	@Override
	protected void processSuggestion(
		int suggester,
		Suggestion suggestion,
		Integer disprover,
		Card shown
	) {
		if (suggester == getIndex()) {
			last = suggestion;
			disproved = disprover != null;
		}
	}
	
	//*********************** Public Static Interface ************************//
	/**
	 * "java -jar randomAI.jar identifier serverPortNumber [logMessages]"
	 * @param args
	 * - identifier: a unique name to identify the AI
	 * 
	 * - serverPortNumber: the port the server will use to send and receive
	 * messages
	 * 
	 * - logMessages: whether the AI should report messages on stdout and stderr 
	 */
	public static void main(String[] args) {
		String identifier = "RandomAI";
		try {
			identifier = args[0];
			boolean logMessages = (
				args.length == 3 ?
					Boolean.parseBoolean(args[2]) :
					false
			);
			new RandomAI(
				identifier,
				Integer.parseInt(args[1]),
				logMessages
			).run();
		} catch (Exception e) {
			try (
				PrintWriter pw = new PrintWriter(
					new File(
						String.format("error_%s.txt", identifier)
					)
				)
			) {
				pw.format("%s\n", e.getMessage());
				for (StackTraceElement ste : e.getStackTrace()) {
					pw.format("  %s\n", ste.toString());
				}
			} catch (FileNotFoundException fne) {}
		}
	}
}
