#!/usr/bin/python

import websocket
import logging
import sys
import ssl

import commands

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

def on_error(ws, message):
    print "websocket error:", message

ws = websocket.WebSocketApp(sys.argv[1],
                            on_error = on_error,
                            on_message = on_message)

ws.run_forever(sslopt={"cert_reqs": ssl.CERT_NONE})
