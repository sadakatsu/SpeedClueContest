## The Speed Clue Tournament ##

The structure of the tournament will be a comprehensive match-up of all entrants
against each other.  Which match-ups, and how many games for each match up, is
described by the following pseudocode:

	for playerCount in range[3..min(6, number of entries)]:
	  for each combination of playerCount AIs from all entries:
	    for each permutation in the current combination:
	      play 100 matches

This will fairly give every entry any potential benefits that may arise due to
position in play order and number of cards while reducing the odds that a player
will be eliminated due to poor luck in a couple games.

If there are more than six people who submit entries for the contest, I will
only use one entry per person.  This will decrease the chances of someone
winning the contest by having more entries than other people.  If too few people
enter the contest, I will use all the entries I receive to try to make the
competition interesting.

The winner will be the entry that wins the most games.  If there are entries
that cannot be shown to have statistically different performance from the
winner, these will be listed as honorable mentions.  If there are entries that
did not win but performed statistically significantly better than the others in
a given player Count / play position combination, they too will be awarded
honorable mentions.

In addition to the above, I intend to calculate and report statistics from the
tournament results about the game of Clue:

- Does one's position in the play order matter?

- Do some hand "shapes" `(x suspects, y weapons, z rooms)` give distinct
  advantages?

- Is the final round number stable enough to guess whether a player will get
  another turn?