// the seed job dsl script
import org.yaml.snakeyaml.Yaml


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
    def parent_folder = repo.split('/')[-2,-1].join('.')
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
        println "Processing branch: $branch"

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
