package com.sadakatsu.clue.knowledge;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import com.sadakatsu.clue.cards.Card;
import com.sadakatsu.clue.cards.Hand;
import com.sadakatsu.clue.cards.Room;
import com.sadakatsu.clue.cards.Suggestion;
import com.sadakatsu.clue.cards.Suspect;
import com.sadakatsu.clue.cards.Weapon;
import com.sadakatsu.clue.exception.InvalidPlayerCount;
import com.sadakatsu.clue.knowledge.exceptions.Contradiction;
import com.sadakatsu.util.ImmutablePair;

public class Knowledge {
	//********************* Protected and Private Fields *********************//
	private Integer playerIndex;
	private List<Record> history;
	private Map<Card, PlayerFacts> holders;
	private Map<Class<? extends Card>, Set<Card>> candidates;
	private PlayerFacts[] players;
	private Set<Suggestion> possibleSolutions;
	
	//*************************** Public Interface ***************************//
	public Knowledge(int playerCount) throws InvalidPlayerCount {
		init(playerCount, null, null);
	}
	
	public Knowledge(int playerCount, int playerIndex, Hand hand)
	throws InvalidPlayerCount {
		init(playerCount, playerIndex, hand);
	}
	
	public Knowledge assumePlayerHolds(int index, Hand hand)
	throws Contradiction {
		/*
		// How does this fail to find the matches?
		if (!(players[index].getPossibleHands().contains(hand))) {
			// System.out.println(players[index].getPossibleHands());
			throw new Contradiction(
				"%s was asserted to have the hand %s.",
					players[index],
					hand
			);
		}
		//*/
		
		Knowledge next = new Knowledge(this);
		next.players[index] = new PlayerFacts(hand);
		next.deduce();
		return next;
	}
	
	public Knowledge recordAccusation(
		int accuser,
		Suggestion accusation,
		boolean correct
	) throws Contradiction {
		Knowledge next = new Knowledge(this);
		next.addAccusationToHistory(accuser, accusation, correct);
		next.notePlayerCouldNotDisprove(accuser, accusation);
		if (correct) {
			next.filterSolutions(
				true,
				accusation.getCards().toArray(new Card[3])
			);
		} else {
			next.possibleSolutions.remove(accusation);
			// TODO: Consider whether I want more processing for eliminating
			// candidates here or whether such processing should go into
			// bubbleInferences().
		}
		next.deduce();
		return next;
	}
	
	public Knowledge recordSuggestion(
		int suggester,
		Suggestion suggestion,
		Integer disprover,
		Card shown
	) throws Contradiction {
		Knowledge next = new Knowledge(this);
		next.learnFromAbsentAccusation();
		next.addSuggestionToHistory(suggester, suggestion, disprover, shown);
		next.learnBasicInformationFromSuggestion();
		next.deduce();
		next.refineHistory();
		return next;
	}
	
	public int getPossibleSolutionCount() {
		return possibleSolutions.size();
	}
	
	public List<ImmutablePair<Integer, Card>> getOutcomes(
		int suggester,
		Suggestion suggestion
	) {
		boolean stopped = false;
		List<ImmutablePair<Integer, Card>> outcomes = new ArrayList<>();
		for (
			int i = getNext(suggester);
			!stopped && i != suggester;
			i = getNext(i)
		) {
			Collection<Card> canShow = suggestion.getCards();
			canShow.removeAll(players[i].getKnownToNotHaveCards());
			for (Card c : canShow) {
				outcomes.add(new ImmutablePair<>(i, c));
			}
			
			Collection<Card> kth = suggestion.getCards();
			kth.retainAll(players[i].getKnownToHaveCards());
			if (kth.size() > 0) {
				stopped = true;
			}
		}
		
		if (!stopped) {
			outcomes.add(new ImmutablePair<Integer, Card>(null, null));
		}
		
		return outcomes;
	}
	
	public List<Integer> getPossibleHandCounts() {
		List<Integer> possibleHandCounts = new ArrayList<>();
		for (PlayerFacts pf : players) {
			possibleHandCounts.add(pf.getPossibleHandCount());
		}
		return possibleHandCounts;
	}
	
	public Set<Card> getRemainingRooms() {
		return new HashSet<>(candidates.get(Room.class));
	}
	
	public Set<Card> getRemainingSuspects() {
		return new HashSet<>(candidates.get(Suspect.class));
	}
	
	public Set<Card> getRemainingWeapons() {
		return new HashSet<>(candidates.get(Weapon.class));
	}
	
	public Set<Hand> getPossibleHandsFor(int index) {
		return players[index].getPossibleHands();
	}
	
	/**
	 * @return
	 * A worst-case number of possible combinations of possible solutions and
	 * Player Hands.
	 */
	public long estimateNumberOfPossibilities() {
		long possibilities = possibleSolutions.size();
		for (PlayerFacts p : players) {
			possibilities *= p.getPossibleHandCount();
		}
		return possibilities;
	}
	
