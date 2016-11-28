seed_job_dir=/var/jenkins_home/workspace/seed

mkdir -vp ${seed_job_dir}
cp -rv ${JENKINS_CONFIG_HOME}/seed/* ${seed_job_dir}/
