#!/usr/bin/env python
from __future__ import print_function
import os
import datetime
import time
import sys
from argparse import ArgumentParser

def _parseArgs():
    parser = ArgumentParser(description='AWE test script.')
    parser.add_argument('--error', action='store_true',
                         help='cause a task error.')
    parser.add_argument('--delay', type=int,
                         help='wait for X seconds.')
    parser.add_argument('--infiles', nargs='*', help='input files')
    parser.add_argument('--outfiles', nargs='*', help='output files')
    parser.add_argument('-l', '--domain and use the standard gc.')
    return parser.parse_args()

if __name__ == '__main__':
    print('sys.argv: ' + str(sys.argv))
    args = _parseArgs()
    if (args.error):
        print("I was told to error out, now I'm printing to stdout")
        sys.stderr.write("I was told to error out, now I'm printing to stderr\n")
        sys.exit(1);
    if (args.delay > 0):
        time.sleep(args.delay)
    
    if (args.outfiles and len(args.outfiles) > 0):
        for i in xrange(len(args.outfiles)):
            with open(args.outfiles[i], 'w') as outfile:
                if (args.infiles and len(args.infiles) > i):
                    with open(args.infiles[1]) as infile:
                        outfile.write(infile.read())
                outfile.write(args.outfiles[i] + '\n')
                    

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
