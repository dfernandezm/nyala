#!/bin/bash

#DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"
#. ${DIR}/../env/dev/aws-access-env.sh

DOCKER_TOKEN=$(aws ecr get-login --region eu-west-1 --no-include-email | awk -F ' ' '{print $6}')
echo $DOCKER_TOKEN

