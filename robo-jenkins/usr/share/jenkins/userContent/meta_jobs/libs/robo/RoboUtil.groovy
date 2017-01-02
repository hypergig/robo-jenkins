package robo

import org.yaml.snakeyaml.Yaml

class RoboUtil {

    /**
     * Helper method that creating an easy to read header for logs
     *
     */
    static String header(String line) {
        return line.center(50,'-')
    }

    /**
     * Helper method that executes a command
     *
     */
    static List executeHelper(def command, String wd = '/'){
        def process = command.execute(null, new File(wd))
        process.waitForOrKill(5000)

        def stdout = new StringBuilder(), stderr = new StringBuilder()
        process.waitForProcessOutput(stdout, stderr)

        stdout = stdout.toString()
        stderr = stderr.toString()
        int rc = process.exitValue()

        if (rc) {
            throw(new javaposse.jobdsl.dsl.DslException(
                "rc: $rc\nstderr: $stderr"))
        }
        return [stdout, stderr]
    }

    /**
     * Parse the git transpart protocol from a repository URL.
     * Currently assumes git if the url starts with 'git' and HTTPS otherwise
     * @param repo Repository URL to parse.
     */
    static String getRepoProto(def repo) {
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
    static String getRepoFriendlyName(def repo){
        def clean_repo_name = repo

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
            return clean_repo_name.split('://')[1].tr('/','.')
        }
    }

    /**
    * Reads each yaml file in the given directory and extract a list from the given
    * top level key.  These lists are then merged togetehr and then returned.
    *
    * @param yaml_dir Directory to scan for yaml files
    * @param yaml_key Top level key which stores a list in each yaml file
    */
    static List mergeYamlLists(def yaml_dir, def yaml_key){
        def merged = []
        new File("$yaml_dir").eachFileMatch(~/.*\.yml$/) {
            def yaml_path = it.toString()
            try {
                merged.addAll(new Yaml().load(it.newReader()).get(yaml_key))
            }
            catch(all) {
                println "Failed to ingest yml file: $yaml_path\n[$all]"
            }
        }
        return merged
    }


    /**
     * Parses the repository name from the repo url
     *
     * @param repo Repository url to parse
     */
    static String getRepoName(def repo) {
        def parent_toks = this.getRepoFriendlyName(repo).tokenize(".")

        if(parent_toks.size() > 2){
            return parent_toks[1,-1].join(".")
        }
        else if (parent_toks.size() == 2) {
            return parent_toks[1]
        }
        else {
            return parent_toks[0]
        }
    }


    /**
     * Parses the repository orginzation from the repo url.
     * This is the slash seperated token between the git host and the repo name.
     * (which may be empty)
     * @param repo Repository url to parse
     */
    static String getRepoOrg(def repo) {
        def parent_toks = this.getRepoFriendlyName(repo).tokenize(".")

        if(parent_toks.size() > 1) {
            return parent_toks[0]
        }
        else {
            return ""
        }
    }

    /**
     * Constructs a build id from variables passed in from the build context.
     *
     * @param variables The map of variables from the build job binding
     */
    static String getBuildId(def variables){
        return variables.BUILD_NUMBER
    }
}
