#!/usr/bin/env python
import itertools

from speedclue.playerproxy import Player, main
from speedclue.cards import CARDS
from speedclue.protocol import BufMessager, LineMessager
import random


class SimpleAI(Player):
    def reset(self, player_count, player_id, cards):
        self.n_players = player_count
        self.id = player_id
        self.cards = cards
        self.suggest_choices = list(itertools.product(*CARDS))

    def suggest(self):
        cards = random.choice(self.suggest_choices)
        self.suggest_choices.remove(cards)
        return cards

    def suggestion(self, player_id, cards, disprove_player_id=None, card=None):
        pass

    def accuse(self):
        return None

    def disprove(self, suggest_player_id, cards):
        for card in self.cards:
            if card in cards:
                return card

    def accusation(self, player_id, cards, is_win):
        return None


if __name__ == '__main__':
    main(SimpleAI, BufMessager)
    # main(SimpleAI, LineMessager)
