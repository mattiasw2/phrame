#!/usr/bin/python

import pygame
import time
import os
import sys
import subprocess
import io
import math
from urllib2 import urlopen
import system_id
import json

os.putenv('SDL_VIDEODRIVER', 'fbcon')

class Screen:
    def __init__(self, width, height):
        self.width = width
        self.height = height
        self.aspect = (float(width) / height)
        self.handle = pygame.display.set_mode((width, height))
    
screen = None
ws = None

def do_login(ws_):
    ws = ws_
    ws.send("login %s" % (json.dumps({'id': system_id.get(),
                                      'token': get_token(),
                                      'screen': {'width': screen.width,
                                                 'height': screen.height}})))

def load(url):
    img_str = urlopen(url).read()
    img_file = io.BytesIO(img_str)

    img = pygame.image.load(img_file)
    (img_width, img_height) = (img.get_width(), img.get_height())
    img_aspect = (float(img_width) / img_height)
    print "image size: %d/%d" % (img_width, img_height)
    ratio = 0
    if math.copysign(1.0, screen.aspect - 1.0) == math.copysign(1.0, img_aspect - 1.0):
        if screen.aspect > 1:
            ratio = float(screen.height) / img_height
        else:
            ratio = float(screen.width) / img_width
    else:
        if screen.aspect > 1:
            ratio = float(screen.width) / img_width
        else:
            ratio = float(screen.height) / img_height
    x_offset = (screen.width - (img_width * ratio)) / 2
    y_offset = (screen.height - (img_height * ratio)) / 2
    scaled_width = int(img_width * ratio)
    scaled_height = int(img_height * ratio)
    img = pygame.transform.scale(img, (scaled_width, scaled_height))
    screen.handle.fill((0, 0, 0))
    screen.handle.blit(img, (x_offset, y_offset))
    print "done"

def flip():
    pygame.display.flip()

def ping():
    pass

def power_down():
    pygame.display.quit()
    subprocess.call(['tvservice', '-o'])

def power_up():
    subprocess.call(['tvservice', '-p'])
    pygame.init()
    pygame.mouse.set_visible(0)
    info = pygame.display.Info()
    global screen
    screen = Screen(info.current_w, info.current_h)

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

power_up()
