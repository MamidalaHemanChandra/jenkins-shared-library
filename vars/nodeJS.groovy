def call (Map configMap) {
    pipeline {
    agent {
        node {
            label 'Agent-1'
        }
    }

    environment { 
        Course = 'Jenkins'
        appVersion = ""
        ACC_ID = "634758830486"
        PROJECT = configMap.get('project')
        COMPONENT = configMap.get('component')

    }

    options {
        timeout(time: 60, unit: 'MINUTES') 
        disableConcurrentBuilds()
    }

    stages {

        stage('Read App Version') {
            steps {
                script {
                    def packageJSON = readJSON file: 'package.json'
                    appVersion = packageJSON.version
                    echo "App Version is: ${appVersion}"
                } 
            }
        }

        stage('Install Dependencies') {
            steps {
                script {
                    sh """
                    npm install 
                    """
                }
            }
        }



        stage('Build Image ECR') {
            steps {
                script {
                    withAWS(region:'us-east-1',credentials:'aws-creds') {
                        sh """
                        aws ecr get-login-password --region us-east-1 | docker login --username AWS --password-stdin ${ACC_ID}.dkr.ecr.us-east-1.amazonaws.com
                        docker build -t ${ACC_ID}.dkr.ecr.us-east-1.amazonaws.com/${PROJECT}/${COMPONENT}:${appVersion} .
                        docker push ${ACC_ID}.dkr.ecr.us-east-1.amazonaws.com/${PROJECT}/${COMPONENT}:${appVersion}
                        """
                    }
                    
                }
            }
        }
    }


    }

    post { 
        always { 
            echo 'I will always say Hello again!'
            cleanWs()
        }
        success {
            echo 'I will run if success'
        }
        failure {
            echo 'I will not run if failure'
        }
        aborted {
            echo 'Pipeline is aborted'
        }
    }
}