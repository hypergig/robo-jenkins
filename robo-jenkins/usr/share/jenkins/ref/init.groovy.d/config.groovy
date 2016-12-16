import jenkins.model.*

def env = System.getenv()
String jenkins_config_home = env['JENKINS_CONFIG_HOME'].toString()


// execute config scripts
println "--> executing config scripts"
def process = "$jenkins_config_home/config.sh".execute()
process.waitForOrKill(10000)

def stdout = new StringBuilder(), stderr = new StringBuilder()
process.waitForProcessOutput(stdout, stderr)
int rc = process.exitValue()

println "standard out:\n$stdout"
println "standard err:\n$stderr"
println "return code: $rc"

if (rc) {
  println 'config scripts failed'
  System.exit(rc)
}
println '--> executing config scripts... done'


// setting jenkins url
String url =  env.containsKey('JENKINS_URL') ? env['JENKINS_URL'].toString() : 'http://localhost/'
println "--> setting jenkins url to $url"
jlc = JenkinsLocationConfiguration.get()
jlc.setUrl(url)
jlc.save()
println "--> setting jenkins url to $url... done"


// fixing master executers
int exes =  env.containsKey('JENKINS_EXECUTERS') ? env['JENKINS_EXECUTERS'].toInteger() : 25
println "--> setting number of executors on master to $exes"
Jenkins.instance.setNumExecutors(exes)
println "--> setting number of executors on master to $exes... done"

// set global quiet period if JENKINS_QUIET_PERIOD is set
// If not set, Jenkins defaults to 5 seconds
if (env.containsKey('JENKINS_QUIET_PERIOD')) {
    int qp = env['JENKINS_QUIET_PERIOD'].toInteger()
    println "--> setting global quiet period on master to $qp"
    Jenkins.instance.setQuietPeriod(qp)
    println "--> setting global quiet period on master to $qp... done"
}

// create seed job
println '--> bootstrapping seed job'
def jobName = "seed"
String configXml = new File("$jenkins_config_home/seed.xml").text
def xmlStream = new ByteArrayInputStream( configXml.getBytes() )
Jenkins.instance.createProjectFromXML(jobName, xmlStream)
println '--> bootstrapping seed job... done'
