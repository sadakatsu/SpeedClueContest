package com.sadakatsu.clue.contestserver;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.sadakatsu.clue.cards.Card;
import com.sadakatsu.clue.cards.Hand;
import com.sadakatsu.clue.cards.Suggestion;
import com.sadakatsu.clue.exception.DisqualifiedPlayer;
import com.sadakatsu.clue.exception.DuplicateSuggestion;
import com.sadakatsu.clue.exception.InvalidDisprove;
import com.sadakatsu.clue.exception.InvalidPlayerCount;
import com.sadakatsu.clue.exception.MissedAccusation;
import com.sadakatsu.clue.exception.ProtocolViolation;
import com.sadakatsu.clue.exception.SuicidalAccusation;
import com.sadakatsu.clue.exception.TimeoutViolation;

/**
 * The Match class runs a single Speed Clue game among the passed Players and
 * saves data about the game played in such a way that it can be treated
 * conceptually like a database record.
 * 
 * @author Joseph A. Craig
 */
public class Match {
	//********************* Protected and Private Fields *********************//
	private BufferedWriter log;
	private Card shown;
	private int activeIndex;
	private int playersInGame;
	private int rounds;
	private Integer disproverIndex;
	private List<Hand> hands;
	private List<Player> players;
	private Player activePlayer;
	private Player winner;
	private Suggestion last;
	private Suggestion solution;
	
	//*************************** Public Interface ***************************//
	/**
	 * Runs a Speed Clue game and stores the results.  This constructor does not
	 * log the game play.
	 * @param players
	 * The Players participating in the Match, listed in play order.
	 * @throws ProtocolViolation
	 * @throws TimeoutViolation
	 * @throws IOException
	 * @throws DisqualifiedPlayer
	 * @throws DuplicateSuggestion
	 * @throws InvalidDisprove
	 * @throws InvalidPlayerCount
	 * @throws SuicidalAccusation
	 * @throws MissedAccusation
	 */
	public Match(List<Player> players)
	throws
		ProtocolViolation,
		TimeoutViolation,
		IOException,
		DisqualifiedPlayer,
		DuplicateSuggestion,
		InvalidDisprove,
		InvalidPlayerCount,
		SuicidalAccusation,
		MissedAccusation
	{
		log = null;
		init(players);
	}
	
	/**
	 * Runs a Speed Clue game, writing log information to the passed
	 * OutputStream.  The game's results are stored for later querying.
	 * @param players
	 * The Players participating in the Match, listed in play order.
	 * @param log
	 * The OutputStream to which to write the Match log.  If null, the game
	 * play will not be logged (as if Match(players) had been called).
	 * @throws ProtocolViolation
	 * @throws TimeoutViolation
	 * @throws IOException
	 * @throws DisqualifiedPlayer
	 * @throws DuplicateSuggestion
	 * @throws InvalidDisprove
	 * @throws InvalidPlayerCount
	 * @throws SuicidalAccusation
	 * @throws MissedAccusation
	 */
	public Match(
		List<Player> players,
		OutputStream log
	) throws
		ProtocolViolation,
		TimeoutViolation,
		IOException,
		DisqualifiedPlayer,
		DuplicateSuggestion,
		InvalidDisprove,
		InvalidPlayerCount,
		SuicidalAccusation,
		MissedAccusation
	{
		if (log != null) {
			this.log = new BufferedWriter(new PrintWriter(log));
		} else {
			this.log = null;
		}
		init(players);
	}
	
	/**
	 * Determines whether any of the Players who competed in this Match were
	 * later disqualified, invalidating this Match's results.
	 * @return
	 * true if any of the Players were disqualified, false otherwise.
	 */
	public boolean hasDisqualifiedPlayer() {
		boolean has = false;
		
		for (Player p : players) {
			if ((has = p.isDisqualified())) {
				break;
			}
		}
		
		return has;
	}
	
	/**
	 * @return
	 * The number of Players who participated in the match.
	 */
	public int getPlayerCount() {
		return players.size();
	}
	
	/**
	 * @return
	 * The number of rounds this game lasted.
	 */
	public int getRounds() {
		return rounds;
	}
	
	/**
	 * @return
	 * An unmodifiable list of the Hands dealt to each of the Players in
	 * ascending play order. 
	 */
	public List<Hand> getHands() {
		return Collections.unmodifiableList(hands);
	}
	
	/**
	 * @return
	 * An unmodifiable list of the Players who participated in the game.
	 */
	public List<Player> getPlayers() {
		return players;
	}
	
	/**
	 * @return
	 * The Player that won the game.
	 */
	public Player getWinner() {
		return winner;
	}
	
