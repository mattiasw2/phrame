#!/usr/bin/python

import websocket
import logging
import commands
import sys

logging.basicConfig()

def on_message(ws, message):
    split = message.split()
    command = split[0]
    args = split[1:]
    if command in dir(commands):
        handler = getattr(commands, command)
        print "calling", handler, "for", command
        try:
            handler(*args)
        except:
            print "message", message, "caused error:", sys.exc_info()[0]
    else:
        print "unknown command", command

ws = websocket.WebSocketApp("ws://localhost:9090/websocket",
                            on_message = on_message)

ws.run_forever()
