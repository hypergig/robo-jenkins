// the brancher job dsl script
// todo - break this up into multiple files and/or methods
import org.yaml.snakeyaml.Yaml
import robo.RoboUtil


println RoboUtil.header('Variables')
// get my repo registry entry
def my_repo_registry_entry = Eval.me("${MY_REPO_REGISTRY_ENTRY}")
println "My repo registry entry:\n$my_repo_registry_entry"

// repo associated with this brancher
def repo = my_repo_registry_entry.url
println "This repo: $repo"

// set some vars
def workspace = __FILE__.split('/')[0..-5].join('/')
println "Workspace path: $workspace"

def brancher_path  = "$workspace/userContent/meta_jobs/brancher"
println "Brancher path: $brancher_path"

def builtin_job_templates_path = "$workspace/userContent/job_templates"
println "Builtin job templates path: $builtin_job_templates_path"

def external_job_templates_path = "$workspace/external_job_templates"
println "External Job Templates Path: $external_job_templates_path"

def repo_path = "$workspace/repo"
println "Repo source code path: $repo_path"

def robo_file_path = "$repo_path/.robo"
println "Robo file path: $robo_file_path"


// find all job templates, external templates of the same name override builtin
println RoboUtil.header('Cache job templates')
def job_templates_dirs = []
new File(builtin_job_templates_path).eachDir {
    job_templates_dirs += it
}
if (! ROBO_JOB_TEMPLATE_REPO.empty) {
    println "Finding external job templates, these override the builtin ones"
    new File(external_job_templates_path).eachDir {
        job_templates_dirs += it
    }
}
else {
    println "External job template repo not defined"
}

// read and cache all job templates
job_template_parts = ['job','get-version']
def job_template_cache = [:].withDefault{ [:] }
job_templates_dirs.each {
    def template_path = it.toString()
    def template_name = template_path.split('/')[-1]
    println "Adding job template to cache: $template_name"

    // read and cache all the job template parts
    job_template_parts.each {
        def part = it
        def part_path = "${template_path}/${part}.groovy"

        if (new File(part_path).exists()) {
            job_template_cache[template_name][part] =
                readFileFromWorkspace(part_path)
        }
    }    
}


// assimilate repo - resistance is futile
println RoboUtil.header('Assimilating repo')
def is_repo_managed = false

if (ROBO_NO_REPO_MANAGEMENT.toBoolean()) {
    println 'repo management is turned off'
} else {
    // mark repo as managed
    def root_commit = RoboUtil.executeHelper(
        'git rev-list --max-parents=0 HEAD', repo_path)[0].trim()
    println "Root commit: $root_commit"

    def tag_message = """|This git repository is managed by robo-jenkins
                         |
                         |Robo-jenkins sets hooks and pushes tags to this
                         |repository as part of it's normal operation.
                         |Touching tags prefixed with  'robo-' or changing
                         |the hooks may interfere with the build process.
                         |
                         |Jenkins url: ${JENKINS_URL}
                         |This build url: ${BUILD_URL}
                         |This build tag: ${BUILD_TAG}
                         |This build id: ${BUILD_ID}
                         |""".stripMargin()

    println RoboUtil.executeHelper(
        ['git', 'tag', '--force', "--message=$tag_message", 'robo-jenkins',
        root_commit], repo_path).join(' ')
    try {
        println RoboUtil.executeHelper(
            'git push --force --tags', repo_path).join(' ')
        is_repo_managed = true
    }
    catch (all) {
        println "Push access to repo denied: $all"
    }
    
    // setup job trigger hooks 
    // todo
}
println "Repo managed: $is_repo_managed"


// discover remote branches
println RoboUtil.header('Create build jobs for all branches')
def branches = [:]
def remote_branches_refs = "$repo_path/.git/refs/remotes/origin/"
new File(remote_branches_refs).eachFileRecurse(groovy.io.FileType.FILES) {
    def ref_path = it.toString()
    branches[(ref_path - remote_branches_refs)] = readFileFromWorkspace(
        ref_path).trim()
}
println "Branches in $repo:"
branches.each{ println it }


// create parent folder, which will hold all jobs for this repo
def parent_folder = RoboUtil.getRepoFriendlyName(repo)
folder(parent_folder) { description("Build jobs for $repo") }


// process each branch in each repo
branches.each{
    def branch_name = it.key
    def branch_friendly_name = branch_name.replaceAll('/','-')
    def sha = it.value
    println "Processing branch: $branch_name ($sha) of $repo"
    println RoboUtil.executeHelper("git checkout -f $sha", repo_path)[1]

    def job_name = "$parent_folder/$branch_friendly_name"
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

    // build environment variable injector
    def env_inject_code =  new StringBuilder()
    env_inject_code << 'def version = Eval.me($/\n'
    job_template_cache[target_job_template]['get-version'].eachLine {
        env_inject_code << "    $it\n"
    }
    env_inject_code << '/$)\n\n' << readFileFromWorkspace(
        "${brancher_path}/env-inject.groovy")

    // standard job configuration
    def build_job = job("$job_name") {
        scm {
            git {
                remote {
                    name('origin')
                    url(repo)
                }
                branch(branch_name)
            }
        }
        wrappers { 
            environmentVariables {
                env('MY_REPO_REGISTRY_ENTRY', my_repo_registry_entry.inspect())
                env('IS_REPO_MANAGED', is_repo_managed)
                envs(binding.variables.findAll{ it =~ /^ROBO_.*/ })
                groovy(env_inject_code.toString())
            }
        }
        // only process releases or attempt tagging if repo is managed
        if (is_repo_managed) {
            steps {
                shell(readFileFromWorkspace(
                    "$brancher_path/release_checker.sh"))
            }
            publishers {
                groovyPostBuild {
                    script(readFileFromWorkspace(
                        "$brancher_path/groovy-post.groovy"))
                }
                git {
                    pushOnlyIfSuccess()
                    forcePush(true)
                    tag('origin', 'robo-ver-$VERSION') {
                        create(true)
                        update(true)
                        message('''|This version was built by robo-jenkins
                                   |
                                   |Version: $VERSION
                                   |Is release: $IS_RELEASE
                                   |Is snapshot: $IS_SNAPSHOT
                                   |
                                   |Jenkins url: $JENKINS_URL
                                   |This build url: $BUILD_URL
                                   |This build tag: $BUILD_TAG
                                   |This build id: $BUILD_ID
                                   |'''.stripMargin())
                    }
                }
            }
        }
    }

    // create build job from template
    Eval.me('job', build_job, job_template_cache[target_job_template]['job'])
}
