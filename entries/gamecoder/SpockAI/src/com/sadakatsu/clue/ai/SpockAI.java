package com.sadakatsu.clue.ai;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.CompletionService;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorCompletionService;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Callable;

import com.sadakatsu.clue.cards.Card;
import com.sadakatsu.clue.cards.Hand;
import com.sadakatsu.clue.cards.Room;
import com.sadakatsu.clue.cards.Suggestion;
import com.sadakatsu.clue.cards.Suspect;
import com.sadakatsu.clue.cards.Weapon;
import com.sadakatsu.clue.exception.InvalidPlayerCount;
import com.sadakatsu.clue.knowledge.Knowledge;
import com.sadakatsu.clue.knowledge.exceptions.Contradiction;
import com.sadakatsu.util.ImmutablePair;

public class SpockAI extends SpeedClueAI {
	//********************* Protected and Private Fields *********************//
	private boolean[] stillPlaying;
	private ExecutorService threadPool;
	private Hand hand;
	private Knowledge knowledge;
	private Map<Card, Set<Integer>> shown;
	private Set<Suggestion> availableSuggestions;
	
	//*************************** Public Interface ***************************//
	public SpockAI(
		String identifier,
		int serverPort,
		boolean logMessages
	) throws IOException {
		super(identifier, serverPort, logMessages);
		
		shown = new HashMap<>();
		
		//*
		threadPool = Executors.newFixedThreadPool(
			Math.max(
				Runtime.getRuntime().availableProcessors(),
				1
			)
		);
		//*/
		// threadPool = Executors.newCachedThreadPool();
	}
	
	public void cleanUp() {
		threadPool.shutdownNow();
	}
	
	//******************* Protected and Private Interface ********************//
	@Override
	protected Suggestion accuse() {
		Suggestion accusation = null;
		if (knowledge.getPossibleSolutionCount() == 1) {
			accusation =
				knowledge.getPossibleSolutions().iterator().next();
		}
		return accusation;
	}

	@Override
	protected Card disprove(int suggester, Suggestion suggestion) {
		List<Card> candidates = new ArrayList<>(
			hand.getDisproveCards(suggestion)
		);
		for (Card c : candidates) {
			if (shown.get(c).contains(suggester)) {
				return c;
			}
		}
		
		List<ImmutablePair<Set<Card>, Card>> categories = new ArrayList<>();
		categories.add(
			new ImmutablePair<>(
				knowledge.getRemainingRooms(),
				suggestion.getRoom()
			)
		);
		categories.add(
			new ImmutablePair<>(
				knowledge.getRemainingSuspects(),
				suggestion.getSuspect()
			)
		);
		categories.add(
			new ImmutablePair<>(
				knowledge.getRemainingWeapons(),
				suggestion.getWeapon()
			)
		);
		Collections.sort(
			categories,
			new Comparator<ImmutablePair<Set<Card>, Card>>() {
				@Override
				public int compare(
					ImmutablePair<Set<Card>, Card> arg0,
					ImmutablePair<Set<Card>, Card> arg1
				) {
					return Integer.compare(
						arg1.getFirst().size(),
						arg0.getFirst().size()
					);
				}
			}
		);
		
		Card toReturn = null;
		for (ImmutablePair<Set<Card>, Card> pair : categories) {
			Card couldShow = pair.getSecond();
			if (hand.has(couldShow)) {
				toReturn = couldShow;
				break;
			}
		}
		return toReturn;
	}

	@Override
	protected void prepareForGame() {
		final int N = this.getPlayerCount();
		stillPlaying = new boolean[N];
		for (int i = 0; i < N; ++i) {
			stillPlaying[i] = true;
		}
		
		List<Card> cards = getHand();
		hand = new Hand(cards);
		
		shown.clear();
		for (Card c : cards) {
			shown.put(c, new HashSet<Integer>(N-1));
		}
		
		try {
			knowledge = new Knowledge(N, getIndex(), hand);
		} catch (InvalidPlayerCount e) {
			throw new IllegalStateException(
				String.format(
					"Somehow, Spock was passed an invalid player count: %s",
					N
				)
			);
		}
		
		availableSuggestions = new HashSet<>(Suggestion.getSuggestions());
		
		if (this.logMessages) {
			System.out.println(knowledge);
		}
	}

	@Override
	protected void processAccusation(
		int accuser,
		Suggestion accusation,
		boolean correct
	) {
		try {
			knowledge = knowledge.recordAccusation(
				accuser,
				accusation,
				correct
			);
			
			Integer winner = null;
			if (correct) {
				winner = accuser;
			} else {
				stillPlaying[accuser] = false;
				
				int playing = 0;
				for (int i = 0; playing < 2 && i < stillPlaying.length; ++i) {
					if (stillPlaying[i]) {
						winner = i;
						playing++;
					}
				}
				if (playing != 1) {
					winner = null;
				}
			}
			
			if (this.logMessages) {
				System.out.println(knowledge);
				
				if (winner != null) {
					System.out.format(
						"The game is over.  Player %s wins.\n\n",
						winner
					);
					
					for (int i = 0; i < 80; ++i) {
						System.out.print("=");
					}
					System.out.println("\n\n");
				}
			}
		} catch (Contradiction c) {
			throwException(c);
		}
	}

