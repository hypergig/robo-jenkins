if [ -S /var/run/docker.sock ]
  then
    sudo chown -v jenkins:jenkins /var/run/docker.sock
  else
    echo "No docker socket volume found, robo-jenkins needs this to work!"
    echo "Add '-v /var/run/docker.sock:/var/run/docker.sock' to your docker run command"
    exit 1
fi
