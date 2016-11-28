job("$job_name") {
    println "Creating basic docker job: $job_name"
    scm {
        git(repo, branch)
    }
    steps {
        // todo - convert shell step to docker step
        shell("""
                bash -c '
                    set -e
                    echo \"---| environment variables |---\"
                    env | sort
                    contexts=\"\$(find . -type f -name Dockerfile -exec dirname {} \\;)\"
                    echo \"---| docker contexts |---\"
                    echo \"\$contexts\"
                    while read context
                      do
                        echo \"---| building docker context \$context |---\"
                        docker build \"\$context\" && {
                            echo \"---| successfully built context \$context |---\"; } || {
                            echo \"---| failed to build context \$context |---\"; }
                    done <<< \"\$contexts\"
                '
            """)
    }
}
