import robo.RoboUtil
// the brancher job dsl script

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
parent_folder = RoboUtil.getRepoFriendlyName(repo)
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