	public Set<Suggestion> getPossibleSolutions() {
		return new HashSet<>(possibleSolutions);
	}
	
	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		
		StringBuilder sepB = new StringBuilder();
		for (int i = 0; i < 78; ++i) {
			sepB.append("-");
		}
		sepB.append("\n");
		String sep = sepB.toString();
		
		sb.append(sep);
		
		sb.append(String.format("PS:  %3d | ", possibleSolutions.size()));
		for (Card c : Suspect.getSuspects()) {
			sb.append(String.format("%s ", c.getAbbreviation()));
		}
		sb.append("| ");
		for (Card c : Weapon.getWeapons()) {
			sb.append(String.format("%s ", c.getAbbreviation()));
		}
		sb.append("| ");
		for (Card c : Room.getRooms()) {
			sb.append(String.format("%s ", c.getAbbreviation()));
		}
		sb.append("\n");
		
		sb.append(sep);
		
		for (int i = 0; i < players.length; ++i) {
			sb.append(
				String.format("%d:%6d |", i, players[i].getPossibleHandCount())
			);
			
			for (Card c : Suspect.getSuspects()) {
				String toPrint;
				switch (players[i].has(c)) {
					case NO:    toPrint = "x"; break;
					case YES:   toPrint = "O"; break;
					default:    toPrint = " "; break;
				}
				sb.append(String.format("  %s", toPrint));
			}
			
			sb.append(" |");
			
			for (Card c : Weapon.getWeapons()) {
				String toPrint;
				switch (players[i].has(c)) {
					case NO:    toPrint = "x"; break;
					case YES:   toPrint = "O"; break;
					default:    toPrint = " "; break;
				}
				sb.append(String.format("  %s", toPrint));
			}
			
			sb.append(" |");
			
			for (Card c : Room.getRooms()) {
				String toPrint;
				switch (players[i].has(c)) {
					case NO:    toPrint = "x"; break;
					case YES:   toPrint = "O"; break;
					default:    toPrint = " "; break;
				}
				sb.append(String.format("  %s", toPrint));
			}
			
			sb.append("\n");
		}
		
		sb.append(sep);
		
