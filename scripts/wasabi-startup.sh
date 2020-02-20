#!/bin/bash
echo "wasabi app started " $(date) $VLC_PLUGIN_PATH >> /home/pi/boot.txt

cd /home/pi/wasabi-embedded/
./run  &
cd wakeword-app
./run 


