#!/usr/bin/python

import websocket
import logging

logging.basicConfig()

class Commands:
    def hello():
        print "hello called"

def on_message(ws, message):
    print "received: ", message
    print "he", message.split(None, 1)
    split = message.split(None, 1)
    command = split[0]
    args = split[1:]
    print "command:", command, "args:", args, Commands
    if command in Commands:
        handler = Commands[command]
        print "handler: ", handler
        handler(*args.split())
    else:
        print "unknown command"

ws = websocket.WebSocketApp("ws://localhost:9090/websocket",
                            on_message = on_message)

ws.run_forever()