		return sb.toString();
	}
	
	//******************* Protected and Private Interface ********************//
	private Knowledge(Knowledge that) {
		this.candidates = new HashMap<>();
		for (
			Entry<Class<? extends Card>, Set<Card>> entry :
			that.candidates.entrySet()
		) {
			candidates.put(entry.getKey(), new HashSet<>(entry.getValue()));
		}
		
		
		this.history = new ArrayList<>(that.history);
		this.holders = new HashMap<>(that.holders);
		this.playerIndex = that.playerIndex;
		this.players = that.players.clone();
		this.possibleSolutions = new HashSet<>(that.possibleSolutions);
	}
	
	private Collection<Card> getCardsFor(Class<? extends Card> category) {
		if (category == Suspect.class) {
			return Suspect.getSuspects();
		} else if (category == Weapon.class) {
			return Weapon.getWeapons();
		} else if (category == Room.class) {
			return Room.getRooms();
		}
		
		return null;
	}
	
	private Collection<Card> getDeck(Hand exclude) {
		Collection<Card> deck = Card.getCards();
		if (exclude != null) {
			deck.removeAll(exclude.getCards());
		}
		return deck;
	}
	
	private int getNext(int current) {
		return (current + 1) % players.length;
	}
	
	private List<Integer> whoCanHold(Card card) {
		List<Integer> answer = new ArrayList<>();
		for (int i = 0; i < players.length; ++i) {
			if (players[i].has(card) != PlayerHasCard.NO) {
				answer.add(i);
			}
		}
		return answer;
	}
	
	private Record getLastRecord() {
		return history.get(history.size() - 1);
	}
	
	private void addAccusationToHistory(
		int accuser,
		Suggestion accusation,
		boolean correct
	) {
		history.add(new AccusationRecord(accuser, accusation, correct));
	}
	
	private void addSuggestionToHistory(
		int suggester,
		Suggestion suggestion,
		Integer disprover,
		Card shown
	) {
		history.add(
			new SuggestionRecord(
				suggester,
				suggestion,
				disprover,
				shown,
				disprover == null ?
					null :
					players[disprover]
			)
		);
	}
	
	private void bubbleInferences()
	throws Contradiction {
		// Use what has been learned to deduce more information until nothing
		// more can be deduced.
		PlayerFacts[] previous;
		do {
			// Clone the PlayerFacts states before any deductions have been
			// made.
			previous = players.clone();
			
			// Ensure that any Cards that may have been eliminated from the
			// solution have been noted as part of the solution.
			Map<Class<? extends Card>, Set<Card>> deducedCandidates;
			deducedCandidates = new HashMap<>();
			deducedCandidates.put(
				Suspect.class,
				new HashSet<>(Suspect.getSuspects())
			);
			deducedCandidates.put(
				Weapon.class,
				new HashSet<>(Weapon.getWeapons())
			);
			deducedCandidates.put(
				Room.class,
				new HashSet<>(Room.getRooms())
			);
			for (PlayerFacts pf : players) {
				for (Card c : pf.getKnownToHaveCards()) {
					deducedCandidates.get(c.getClass()).remove(c);
				}
			}
			
			for (Set<Card> categoryCandidates : deducedCandidates.values()) {
				if (categoryCandidates.size() == 1) {
					filterSolutions(true, categoryCandidates.iterator().next());
					for (int i = 0; i < players.length; ++i) {
						players[i] = players[i].ifHasNone(categoryCandidates);
					}
				}
			}
			
			Set<Card> inSolution = new HashSet<>(Card.getCards());
			for (PlayerFacts pf : players) {
				inSolution.retainAll(pf.getKnownToNotHaveCards());
				if (inSolution.size() == 0) {
					break;
				}
			}
			for (Card c : inSolution) {
				filterSolutions(true, c);
			}
			
			// If the Card from a given category for the solution is known, then
			// any other Cards in that category must be in a Player's Hand.  If
			// such a Card can be in only one Player's Hand, then that Player
			// holds that Card.
			for (
				Entry<Class<? extends Card>, Set<Card>> entry :
				candidates.entrySet()
			) {
				Class<? extends Card> category = entry.getKey();
				Set<Card> unknown = entry.getValue();
				if (unknown.size() == 1) {
					Card proven = unknown.iterator().next();
					for (int i = 0; i < players.length; ++i) {
						players[i] = players[i].ifHasNone(proven);
					}
					
					for (Card c : getCardsFor(category)) {
						if (!holders.containsKey(c)) {
							List<Integer> canHold = whoCanHold(c);
							if (canHold.size() == 1) {
								notePlayerHas(canHold.get(0), c);
							}
						}
					}
				}
			}
			
			// Guarantee that each Card that a Player has been deduced to hold
			// is removed from every other Player's Hand and has been removed
			// from every possible solution.
			for (int i = 0; i < players.length; ++i) {
				Collection<Card> kth = players[i].getKnownToHaveCards();
				if (kth.size() > 0) {
					for (int j = 0; j < players.length; ++j) {
						if (i != j) {
							players[j] = players[j].ifHasNone(kth);
						}
					}
					
					filterSolutions(false, kth.toArray(new Card[kth.size()]));
				}
			}
		} while (!Arrays.equals(previous, players));
	}
	
	private void deduce() throws Contradiction {
		bubbleInferences();
		if (estimateNumberOfPossibilities() <= THRESHOLD) {
			filterPossibilities();
		}
	}
	
	private void filterPossibilities() {
		// TODO: Write the method to eliminate any possible solutions or hands
		// for which there is no legal combination with the other
		// solutions/hands.
	}
	
	private void filterSolutions(boolean has, Card...cards)
	throws Contradiction {
		for (Card c : cards) {
			noteForSolution(c, has);
		}
		
		Iterator<Suggestion> iterator = possibleSolutions.iterator();
		while (iterator.hasNext()) {
			Suggestion s = iterator.next();
			for (Card c : cards) {
				if (s.has(c) ^ has) {
					iterator.remove();
					break;
				}
			}
		}
		if (possibleSolutions.size() == 0) {
			throw new Contradiction(
				"All possible solutions have been eliminated."
			);
		}
	}
	
	private void init(int playerCount, Integer playerIndex, Hand hand)
	throws InvalidPlayerCount {
		validatePlayerCount(playerCount);
		
		this.playerIndex = playerIndex;
		seedPossibleSolutions(hand);
		
		holders = new HashMap<>();
		
		candidates = new HashMap<>();
		candidates.put(Suspect.class, new HashSet<>(Suspect.getSuspects()));
		candidates.put(Weapon.class, new HashSet<>(Weapon.getWeapons()));
		candidates.put(Room.class, new HashSet<>(Room.getRooms()));
		
		players = new PlayerFacts[playerCount];
		Collection<Card> deck = getDeck(hand);
		int[] handSizes = getHandSizes(playerCount);
		for (int i = 0; i < playerCount; ++i) {
			if (playerIndex == null || i != playerIndex || hand == null) {
				players[i] = new PlayerFacts(handSizes[i], deck);
			} else {
				players[i] = new PlayerFacts(hand);
			}
		}
		
		if (hand != null) {
			for (Card c : hand.getCards()) {
				candidates.get(c.getClass()).remove(c);
				holders.put(c, players[playerIndex]);
			}
		}
		
		history = new ArrayList<>();
	}
	
	private void learnBasicInformationFromSuggestion() throws Contradiction {
		SuggestionRecord sr = (SuggestionRecord) getLastRecord();
		Card shown = sr.getShownCard();
		for (
			int i = getNext(sr.suggester);
			i != sr.suggester && (
				sr.disprover == null || i != getNext(sr.disprover)
			);
			i = getNext(i)
		) {
			if (sr.disprover == null || i != sr.disprover) {
				notePlayerCouldNotDisprove(i, sr.suggestion);
			} else {
				if (
					playerIndex != null &&
					playerIndex == sr.suggester &&
					shown != null
				) {
					notePlayerHas(i, shown);
				} else {
					notePlayerCouldDisprove(i, sr.suggestion);
				}
			}
		}
		
		if (playerIndex != null && sr.suggester == playerIndex) {
			if (sr.disprover == null) {
				for (Card c : sr.suggestion.getCards()) {
					if (players[playerIndex].has(c) == PlayerHasCard.NO) {
						filterSolutions(true, c);
					}
				}
			} else if (shown != null) {
				filterSolutions(false, shown);
			}
		}
	}
	
	private void learnFromAbsentAccusation() throws Contradiction {
		if (history.size() > 0) {
			Record last = getLastRecord();
			if (last instanceof SuggestionRecord) {
				SuggestionRecord sr = (SuggestionRecord) last;
				int i = sr.suggester;
				if (
					(playerIndex == null || i != playerIndex) &&
					sr.disprover == null
				) {
					notePlayerCouldDisprove(i, sr.suggestion);
				}
			}
		}
	}
	
	private void noteForSolution(Card card, boolean partOfSolution)
	throws Contradiction {
		Set<Card> category = candidates.get(card.getClass());
		if (holders.containsKey(card)) {
			if (holders.get(card) == null ^ partOfSolution) {
				throw new Contradiction(
					"Somehow the reasoning determined that %s is %spart of " +
					"the solution when it was previously recorded that it is" +
					"%s.",
					card,
					partOfSolution ? "" : "not ",
					partOfSolution ? " not" : ""
				);
			}
			return;
		}
		
		if (partOfSolution) {
			holders.put(card, null);
			category.clear();
			category.add(card);
		} else {
			category.remove(card);
			if (category.size() == 1) {
				holders.put(category.iterator().next(), null);
			}
		}
		
		if (category.size() == 0) {
			throw new Contradiction(
				"There are no possible %ss left.",
				card.getClass().getSimpleName()
			);
		}
	}
	
	private void notePlayerCouldDisprove(int index, Suggestion suggestion)
	throws Contradiction {
		players[index] = players[index].ifHas(suggestion);
	}
	
	private void notePlayerCouldNotDisprove(int index, Suggestion suggestion)
	throws Contradiction {
		players[index] = players[index].ifHasNone(suggestion);
	}
	
	private void notePlayerHas(int index, Card shown) throws Contradiction {
		holders.put(shown, players[index]);
		for (int i = 0; i < players.length; ++i) {
			players[i] = (
				i == index ?
					players[i].ifHas(shown) :
					players[i].ifHasNone(shown)
			);
		}
	}
	
	private void refineHistory() {
		for (int i = 0; i < history.size(); ++i) {
			Record r = history.get(i);
			if (r instanceof SuggestionRecord) {
				SuggestionRecord sr = (SuggestionRecord) r;
				if (sr.disprover != null) {
					history.set(i, sr.refine(players[sr.disprover]));
				}
			}
		}
	}
	
	private void seedPossibleSolutions(Hand hand) {
		possibleSolutions = new HashSet<>(Suggestion.getSuggestions());
		if (hand != null) {
			Iterator<Suggestion> iterator = possibleSolutions.iterator();
			while (iterator.hasNext()) {
				if (hand.getDisproveCards(iterator.next()).size() > 0) {
					iterator.remove();
				}
			}
		}
	}
	
	private void validatePlayerCount(int playerCount)
	throws InvalidPlayerCount {
		if (playerCount < 3 || playerCount > 6) {
			throw new InvalidPlayerCount(playerCount);
		}
	}
	
	//***************** Protected and Private Static Fields ******************//
	private static final int THRESHOLD = 1_000_000_000;
	
	//**************** Protected and Private Static Interface ****************//
	private static int[] getHandSizes(int playerCount) {
		final int DEALT = 18;
		int[] handSizes = new int[playerCount];
		int baseSize = DEALT / playerCount;
		int remainder = DEALT % playerCount;
		for (int i = 0; i < playerCount; ++i) {
			handSizes[i] = baseSize + (remainder > i ? 1 : 0);
		}
		return handSizes;
	}
}
