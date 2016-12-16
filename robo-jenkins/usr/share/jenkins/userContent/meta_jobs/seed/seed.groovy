// the seed job dsl script
import org.yaml.snakeyaml.Yaml
import robo.RoboUtil

// the git userContent repo needs to be exercised to force it to startup
// or else most of the brancers jobs will fail their first time running
if ("${BUILD_NUMBER}" == "1") {
    println 'First seed job run detected, preheating git userContent server'
    println RoboUtil.executeHelper(
        'git ls-remote -ht http://localhost:8080/userContent.git')[0]
}

// ingest repo registry
def repo_registry = []
println 'Ingesting repo registry'
new File("${WORKSPACE}/repo_registry/").eachFileMatch(~/.*\.yml$/) {
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

// make branchers folder
folder('branchers') { description("Folder to hold all brancher jos") }

// process each repo
repo_registry.each {
    def my_repo_registry_entry = it
    def repo = my_repo_registry_entry.url
    def brancher_name = "branchers/${RoboUtil.getRepoFriendlyName(repo)}"
    println "Creating brancher job: $brancher_name"

    // create brancher for repo
    pipelineJob(brancher_name) {
        environmentVariables {
            keepBuildVariables (true)
            keepSystemVariables (true)
            env('MY_REPO_REGISTRY_ENTRY', my_repo_registry_entry.inspect())
            envs(binding.variables.findAll{ it =~ /^ROBO_.*/ })
        }
        definition {
            cps {
                script(readFileFromWorkspace(
                    'meta_jobs/seed/brancher_workflow.groovy'))
            }
        }
    }

    // queue brancher job to run
    // todo - only trigger new/updated brancher jobs
    queue(brancher_name)
}
