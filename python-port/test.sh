if test -n "$1"
then
    PORT=$1
else
    PORT=8889
fi
BUF=--buf
python3 -m speedclue $BUF --port $PORT --count 500 \
    ../entries/peter_taylor/dist/InferencePlayer.jar\
    ../entries/peter_taylor/dist/SimpleCluedoPlayer.jar\
    ../core/randomAI.jar\
    ../entries/CluePaddle/bin/Debug/CluePaddle.exe\
    ../entries/ClueByFour/bin/Debug/ClueByFour.exe\
    ./simpleai.py
