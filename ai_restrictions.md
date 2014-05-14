You may write your AI with whatever technology you wish and whatever algorithm
you wish so long as it obeys the following restrictions:

1. It must be easily runnable or compilable on Windows 7 x64.  I am currently
   prepared to compile C/C++ code using GCC 4.8.1 via MinGW-W64, Java 7, and
   Python 2.7.6.  If you choose to use a different language or technology, you
   must be prepared to at least point me in the right direction to get it
   working if I ask.
   
2. It must receive as command line arguments a string identifier and a server
   port number.
   
3. It must be use a TCP/IP connection to communicate with the server [using this
   application protocol][1].
   
4. Your AI must be designed to win games.  While your AI does not have to be
   perfect, and it is acceptable for it to take calculated risks, but it must
   not ever intentionally lose a game.  Enforced
   violations include:

   - Making an accusation that has a card it is holding.
   
   - Failing to make an accusation when it has (or should have) learned the
     solution, though this will only be enforced in situations when no player
     could disprove its suggestion.

5. It may never make any suggestion more than one time in a game.  While this is
   not forbidden in actual Speed Clue rules, allowing AIs to make the same
   suggestion more than once could result in games that never end.
   
6. It must take no more than 10 seconds for your AI to respond to any message
   from the server.  I intend to run thousands of games to determine the winner,
   so your code must be reasonably fast.
   
7. It may not use any technique that either interferes with other AIs or steals
   information that cannot be inferred from what the server sends directly to
   it.  Examples include, but are not limited to, claiming large blocks of
   memory for extended periods of time, continuously running a large number of
   threads, sniffing for packets communicated by opponents, or tampering with
   packets.  If you use a technique that does consume a lot of memory or needs
   many threads, limit the time such resources are used so other AIs get their
   access to the resources as well.
   
8. Any ideas or code you borrow from others must be noticeably cited.

I reserve the right to disqualify any entry that does not follow, or flaunts the
spirit of, these rules.

[1]: https://github.com/sadakatsu/SpeedClueContest/tree/master/protocol.md 
