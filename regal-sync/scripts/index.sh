#! /bin/bash

source pidlist.sh

function index()
{
type=$1
user=$2
password=$3
server=$4
for i in `pidlist $type $server`
do
curl -u ${user}:${password} -XPOST http://$server/utils/index/$i;echo
done
}


index $1 $2 $3 $4
