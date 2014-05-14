# Protocol #

All messages received from and passed to the server must be ASCII text.  These
messages need not be case-sensitive, but spelling and whitespace are important.
If your language requires it, your message may have a terminating NULL
character.

When your program starts, you must send the message `{identifier} alive` to the
server.  Replace `{identifier}` with the identifier the server passed as a
command line argument to your program.  This message allows the server to know
that your program is ready to play and what port number your program got.

Every other message your program sends will be in response to one sent by the
server.  The messages your program will receive are detailed below, showing
their syntaxes, example messages, examples of how your program should respond,
and explanations of what the messages mean and when your program will receive
them.

> **`reset`**:

> syntax: `reset playerCount playerIndex cards`

> example: `reset 6 3 Gr Ca Ba`

> response: `ok`

> The `reset` message tells your program that it is participating in a game.
  `playerCount` is the number of players in the game and will be 3, 4, 5, or 6.
  `playerIndex` is your program's position in the play order (0 is first).
  After that, you will receive anywhere between 3 and 6 card abbreviations as
  shown in the "Elements" section separated by spaces to designate your
  program's hand.  Once your program has configured its internal state to play,
  send the message `ok`.

> **`suggest`**:

> syntax: `suggest`

> response: `suggest Suspect Weapon Room`

> example response: `suggest Pl Kn Li`

> The `suggest` message tells your program that it is its turn to make a
  suggestion.  Your program must respond with a suggestion it has not previously
  made.  The `Suspect`, `Weapon`, and `Room` must be two-letter abbreviations
  for cards from the respective sections in the [card abbreviations][1].

> **`disprove`**:

> syntax: `disprove suggestingPlayer Suspect Weapon Room`

> example: `disprove 4 Sc Re Lo`

> response: `show Suspect|Weapon|Room`

> example response: `show Re`

> The `disprove` command is sent to the first player who has a card that can
  disprove the passed suggestion.  However, to speed the tournament, this
  message is only sent if that player has more than one of the cards in the
  suggestion.  The server will track the game state and will know whether a
  player cannot disprove the suggestion or only has one card to disprove the
  suggestion.  Your program must respond by sending `show Xx`, where `Xx` is the
  [abbreviation][1] for a card in the suggestion that is in your program's hand.

> **`suggestion`**:

> syntax: `suggestion activePlayerIndex Suspect Weapon Room
  disprovingPlayerIndex|-[ Card]`

> example 1: `suggestion 0 Gr Ca Ba 3`

> example 2: `suggestion 0 Gr Ca Ba 3 Ca`

> example 3: `suggestion 2 Wh Pi Bi -`

> response: `ok`

> The `suggestion` message is sent to every AI playing the current game.  It
  shows who the active player is (`activePlayerIndex`), what suggestion he made
  (`Suspect Weapon Room`), and whether anyone was able to disprove it.  If no
  player was able to disprove the suggestion, the message ends with a hyphen
  (`-`).  Otherwise, it then lists which player disproved the suggestion
  (`disprovingPlayerIndex`).  If your program is either the active player or the
  disproving player and the suggestion was disproved, then an additional token
  for the card shown ends the message (`Card`).  This both allows the active
  player to learn what card he was shown and the disprover to learn which card
  he showed.

> Once your program has finished processing this information, it must send the
  `ok` message.

> **`accuse`**:

> syntax: `accuse`

> response: `-|[accuse Suspect Weapon Room]`

> example response 1: `-`

> example response 2: `accuse Mu Ro St`

> The `accuse` message gives your program the chance to make an accusation.
  Your program either responds with a hyphen (`-`) to indicate that it does not
  want to make an Accusation or with `accuse Suspect Weapon Room` to make an
  Accusation.  `Suspect`, `Weapon`, and `Room` must be valid [abbreviations][1]
  for those categories as found in the "Element" section.

> **`accusation`**:

> syntax: `accusation player Suspect Weapon Room [+|-]`

> example: `accusation 3 Pe Kn Ha -`

> response: `ok`

> The `accusation` message is sent to all players to inform them that an
  accusation was made and what its result was.  `player` is the index of the
  player who made the Accusation, `Suspect Weapon Room` is the accusation as
  shown in the `accuse` discussion above, and the last token shows the result.
  A plus sign (`+`) means that the accusation is correct and that `player` wins.
  A minus sign (`-`) means that the Accusation is incorrect and that `player`
  loses. Once your program has processed this information, it must send the
  message `ok` to the server.

> Note that there is no corollary message for no Accusation having been made.
  On most turns, no player will make an Accusation, so it makes the tournaments
  faster to not require such a message.  If your AI needs to track whether a
  player did not make an accusation, I recommend using internal state to account
  for the lack of a suggestion when the next `suggestion` message arrives.

> **`done`**:

> syntax: `done`

> response: `dead`

> The `done` message allows the server to inform your program to close.  Your
  program must shut down after receiving this message. At some point between
  receiving the `done` message and ceasing execution, your program must respond
  with the `dead` message.

[1]: https://github.com/sadakatsu/SpeedClueContest/tree/master/card_abbreviations.md