	//******************* Protected and Private Interface ********************//
	/**
	 * @return
	 * true if there is no winner, false otherwise.
	 */
	private boolean gameNotOver() {
		return winner == null;
	}
	
	/**
	 * @param playerIndex
	 * A valid play order index.
	 * @return
	 * The next index in the play order.
	 */
	private int getPlayerAfter(int playerIndex) {
		return (playerIndex + 1) % players.size();
	}
	
	/**
	 * @param solution
	 * The chosen solution.
	 * @return
	 * A List of all the Clue Cards except for those in the solution.
	 */
	private List<Card> getDeck() {
		List<Card> deck = Card.getCards();
		deck.removeAll(solution.getCards());
		return deck;
	}	
		
	/**
	 * Builds random Hands to be assigned to the Players and stores them in the
	 * "hands" field in ascending play order.
	 */
	private void buildHands() {
		hands = new ArrayList<>();
		
		List<Card> deck = getDeck();
		Collections.shuffle(deck);
		
		int cardCount = deck.size();
		int playerCount = players.size();
		int minHandSize = cardCount / playerCount;
		int remainder = cardCount % playerCount;
		
		for (int i = 0, dealt = 0; i < playerCount; ++i) {
			int handSize = minHandSize + (i < remainder ? 1 : 0);
			hands.add(new Hand(deck.subList(dealt, dealt + handSize)));
			dealt += handSize;
		}
	}
	
	/**
	 * Chooses a random solution for the game, logging it if this Match is
	 * logging.
	 * @throws IOException
	 */
	private void chooseSolution() throws IOException {
		List<Suggestion> solutions = Suggestion.getSuggestions();
		Collections.shuffle(solutions);
		solution = solutions.get(0);
		
		writeToLog("The solution is %s.\n", solution);
	}
	
	/**
	 * Builds Hands for the Players and informs each of them that the game is
	 * starting.
	 * @throws ProtocolViolation
	 * @throws TimeoutViolation
	 * @throws IOException
	 */
	private void dealCards()
	throws ProtocolViolation, TimeoutViolation, IOException {
		buildHands();
		resetPlayers();
	}
	
	/**
	 * Determines which Player, if any, is the first that can disprove the
	 * current Suggestion (stored in the "last" field) made by "activePlayer".
	 * The play order index for this Player is stored in the "disproverIndex"
	 * field should there be a possible disprover; otherwise, it will hold null.
	 * @throws IOException
	 */
	private void determineDisprover() {
		disproverIndex = null;
		
		for (
			int i = getPlayerAfter(activeIndex);
			disproverIndex == null && i != activeIndex;
			i = getPlayerAfter(i)
		) {
			if (players.get(i).canDisprove(last)) {
				disproverIndex = i;
			}
		}
	}
	
	/**
	 * Determines which Player can disprove the current Suggestion (stored in
	 * the "last" field) made by the "activePlayer", asks that Player to select
	 * a Card to disprove the Suggestion, and stores that Card in the "shown"
	 * field.  If this Match is logging, these outcomes are written to the log.
	 * @throws ProtocolViolation
	 * @throws InvalidDisprove
	 * @throws TimeoutViolation
	 * @throws IOException
	 */
	private void disproveSuggestion()
	throws ProtocolViolation, InvalidDisprove, TimeoutViolation, IOException {
		determineDisprover();
		if (disproverIndex != null) {
			Player disprover = players.get(disproverIndex);
			writeToLog("%s can disprove.\n", disprover);
			
			getDisprovingCard();
			
			writeToLog("%s shows %s.\n", disprover, shown);
		} else {
			shown = null;
			writeToLog("No player can disprove!\n");
		}
	}
	
	/**
	 * Asks the Player in play position "disproverIndex" to disprove the
	 * "last" Suggestion, then stores the Card it chooses in the "shown" field. 
	 * @throws ProtocolViolation
	 * @throws InvalidDisprove
	 * @throws TimeoutViolation
	 * @throws IOException
	 */
	private void getDisprovingCard()
	throws ProtocolViolation, InvalidDisprove, TimeoutViolation, IOException {
		shown = players.get(disproverIndex).disprove(activeIndex, last);
	}
	
	/**
	 * Asks the "activePlayer" to select a Suggestion, then stores that
	 * Suggestion in the "last" field.  If this Match is logging, this method
	 * logs the chosen Suggestion.
	 * @throws ProtocolViolation
	 * @throws DuplicateSuggestion
	 * @throws TimeoutViolation
	 * @throws IOException
	 */
	private void getSuggestion()
	throws
		ProtocolViolation,
		DuplicateSuggestion,
		TimeoutViolation,
		IOException
	{
		last = activePlayer.suggest();
		
		writeToLog("%s suggests %s.\n", activePlayer, last);
	}
	
