#!/bin/bash

# The first parameter is the name of the local directory that contains the
# source code of the project. You must specify the local absolute path.

docker run \
-v /home/drudao/Desktop/EPFL/Year_I/DistributedAlgorithms/Project/distributed-algorithms/java:/root/java \
-w /root/java \
-it da_image /bin/bash 
