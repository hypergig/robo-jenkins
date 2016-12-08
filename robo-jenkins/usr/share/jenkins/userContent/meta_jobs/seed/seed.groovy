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
    def brancher_name = "branchers/${getRepoFriendlyName(repo)}"
    println "Creating brancher job: $brancher_name"

    // create brancher for repo
    job(brancher_name) {
        scm {
            git {
                remote {
                    url(repo)
                }
                extensions {
                    pruneStaleBranch()
                    // doing a shallow clone without specifying a branch
                    // results in a error
                    //cloneOptions { shallow() }
                    relativeTargetDirectory('repo')
                }
            }
        }
        wrappers {
            preBuildCleanup {
                includePattern('meta_jobs,job_templates')
                deleteDirectories()
            }
            copyToSlaveBuildWrapper {
                includes('meta_jobs/brancher/**,meta_jobs/libs/**,job_templates/**')
                excludes('')
                flatten(false)
                includeAntExcludes(false)
                relativeTo('userContent')
                hudsonHomeRelative(false)
            }
        }
        steps {
            environmentVariables {
                env('MY_REPO_REGISTRY_ENTRY', my_repo_registry_entry.inspect())
            }
            dsl{
                external('meta_jobs/brancher/brancher.groovy')
                removeAction('DELETE')
                additionalClasspath('meta_jobs/libs/**')
            }
        }
    }
    
    // queue brancher job to run
    // todo - only trigger new/updated brancher jobs
    queue(brancher_name)
}
