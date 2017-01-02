#!/bin/bash

# first step for all managed repo builds
# only ever build releases once
if [ $IS_RELEASE == 'true' ]
then
    if [ $(git tag --list robo-ver-$VERSION | wc -l ) != 0 ]
    then
        echo 'Release already built'
        exit 1
    fi
fi
