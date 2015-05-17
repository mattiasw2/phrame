#!/bin/sh

PHRAME_URL=${1:-wss://netzhansa.com/websocket}

sudo fbset -depth 24
while true
do
    sudo python client.py $PHRAME_URL
    echo client exited
    sleep 1
done
