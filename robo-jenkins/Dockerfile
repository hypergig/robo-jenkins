FROM jenkins:2.19.4-alpine
MAINTAINER hypergig@gmail.com

# environmet vars
ENV JAVA_OPTS="-Djenkins.install.runSetupWizard=false" \
    GIT_COMMITTER_NAME="robo-jenkins" \
    GIT_COMMITTER_EMAIL="robo-jenkins@robo-jenkins.foo" \
    JENKINS_CONFIG_HOME="/usr/share/jenkins" \
    ROBO_REPO_REGISTRY="/usr/share/jenkins/userContent/repo_registry" \
    ROBO_JOB_TEMPLATES="/usr/share/jenkins/userContent/job_templates" \
    ROBO_DEFAULT_JOB="basic-docker" \
    DOCKER_VERSION="1.12.3" \
    DOCKER_SHA256="626601deb41d9706ac98da23f673af6c0d4631c4d194a677a9a1a07d7219fa0f" \
    DOCKER_URL="get.docker.com"

# packages and sudoers
USER root

# Install the docker client, as done by the offical docker library images
# This code is taken from https://github.com/docker-library/docker/blob/c1af76ec4c97ff24dcf6675b55b12105216dc711/1.12/Dockerfile,
# which is licenses Apache and is thus GPL compatible in this direction
RUN echo "Installing docker v${DOCKER_VERSION} client" && \
    curl -fSL "https://${DOCKER_URL}/builds/Linux/x86_64/docker-${DOCKER_VERSION}.tgz" -o /tmp/docker.tgz && \
    sha256sum /tmp/docker.tgz && \
    echo "${DOCKER_SHA256}  /tmp/docker.tgz" | sha256sum -c - && \
    cd /tmp && \
    tar -xzvf /tmp/docker.tgz && \
    mv /tmp/docker/* /usr/local/bin/ && \
    rm -rf /tmp/docker/ && \
    rm -f /tmp/docker.tgz && \
    chmod +x /usr/local/bin/docker && \
    docker --version

RUN apk add --no-cache sudo \
    && echo "jenkins ALL=NOPASSWD: ALL" >> /etc/sudoers

USER jenkins

# jenkins plugins
RUN /usr/local/bin/install-plugins.sh \
      job-dsl \
      git \
      cloudbees-folder \
      # todo - trigger the seed job another way and ditch this plugin
      startup-trigger-plugin \
      copy-to-slave \
      ws-cleanup \
      envinject

# run time requirements
COPY usr /usr
ADD https://repo1.maven.org/maven2/org/yaml/snakeyaml/1.17/snakeyaml-1.17.jar \
    ${JENKINS_CONFIG_HOME}/userContent/meta_jobs/libs/
