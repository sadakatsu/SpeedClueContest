"""
Example usage:

    python gameserver.py --port 8899 --count 20 ai-1.jar ai-2.jar ./ai-3.py

This will start the server on port 8899 with 3 AI players. Then it runs 20 times
and output the final result.

"""
import socket
import itertools
import random
from threading import Thread, RLock
from time import sleep

from . import protocol
from .cards import CARDS

class InvalidMoveError(Exception):
    pass

CARDS = (
    frozenset(('Gr', 'Mu', 'Pe', 'Pl', 'Sc', 'Wh')),
    frozenset(('Ca', 'Kn', 'Pi', 'Re', 'Ro', 'Wr')),
    frozenset(('Ba', 'Bi', 'Co', 'Di', 'Ha', 'Ki', 'Li', 'Lo', 'St')),
)


class ParallelRunner:
    def __init__(self):
        self._threads = []

    def add_task(self, target, *args):
        th = Thread(target=target, args=args)
        th.daemon = True
        th.start()
        self._threads.append(th)

    def wait(self):
        for th in self._threads:
            th.join()

class Timer:
    def __init__(self, timeout):
        pass

    def on_finish(self):
        pass

    def start(self):
        pass

    def abort(self):
        pass


class Player:
    TIMEOUT = 2

    def __init__(self, name, messager):
        self.name = name
        self.messager = messager
        self.cards = []
        self.alive = True
        self.score = 0
        # Doubly linked list
        self.prev = None
        self.next = None

    def interact(self, command):
        command = command.strip()
        print('  {} =<< [{}]'.format(self.name, command))
        self.messager.send(command)
        finished = False

        def kill():
            nonlocal timeout
            sleep(self.TIMEOUT)
            if not finished:
                self._sock.shutdown(socket.SHUT_RDWR)
                timeout = True

        timeout = False
        timer = Timer(self.TIMEOUT)
        timer.on_finish = kill
        timer.start()
        response = self.messager.recv()
        if timeout:
            raise InvalidMoveError(self.name, 'timeout')
        else:
            timer.abort()
        assert response
        print('  {} >>= [{}]'.format(self.name, response))
        finished = True
        return response

    def reset(self, player_count, player_id, cards):
        self.alive = True
        self.id = player_id
        self._suggested = set()
        result = self.interact('reset {} {} {}'.format(
            player_count, player_id, ' '.join(cards)))
        assert result == 'ok'

    def suggest(self):
        """
        return suggesting cards. Thess cards should be different from
        suggested cards by this player.
        """
        result = self.interact('suggest')
        cmd, *cards = result.split()
        key = frozenset(cards)
        assert cmd == 'suggest'
        assert key not in self._suggested
        assert all((c in g) for c, g in zip(cards, CARDS)), cards
        return cards

    def disprove(self, suggest_player_id, cards):
        """
        return None or a card. This card should be in `cards` and in `self.cards`.
        """
        response = self.interact('disprove {} {}'.format(
            suggest_player_id, ' '.join(cards)))
        cmd, card = response.split()
        assert cmd == 'show'
        assert card in self.cards and card in cards
        return card

    def suggestion(self, player_id, cards, disprove_player_id=None, card=None): 
        """
        Notify this player with a suggestion.
        """
        if disprove_player_id is None:
            result = self.interact('suggestion {} {} -'.format(player_id, ' '.join(cards)))
        else:
            result = self.interact('suggestion {} {} {} {}'.format(
                player_id, ' '.join(cards), disprove_player_id, card or ''))
        assert result == 'ok'

    def accuse(self):
        """
        return None or a list of cards.
        """
        result = self.interact('accuse')
        if result == '-':
            return None
        _, *accuse_cards = result.split()
        if len(accuse_cards) == 3 and \
                all((c in g) for g, c in zip(CARDS, accuse_cards)):
            return accuse_cards
        else:
            raise InvalidMoveError(accuse_cards)

    def accusation(self, player_id, cards, is_win):
        """
        Notify this player with an accusation.
        """
        result = self.interact('accusation {} {} {}'.format(
            player_id, ' '.join(cards), '-+'[is_win]))
        assert result == 'ok'

    def done(self):
        self.interact('done')

    def quit(self):
        self.alive = False
        self._sock.close()

    def lose(self):
        self.prev.next = self.next
        self.next.prev = self.prev
        self.quit()

    def iter_from_next(self):
        p = self
        while p.next is not self:
            p = p.next
            yield p

    def iter(self):
        p = self
        yield p
        while p.next is not self:
            p = p.next
            yield p


