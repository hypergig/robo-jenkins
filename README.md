# robo-jenkins
Using docker to hyper modularize Jenkins


## Stay tuned!
Robo-jenknins is underdevelopment and is considered only a POC as of now


## What works?
* Boilerplate dockerized Jenkins master
* Sane defaults
* Working use of local `docker.sock`
* Working config.d framework for easy startup bashing
* Basic yaml based repo registry mechanic with job template override support
* Framework for repo level configuration file, ie `.robo` file 
* Automation that creates one build job for every branch in every repo in the repo registry
* Working scaffolding for supporting different and pluggable job templates
* Working job template override from repo registry or `.robo` file
* Job template that builds all `Dockerfiles`
* Local private docker registry and git server for intermediate steps and local testing of build pipelines
* Repo management including version git tagging and build once releases


## Local Environment
You can get a local environment of robo-jenkins up and running pretty easily with docker compose:
```
docker-compose build
docker-compose up
```
Don't forget to properly bring down the environment in between builds:
```
docker-compose down -v
```
The `-v` is important as it will delete the jenkins home volume, ensuring your start fresh the next time.

The local environment includes a robo-jenkins master, a docker registry, and a git server... everything you need for testing.  You probably should set `ROBO_NO_REPO_MANAGEMENT` to `true` if you are testing with "real" repos, you don't want to push up or blow out robo tags from your local test environment. Unless you want to.

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


## Repo Registry
The repo registry is an abstract concept, in it's simplest form it is just a list of github repositories you want robo-jenkins to consider for build jobs. Support for non-github repos such as local repos are coming soon.

Currently it is just a directory located at `ROBO_REPO_REGISTRY` (`/usr/share/jenkins/userContent/repo_registry`) which is sniffed out by the seed job.  Just drop yaml files into this directory containing a list of github repos (urls) you would like robo-jenkins to consider for ingestion. The seed job aggregates all the yaml files in this directory, does some sanitation (removes repos with out a url / dedup), and creates a "master list" of repos for it to work off of. The contents in the `ROBO_REPO_REGISTRY` directory can be modified at anytime, the next run of the seed job will do the right thing (mostly). An example of this might look like:
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


## Robo file
The `.robo` file is a special file that contains some configuration to help robo-jenkins make certain decisions. It lives at the root of the repo and can be different in every branch. Right now it doesn't do too much, but you can use it to override the job template target for your branch. Check out the [overrides](#overrides) section below for more information.


## Job Template
Job templates are used by the brancher jobs to spawn the actual build jobs that build things.  A job template is just directory of some scripts, each serves a purpose. Currently only two are supported:
* `job.groovy` - the jobs dsl code used to spawn the build job
* `get-version.groovy` - a script that is evaluated at the beginning of the build that returns the version number of this commit

The file structure for a job template called `example` might look like:
```
robo-jenkins/usr/share/jenkins/userContent/job_templates/example/
├── get-version.groovy
└── job.groovy
```

Example of `get-version.groovy`:
```
// read a version file from the root of the repo
def ver_file = new File("$WORKSPACE/version.txt")
return ver_file.exists() ? ver_file.text.trim() : null
```

Example of a `job.groovy`:
```
// example build job
job.with {
    // todo - more stuff
    steps {
        shell('echo I don't do very much')
    }
}
```

Job templates get a few things by default. The job itself is precooked by the brancher and passed via the `job` parameter. This conveniently sets the scm for the repo and the job name, so it isn't needed in the template. Also, if the repo is managed, the brancher cooks in what is needed for pushing tags, checking releases, etc.

