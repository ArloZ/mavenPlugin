#!/bin/bash

mvn clean

mvn com.arloz.maven:plugin-tools-statistic:sourceCount

echo ""
echo ""
echo "------------------------------------------------------------------------"
echo "                              Source Count"
echo "------------------------------------------------------------------------"

find ./ -name *source_count.txt | xargs tail -n 1 | awk -F ':' '{print $2 }' | grep '%' | awk -F',' 'BEGIN{total=0 ;source=0}{total+=$1;source+=$2} END {print "Total:" total ", Source:" source ", " source/total*100 "%" }'
