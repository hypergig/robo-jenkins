# robo-jenkins
Using docker to hyper modularize Jenkins

## Stay tuned!
Robo-jenknins is underdevelopment and is considered only a POC as of now 

## What works?
* Boilerplate dockerized Jenkins master
* Sane defaults
* Working use of local `docker.sock`
* Working config.d framework for easy startup bashing
* Basic repo registry mechanic
* Build all `Dockerfiles`, in all branches, in all repos, in the repo registry

### Repo Registry
The repo registry is an abstract concept, in it's simplest form it is just a list of github repositories you want rebo-jenkins to consider for build jobs. Support for non-github repos such as local repos are coming soon.

Currently it is just a directory located at `JENKINS_REPO_REGISTRY`, defaulted to `/usr/share/jenkins/repo_registry` which is sniffed out by the seed job.  Just drop text files into this directory containing a list of github repos (urls), one per line, you would like robo-jenkins to consider for ingestion.   A subprocess aggregates all the files in this directory recursively, does some sanitation (removes `#comments` and extraneous white spaces), dedups, and creates a "master list" of repos for the seed job to consume and work off of. The contents in the `JENKINS_REPO_REGISTRY` directory can be modified at anytime, the next run of the seed job will do the right thing (mostly).

#### Issues
* No cleanup of non used jobs and repos yet
* Not the best vetting of github repos (urls) 

## What's Next?
* ~~Build on the concept of a "repo registry" with auto-discoverable local repos via a docker volume~~
* Proper templated, dynamic, build job pipelines

## Where is this going?
Robo-jenkins will be a CI/CD platform with as little Jenkins Koolaid as possible.  Born from the pain of maintaining a Jenkins environment, with hundreds of build job types, and plugins, and conflicting requirements... robo-jenkins leverages docker to abstract out and distribute complexity from Jenkins to other places.  Mainly, docker containers which can be crated by anyone, enabling others to partake in the joys of Jenkins administration without actually needing to touch Jenkins.

## Key points
* All build steps will be distilled into docker runs, builds, and pushes (expect for meta-jobs)
* Minimal Jenkins plugins
* No groovy necessary!
* Relatively small footprint
* Just add docker! (via `docker.sock`)
* Run robo-jenkins locally for your own personal build environment
* Run robo-jenkins in a docker swarm as a single master
* Run robo-jenkins in multi docker daemon configurations via generic robo-slaves 
* Standards for environment vars and volumes
* Distributed, simple, friendly yaml configuration of build jobs
* Support for upstream links
* Support for building all types of applications, not just docker
* Autodiscover all the things
* Default all the things
* Override only the things you need to
