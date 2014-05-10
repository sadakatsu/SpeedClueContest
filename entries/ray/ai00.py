#!/usr/bin/env python
import itertools
import sys

from speedclue.playerproxy import Player, main
from speedclue.cards import CARDS
from speedclue.protocol import BufMessager

# import crash_on_ipy


class Card:
    def __init__(self, name, type):
        self.name = name
        self.possible_owners = []
        self.owner = None
        self.in_solution = False
        self.disproved_to = set()
        self.type = type

    def __repr__(self):
        return self.name

    def log(self, *args, **kwargs):
        pass

    def set_owner(self, owner):
        assert self.owner is None
        assert self in owner.may_have
        for player in self.possible_owners:
            player.may_have.remove(self)
        self.possible_owners.clear()
        self.owner = owner
        owner.must_have.add(self)

    def set_as_solution(self):
        # import pdb; pdb.set_trace()
        assert self.owner is None
        self.type.solution = self
        self.in_solution = True
        for player in self.possible_owners:
            player.may_have.remove(self)
        self.possible_owners.clear()

    def __hash__(self):
        return hash(self.name)


class CardType:
    def __init__(self, type_id):
        self.type_id = type_id
        self.cards = [Card(name, self) for name in CARDS[type_id]]
        self.solution = None


class PlayerInfo:
    def __init__(self, id):
        self.id = id
        self.must_have = set()
        self.may_have = set()
        self.selection_groups = []
        self.n_cards = None

    def __hash__(self):
        return hash(self.id)

    def set_have_not_card(self, card):
        if card in self.may_have:
            self.may_have.remove(card)
            card.possible_owners.remove(self)

    def log(self, *args, **kwargs):
        pass

    def update(self):
        static = False
        updated = False
        while not static:
            static = True
            if len(self.must_have) == self.n_cards:
                if not self.may_have:
                    break
                for card in self.may_have:
                    card.possible_owners.remove(self)
                self.may_have.clear()
                static = False
                updated = True
            if len(self.must_have) + len(self.may_have) == self.n_cards:
                static = False
                updated = True
                for card in list(self.may_have):
                    card.set_owner(self)

            new_groups = []
            for group in self.selection_groups:
                group1 = []
                for card in group:
                    if card in self.must_have:
                        break
                    if card in self.may_have:
                        group1.append(card)
                else:
                    if len(group1) == 1:
                        group1[0].set_owner(self)
                        updated = True
                        static = False
                    elif group1:
                        new_groups.append(group1)
            self.selection_groups = new_groups

            if len(self.must_have) + 1 == self.n_cards:
                cards = set(self.may_have)
                for group in self.selection_groups:
                    group = set(group)
                    if not (group & self.must_have):
                        cards &= group
                for card in [card for card in self.may_have if card not in cards]:
                    static = False
                    updated = True
                    self.set_have_not_card(card)

        return updated


class Suggestion:
    def __init__(self, player, cards, dplayer, dcard):
        self.player = player
        self.cards = cards
        self.dplayer = dplayer
        self.dcard = dcard
        self.disproved = dplayer is not None


