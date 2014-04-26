"""
Example usage:

    python -m speedclue --port 8899 --count 20 ai-1.jar ai-2.jar ./ai-3.py

This will start the server on port 8899 with 3 AI players. Then it runs 20 times
and output the final result.

"""
import os
from subprocess import Popen
import argparse
from .gameserver import GameServer, BufGameServer


def launch_player_programs(programs, port):
    procs = {}
    for i, prog in enumerate(programs):
        name = 'player#{}-{}'.format(i, os.path.split(prog)[1])
        if prog.endswith('jar'):
            args = ['java', '-jar', prog, name, str(port)]
        elif prog.endswith('.py'):
            args = ['python', prog, name, str(port)]
        else:
            args = [prog, name, str(port)]
        proc = Popen(args)
        print('launched {}[{}], pid={}'.format(name, ' '.join(args), proc.pid))
        proc.daemon = True
        procs[name] = proc
    return procs

parser = argparse.ArgumentParser('Speed Clue game server.')
parser.add_argument('--count', dest='count', type=int, default=1)
parser.add_argument('--port', dest='port', type=int, default=8080)
parser.add_argument('--buf', help='Use buffer protocol',
    dest='buf', action='store_const', const=True, default=False)
parser.add_argument('programs', metavar='PROGRAM', nargs=argparse.REMAINDER)
args = parser.parse_args()
if len(args.programs) < 3:
    print('Error: Need 3 to 6 players.')
    exit(1)

server = (GameServer if not args.buf else BufGameServer)(args.port)
procs = launch_player_programs(args.programs, args.port)
try:
    server.collect_players(list(procs.keys()))
    for i in range(args.count):
        server.run_game()
    server.quit()
finally:
    server.close()
    for proc in procs.values():
        proc.kill()
