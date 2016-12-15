import robo.RoboUtil

class BasicDockerJob {
    static createJob(dslFactory, job_name, repo, branch){
        dslFactory.job("$job_name") {
            println "Creating basic docker job: $job_name"
            def app_name = RoboUtil.getRepoName(repo)
            def app_org = RoboUtil.getRepoOrg(repo)
            def app_tag = RoboUtil.getBuildId(dslFactory.binding.variables)
            println "$app_org/$app_name:$app_tag"
            scm {
                git(repo, branch)
            }
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
