#!/bin/sh
rm -rf .pio
pio run
cp .pio/build/bluepill-f103c8/firmware.bin bin/bluepill-f103c8.bin
cp .pio/build/blackpill-f401cc/firmware.bin bin/blackpill-f401cc.bin
cp .pio/build/blackpill-f411ce/firmware.bin bin/blackpill-f411ce.bin
cp .pio/build/disco_f723ie/firmware.bin bin/disco_f723ie.bin
