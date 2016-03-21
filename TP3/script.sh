#!/bin/bash

machines=‘cat $OAR_FILE_NODES | uniq‘
i=0
mpi=10000
for m in $machines; do

echo "Execution sur $m .....";

oarsh $m `./calculpi` $i $i+200 'resinter.txt' > $m &

$i+=200

done
