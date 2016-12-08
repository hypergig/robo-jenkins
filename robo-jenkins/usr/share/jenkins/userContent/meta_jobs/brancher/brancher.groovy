// the brancher job dsl script

/**
 * Parse the git transpart protocol from a repository URL.
 * Currently assumes git if the url starts with 'git' and HTTPS otherwise
 * @param repo Repository URL to parse.
 */
def getRepoProto(repo) {
  if (repo.startsWith('git')){
    return "git"
  }
  else {
    return "https"
  }
}

/**
 * Transforms a git repository URL into a form that is suitable for use as a
 * Jenkins folder name.  This remove all protocol specific tokens
 * (like 'https://...' or 'git@...'), drops '.git' from the end of the URL if present, and
 * replaces all slashes with dots.
 *
 * @param repo Repository url to transform
 */
def getRepoFriendlyName(repo){
  clean_repo_name = repo

  // Remove trailing '.git' if present
  if (clean_repo_name.endsWith('.git')){
    clean_repo_name = clean_repo_name[0..-5]
  }

  if (getRepoProto(repo).equals('https')){
    //Remove https proto tokens and convert slashes to dot
    return clean_repo_name.split('/')[-2,-1].join('.')
  }
  else {
    //Remove git proto tokens and convert slashes to dot
    return clean_repo_name.split(':')[1].tr('/','.')
  }
}

// add an execute helper method
String.metaClass.executeHelper = { wd = '/' ->
    def process = delegate.execute(null, new File(wd))
    process.waitForOrKill(5000)

    def stdout = new StringBuilder(), stderr = new StringBuilder()
    process.waitForProcessOutput(stdout, stderr)
    
    stdout = stdout.toString()
    stderr = stderr.toString()
    int rc = process.exitValue()
    
    if (rc) {
        throw(new Throwable("rc: $rc\nstderr: $stderr"))
    }
    [stdout, stderr]
}

// get my repo registry entry
my_repo_registry_entry = Eval.me("${MY_REPO_REGISTRY_ENTRY}")
println "My repo registry entry:\n$my_repo_registry_entry"

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

// discover remote branches
println "Discovering branches"
def branches = [:]
def remote_branches_refs = "${WORKSPACE}/repo/.git/refs/remotes/origin/"
new File(remote_branches_refs).eachFileRecurse {
    def ref_path = it.toString()
    branches.put(ref_path - remote_branches_refs,
        readFileFromWorkspace(ref_path))
}

repo = "${GIT_URL}"
println "Branches in $repo:"
branches.each{ println it }

// create parent folder, which will hold all jobs for this repo
parent_folder = getRepoFriendlyName(repo)
folder(parent_folder) { description("Build jobs for $repo") }

// process each branch in each repo
branches.each{
    branch = it.key
    sha = it.value
    println "Processing branch: $branch ($sha) of $repo"
    println "git checkout -f $sha".executeHelper("${WORKSPACE}/repo")[1]

    job_name = "$parent_folder/$branch"
    println "Processing job: $job_name"

    // coming soon - more job templates and target override mechanic
    def target_job_template = "${ROBO_DEFAULT_JOB}"
    println "Target job template: $target_job_template"

    // create build job from template
    job_template_code = job_template_cache[target_job_template]
    def wrapper = """
        metaClass.'static'.println = { Object o -> x.println o }
        x.with { $job_template_code }"""
    Eval.x(this, wrapper)
}
