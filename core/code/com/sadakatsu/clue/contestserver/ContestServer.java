package com.sadakatsu.clue.contestserver;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.sadakatsu.clue.cards.Hand;
import com.sadakatsu.clue.exception.ClueException;
import com.sadakatsu.clue.exception.DisqualifiedPlayer;
import com.sadakatsu.clue.exception.DuplicateIdentifier;
import com.sadakatsu.clue.exception.InvalidPlayerCount;
import com.sadakatsu.clue.exception.ProtocolViolation;
import com.sadakatsu.clue.exception.TimeoutViolation;
import com.sadakatsu.util.EntryScript;
import com.sadakatsu.util.Combinations;
import com.sadakatsu.util.Permutations;

/**
 * The ContestServer is the class that will be used to run the Speed Clue AI
 * contest.  It pits the entrants in matches of 3-6 (given enough players) with
 * all combinations of entrants and all permutations of entrants.  If any AI
 * violates the rules, it is disqualified and any previous matches in which it
 * played are invalidated.  All players' results are reported in the file
 * "playerOutcomes.txt" and all legal match's data are reported in the
 * "matchTranscripts.txt".
 * 
 * It receives the arguments "entrantsFile gamesPerPermutation".  The first
 * argument is the name of the file listing the launch commands for the contest
 * entries; see com.sadakatsu.util.EntryScript for a description of the format
 * for the file.  The second argument describes the number of games each match-
 * up of entrants will play.
 * 
 * The structure of the tournament is described in
 * "SpeedClueContest/tournament.md".
 * 
 * @author Joseph A. Craig
 *
 */
public class ContestServer {
	//********************* Protected and Private Fields *********************//
	private BufferedWriter matchTranscripts;
	private BufferedWriter playerOutcomes;
	private List<Match> matches;
	private Map<String, Player> players;
	
	//*************************** Public Interface ***************************//
	/**
	 * Runs the contest.
	 * @param agentScript
	 * The file that contains the launch commands for the contest entrants.
	 * @param gamesPerPermutation
	 * The number of games each permutation of players will play against each
	 * other.
	 * @throws IOException
	 * @throws InterruptedException
	 * @throws DuplicateIdentifier
	 * @throws DisqualifiedPlayer
	 * @throws InvalidPlayerCount
	 */
	public ContestServer(File agentScript, int gamesPerPermutation)
	throws
		IOException,
		InterruptedException,
		DuplicateIdentifier,
		DisqualifiedPlayer,
		InvalidPlayerCount
	{
		try (ServerSocket accept = new ServerSocket(0)) {
			accept.setSoTimeout(TimeoutViolation.TIMEOUT);
			matchTranscripts = new BufferedWriter(
				new FileWriter("matchTranscripts.txt")
			);
			playerOutcomes = new BufferedWriter(
				new FileWriter("playerOutcomes.txt")
			);
			matches = new ArrayList<>();
			
			players = EntryScript.process(agentScript, accept, false);
			
			List<Player> playing = new ArrayList<>();
			for (Player p : players.values()) {
				if (p != null && !p.isDisqualified()) {
					playing.add(p);
				}
			}
			
			final int MAX_COUNT = Math.min(6, players.size());
			for (int i = 3; i <= MAX_COUNT; ++i) {
				for (
					Collection<Player> inGame : Combinations.get(playing, i)
				) {
					if (!mayPlay(inGame)) {
						continue;
					}
					
					try {
						for (List<Player> order : Permutations.get(inGame)) {
							System.out.println(order);
							for (int j = 0; j < gamesPerPermutation; ++j) {
								System.out.format(" %d", j + 1);
								matches.add(new Match(order));
							}
							System.out.println();
						}
					} catch (ClueException e) {
						e.getOffender().disqualify(e);
						System.out.println();
					}
				}
			}
		} finally {
			cleanUp();
		}
	}
	
	//******************* Protected and Private Interface ********************//
	/**
	 * Determines whether all the players in the proposed combination are
	 * eligible to play.
	 * @param candidates
	 * The Players in question.
	 * @return
	 * true if none of the Players have been disqualified, false otherwise.
	 */
	private boolean mayPlay(Collection<Player> candidates) {
		boolean valid = true;
		for (Player p : candidates) {
			if (p.isDisqualified()) {
				valid = false;
				break;
			}
		}
		return valid;
	}

