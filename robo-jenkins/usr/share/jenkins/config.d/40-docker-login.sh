#!/bin/bash -eu

set -e
set -u

if [ ! -z ${REGISTRY_PW:-} ]; then
  docker login -u ${REGISTRY_USER} -p ${REGISTRY_PW} ${REGISTRY_HOST}
fi
