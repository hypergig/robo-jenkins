# robo-jenkins
Using docker to hyper modularize Jenkins

## Stay tuned!
Robo-jenknins is underdevelopment and is considered only a POC as of now 

## What works?
* Boilerplate dockerized Jenkins master
* Sane defaults
* Working use of local `docker.sock`
* Example build of official nginx container
* Working config.d framework for easy bashing

## What's Next?
Build on the concept of a "repo registry" with auto-discoverable local repos via a docker volume

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
