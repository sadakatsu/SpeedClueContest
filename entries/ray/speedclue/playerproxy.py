import socket

class Player:
    def __init__(self, name, addr, messager_class):
        self.name = name
        sock = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
        sock.connect(addr)
        self._logfile = open(name + '.log', 'w')
        self._verbosity = 0
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
        self.prepare()

    def set_verbosity(self, v):
        self._verbosity = v

    def handle_reset(self, player_count, player_id, *cards):
        self.reset(int(player_count), int(player_id), cards)
        self.send('ok')

    def reset(self, player_count, player_id, cards):
        raise NotImplementedError

    def handle_suggest(self):
        cards = self.suggest()
        self.send('suggest ' + ' '.join(cards))

    def suggest(self):
        raise NotImplementedError

    def handle_suggestion(self, player_id, c1, c2, c3, disprove_player_id, card=None):
        player_id = int(player_id)
        self.suggestion(int(player_id), [c1, c2, c3],
            *(() if disprove_player_id == '-' else (int(disprove_player_id), card)))
        self.send('ok')

    def suggestion(self, player_id, cards, disprove_player_id=None, card=None):
        raise NotImplementedError

    def handle_disprove(self, suggest_player_id, *cards):
        result = self.disprove(int(suggest_player_id), cards)
        self.send('show ' + result)

    def handle_accuse(self):
        result = self.accuse()
        if not result:
            self.send('-')
        else:
            self.send('accuse ' + ' '.join(result))

    def accuse(self):
        raise NotImplementedError

    def handle_accusation(self, player_id, c1, c2, c3, is_win):
        self.accusation(int(player_id), [c1, c2, c3], is_win == '+')
        self.send('ok')

    def accusation(self):
        raise NotImplementedError

    def handle_done(self):
        self.done()
        self._logfile.flush()
        self.send('dead')
        self.messager.close()
        self._quit = True

    def done(self):
        pass

    def send(self, msg):
        if self._verbosity > 0:
            self.log('send:[{}]'.format(msg))
        self.messager.send(msg)

    def prepare(self):
        pass

    def run(self):
        self.send('{} alive'.format(self.name))
        self._quit = False
        while not self._quit:
            msg = self.messager.recv()
            cmd, *args = msg.split()
            if cmd in self._handlers:
                if self._verbosity > 0:
                    self.log('recv:[{}]'.format(msg))
                self._handlers[cmd](*args)
            else:
                self.log('unknown command:', cmd, 'msg:', msg)

    def log(self, *args, **kwargs):
        if self._verbosity == 0:
            return
        print(*args, file=self._logfile, **kwargs)

    def __del__(self):
        self._logfile.close()


def main(player_class, messager_class):
    import sys
    name = sys.argv[1]
    port = int(sys.argv[2])
    player = player_class(name, ('localhost', port), messager_class)
    player.run()
