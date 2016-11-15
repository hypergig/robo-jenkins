#!/bin/bash
set -e

ls /usr/share/jenkins/config.d/*.sh | sort -n | while read script
  do
      echo "----> executing \"$script\""
      source $script
      echo "----> executing \"$script\"... done"
  done
