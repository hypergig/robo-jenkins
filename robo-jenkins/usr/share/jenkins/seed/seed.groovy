// the seed job dsl script

job('nginx-test') {
    scm {
        git('https://github.com/nginxinc/docker-nginx')
    }
    steps {
        shell('set -e; env | sort; docker build stable/alpine/.')
    }
}
