#! /bin/bash

#curl -XGET http://localhost/resource/ 2> /dev/null| sed s/"\,"/"\n"/g | sed s/"\"\([^\"]*\)".*/"\1"/ | tail -n +2 > allPids.txt 
#echo Found `wc -l allPids.txt` resources
#curl -XGET http://localhost/monograph/ 2> /dev/null| sed s/"\,"/"\n"/g | sed s/"\"\([^\"]*\)".*/"\1"/ | tail -n +2 > monographPids.txt  
#echo Found `wc -l monographPids.txt` monographs
#curl -XGET http://localhost/journal/ 2> /dev/null | sed s/"\,"/"\n"/g | sed s/"\"\([^\"]*\)".*/"\1"/ | tail -n +2 > journalPids.txt
#echo Found `wc -l journalPids.txt` journals
#curl -XGET http://localhost/webpage/ 2> /dev/null| sed s/"\,"/"\n"/g | sed s/"\"\([^\"]*\)".*/"\1"/ | tail -n +2 > webpagePids.txt 
#echo Found `wc -l webpagePids.txt` webpages


function pidlist()
{
type=$1
server=$2
curl -XGET http://$server/$type/ 2> /dev/null| sed s/"\,"/"\n"/g | sed s/"\"\([^\"]*\)".*/"\1"/ | tail -n +2
echo
}

