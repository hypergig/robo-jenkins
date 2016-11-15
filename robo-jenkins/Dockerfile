FROM jenkins:2.19.1-alpine
MAINTAINER hypergig@gmail.com

# environmet vars
ENV JAVA_OPTS="-Djenkins.install.runSetupWizard=false" \
    GIT_COMMITTER_NAME="robo-jenkins" \
    GIT_COMMITTER_EMAIL="robo-jenkins@robo-jenkins.foo"

# packages and sudoers
USER root
RUN apk add --no-cache docker sudo && echo "jenkins ALL=NOPASSWD: ALL" >> /etc/sudoers
USER jenkins

# jenkins plugins
RUN /usr/local/bin/install-plugins.sh \
      job-dsl \
      git \
      cloudbees-folder \
      startup-trigger-plugin

# run time requirements
COPY usr /usr
