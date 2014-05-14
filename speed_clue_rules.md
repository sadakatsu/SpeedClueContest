# Speed Clue #

Speed Clue is a variation of the classic board game Cluedo/Clue.  Three to six
players race to determine who murdered Mr. Boddy with what weapon in which room.
Whoever determines this solution first wins.

---

## Game Elements ##

There are 21 cards in the game that serve as evidence.  These cards fall into
three categories: Suspect, Weapon, and Room.  The full list of cards in each
category (as well as their abbreviations in the contest's application protocol)
can be found in the file [card_abbreviations.md][1].

Optional but recommended is one sheet of paper per player.  Each player can
record notes about what he learns over the course of the game.  

---

## Game Setup ##

1. The first step is to randomly select the solution for the game.  One card
   from each of the categories is drawn secretly and set aside as the solution.
   No player may look at the solution cards until he makes an accusation.  The
   rest of the cards are shuffled into a single deck.

2. The next step is to determine a play order for the players.  What mechanism
   is used to make this determine the play order does not matter.  The end
   result is a linear ordering of the players.  The first player will be indexed
   as "0" in the game protocol, and each player after that is assigned a
   sequential index up to "n - 1".  The play order loops, so the "0" player
   comes after the "n-1" play order.

3. The cards are then dealt to each of the players in turn, one at a time, until
   all remaining 18 cards have been dealt to the players.  The number of cards
   each player is dealt can be determined from the table below.


		Count \ Index | 0 | 1 | 2 | 3 | 4 | 5
		--------------+---+---+---+---+---+---
					3 | 6 | 6 | 6 | - | - | -
					4 | 5 | 5 | 4 | 4 | - | -
					5 | 4 | 4 | 4 | 3 | 3 | -
					6 | 3 | 3 | 3 | 3 | 3 | 3

   These values can be determined programmatically using the formula
   
		handSize(c,i) = floor(18 / c) + (18 % c > i)
   
   where `c` is the player count and `i` is the player's position.

4. The players look at the hand of cards dealt to them.  They must not show
   these cards to the other players except when specifically required by the
   rules.

5. The players may then record any information legally available to them as they
   see fit.  Once all players have performed their preparations, the first game
   turn starts with player "0" as the active player.
 
---
 
## Game Play ##
 
The game progresses as described below.  Each of these portions are described in
their own subsections.
 
    while no one has won:
	  active player announces a suggestion
	  all other players disprove the suggestion
	    if the active player is ready to make an accusation:
	      the active player announces the accusation
	      the active player checks the solution
	      if the active player's accusation is the solution:
	        the active player wins
	      else:
	        the active player loses
	        if only one player has not lost:
	          that player wins
	  if no player has won:
	    the next player in the play order who has not lost becomes the active player

While playing, all players may take notes of whatever information to which they
legally have access as they wish.  Because of this, play must pause at any point
if a player needs time to record, process, or correct his notes.

If a player needs assistance in fact-checking any information to which he is
entitled, other players must assist as far as they are allowed.  All players can
help with ensuring he has recorded suggestions or accusations properly.  If
another player showed him a card or was shown a card by him, that player must
secretly tell him what was shown.

### Suggestion ###

The active player has to make a suggestion when his turn begins.  A suggestion
is a query for evidence against a possible solution.  Like the solution, a
suggestion has one suspect, one weapon, and one room.  The active player may use
his notes to carefully choose whatever suggestion he wishes.  There is no
prohibition against repeating another player's suggestion.  It is even legal to
repeat one's own suggestion (although doing so is almost never helpful, and is
explicitly forbidden in the contest).

Once the active player has chosen his suggestion, he announces his suggestion
aloud so every player can hear it.

### Disproving the Suggestion ###

The disproving stage starts with the player after the active player in the play
order.  It continues until either a player has disproved the suggestion or none
of the players other than the active player can disprove the suggestion.

When it is a player's turn to disprove a suggestion, he looks at the cards he
was dealt to determine whether he holds any of the cards announced in the
suggestion.  There are two possible outcomes:

- The player does not hold any of the cards from the suggestion. He announces
  aloud so all players can hear that he cannot disprove the suggestion.  Then
  the next player in the turn order has his turn to disprove the suggestion
  unless the next player is the active player.

- The player holds one or more of the cards from the suggestion.  He announces
  aloud that he can disprove the suggestion, then chooses one of the cards from
  the suggestion that he holds to secretly show to the active player.

Note that the active player never announces whether he can disprove his own
suggestion.  Note also that there are no rules specifying which card is shown
to disprove a suggestion other than the disproving player must show a card that
was in the suggestion.  The same card may be used to disprove multiple
suggestions over the course of the game.

### Accusation ###

If the active player is ready to try to win the game, he may make an accusation.
He announces that he is making an accusation, and then he announces what he
thinks the solution is.  He then secretly checks the solution.  If he is
correct, he shows everyone the solution to prove that he won.  If he is
incorrect, he sets the solution aside again and announces that he is incorrect.

[1]: https://github.com/sadakatsu/SpeedClueContest/tree/master/card_abbreviations.md