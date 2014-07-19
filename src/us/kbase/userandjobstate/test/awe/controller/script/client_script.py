#!/usr/bin/env python
from __future__ import print_function
import os
import datetime
import time
import sys

#if (len(sys.argv) < 3):
#	print('Expected 2 arguments, input file & output file')
#	print('got: ' + str(sys.argv)) 
#	sys.exit(1)

print('sys.argv: ' + str(sys.argv))
if (len(sys.argv) == 1):
    sys.exit(0);

if (sys.argv[1] == 'error'):
    print("I was told to error out, now I'm printing to stdout")
    sys.stderr.write("I was told to error out, now I'm printing to stderr\n")
    sys.exit(1);
    
if (sys.argv[1] == 'delay'):
    delay = 10;
    if (len(sys.argv) > 2):
        try:
            delay = int(sys.argv[2]);
        except TypeError:
            pass # just use 30s
    time.sleep(delay)

#if (sys.argv[1] != 'fakefake'):
#	with open(sys.argv[1], 'r') as f:
#		l = f.readline()
#else:
#	l = "no incoming file"
#
#with open(sys.argv[2], 'w') as f:
#	f.write('first line: ' + l + '\n')
#	for i in xrange(1, 10):
#		f.write('All work and no play makes Jack a dull boy ' + str(i)
#		+ '\n')
