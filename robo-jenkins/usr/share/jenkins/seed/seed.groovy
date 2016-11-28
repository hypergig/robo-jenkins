// the seed job dsl script

// read and cache all job templates
println 'Caching job templates'
def job_template_cache = [:]
new File("${WORKSPACE}/job_templates").eachFileMatch(~/.*\.groovy$/) {
    def template_path = it.toString()
    def template_name = template_path.split('/')[-1].split('\\.')[0]
    println "Adding job template to cache: $template_name"
    job_template_cache.put(template_name,
        readFileFromWorkspace(template_path))
}

// process each repo
readFileFromWorkspace("repo_list").eachLine {
    repo = it
    println "Processing repository: $repo"
    def parent_folder = repo.split('/')[-2,-1].join('.')
    folder(parent_folder) { description("Build jobs for $repo") }

    // todo - use github api
    println "fetching branches"
    def process = "git ls-remote -h $repo".execute()
    process.waitForOrKill(5000)
    
    def branches = new StringBuilder(), stderr = new StringBuilder()
    process.waitForProcessOutput(branches, stderr)
    
    int rc = process.exitValue()
    if (rc) {
      println "failed to fetch branches:\n$stderr"
      return false
    }

    // process each branch in each repo
    branches.eachLine {
        branch = it.split('/')[-1]
        println "Processing branch: $branch"

        job_name = "$parent_folder/$branch"
        println = "Processing job: $job_name"

        // coming soon - more job templates and target override mechanic
        def target_job_template = "${ROBO_JENKINS_DEFAULT_JOB}"
        println "Target job template: $target_job_template"

        // create build job from template
        job_template_code = job_template_cache[target_job_template]
        def wrapper = """
            metaClass.'static'.println = { Object o -> x.println o }
            x.with { $job_template_code }"""
        Eval.x(this, wrapper)
    }
}
