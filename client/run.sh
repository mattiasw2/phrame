#!/bin/sh

while true
do
    sudo python client.py ws://localhost:9090/websocket
    echo client exited
    sleep 1
done