class AI00(Player):
    def prepare(self):
        self.set_verbosity(1)

    def reset(self, player_count, player_id, card_names):
        self.log('reset', 'id=', player_id, card_names)
        self.card_types = [CardType(i) for i in range(len(CARDS))]
        self.cards = list(itertools.chain(*(ct.cards for ct in self.card_types)))
        for card in self.cards:
            card.log = self.log
        self.card_map = {card.name: card for card in self.cards}
        self.owned_cards = [self.card_map[name] for name in card_names]
        self.players = [PlayerInfo(i) for i in range(player_count)]
        for player in self.players:
            player.log = self.log
        self.player = self.players[player_id]
        for card in self.cards:
            card.possible_owners = list(self.players)
        n_avail_cards = len(self.cards) - len(CARDS)
        for player in self.players:
            player.may_have = set(self.cards)
            player.n_cards = n_avail_cards // player_count \
                + (player.id < n_avail_cards % player_count)
        for card in self.owned_cards:
            card.set_owner(self.player)
        for card in self.cards:
            if card not in self.owned_cards:
                self.player.set_have_not_card(card)
        self.suggestions = []
        self.avail_suggestions = set(itertools.product(*CARDS))
        self.dump()

    def suggest(self):
        sg = []
        for type in self.card_types:
            card = min((card for card in type.cards if card.owner is None),
                key=lambda card: len(card.possible_owners))
            sg.append(card.name)
        sg = tuple(sg)

        if sg not in self.avail_suggestions:
            sg = self.avail_suggestions.pop()
        else:
            self.avail_suggestions.remove(sg)
        return sg

    def suggestion(self, player_id, cards, disprove_player_id=None, card=None):
        sg = Suggestion(
            self.players[player_id],
            self.get_cards_by_names(cards),
            self.players[disprove_player_id] if disprove_player_id is not None else None,
            self.card_map[card] if card else None,
        )
        self.suggestions.append(sg)
        # Iter through the non-disproving players and update their may_have
        end_id = sg.dplayer.id if sg.disproved else sg.player.id
        for player in self.iter_players(sg.player.id + 1, end_id):
            if player is self.player:
                continue
            for card in sg.cards:
                player.set_have_not_card(card)
        if sg.disproved:
            # The disproving player has sg.dcard
            if sg.dcard:
                if sg.dcard.owner is None:
                    sg.dcard.set_owner(sg.dplayer)
            else:
                # Add a selection group to the disproving player
                sg.dplayer.selection_groups.append(sg.cards)

        static = False
        while not static:
            static = True
            for card in self.cards:
                if card.owner is not None or card.in_solution:
                    continue
                if len(card.possible_owners) == 0 and card.type.solution is None:
                    # In solution
                    card.set_as_solution()
                    static = False

            for type in self.card_types:
                if type.solution is not None:
                    continue
                if sum(1 for card in type.cards if card.owner) + 1 == len(type.cards):
                    card = next(card for card in type.cards if card.owner is None)
                    card.set_as_solution()
                    static = False

            for player in self.players:
                if player is not self.player:
                    if player.update():
                        static = False
        self.dump()

    def iter_players(self, start_id, end_id):
        n = len(self.players)
        for i in range(start_id, start_id + n):
            if i % n == end_id:
                break
            yield self.players[i % n]

    def accuse(self):
        if all(type.solution for type in self.card_types):
            return [type.solution.name for type in self.card_types]
        return None

    def disprove(self, suggest_player_id, cards):
        cards = self.get_cards_by_names(cards)
        sg_player = self.players[suggest_player_id]
        cards = [card for card in cards if card in self.owned_cards]
        for card in cards:
            if sg_player in card.disproved_to:
                return card.name
        return max(cards, key=lambda c: len(c.disproved_to)).name

    def accusation(self, player_id, cards, is_win):
        if not is_win:
            player = self.players[player_id]
            for card in self.get_cards_by_names(cards):
                player.set_have_not_card(card)
            player.update()

    def get_cards_by_names(self, names):
        return [self.card_map[name] for name in names]

    def dump(self):
        self.log()
        for player in self.players:
            self.log(player.id,
                sorted(player.must_have, key=lambda x:x.name),
                sorted(player.may_have, key=lambda x:x.name))
        self.log([type.solution for type in self.card_types])
        self.log('id|', end='')

        def end():
            return ' | ' if card.name in [g[-1] for g in CARDS] else '|'

        for card in self.cards:
            self.log(card.name, end=end())
        self.log()
        for player in self.players:
            self.log(' *'[player.id == self.player.id] + str(player.id), end='|')
            for card in self.cards:
                self.log(' ' + 'xo'[player in card.possible_owners or player is card.owner], end=end())
            self.log()


if __name__ == '__main__':
    main(AI00, BufMessager)