The `job.groovy` script should be written in accordance with the Jenkins [`job-dsl-plugin`](https://github.com/jenkinsci/job-dsl-plugin/wiki) and dropped into the `ROBO_JOB_TEMPLATES` (`/usr/share/jenkins/userContent/job_templates/`) directory. The code will be evaluated _almost_ as if it was part of the brancher job. All methods of the jobs dsl [api](https://jenkinsci.github.io/job-dsl-plugin) should work barring any dsl specific to a particular Jenkins plugin that has not been installed. All templates in this directory will be considered when a brancher job runs, the directory name is used as the template name.

### External Job Template Repo
Robo-jenkins supports the use of external job templates by way of git. By default, robo-jenkins will ship with a few ultra generic job templates, but in must situations a Jenkins administration would rather user their own. This is easily done by setting the `ROBO_JOB_TEMPLATE_REPO` environment var to the git url of your external job template repo. Robo-jenkins will ingest all the templates at the master branch of this repo **after** it ingests the built in templates. This is an important point, as it allows you to override the built in templates with your own, so long as they are of the same name.

### Overrides
The target job template is determined when a brancher job processes a branch of a repo. By default the template set in the `ROBO_DEFAULT_JOB` environment variable is used, which also by default, is set to `basic-docker`. You can override this variable on the command line when you start robo-jenkins to any job template it has access to. Furthermore, you can override your target per repo right in the repo registry yaml.  This is easily done by setting the `robo-job` key to your template. Note, this overrides **all** the branches in said repo. An example of this may look like:
```
---
repos:
  - url: https://github.com/hypergig/robo-jenkins
    robo-job: basic-docker
```
But wait there's more! Additionally, you can override the target job template from the `.robo` file. This override wins out over all other forms of targeting. The cool thing about overriding your template this way is, you can use different job templates per branch in your repo, as you can have different `.robo` files in all your branches. An example of this may look like:
```
---
version: 1.0.0-SNAPSHOT
robo-job: basic-docker
```
In summary, the job template override mechanic works like this:
`.robo` file > repo registry > `ROBO_DEFAULT_JOB` environment variable

## Seed Job
The seed job is the first Jenkins job to be created and is responsible for sparking the all job automation in robo-jenkins.  It is boot strapped at startup from xml, then trigged when Jenkins starts. It ingests the repo registry and creates a Jenkins folder and bracher job for each repo. 

### Triggers
* on robo-jenkins startup
* on repo registry changes _(not implemented)_

## Brancher Job
The brancher jobs are created by the seed job, and are responsible for processing all the branches in it's repo. It will iterate through each branch, determine what job template to use, and create the resulting build job. Determining the job template can happen one of three ways: From the the repo registry, from the `.robo` file at the root of the repo, or by auto discovery _(not implemented)_. The brancher is to uphold the brancher contract with the job templates.

### Triggers
* on external job template repo _(not implemented)_ changes via git hook
* on repo changes via git hook
* queued by the seed job

### Brancher Contract
* The brancher job is responsible for executing a job template's code for each branch in a given repo
* The brancher job is responsible for determining the correct job template to use for each branch
  * via the repo registry
  * via the `.robo` file
  * via auto discovery _(not implemented)_
* Job templates are responsible for creating the "real" build jobs, including the build steps, publishers, etc.
* Job templates may provide a `get-version.groovy` script which should return a version number if ran at the root of the repo
* The following variables are injected into the build job:
  * `ROBO_FILE_MAP`
  * `IS_SEMVER`
  * `IS_RELEASE`
  * `IS_SNAPSHOT`
  * `VERSION`
  * `IS_REPO_MANAGED`
  * `MY_REPO_REGISTRY_ENTRY`
  * `ROBO_DEFAULT_JOB`
  * `ROBO_REPO_REGISTRY`
  * `ROBO_NO_REPO_MANAGEMENT`
  * `ROBO_JOB_TEMPLATES`
  * `ROBO_JOB_TEMPLATE_REPO`


## Repo Management
Repos can be managed by robo-jenkins. If robo-jenkins has push access to the repo, it will git tag every build with the version number and only build release versions once. It follows the rules of [semantic versioning](http://semver.org/) in order to make those determinations. You can turn repo management off by setting the environment variable `ROBO_NO_REPO_MANAGEMENT` to `true`. Good practice for local testing.


## Configuration and Secrets
Most configuration for robo-jenkins is done using environment variables.  While
these can be added directly to the environment key of a docker compose file, this is not appropriate for secrets or other developer specific configuration.

Instead, these types of variables should be added to `testing/data/developer.env`.  This is a standard docker-compose env file that is sourced by this project's compose files.  This file is mandatory.  An example developer.env that contains all possible variables is provided at `testing/data/developer.env.template`.  Before using this project to build projects that require private resources, you must set whatever variables are required to perform your builds in developer.env.  This will generally at least be the `GIT_HTTPS_USER`, `GIT_HTTPS_PW`, and `GIT_HOST` variables for builds requiring private git repositories and the REGISTRY_USER, REGISTRY_PW, and REGISTRY_HOST variables for build requiring private Docker images.  See `testing/data/developer.env.template` for more information.


## Issues
* ~~No cleanup of non used jobs and repos yet~~
* Not the best vetting of github repos (urls)
* ~~No overrides or other types of build jobs~~
* Just basic docker job template for now


## What's Next?
* ~~A "repo registry" and the ability to register repos through a docker volume~~
* ~~Proper templated, dynamic, build job pipelines~~ _sorta done, need more templates_
* ~~Override target job template~~
* Auto discover target job template
* More jobs templates
* Org scrapper


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
