#!/usr/bin/python

import websocket
import logging
import sys
import ssl
import os

import commands
import system_id

logging.basicConfig()

def on_open(ws):
    print "connected, sending id"
    ws.send("login %s %s" % (system_id.get(), commands.get_token()))

def on_message(ws, message):
    print "message:", message
    split = message.split()
    command = split[0]
    args = split[1:]
    if command in dir(commands):
        handler = getattr(commands, command)
        try:
            handler(*args)
            ws.send("ack")
        except SystemExit:
            ws.close()
            pass
        except:
            print "message", message, "caused error:", sys.exc_info()[0]
    else:
        print "unknown command", command

def on_error(ws, message):
    print "websocket error:", message

def on_close(ws):
    print "websocket closed, exiting"
    sys.exit(1)

if len(sys.argv) < 2:
    print "missing URL argument"
    sys.exit(1)

url = sys.argv[1]

ws = websocket.WebSocketApp(url,
                            on_error = on_error,
                            on_message = on_message,
                            on_open = on_open,
                            on_close = on_close)

ws.run_forever(sslopt={"cert_reqs": ssl.CERT_NONE})