class GameServer:
    MAX_PLAYERS = 6
    messager_class = protocol.LineMessager

    def __init__(self, port):
        self.port = port
        self.listen_sock = sock = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
        addr = ('', port)
        print('Game server listen on ({}, {}).'.format(*addr))
        sock.bind(addr)
        sock.listen(6)
        self.players = {}

    def collect_players(self, names):
        assert len(names) <= self.MAX_PLAYERS

        def add(messager):
            try:
                name, cmd = messager.recv().split()
                if cmd == 'alive' and name in names:
                    with lock_players:
                        self.players[name] = Player(name, messager)
                        print('players:', list(self.players.keys()))
                    return
            except:
                raise
            else:
                messager.close()

        lock_players = RLock()

        connected = 0
        while connected < len(names):
            client_sock, client_addr = self.listen_sock.accept()
            connected += 1
            thread = Thread(target=add, args=(self.messager_class(client_sock),))
            thread.daemon = True
            thread.start()

        while True:
            with lock_players:
                if len(self.players) == len(names):
                    break
            sleep(0.1)

    # @profile
    def run_game(self):
        players = list(self.players.values())
        random.shuffle(players)
        # Choose 3 cards as target
        target = [random.choice(list(g)) for g in CARDS] 
        # Collect remaining cards
        cards = []
        for t, g in zip(target, CARDS):
            cards.extend(filter(t.__ne__, g))
        random.shuffle(cards)
        # Assign cards to players
        for player in players:
            player.cards.clear()
        for player, card in zip(itertools.cycle(players), cards):
            player.cards.append(card)

        for i, player in enumerate(players):
            try:
                player.reset(len(players), i, player.cards)
            except Exception as e:
                self.kill_player(player, e)

        players = [player for player in players if player.alive]
        for i, player in enumerate(players):
            player.prev = players[i - 1]
            players[i - 1].next = player

        end = False
        active_player = players[-1]
        while not end:
            print('=' * 80)
            if active_player is active_player.next:
                self.player_win(active_player)
                break
            active_player = active_player.next

            # suggest
            try:
                cards = active_player.suggest()
            except Exception as e:
                self.kill_player(active_player, e) 
                continue
            # disprove
            for player in active_player.iter_from_next():
                if set(cards) & set(player.cards):
                    try:
                        disprove = player.disprove(active_player.id, cards)
                        if disprove is not None:
                            disprove_player = player
                            break
                    except Exception as e:
                        self.kill_player(player, e)
            else:
                disprove = None

            def task(player):
                try:
                    if disprove is not None:
                        if player is active_player or player is disprove_player:
                            player.suggestion(active_player.id, cards, disprove_player.id, disprove)
                        else:
                            player.suggestion(active_player.id, cards, disprove_player.id)
                    else:
                        player.suggestion(active_player.id, cards)
                except Exception as e:
                    self.kill_player(player, e)
                
            # for player in active_player.iter():
            #     task(player)
            runner = ParallelRunner()
            for player in active_player.iter():
                runner.add_task(task, player)
            runner.wait()
            # accuse
            try:
                accuse_cards = active_player.accuse()
            except Exception as e:
                self.kill_player(active_player, e)
                continue
            if accuse_cards is not None:
                is_win = target == accuse_cards
                for player in active_player.iter():
                    try:
                        player.accusation(active_player.id, accuse_cards, is_win)
                    except Exception as e:
                        self.kill_player(player, e)
                if is_win:
                    self.player_win(active_player)
                    end = True
                else:
                    self.player_lose(active_player)
        print('-' * 80)
        print('Scores:')
        for player in sorted(self.players.values(), key=lambda x: x.name):
            print('  {:20s}: {}'.format(player.name, player.score))
        print('-' * 80)


    def kill_player(self, player, e):
        raise
        print(player.name, 'perform invalid move:', e)
        print('kill', player.name)
        player.lose()

    def player_win(self, player):
        player.score += 1
        print('{} win, game over.'.format(player.name))

    def player_lose(self, player):
        print('{} lose, game over.'.format(player.name))
        player.lose()

    def quit(self):
        for player in self.players.values():
            player.done()

    def close(self):
        self.listen_sock.close()


class BufGameServer(GameServer):
    messager_class = protocol.BufMessager
