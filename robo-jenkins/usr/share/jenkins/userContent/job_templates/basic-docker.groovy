// basic docker job

class BasicDockerJob {
    static void createJob(job) {
        job.with {
            steps {
                // todo - convert shell step to docker step
                shell("""
                        bash -c '
                            set -e
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
    }
}
