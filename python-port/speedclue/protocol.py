import socket

class Messager:
    def __init__(self, sock):
        self.sock = sock

    def send(self, msg):
        raise NotImplementedError

    def recv(self):
        raise NotImplementedError

    def close(self):
        self.sock.close()


class LineMessager(Messager):
    def __init__(self, sock):
        super().__init__(sock)
        self._rfile = self.sock.makefile('r')
        self._wfile = self.sock.makefile('w')

    def send(self, msg):
        self._wfile.write(msg + '\n')
        self._wfile.flush()

    def recv(self):
        return self._rfile.readline()[:-1]


class BufMessager(Messager):
    BUFSIZE = 512

    def __init__(self, sock):
        super().__init__(sock)

    def send(self, msg):
        self.sock.sendall(msg.encode())

    def recv(self):
        msg = self.sock.recv(self.BUFSIZE).decode()
        return msg