	/**
	 * Asks the "activePlayer" to make an accusation.  If the player does, it is
	 * compared against the solution: a correct accusation makes "activePlayer"
	 * the winner, and an incorrect accusation makes "activePlayer" lose.  If
	 * this leaves only one Player still in the game, that Player wins by
	 * default.  Any accusation the Player makes and its result is reported to
	 * all Players.  If this Match is logging, then the outcome of this step is
	 * logged.
	 * 
	 * Note that the accusation the Player makes is stored in the "last" field,
	 * so the previous Suggestion is lost.  However, it should no longer be
	 * needed at this point in the turn.
	 * @throws ProtocolViolation
	 * @throws TimeoutViolation
	 * @throws IOException
	 * @throws SuicidalAccusation
	 * @throws MissedAccusation
	 */
	private void handleAccusation()
	throws
		ProtocolViolation,
		TimeoutViolation,
		IOException,
		SuicidalAccusation,
		MissedAccusation
	{
		last = activePlayer.accuse();
		if (last != null) {
			boolean correct = last.equals(solution);
			if (correct) {
				winner = activePlayer;
			} else {
				activePlayer.lose();
				--playersInGame;
			}
			
			writeToLog(
				"%s accuses %s.\nIt is %s\n",
					activePlayer,
					last,
					(correct ? "correct!" : "incorrect.")
			);
			
			reportAccusation(correct);
			
			if (playersInGame == 1) {
				startNextPlayerTurn();
				winner = activePlayer;
			}
		} else {
			writeToLog("%s makes no accusation.\n", activePlayer);
		}
	}
	
	
	/**
	 * This method validates the List of Players passed in to play the game.  If
	 * there are no problems, it then chooses a solution, deals Hands to each of
	 * the Players, and then starts the turns with the first Player in the List.
	 * If this Match is logging, most of these steps will be logged.
	 * @param players
	 * The Players to play this game.
	 * @throws DisqualifiedPlayer
	 * @throws InvalidPlayerCount
	 * @throws ProtocolViolation
	 * @throws TimeoutViolation
	 * @throws IOException
	 * @throws DuplicateSuggestion
	 * @throws InvalidDisprove
	 * @throws SuicidalAccusation
	 * @throws MissedAccusation
	 */
	private void init(List<Player> players)
	throws
		DisqualifiedPlayer,
		InvalidPlayerCount,
		ProtocolViolation,
		TimeoutViolation,
		IOException,
		DuplicateSuggestion,
		InvalidDisprove,
		SuicidalAccusation,
		MissedAccusation
	{
		processPlayers(players);
		chooseSolution();
		dealCards();
		runGame();
	}
	
	/**
	 * If this Match is logging, this method writes the last output for the log:
	 * who won and in which round.
	 * @throws IOException
	 */
	private void logFinal() throws IOException {
		if (log == null) {
			return;
		}
		
		writeToLog(SEPARATOR);
		writeToLog("The winner is %s after %d rounds.\n", winner, rounds);
	}
	
	/**
	 * If this Match is logging, this method writes the Players who are in this
	 * game.
	 * @throws IOException
	 */
	private void logPlayers() throws IOException {
		if (log == null) {
			return;
		}
		
		writeToLog("This game's players are:\n");
		for (Player p : this.players) {
			writeToLog("  %s\n", p);
		}
		writeToLog("\n");
	}
	
	/**
	 * If this Match is logging, this method writes a header to make it easier
	 * for a reader to determine where in the game the current turn falls.
	 * @throws IOException
	 */
	private void logTurnHeader() throws IOException {
		if (log == null) {
			return;
		}
		
		writeToLog(SEPARATOR);
		writeToLog("Round %d, active: %s\n\n", rounds, activePlayer);
	}
	
	/**
	 * Informs all Players about the "last" accusation, who made it, and whether
	 * the accusation is correct.
	 * @param correct
	 * @throws ProtocolViolation
	 * @throws TimeoutViolation
	 * @throws IOException
	 */
	private void reportAccusation(boolean correct)
	throws ProtocolViolation, TimeoutViolation, IOException {
		for (Player p : players) {
			p.accusation(activeIndex, last, correct);
		}
	}
	
	/**
	 * Informs all Players about the "last" Suggestion, who made it, who
	 * disproved it (if anyone), and which Card was used to disprove it.  The
	 * Player class handles ensuring that no Player sees information to which it
	 * should not have access.
	 * @throws ProtocolViolation
	 * @throws TimeoutViolation
	 * @throws IOException
	 */
	private void reportSuggestion()
	throws ProtocolViolation, TimeoutViolation, IOException {
		for (Player p : players) {
			p.suggestion(
				activeIndex,
				last,
				disproverIndex,
				shown
			);
		}
	}
	
