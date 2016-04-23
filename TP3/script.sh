#!/bin/bash

machines=`cat $OAR_FILE_NODES | uniq`
i=0
j=200
mpi=10000
for m in $machines; do

oarsh $m ./calculpi $i $j 'resinter.txt' > $m &

i=$(($i + 200))
j=$(($j + 200))

done


