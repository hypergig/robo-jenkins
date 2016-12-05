// the seed job dsl script
import org.yaml.snakeyaml.Yaml


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
def getRepoParentFolder(repo){
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
    //Remove git proto tokesns and convert slashes to dot
    return clean_repo_name.split(':')[1].tr('/','.')
  }
}

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

// ingest repo registry
def repo_registry = []
println 'Ingesting repo registry'
new File("${JENKINS_REPO_REGISTRY}").eachFileMatch(~/.*\.yml$/) {
    def repo_yml_path = it.toString()
    println "Adding repo yaml: $repo_yml_path"
    try {
        repo_registry.addAll(new Yaml().load(it.newReader()).repos)
    }
    catch(all) {
        println "Failed to ingest yml file: $repo_yml_path\n[$all]"
    }
}

println "Sanitizing repo registry"
// remove entries that don't have a url
repo_registry -= repo_registry.grep { ! it.url }
// remove duplicates based on url
repo_registry.unique { a, b -> a.url <=> b.url }

println 'The current repo registry is:'
println new Yaml().dumpAsMap(repo_registry)

// process each repo
repo_registry.each {
    repo = it.url
    println "Processing repository: $repo"

    parent_folder = getRepoParentFolder(repo)

    folder(parent_folder) { description("Build jobs for $repo") }

    // todo - use github api
    println "Fetching branches"
    def process = "git ls-remote -h $repo".execute()
    process.waitForOrKill(5000)

    def branches = new StringBuilder(), stderr = new StringBuilder()
    process.waitForProcessOutput(branches, stderr)

    int rc = process.exitValue()
    if (rc) {
      println "Failed to fetch branches:\n$stderr"
      return false
    }

    // process each branch in each repo
    branches.eachLine {
        branch = it.split('/')[-1]
        println "Processing branch: $branch of $repo"
        job_name = "$parent_folder/$branch"
        println "Processing job: $job_name"

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
