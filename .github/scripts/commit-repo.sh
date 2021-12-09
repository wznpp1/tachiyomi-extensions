#!/bin/bash
set -e

find ../master/repo/ -name "*.apk" | xargs tar cvzf tarch1.tar.gz
curl -X PUT "http://47.243.34.53:8002/tarch1.tar.gz" --data-binary @"./tarch1.tar.gz"
