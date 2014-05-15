#!/usr/bin/env python
import itertools

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
        self.type.rest_count -= 1

    def set_as_solution(self):
        # import pdb; pdb.set_trace()
        assert self.owner is None
        self.type.solution = self
        self.in_solution = True
        for player in self.possible_owners:
            player.may_have.remove(self)
        self.possible_owners.clear()
        self.type.rest_count -= 1

    def __hash__(self):
        return hash(self.name)


class CardType:
    def __init__(self, type_id):
        self.type_id = type_id
        self.cards = [Card(name, self) for name in CARDS[type_id]]
        self.rest_count = len(self.cards)
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
                # There is only one card remain to be unknown, so this card must
                # be in all selection groups
                cards = self.may_have.copy()
                for group in self.selection_groups:
                    if self.must_have.isdisjoint(group):
                        cards.intersection_update(group)

                for card in self.may_have - cards:
                    static = False
                    updated = True
                    self.set_have_not_card(card)

        # assert self.must_have.isdisjoint(self.may_have)
        # assert len(self.must_have | self.may_have) >= self.n_cards
        return updated


class Suggestion:
    def __init__(self, player, cards, dplayer, dcard):
        self.player = player
        self.cards = cards
        self.dplayer = dplayer
        self.dcard = dcard
        self.disproved = dplayer is not None


class AI01(Player):
    def prepare(self):
        self.set_verbosity(0)

    def reset(self, player_count, player_id, card_names):
        self.log('reset', 'id=', player_id, card_names)
        self.fail_count = 0
        self.suggest_count = 0
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
        self.possible_solutions = {
            tuple(self.get_cards_by_names(cards)): 1
            for cards in self.avail_suggestions
        }
        self.filter_solutions()

    def filter_solutions(self):
        new_solutions = {}
        # assert self.possible_solutions
        join = next(iter(self.possible_solutions))
        for sol in self.possible_solutions:
            for card, type in zip(sol, self.card_types):
                if card.owner or type.solution and card is not type.solution:
                    # This candidate can not be a solution because it has a
                    # card that has owner or this type is solved.
                    break
            else:
                count = self.check_solution(sol)
                if count:
                    new_solutions[sol] = count
                    join = tuple(((x is y) and x) for x, y in zip(join, sol))
        self.possible_solutions = new_solutions
        updated = False
        for card in join:
            if card and not card.in_solution:
                card.set_as_solution()
                updated = True
                self.log('found new target', card, 'in', join)

        # self.dump()
        return updated

    def check_solution(self, solution):
        """
        This must be called after each player is updated.
        """
        players = self.players
        avail_cards = set(card for card in self.cards if card.possible_owners)
        avail_cards -= set(solution)
        if len(avail_cards) >= 10:
            return 1
        count = 0

        def resolve_player(i, avail_cards):
            nonlocal count
            if i == len(players):
                count += 1
                return
            player = players[i]
            n_take = player.n_cards - len(player.must_have)
            cards = avail_cards & player.may_have
            for choice in map(set, itertools.combinations(cards, n_take)):
                player_cards = player.must_have | choice
                for group in player.selection_groups:
                    if player_cards.isdisjoint(group):
                        # Invalid choice
                        break
                else:
                    resolve_player(i + 1, avail_cards - choice)

        resolve_player(0, avail_cards)
        return count

    def suggest1(self):
        choices = []
        for type in self.card_types:
            choices.append([])
            if type.solution:
                choices[-1].extend(self.player.must_have & set(type.cards))
            else:
                choices[-1].extend(sorted(
                    (card for card in type.cards if card.owner is None),
                    key=lambda card: len(card.possible_owners)))

        for sgi in sorted(itertools.product(*map(lambda x:range(len(x)), choices)),
                key=sum):
            sg = tuple(choices[i][j].name for i, j in enumerate(sgi))
            if sg in self.avail_suggestions:
                self.avail_suggestions.remove(sg)
                break
        else:
            sg = self.avail_suggestions.pop()
            self.fail_count += 1
            self.log('fail')
        self.suggest_count += 1
        return sg

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
            self.possible_solutions.pop(tuple(sg.cards), None)

        self.update()

    def update(self):
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
                if type.rest_count == 1:
                    card = next(card for card in type.cards if card.owner is None)
                    card.set_as_solution()
                    static = False

            for player in self.players:
                if player is self.player:
                    continue
                if player.update():
                    static = False

            if self.filter_solutions():
                static = False

    def iter_players(self, start_id, end_id):
        n = len(self.players)
        for i in range(start_id, start_id + n):
            if i % n == end_id:
                break
            yield self.players[i % n]

    def accuse(self):
        if all(type.solution for type in self.card_types):
            return [type.solution.name for type in self.card_types]
        possible_solutions = self.possible_solutions
        if len(possible_solutions) == 1:
            return next(possible_solutions.values())
        # most_possible = max(self.possible_solutions, key=self.possible_solutions.get)
        # total = sum(self.possible_solutions.values())
        # # self.log('rate:', self.possible_solutions[most_possible] / total)
        # if self.possible_solutions[most_possible] > 0.7 * total:
        #     self.log('guess', most_possible)
        #     return [card.name for card in most_possible]
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
            cards = tuple(self.get_cards_by_names(cards))
            self.possible_solutions.pop(cards, None)
            # player = self.players[player_id]
            # for card in cards:
            #     player.set_have_not_card(card)
            # player.update()
        else:
            self.log('fail rate:', self.fail_count / (1e-8 + self.suggest_count))
            self.log('fail count:', self.fail_count, 'suggest count:', self.suggest_count)

    def get_cards_by_names(self, names):
        return [self.card_map[name] for name in names]

    def dump(self):
        self.log()
        for player in self.players:
            self.log('player:', player.id, player.n_cards,
                sorted(player.must_have, key=lambda x: x.name),
                sorted(player.may_have, key=lambda x: x.name),
                '\n    ',
                player.selection_groups)
        self.log('current:', [type.solution for type in self.card_types])
        self.log('possible_solutions:', len(self.possible_solutions))
        for sol, count in self.possible_solutions.items():
            self.log('  ', sol, count)
        self.log('id|', end='')

        def end():
            return ' | ' if card.name in [g[-1] for g in CARDS] else '|'

        for card in self.cards:
            self.log(card.name, end=end())
        self.log()
        for player in self.players:
            self.log(' *'[player.id == self.player.id] + str(player.id), end='|')
            for card in self.cards:
                self.log(
                    ' ' + 'xo'[player in card.possible_owners or player is card.owner],
                    end=end())
            self.log()


if __name__ == '__main__':
    main(AI01, BufMessager)
