#!/usr/bin/python

import pygame
import time
import os
import sys
import io
from urllib2 import urlopen

os.putenv('SDL_VIDEODRIVER', 'fbcon')

pygame.display.init()
info = pygame.display.Info()
screen_width = info.current_w
screen_height = info.current_h
screen_size = (info.current_w, info.current_h)
screen = pygame.display.set_mode(screen_size)

def load(url):
    print "loading ", url
    img_str = urlopen(url).read()
    img_file = io.BytesIO(img_str)

    img = pygame.image.load(img_file)
    (img_width, img_height) = (img.get_width(), img.get_height())
    ratio = min(float(screen_width) / img_width, float(screen_height) / img_height)
    scaled_width = int(img_width * ratio)
    scaled_height = int(img_height * ratio)
    img = pygame.transform.scale(img, (scaled_width, scaled_height))
    screen.fill((0, 0, 0))
    screen.blit(img, (screen_width / 2 - scaled_width / 2, screen_height / 2 - scaled_height / 2))
    print "done"

def flip():
    pygame.display.flip()

