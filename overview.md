## Speed Clue ##

[Cluedo/Clue][1] is a classic board game with a compelling deduction gameplay
component.  Speed Clue is a 3-6 player variant that emphasizes this component by
using only the cards.  The result is that the only difference between standard
Cluedo and Speed Clue is that each player still in the game may make any
suggestion he pleases on his turn instead of waiting to reach a specific room at
the mercy of dice rolls and other players' suggestions.  If you have never
played Cluedo before, or want to be certain of the explicit differences between
the two versions, you may find a [complete Speed Clue rule set here][2].

---

## Goal ##

Write and submit an AI program to play Speed Clue before 15 May 2014 00:00 GMT.
After that time, I will run a [tournament][3] using all the legal entries.  The
entrant whose AI wins the most games in the tournament wins the challenge.

---

## AI Specifications ##

You can write your AI in pretty much any language you choose, using whatever
techniques you use, so long as it strictly uses [the application protocol][4]
over a TCP/IP connection to play games with the server.  A detailed explanation
of all the restrictions can be found [here][11].

---

## How to Play ##

Start by forking the [contest GitHub repository][5].  Add a directory under the
[`entries`][9] directory named using your StackExchange user name, and develop
your code in that folder.  When you are ready to submit your entry, make a pull
request with your revisions, then follow [these instructions][10] for announcing
your entry on this site.

I have provided some code and JARs in the [`core`][6] directory for getting you
started; see [my site][7] for a rough guide for the materials.  In addition,
other players are submitting helper code in addition to their entries to help
you get up and running.  Take some time to explore the entries, and don't forget
to test your entry against others' entries before submitting!

---

## Have Fun! ##

[1]: http://en.wikipedia.org/wiki/Cluedo
[2]: https://github.com/sadakatsu/SpeedClueContest/tree/master/speed_clue_rules.md
[3]: https://github.com/sadakatsu/SpeedClueContest/tree/master/tournament.md
[4]: https://github.com/sadakatsu/SpeedClueContest/tree/master/protocol.md
[5]: https://github.com/sadakatsu/SpeedClueContest
[6]: https://github.com/sadakatsu/SpeedClueContest/tree/master/core
[7]: http://sadakatsu.com
[8]: https://github.com/sadakatsu/SpeedClueContest/tree/master/python-port
[9]: https://github.com/sadakatsu/SpeedClueContest/tree/master/entries
[10]: https://github.com/sadakatsu/SpeedClueContest/tree/master/submission.md
[11]: https://github.com/sadakatsu/SpeedClueContest/tree/master/ai_restrictions.md