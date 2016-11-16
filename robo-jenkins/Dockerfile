FROM jenkins:2.19.2-alpine
MAINTAINER hypergig@gmail.com

# environmet vars
ENV JAVA_OPTS="-Djenkins.install.runSetupWizard=false" \
    GIT_COMMITTER_NAME="robo-jenkins" \
    GIT_COMMITTER_EMAIL="robo-jenkins@robo-jenkins.foo" \
    JENKINS_CONFIG_HOME="/usr/share/jenkins" \
    JENKINS_REPO_REGISTRY="/usr/share/jenkins/repo_registry"

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
