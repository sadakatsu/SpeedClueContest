import socket

class Player:
    def __init__(self, name, addr, messager_class):
        self.name = name
        sock = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
        sock.connect(addr)
        self.messager = messager_class(sock)
        self._handlers = {
            'reset': self.handle_reset,
            'suggest': self.handle_suggest,
            'suggestion': self.handle_suggestion,
            'disprove': self.handle_disprove,
            'accuse': self.handle_accuse,
            'accusation': self.handle_accusation,
            'done': self.handle_done,
        }

    def handle_reset(self, player_count, player_id, *cards):
        self.reset(int(player_count), int(player_id), cards)
        self.messager.send('ok')

    def reset(self, player_count, player_id, cards):
        raise NotImplementedError

    def handle_suggest(self):
        cards = self.suggest()
        self.messager.send('suggest ' + ' '.join(cards))

    def suggest(self):
        raise NotImplementedError

    def handle_suggestion(self, player_id, c1, c2, c3, disprove_player_id, card=None):
        player_id = int(player_id)
        self.suggestion(int(player_id), [c1, c2, c3],
            *(() if disprove_player_id == '-' else (int(disprove_player_id), card)))
        self.messager.send('ok')

    def suggestion(self, player_id, cards, disprove_player_id=None, card=None):
        raise NotImplementedError

    def handle_disprove(self, suggest_player_id, *cards):
        result = self.disprove(int(suggest_player_id), cards)
        self.messager.send('show ' + result)

    def handle_accuse(self):
        result = self.accuse()
        if not result:
            self.messager.send('-')
        else:
            self.messager.send('accuse ' + ' '.join(result))

    def accuse(self):
        raise NotImplementedError

    def handle_accusation(self, player_id, c1, c2, c3, is_win):
        self.accusation(int(player_id), [c1, c2, c3], is_win == '+')
        self.messager.send('ok')

    def accusation(self):
        raise NotImplementedError

    def handle_done(self):
        self.done()
        self.messager.send('dead')
        self.messager.close()
        self._quit = True

    def done(self):
        pass

    def run(self):
        self.messager.send('{} alive'.format(self.name))
        self._quit = False
        while not self._quit:
            msg = self.messager.recv()
            cmd, *args = msg.split()
            if cmd in self._handlers:
                self._handlers[cmd](*args)
            else:
                self.log('unknown command:', cmd, 'msg:', msg)

    def log(self, *args, **kwargs):
        print('[{}]:'.format(self.name), *args, **kwargs)


def main(player_class, messager_class):
    import sys
    name = sys.argv[1]
    port = int(sys.argv[2])
    player = player_class(name, ('localhost', port), messager_class)
    player.run()
