seed_job_dir=/var/jenkins_home/workspace/seed

mkdir -vp ${seed_job_dir}
cp -v ${JENKINS_CONFIG_HOME}/seed/* ${seed_job_dir}/
