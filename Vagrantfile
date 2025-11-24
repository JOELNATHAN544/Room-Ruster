# -*- mode: ruby -*-
# vi: set ft=ruby :

Vagrant.configure("2") do |config|
  config.vm.box = "ubuntu/focal64"
  config.vm.hostname = "room-ruster"

  # Sync current folder to /vagrant (default)
  config.vm.synced_folder ".", "/vagrant", type: "virtualbox"

  config.vm.provision "shell", inline: <<-SHELL
    set -e
    sudo apt-get update -y
    sudo apt-get install -y openjdk-17-jdk maven

    echo "Java version:"
    java -version
    echo "Maven version:"
    mvn -v

    cd /vagrant
    mvn -q -DskipTests package

    echo "\nBuild complete. To run inside the VM:"
    echo "  cd /vagrant"
    echo "  # Simulation every 5s without Discord webhook"
    echo "  java -jar target/room-ruster-0.1.0.jar --daemon-sim --start 2025-01-06 --start-week 1 --interval-seconds 5"
    echo "\nFor production (weekly Mondays), export DISCORD_WEBHOOK_URL then:"
    echo "  java -jar target/room-ruster-0.1.0.jar --daemon --start 2025-01-06 --time 09:00"
  SHELL
end
