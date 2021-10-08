#!/bin/bash

vc1=$1
vc2=$2

if [ -z $1 ]||[ -z $2 ];
then
    echo "Please use the following command:"
    echo "./compareVC.sh [virtual-corpus-file-1] [virtual-corpus-file-2]"
    exit
fi

cat $vc1 | sed -E 's/<\/?text>//g' - |  sed 's/\./\//' -| sort > vc1

cat $vc2 | sort > vc2

echo $1
wc -l vc1
echo $2
wc -l vc2

meld vc1 vc2
