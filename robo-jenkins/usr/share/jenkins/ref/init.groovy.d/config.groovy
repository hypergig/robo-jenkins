import jenkins.model.*
import java.nio.file.Files


def env = System.getenv()
String jenkins_home = "/usr/share/jenkins"


// execute config scripts
println "--> executing config scripts"
def process = "$jenkins_home/config.sh".execute()
process.waitForOrKill(10000)

def stdout = new StringBuilder(), stderr = new StringBuilder()
process.consumeProcessOutput(stdout, stderr)
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
int exes =  env.containsKey('JENKINS_EXES') ? env['JENKINS_EXES'].toInteger() : 25
println "--> setting number of executors on master to $exes"
Jenkins.instance.setNumExecutors(exes)
println "--> setting number of executors on master to $exes... done"


// create seed job
println '--> bootstrapping seed job'
def jobName = "seed"
String configXml = new File("$jenkins_home/seed/seed.xml").text
def xmlStream = new ByteArrayInputStream( configXml.getBytes() )
Jenkins.instance.createProjectFromXML(jobName, xmlStream)
println '--> bootstrapping seed job... done'
