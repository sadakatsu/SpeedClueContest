if test -n "$1"
then
    PORT=$1
else
    PORT=$((9000 + $RANDOM % 2000))
fi
BUF=--buf
python -m speedclue $BUF --port $PORT --count 100 \
    ../../core/randomAI.jar\
    ./ai01.py\
    ../peter_taylor/dist/InferencePlayer.jar\
    # ./ai00.py\
    #
