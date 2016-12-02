#!/bin/bash -eu

set -e
set -u

if [ ! -z ${GIT_HTTPS_PW:-} ]; then
  git config --global credential.helper 'store --file ~/.git_https_credentials'
  echo "https://${GIT_HTTPS_USER}:${GIT_HTTPS_PW}@${GIT_HOST}" > ~/.git_https_credentials
fi
