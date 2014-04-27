if test -n "$1"
then
    PORT=$1
else
    PORT=$((9000 + $RANDOM % 2000))
fi
BUF=--buf
python -m speedclue $BUF --port $PORT --count 100 \
    ./ai00.py\
    ./ai00.py\
    ../peter_taylor/dist/InferencePlayer.jar\
    ../peter_taylor/dist/InferencePlayer.jar\
    #
