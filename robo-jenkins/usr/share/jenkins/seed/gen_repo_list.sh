#!/bin/bash
set -e

## todo - dekludge this and move to groovy seed job

# find all files not .md
# grep out comments and empty lines
# sed out leading spaces
# sed out trailing spaces
# sort and dedup
# save to file

echo "repo registry: $JENKINS_REPO_REGISTRY"
find $JENKINS_REPO_REGISTRY -type f ! -name '*.md' \
| xargs grep -hvE "^#.*$|^\s*$" \
| sed 's/^\s*//' \
| sed 's/\s*$//' \
| sort -u \
> $WORKSPACE/repo_list

echo "The current repo list is:"
cat $WORKSPACE/repo_list
