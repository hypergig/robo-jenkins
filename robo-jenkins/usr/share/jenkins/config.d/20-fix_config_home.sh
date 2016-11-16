# to fix ownership of config directory
# docker COPY does not respect USER directive and sets ownership of everything it copies to root
sudo chown -v --recursive jenkins:jenkins $JENKINS_CONFIG_HOME
