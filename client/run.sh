#!/bin/sh

while true
do
    sudo python client.py wss://netzhansa.com/websocket
    echo client exited
    sleep 1
done
