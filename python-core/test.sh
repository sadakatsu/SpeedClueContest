if test -n "$1"
then
    PORT=$1
else
    PORT=8889
fi
BUF=--buf
python -m speedclue $BUF --port $PORT --count 10 \
    ../entries/peter_taylor/dist/InferencePlayer.jar\
    ../entries/peter_taylor/dist/InferencePlayer.jar\
    ../core/randomAI.jar\
    ../core/randomAI.jar\
    # ./simpleai.py
