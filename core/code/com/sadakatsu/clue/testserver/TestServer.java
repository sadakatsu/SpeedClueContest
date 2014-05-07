package com.sadakatsu.clue.testserver;

import java.io.File;
import java.net.ServerSocket;
import java.util.ArrayList;
import java.util.Map;

import com.sadakatsu.clue.contestserver.Match;
import com.sadakatsu.clue.contestserver.Player;
import com.sadakatsu.clue.exception.ClueException;
import com.sadakatsu.clue.exception.DisqualifiedPlayer;
import com.sadakatsu.clue.exception.DuplicateIdentifier;
import com.sadakatsu.clue.exception.InvalidPlayerCount;
import com.sadakatsu.clue.exception.TimeoutViolation;
import com.sadakatsu.util.EntryScript;

/**
 * The TestServer is designed to play a set of Players against each other in a
 * single match, logging the game's progress so a programmer can ensure that his
 * contest entry performs as he would expect.
 * 
 * @author Joseph A. Craig
 */
public class TestServer {
	//*********************** Public Static Interface ************************//
	/**
	 * Launches the AIs as specified by the "entryScript" and has them play each
	 * other in a single Speed Clue game, writing what happens at each game step
	 * (including any rule violations) to the standard output stream.
	 * 
	 * The intended syntax for usage is:
	 *   "java -jar testServer.jar entryScript"
	 *  
	 * @param args
	 * - entryScript: the path and filename for the launch script; see
	 *   com.sadakatsu.util.EntryScript for a description of such a file's
	 *   format.
	 * @throws Exception
	 */
	public static void main(String[] args) throws Exception {
		File entryScript = new File(args[0]);
		
		try (ServerSocket accept = new ServerSocket(0)) {
			accept.setSoTimeout(TimeoutViolation.TIMEOUT);
			
			Map<String, Player> started = EntryScript.process(
				entryScript,
				accept,
				true
			);
			
			boolean failed = false;
			for (Map.Entry<String, Player> attempt : started.entrySet()) {
				if (attempt.getValue() == null) {
					System.err.format(
						"Failed to start \"%s\".\n",
							attempt.getKey()
					);
					failed = true;
				}
			}
			
			if (!failed) {
				new Match(new ArrayList<>(started.values()), System.out);
			}
		} catch (
			DisqualifiedPlayer |
			DuplicateIdentifier |
			InvalidPlayerCount e
		) {
			e.printStackTrace();
		} catch (ClueException ce) {
			System.err.println(ce.getMessage());
		}
	}
}
