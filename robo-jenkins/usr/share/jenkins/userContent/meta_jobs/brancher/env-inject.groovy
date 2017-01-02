// variable processing and injecting

@Grab('org.yaml:snakeyaml:1.17')
import org.yaml.snakeyaml.Yaml

return_value = [:]

// process .robo file
def robo_file_map = [:]
def robo_file = new File("$WORKSPACE/.robo")
if (robo_file.exists())
{
    robo_file_map = new Yaml().load(robo_file.newReader())
}
return_value['ROBO_FILE_MAP'] = robo_file_map.inspect()

// determine if version is semantic
def is_semver = version ==~ /^(0|[1-9]\d*)\.(0|[1-9]\d*)\.(0|[1-9]\d*)(-(0|[1-9]\d*|\d*[a-zA-Z-][0-9a-zA-Z-]*)(\.(0|[1-9]\d*|\d*[a-zA-Z-][0-9a-zA-Z-]*))*)?(\+[0-9a-zA-Z-]+(\.[0-9a-zA-Z-]+)*)?$/
return_value['IS_SEMVER'] = is_semver

// determine if version is a release
def is_release = version ==~ /^(0|[1-9]\d*)\.(0|[1-9]\d*)\.(0|[1-9]\d*)$/
return_value['IS_RELEASE'] = is_release

// determine if version is a snapshot
def is_snapshot = (!is_release && version)
return_value['IS_SNAPSHOT'] = is_snapshot

// only work with semantic versions
return_value['VERSION'] = is_semver ? version : 'non-semantic'

// return all variables
println 'Injected variables:'
return_value.each { println "${it.key}=${it.value}" }

return return_value
