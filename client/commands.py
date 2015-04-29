#!/usr/bin/python

import pygame
import time
import os
import sys
import io
import math
from urllib2 import urlopen
import system_id
import json

os.putenv('SDL_VIDEODRIVER', 'fbcon')

pygame.display.init()

pygame.mouse.set_visible(0)
info = pygame.display.Info()
screen_width = info.current_w
screen_height = info.current_h
screen_size = (info.current_w, info.current_h)
screen_aspect = (float(screen_width) / screen_height)
screen = pygame.display.set_mode(screen_size)

print "screen size: %d/%d" % (screen_width, screen_height)

ws = None

def do_login(ws_):
    ws = ws_
    ws.send("login %s" % (json.dumps({'id': system_id.get(),
                                      'token': get_token(),
                                      'screen': {'width': screen_width,
                                                 'height': screen_height}})))

def load(url):
    img_str = urlopen(url).read()
    img_file = io.BytesIO(img_str)

    img = pygame.image.load(img_file)
    (img_width, img_height) = (img.get_width(), img.get_height())
    img_aspect = (float(img_width) / img_height)
    print "image size: %d/%d" % (img_width, img_height)
    ratio = 0
    if math.copysign(1.0, screen_aspect - 1.0) == math.copysign(1.0, img_aspect - 1.0):
        if screen_aspect > 1:
            ratio = float(screen_height) / img_height
        else:
            ratio = float(screen_width) / img_width
    else:
        if screen_aspect > 1:
            ratio = float(screen_width) / img_width
        else:
            ratio = float(screen_height) / img_height
    x_offset = (screen_width - (img_width * ratio)) / 2
    y_offset = (screen_height - (img_height * ratio)) / 2
    scaled_width = int(img_width * ratio)
    scaled_height = int(img_height * ratio)
    img = pygame.transform.scale(img, (scaled_width, scaled_height))
    screen.fill((0, 0, 0))
    screen.blit(img, (x_offset, y_offset))
    print "done"

def flip():
    pygame.display.flip()

def ping():
    pass

def login(status):
    if status == "accepted":
        print "Login accepted"
    else:
        print "Could not login in,", status
        sys.exit(1)

token_file = os.path.expanduser("~/.phrame.token")

def get_token():
    try:
        f = open(token_file, 'r')
        token = f.read().rstrip()
        f.close()
        return token
    except:
        return "UNKNOWN"

def set_token(token):
    f = open(token_file, 'w')
    f.write(token)
    f.close()
