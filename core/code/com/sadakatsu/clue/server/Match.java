package com.sadakatsu.clue.server;

import java.util.ArrayList;
import java.util.List;

public class Match {
	//********************* Protected and Private Fields *********************//
	private final List<String> playerIdentifiers;
	private final String representation;
	private final String winner;
	
	//*************************** Public Interface ***************************//
	public Match(List<Player> players) {
		playerIdentifiers = new ArrayList<>();
		winner = runMatch(players);
		representation = getRepresentation();
	}
	
	public int getPlayerCount() {
		return playerIdentifiers.size();
	}
	
	public String getPlayer(int playPosition) {
		if (playPosition < 0 || playPosition >= getPlayerCount()) {
			throw new IllegalArgumentException(
				String.format(
					"Passed playPosition %d, should be in the range [0,%d]",
					playPosition,
					getPlayerCount() - 1
				)
			);
		}
		
		return playerIdentifiers.get(playPosition);
	}
	
	public String getWinner() {
		return winner;
	}
	
	@Override
	public String toString() {
		return representation;
	}
	
	//******************* Protected and Private Interface ********************//
	private String getRepresentation() {
		StringBuilder sb = new StringBuilder();
		sb.append(getPlayerCount());
		sb.append(quote(winner));
		for (String s : playerIdentifiers) {
			sb.append(" ");
			sb.append(quote(s));
		}
		return sb.toString();
	}
	
	private String runMatch(List<Player> players) {
		return null;
	}
	
	private String quote(String s) {
		return String.format("\"%s\"", s);
	}
}