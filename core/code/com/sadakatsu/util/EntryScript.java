package com.sadakatsu.util;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.sadakatsu.clue.contestserver.Player;
import com.sadakatsu.clue.exception.DuplicateIdentifier;
import com.sadakatsu.clue.exception.ProtocolViolation;
import com.sadakatsu.clue.exception.TimeoutViolation;

public class EntryScript {
	public static Map<String, Player> process(
		File filename,
		ServerSocket socket,
		boolean saveProcessOutput
	)
	throws FileNotFoundException, IOException, DuplicateIdentifier {
		int portNumber = socket.getLocalPort(); 
		Map<String, Player> players = new HashMap<>();
		String portString = String.valueOf(portNumber);
		
		try (
			BufferedReader br = new BufferedReader(new FileReader(filename))
		) {
			File outputSwallowFile = null;
			
			if (!saveProcessOutput) {
				outputSwallowFile = File.createTempFile("dump", null);
			}
			
			String line;
			while ((line = br.readLine()) != null) {
				Matcher matcher = IDENTIFIER.matcher(line);
				if (!matcher.find()) {
					throw new IllegalStateException(
						"The identifier must be wrapped with curly braces."
					);
				}
				
				String identifier = matcher.group(1);
				if (players.containsKey(identifier)) {
					throw new DuplicateIdentifier(identifier);
				}
				
				line = matcher.replaceAll(identifier);
				line = line.replaceAll("%%", portString);
				
				ProcessBuilder pb = new ProcessBuilder(line.split(" "));
				if (saveProcessOutput) {
					File f = new File(
						String.format("output_%s.txt", identifier)
					);
					pb.redirectError(f);
					pb.redirectOutput(f);
				} else {
					pb.redirectError(outputSwallowFile);
					pb.redirectOutput(outputSwallowFile);
				}
				pb.start();
				
				try {
					Socket s = socket.accept();
					players.put(identifier, new Player(identifier, s));
				} catch (
					ProtocolViolation |
					TimeoutViolation  |
					SocketTimeoutException e
				) {
					players.put(identifier, null);
				}
			}
		}
		
		return players;
	}
	
	private static final Pattern IDENTIFIER = Pattern.compile("\\{([^}]+)\\}");
}
