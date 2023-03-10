pipeline {
    agent any
    options {
        disableConcurrentBuilds()
    }
    parameters {
        string( name: 'WebSite', defaultValue: 'None', description: "Enter, separated by a comma, the ip address(es) of the host(s) on which you want to prepare the environment for WEBSITE(s). Example: 13.39.107.210;13.38.121.165")
        string( name: 'DataBase', defaultValue: 'None', description: "Enter, separated by a comma, the ip address(es) of the host(s) on which you want to prepare the environment for DATA BASE(s). Example: 13.38.250.255")
    }
    stages {
        stage('CreateEnvironment') {
            steps {
                script {
                    def ipDB = DataBase.split(';') as Set
                    def ipWS = WebSite.split(';') as Set

                    if (ipDB[0] == "None")  { ipDB = [] }
                    if (ipWS[0] == "None")  { ipWS = [] }

                    def ipDocker = ipDB + ipWS

                    for (ip in ipDocker) {
                        //Install Docker by ip address
                        sshagent(credentials: ['websites']) {
                            sh """                                
                                ssh-keyscan -H ${ip} >> ~/.ssh/known_hosts  
                                ssh ubuntu@${ip} '
                                    sudo apt update;
                                    sudo apt install apt-transport-https ca-certificates curl software-properties-common -y;
                                    curl -fsSL https://download.docker.com/linux/ubuntu/gpg | sudo apt-key add -;
                                    sudo add-apt-repository "deb [arch=amd64] https://download.docker.com/linux/ubuntu focal stable";
                                    sudo apt update;
                                    sudo apt-get install s3cmd -y;
                                    sudo apt install mariadb-client-core-10.6 -y;
                                    apt-cache policy docker-ce;
                                    sudo apt install docker-ce -y;
                                    sudo systemctl status docker;
                                '
                            """
                        }
                        if (ipWS.contains(ip)) {
                            //Install Docker-compose by ip address
                            sshagent(credentials: ['websites']) {
                                sh """
                                    ssh-keyscan -H ${ip} >> ~/.ssh/known_hosts
                                    ssh ubuntu@${ip} '
                                        sudo curl -L 'https://github.com/docker/compose/releases/download/1.26.0/docker-compose-\$(uname -s)-\$(uname -m)' -o /usr/local/bin/docker-compose;
                                        sudo chmod +x /usr/local/bin/docker-compose;
                                        docker-compose --version;
                                    '
                                """
                            }
                        }
                    }
                }
            }
        }
    }
    post {
        success {
            script {
                emailext body: "Success!", subject: "createEnvironment(s)", to: "admin@admin.ru"
            }
        }
        failure {
            script {
                emailext body: "Failure!", subject: "createEnvironment(s)", to: "admin@admin.ru"
            }
        }
    }
}