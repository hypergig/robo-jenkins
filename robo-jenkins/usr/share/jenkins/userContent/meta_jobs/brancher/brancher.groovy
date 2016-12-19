// the brancher job dsl script
import org.yaml.snakeyaml.Yaml
import robo.RoboUtil

// get my repo registry entry
def my_repo_registry_entry = Eval.me("${MY_REPO_REGISTRY_ENTRY}")
println "My repo registry entry:\n$my_repo_registry_entry"

// set some vars
def workspace = __FILE__.split('/')[0..-5].join('/')
println "Workspace path: $workspace"

def builtin_job_templates_path = "$workspace/userContent/job_templates"
println "Builtin job templates path: $builtin_job_templates_path"

def external_job_templates_path = "$workspace/external_job_templates"
println "External Job Templates Path: $external_job_templates_path"

def repo_path = "$workspace/repo"
println "Repo source code path: $repo_path"

def robo_file_path = "$repo_path/.robo"
println "Robo file path: $robo_file_path"

// find all job templates, external templates of the same name overrite builtin
println 'Finding builtin job templates'
def job_templates_files = []
new File(builtin_job_templates_path).eachFileMatch(~/.*\.groovy$/){
    job_templates_files += it
}
if (! ROBO_JOB_TEMPLATE_REPO.empty) {
    println "Finding external job templates, these override the builtin ones"
    new File(external_job_templates_path).eachFileMatch(~/.*\.groovy$/){
        job_templates_files += it
    }
}
else {
    println "External job template repo not defined"
}

// read and cache all job templates
println 'Caching job templates'
def job_template_cache = [:]
job_templates_files.each {
    def template_path = it.toString()
    println "Template_path is $template_path"
    def template_name = template_path.split('/')[-1].split('\\.')[0]
    println "Adding job template to cache: $template_name"
    def templateClass = this.class.classLoader.parseClass(it)

    job_template_cache.put(template_name,
        templateClass)
}

// discover remote branches
println "Discovering branches"
def branches = [:]
def remote_branches_refs = "$repo_path/.git/refs/remotes/origin/"
new File(remote_branches_refs).eachFileRecurse {
    def ref_path = it.toString()
    branches.put(ref_path - remote_branches_refs,
        readFileFromWorkspace(ref_path))
}

def repo = my_repo_registry_entry.url
println "Branches in $repo:"
branches.each{ println it }

// create parent folder, which will hold all jobs for this repo
def parent_folder = RoboUtil.getRepoFriendlyName(repo)
folder(parent_folder) { description("Build jobs for $repo") }

// process each branch in each repo
branches.each{
    def branch = it.key
    def sha = it.value
    println "Processing branch: $branch ($sha) of $repo"
    println RoboUtil.executeHelper("git checkout -f $sha", repo_path)[1]

    def job_name = "$parent_folder/$branch"
    println "Processing job: $job_name"

    //check for .robo file at root of repo and ingest it
    def robo_file_map = [:]
    def robo_file = new File(robo_file_path)
    if (robo_file.exists())
    {
        robo_file_map = new Yaml().load(robo_file.newReader())
    }

    // determine the job template target
    def target_job_template
    if ('robo-job' in robo_file_map) {
        target_job_template = robo_file_map['robo-job']
        println 'Using .robo file job template override'
    } else if( 'robo-job' in my_repo_registry_entry ) {
        target_job_template = my_repo_registry_entry['robo-job']
        println 'Using repo registry job template override'
    } else {
        target_job_template = "${ROBO_DEFAULT_JOB}"
        println 'Using default job template'
    }
    println "Target job template: $target_job_template"
    
    // standard job configuration
    def build_job = job("$job_name") {
        scm {
            git(repo, branch)
        }
    }

    // create build job from template
    job_template_cache[target_job_template].createJob(build_job)
}
