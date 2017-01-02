// version information comes from a version.txt file at the root of the repo
def ver_file = new File("$WORKSPACE/version.txt")
return ver_file.exists() ? ver_file.text.trim() : null
