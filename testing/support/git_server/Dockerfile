FROM alpine
MAINTAINER hypergig@gmail.com
RUN apk add --no-cache git-daemon
EXPOSE 9418
COPY etc /etc
WORKDIR /etc/git/test_repo_1 
RUN git --version \
    && git config --global user.email "robo-jenkins@robo-jenkins.foo" \
    && git config --global user.name "robo-jenkins" \
    && git init \
    && git add -v --all \
    && git commit -v -m 'first commit' \
    && git checkout --progress --detach
ENTRYPOINT ["git", \
            "daemon", \
            "--base-path=/etc/git", \
            "--export-all", \
            "--reuseaddr", \
            "--reuseaddr", \
            "--informative-errors", \
            "--verbose", \
            "--enable=receive-pack"]
