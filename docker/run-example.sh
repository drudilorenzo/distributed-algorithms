#!/bin/bash

# The first parameter is the name of the local directory that contains the
# source code of the project. You must specify the local absolute path.

docker run \
--rm \
--name da_container \
-v /home/drudao/Desktop/EPFL/Year_I/DistributedAlgorithms/Project/distributed-algorithms/:/root/da \
-w /root/da/project \
-it da_image /bin/bash
