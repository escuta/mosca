#!/bin/bash
# mosca_jackrec.sh v1.0
# Start or stop jack_rec for Mosca binaural recording.
# Called by MoscaBase.sc via unixCmd.
# Usage: mosca_jackrec.sh start <output_file>
#        mosca_jackrec.sh stop

PIDFILE=/tmp/mosca_jackrec.pid

case "$1" in
    start)
        if [ -z "$2" ]; then
            echo "Usage: mosca_jackrec.sh start <output_file>"
            exit 1
        fi
        OUTFILE="$2"
        jack_rec -f "$OUTFILE" -b 24 supernova:output_1 supernova:output_2 &
        echo $! > "$PIDFILE"
        echo "jack_rec started, PID $(cat $PIDFILE), recording to $OUTFILE"
        ;;
    stop)
        if [ -f "$PIDFILE" ]; then
            kill $(cat "$PIDFILE") 2>/dev/null
            rm "$PIDFILE"
            echo "jack_rec stopped"
        else
            echo "No pidfile found, nothing to stop"
        fi
        ;;
    *)
        echo "Usage: mosca_jackrec.sh start <output_file> | stop"
        exit 1
        ;;
esac
