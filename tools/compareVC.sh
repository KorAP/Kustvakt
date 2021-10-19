#!/bin/bash

printHelp(){
  echo "Please use the following command:"
  echo "  ./compareVC.sh [Cosmas2 VC-file] [VC-file]"
  echo ""
  echo "The Cosmas2 virtual corpus should have the following format: "
  echo "  <text>DOL00/JAN.00504</text>"
  echo ""
  echo "The other VC should contain a simple list of text Sigle, i.e. one text sigle per line. In the following format:"
  echo "  DOL00/APR/00055"
}

vc1=$1
vc2=$2

if [ -z $1 ]||[ -z $2 ];
then
    printHelp
    exit
fi

firstLine="$(head -n 1 $vc1)"

if ! [[ $firstLine =~ ^\<text\> ]];
then
  printHelp
  exit
fi

cat $vc1 | sed -E 's/<\/?text>//g' - |  sed 's/\./\//' -| sort > vc1

cat $vc2 | sort > vc2

echo $1
wc -l vc1
echo $2
wc -l vc2

meld vc1 vc2
