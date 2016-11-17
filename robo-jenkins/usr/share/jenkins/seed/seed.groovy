// the seed job dsl script


File file = new File("${WORKSPACE}/repo_list")
file.readLines().each { repo ->
    println "Processing repository: $repo"
    def (org, name) = repo.split('/')[-2,-1]
    String parent_folder = "$org-$name"

    folder(parent_folder) {
        //displayName('JOB!')
        description("Build jobs for $repo")
    }

    println "fetching branches"
    def process = "git ls-remote -h $repo".execute()
    process.waitForOrKill(5000)
    
    def stdout = new StringBuilder(), stderr = new StringBuilder()
    process.waitForProcessOutput(stdout, stderr)
    
    int rc = process.exitValue()
    if (rc) {
      println "failed to fetch branches:\n$stderr"
      return false
    }

    stdout.eachLine { line ->
        def branch = line.split('/')[-1]
        println "Processing branch: $branch"

        println "Creating job: $parent_folder/$branch"
        job("$parent_folder/$branch") {
            scm {
                git(repo, branch)
            }
            steps {
                // todo - modularize and template build steps 
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
    }
}
