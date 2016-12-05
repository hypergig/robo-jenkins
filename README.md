# robo-jenkins
Using docker to hyper modularize Jenkins.  Currently installs a docker 1.12.3 client from get.docker.com

## Stay tuned!
Robo-jenknins is underdevelopment and is considered only a POC as of now

## What works?
* Boilerplate dockerized Jenkins master
* Sane defaults
* Working use of local `docker.sock`
* Working config.d framework for easy startup bashing
* Basic yaml based repo registry mechanic
* Basic automation that creates one build job for every branch in every repo in the repo registry
* Working scaffolding for supporting different and pluggable job templates
* Job template that builds all `Dockerfiles`
* Local private docker registry for intermediate steps and local testing of build pipelines

### Repo Registry
The repo registry is an abstract concept, in it's simplest form it is just a list of github repositories you want rebo-jenkins to consider for build jobs. Support for non-github repos such as local repos are coming soon.

Currently it is just a directory located at `JENKINS_REPO_REGISTRY`, defaulted to `/usr/share/jenkins/repo_registry` which is sniffed out by the seed job.  Just drop yaml files into this directory containing a list of github repos (urls) you would like robo-jenkins to consider for ingestion. A subprocess aggregates all the yaml files in this directory, does some sanitation (removes repos with out a url / dedup), and creates a "master list" of repos for the seed job to consume and work off of. The contents in the `JENKINS_REPO_REGISTRY` directory can be modified at anytime, the next run of the seed job will do the right thing (mostly).

#### Example
```
---
# a few repos to test against
repos:
  - url: https://github.com/docker-library/buildpack-deps
  - url: https://github.com/docker-library/django
  - url: https://github.com/docker-library/docs
  - url: https://github.com/docker-library/elasticsearch
  - url: https://github.com/docker-library/gcc
  - url: https://github.com/docker-library/ghost
```

### Job Template
Build jobs can be created and used as templates. They should be written in accordance with the Jenkins [`job-dsl-plugin`](https://github.com/jenkinsci/job-dsl-plugin/wiki) and dropped into the `/usr/share/jenkins/seed/job_templates/` directory. The code will be evaluated _almost_ as if it was part of the seed job. All methods of the jobs dsl [api](https://jenkinsci.github.io/job-dsl-plugin) should work barring any dsl specific to a particular Jenkins plugin that has not been installed. All templates in this directory will be considered when the seed job runs.

### Configuration and Secrets
Most configuration for robo-jenkins is done using environment variables.  While
these can be added directly to the environment key of a docker compose file, this is not appropriate for secrets or other developer specific configuration.

Instead, these types of variables should be added to `testing/data/developer.env`.  This is a standard docker-compose env file that is sourced by this project's compose files.  This file is mandatory.  An example developer.env that contains all possible variables is provided at `testing/data/developer.env.template`.  Before using this project to build projects that require private resources, you must set whatever variables are required to perform your builds in developer.env.  This will generally at least be the `GIT_HTTPS_USER`, `GIT_HTTPS_PW`, and `GIT_HOST` variables for builds requiring private git repositories and the REGISTRY_USER, REGISTRY_PW, and REGISTRY_HOST variables for build requiring private Docker images.  See `testing/data/developer.env.template` for more information.

### Local private registry
A local private docker regsitry is provided by the registry service.  It is configured to listen on localhost:5000 on the docker host that is running the compose file.  To push an image to the local private registry, you must tag it with localhost:5000/${image name}:{image tag}.  For example:
```
docker build -t localhost:5000/app_name:master
docker push localhost:5000/app_name:master
```

### Dind alternative
The default docker-compose.yml does docker builds using the same docker daemon that is running Jenkins.  There is an alternative compose file, docker-compose.dind.yml, that instead uses docker dind.  You can use it instead by passing `-f docker-compose.dind.yml` to all docker compose commands.
Example:
```
docker-compose -f docker-compose.dind.yml build
docker-compose -f docker-compose.dind.yml up
```

By default, this will use a docker daemon with the VFS storage driver.  This works everywhere, but is very slow.  It is recommended to set the storage driver to overlay or overlay2 for more performant builds.  You can do this by setting the command key docker-compose.dind.yml to `--storage-driver=overlay` or `--storage-driver=overlay2`.  However, this requires that the docker daemon that is running the dind container have the same storage driver set.  This is non trivial on docker for Mac, but can be done as in http://stackoverflow.com/questions/39455764/change-storage-driver-for-docker-on-os-x


## Issues
* No cleanup of non used jobs and repos yet
* Not the best vetting of github repos (urls)
* No overrides or other types of build jobs, just basic docker for now

## What's Next?
* ~~A "repo registry" and the ability to register repos through a docker volume~~
* ~~Proper templated, dynamic, build job pipelines~~ _sorta done, need more templates_
* Discover and override target job template
* More jobs templates

## Where is this going?
Robo-jenkins will be a CI/CD platform with as little Jenkins Koolaid as possible.  Born from the pain of maintaining a Jenkins environment, with hundreds of build job types, and plugins, and conflicting requirements... robo-jenkins leverages docker to abstract out and distribute complexity from Jenkins to other places.  Mainly, docker containers which can be crated by anyone, enabling others to partake in the joys of Jenkins administration without actually needing to touch Jenkins.

## Key points
* All build steps will be distilled into docker runs, builds, and pushes (expect for meta-jobs)
* Minimal Jenkins plugins
* Relatively small footprint
* Just add docker! (via `docker.sock`)
* Run robo-jenkins locally for your own personal build environment
* Run robo-jenkins in a docker swarm as a single master
* Run robo-jenkins in multi docker daemon configurations via generic robo-slaves
* Standards for environment vars and volumes
* Support for upstream links
* Support for building all types of applications, not just docker
* Autodiscover all the things
* Default all the things
* Override only the things you need to

## Maybe one day
* No groovy necessary!
* Distributed, simple, friendly yaml configuration of build jobs
