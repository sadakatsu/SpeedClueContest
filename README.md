SpeedClueContest
================
This is a repository that I have set up for the [Speed Clue AI contest](http://codegolf.stackexchange.com/questions/25793/king-of-the-hill-speed-clue-ai).
The gist of this competition is that I have challenged other programmers to
write a program that can play a boardless version of Clue/Cluedo for 3-6
players.  The programmer who writes the AI with the highest winning percentage
across all games played wins the competition and gains reputation on the
[Programming Challenges & Code Golf Stack Exchange](http://codegolf.stackexchange.com).

All of my (Joseph A. Craig / sadakatsu) contributions to this repository (such
as the test server and the random player AI) are covered by the MIT license.
Anyone who submits an entry to the competition may use whatever license they
wish to cover their entries, unless that contributor believes his submission to
be a substantial modification of my code.

The highlights of this repository are as follow.

+ ./README.md : `this`;
+ ./core/ : My contributions to get contestants started.
 + ./core/code/ : The Java 7 code for the test server and random player.
 + ./core/randomPlayer.jar : An executable JAR that allows contestants to test
their entries.  The syntax is `java -jar randomPlayer.jar agentLaunchFile`.
 + ./core/testServer.jar : An executable JAR to use as a straw man AI.  The test
server can run it using the syntax `java -jar randomPlayer.jar identifier
serverPort`.
 + ./core/LICENSE : A copy of the MIT License that recursively covers all the
contents of the core directory.
 + ./core/agentLaunch.txt : An example `agentLaunchFile` for the test server to
use.  Each line should be the command to run an agent program.  The text `%%`
must be used as a placeholder for the server port number, and the identifier
must be surrounded by curly braces (`{}`) for the server to be able to use these
values.
+ ./entries/ : My hope is that entrants will add their entries in this
directory.

Overall, my aim is that everyone has fun!