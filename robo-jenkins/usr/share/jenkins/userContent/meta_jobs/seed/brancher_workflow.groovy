// brancher pipeline script
// these are the workflow steps for each brancher job

// get my repo registry entry
def my_repo_registry_entry = Eval.me("${MY_REPO_REGISTRY_ENTRY}")
echo "My repo registry entry:\n$my_repo_registry_entry"

node {
    stage ('git checkouts') {
        // check for external job template repo, checkout if exists
        if (! ROBO_JOB_TEMPLATE_REPO.empty) {
            dir("external_job_templates") { git ROBO_JOB_TEMPLATE_REPO }
        }
        else { echo "External job template repo not defined" }

        // checkout userContent repo from master
        dir("userContent") { git "http://localhost:8080/userContent.git" }
        
        // checkout repo source code
        dir("repo") { git "${my_repo_registry_entry.url}" }
    }
    stage('build brancher jobs') {
        jobDsl (
            removedJobAction: 'DELETE',
            targets: 'userContent/meta_jobs/brancher/brancher.groovy',
            additionalClasspath: 'userContent/meta_jobs/libs/**'
        )
    }
}