	/**
	 * Informs each Player that it is starting a new game, sending it all
	 * pertinent information.  If this Match is logging, each Player's Hand will
	 * be written to the log after each Player confirms it has reset.
	 * @throws ProtocolViolation
	 * @throws TimeoutViolation
	 * @throws IOException
	 */
	private void resetPlayers()
	throws ProtocolViolation, TimeoutViolation, IOException {
		for (int i = 0; i < players.size(); ++i) {
			Hand h = hands.get(i);
			Player p = players.get(i);
			writeToLog("%s is dealt %s.\n", p, h);
			p.reset(players.size(), i, h);
		}
	}
	
	/**
	 * Runs through each round of the game until a Player wins.  If this Match
	 * is logging, the turns and their step results will be logged.
	 * @throws ProtocolViolation
	 * @throws DuplicateSuggestion
	 * @throws TimeoutViolation
	 * @throws IOException
	 * @throws InvalidDisprove
	 * @throws SuicidalAccusation
	 * @throws MissedAccusation
	 */
	private void runGame()
	throws
		ProtocolViolation,
		DuplicateSuggestion,
		TimeoutViolation,
		IOException,
		InvalidDisprove,
		SuicidalAccusation,
		MissedAccusation
	{
		try {
			startGame();
			do {
				logTurnHeader();
				getSuggestion();
				disproveSuggestion();
				reportSuggestion();
				handleAccusation();
				startNextPlayerTurn();
			} while (gameNotOver());
			logFinal();
		} finally {
			releasePlayers();
		}
	}
	
	/**
	 * Validates that there are the correct number of Players and that they are
	 * all allowed to play (none disqualified) before storing them in the
	 * "players" field.
	 * @param players
	 * @throws DisqualifiedPlayer
	 * @throws InvalidPlayerCount
	 * @throws IOException
	 */
	private void processPlayers(List<Player> players)
	throws DisqualifiedPlayer, InvalidPlayerCount, IOException {
		int count = players.size();
		if (count < 3 || count > 6) {
			throw new InvalidPlayerCount(count);
		}
		
		for (Player p : players) {
			if (p.isDisqualified()) {
				throw new DisqualifiedPlayer(p);
			}
		}
		
		this.players = Collections.unmodifiableList(players);
		logPlayers();
	}
	
	/**
	 * Informs each Player that the game has ended.
	 */
	private void releasePlayers() {
		for (Player p : players) {
			p.endGame();
		}
	}
	
	/**
	 * Ensures that all the method variables are in the correct state for the
	 * first round to start at Player 0's turn.
	 */
	private void startGame() {
		activeIndex = 0;
		activePlayer = players.get(0);
		playersInGame = players.size();
		rounds = 1;
	}
	
	/**
	 * Sets "activeIndex" to the index of the next Player to get a turn and
	 * "activePlayer" to that Player.  If this wraps beyond the end of the play
	 * order when the game is not over, this also increments the "rounds"
	 * counter.
	 */
	private void startNextPlayerTurn() {
		boolean wrapped = false;
		int first = activeIndex;
		Player current;
		
		do {
			int before = activeIndex;
			activeIndex = getPlayerAfter(activeIndex);
			
			if (before > activeIndex) {
				wrapped = true;
			}
			
			current = players.get(activeIndex);
		} while (current.isEliminated());
		
		activePlayer = current;
		
		if (
			winner == null &&
			playersInGame > 1 &&
			wrapped &&
			first != activeIndex
		) {
			++rounds;
		}
	}
	
	/**
	 * If this Match is logging, this method writes the String represented by
	 * "format" and "args" to the log and then flushes the buffer.
	 * @param format
	 * @param args
	 * @throws IOException
	 */
	private void writeToLog(String format, Object...args) throws IOException {
		if (log == null) {
			return;
		}
		
		String message = String.format(format, args);
		log.write(message);
		log.flush();
	}
	
	//***************** Protected and Private Static Fields ******************//
	private static final int SEPARATOR_LENGTH = 79;
	private static final String SEPARATOR = buildSeparator();
	
	//**************** Protected and Private Static Interface ****************//
	/**
	 * Builds a String to act as a separator between turn logs.
	 * @return
	 */
	private static String buildSeparator() {
		StringBuilder sb = new StringBuilder("\n");
		for (int i = 0; i < SEPARATOR_LENGTH; ++i) {
			sb.append("=");
		}
		sb.append("\n");
		return sb.toString();
	}
}