	/**
	 * Ensures that all clean up code is performed regardless of any error
	 * states that may exist, and logs the tournament results as far as is
	 * possible.
	 */
	private void cleanUp() {
		if (players != null) {
			for (Player p : players.values()) {
				try {
					if (p != null) {
						p.done();
					}
				} catch (ProtocolViolation | TimeoutViolation e) {
					p.disqualify(e);
				} catch (SocketException se) {
					// blank
				} catch (IOException ioe) {
					ioe.printStackTrace();
				}
			}
			
			if (matchTranscripts != null && playerOutcomes != null) {
				Map<Player, Integer> played = new HashMap<>();
				Map<Player, Integer> won = new HashMap<>();
				for (Player p : players.values()) {
					if (p != null && !p.isDisqualified()) {
						played.put(p, 0);
						won.put(p, 0);
					}
				}
				
				for (Match m : matches) {
					if (!m.hasDisqualifiedPlayer()) {
						Player winner = m.getWinner();
						int wins = won.get(winner) + 1;
						won.put(winner, wins);
						for (Player p : m.getPlayers()) {
							int games = played.get(p) + 1;
							played.put(p, games);
						}
						try {
							recordMatch(m);
						} catch (IOException ioe) {
							System.out.format(
								"Error recording match:\n%s\n%s\n",
									ioe.getMessage(),
									ioe.getStackTrace()
							);
						}
					}
				}
				
				for (Map.Entry<String, Player> entry : players.entrySet()) {
					String identifier = entry.getKey();
					Player player = entry.getValue();
					
					try {
						playerOutcomes.write(identifier);
						playerOutcomes.write(",");
						
						if (player == null) {
							playerOutcomes.write(
								"disqualified - failed to start"
							);
						} else if (player.isDisqualified()) {
							playerOutcomes.write("disqualified - ");
							playerOutcomes.write(
								player.getViolation().getMessage()
							);
						} else {
							playerOutcomes.write(
								String.format(
									"%d,%d",
										won.get(player),
										played.get(player)
								)
							);
						}
						
						playerOutcomes.write("\n");
						playerOutcomes.flush();
					} catch (IOException ioe) {
						System.out.format(
							"Error recording result: ",
								ioe.getMessage(),
								ioe.getStackTrace()
						);
					}
				}
			}
		}
		
		if (matchTranscripts != null) {
			try {
				matchTranscripts.close();
			} catch (IOException ioe) {
				ioe.printStackTrace();
			}
		}
		
		if (playerOutcomes != null) {
			try {
				playerOutcomes.close();
			} catch (IOException ioe) {
				ioe.printStackTrace();
			}
		}
	}
	
	/**
	 * Writes data about a match to "matchTranscripts.txt".  The data written is
	 * a comma-separated line with the following data in the following order:
	 * - number of players
	 * - position in play order of winner
	 * - round when game was won
	 * - for each player by ascending order of play position:
	 * -- player identifier
	 * -- number of suspects dealt
	 * -- number of weapons dealt
	 * -- number of rooms dealt
	 * @param match
	 * The Match to be recorded.
	 * @throws IOException
	 */
	private void recordMatch(Match match) throws IOException {
		int playerCount = match.getPlayerCount();
		List<Hand> hands = match.getHands();
		List<Player> players = match.getPlayers();
		
		StringBuilder sb = new StringBuilder(
			String.format(
				"%d,%d,%d",
					playerCount,
					match.getPlayers().indexOf(match.getWinner()),
					match.getRounds()
			)
		);
		
		for (int i = 0; i < playerCount; ++i) {
			Hand h = hands.get(i);
			Player p = players.get(i);
			sb.append(
				String.format(
					",%s,%s",
						p.getIndentifier(),
						h.getDistributionString()
				)
			);
		}
		
		sb.append("\n");
		
		matchTranscripts.write(sb.toString());
		matchTranscripts.flush();
	}
	
	//*********************** Public Static Interface ************************//
	/**
	 * Runs the contest.
	 * @param args
	 * The first should be the name of the entrant launch script.  The second
	 * should be the number of games each player permutation will play.
	 * @throws NumberFormatException
	 * @throws DisqualifiedPlayer
	 * @throws IOException
	 * @throws InterruptedException
	 * @throws DuplicateIdentifier
	 * @throws InvalidPlayerCount
	 */
	public static void main(String[] args)
	throws
		NumberFormatException,
		DisqualifiedPlayer,
		IOException,
		InterruptedException,
		DuplicateIdentifier,
		InvalidPlayerCount
	{
		new ContestServer(new File(args[0]), Integer.parseInt(args[1]));
	}
}