	@Override
	protected void processSuggestion(
		int suggester,
		Suggestion suggestion,
		Integer disprover,
		Card shown
	) {
		try {
			knowledge = knowledge.recordSuggestion(
				suggester,
				suggestion,
				disprover,
				shown
			);
			
			if (this.logMessages) {
				System.out.println(knowledge);
			}
		} catch (Contradiction c) {
			throwException(c);
		}
		
		if (disprover != null && disprover == getIndex()) {
			this.shown.get(shown).add(suggester);
		}
	}

	@Override
	protected void stopPlaying() {
		cleanUp();
	}

	@Override
	protected Suggestion suggest() {
		CompletionService<ImmutablePair<Suggestion, List<Integer>>> cs =
			new ExecutorCompletionService<>(threadPool);
		for (Suggestion s : availableSuggestions) {
			cs.submit(new SuggestionTest(s));
		}
		
		List<Integer> bestScore = null;
		List<Suggestion> options = new ArrayList<>();
		for (int i = 0; i < availableSuggestions.size(); ++i) {
			ImmutablePair<Suggestion, List<Integer>> returned;
			try {
				 returned = cs.take().get();
			} catch (InterruptedException | ExecutionException e) {
				if (e.getCause() instanceof Contradiction) {
					continue;
				}
				throw new IllegalStateException(e);
			}
			
			Suggestion suggestion = returned.getFirst();
			List<Integer> score = returned.getSecond();
			
			if (bestScore == null) {
				options.add(suggestion);
				bestScore = score;
			} else {
				int result = compare(bestScore, score);
				if (result > 0) {
					options.clear();
					bestScore = score;
				}
				if (result >= 0) {
					options.add(suggestion);
				}
			}
		}
		
		Collections.shuffle(options);
		Suggestion choice = options.get(0);
		availableSuggestions.remove(choice);
		return choice;
	}
	
	private void throwException(Exception e) {
		throw new IllegalStateException(
			String.format(
				"%s\n%s\n",
					e.getMessage(),
					Arrays.toString(e.getStackTrace())
			)
		);
	}
	
	private class SuggestionTest
	implements Callable<ImmutablePair<Suggestion, List<Integer>>> {
		private final Suggestion toTest;
		
		public SuggestionTest(Suggestion toTest) {
			this.toTest = toTest;
		}
		
		@Override
		public ImmutablePair<Suggestion, List<Integer>> call()
		throws Exception {
			final int N = getPlayerCount();
			int worstSC = 0;
			List<Integer> worstHC = getInitialList(N, 0);
			
			for (
				ImmutablePair<Integer, Card> outcome :
				knowledge.getOutcomes(getIndex(), toTest)
			) {
				try {
					Knowledge k = knowledge.recordSuggestion(
						getIndex(),
						toTest,
						outcome.getFirst(),
						outcome.getSecond()
					);

					int sc = k.getPossibleSolutionCount();
					List<Integer> hc = k.getPossibleHandCounts();
					Collections.sort(hc, Collections.reverseOrder());
					
					if (
						sc > worstSC ||
						sc == worstSC && compare(hc, worstHC) > 0
					) {
						worstHC = hc;
						worstSC = sc;
					}
				} catch (Contradiction c) {
					// Any Contradictions here SHOULD mean that the outcome was
					// impossible.  This SHOULD not be a fatal condition.  I
					// don't know how to prove this, though  v_v
				}
			}
			
			List<Integer> score = new ArrayList<>();
			score.add(worstSC);
			score.addAll(worstHC);
			return new ImmutablePair<>(toTest, score);
		}
	}
	
	private static int compare(List<Integer> a, List<Integer> b) {
		final int N = Math.min(a.size(), b.size());
		int result = 0;
		for (int i = 0; result == 0 && i < N; ++i) {
			result = a.get(i).compareTo(b.get(i));
		}
		return result;
	}
	
	private static List<Integer> getInitialList(int count, int value) {
		List<Integer> list = new ArrayList<>();
		for (int i = 0; i < count; ++i) {
			list.add(value);
		}
		return list;
	}
	
	public static void main(String[] args) {
		String identifier = "SpockAI";
		SpockAI spock = null;
		try {
			identifier = args[0];
			boolean logMessages = (
				args.length == 3 ?
					Boolean.parseBoolean(args[2]) :
					false
			);
			spock = new SpockAI(
				identifier,
				Integer.parseInt(args[1]),
				logMessages
			);
			spock.run();
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
		} finally {
			if (spock != null) {
				spock.cleanUp();
			}
		}
	}
}
