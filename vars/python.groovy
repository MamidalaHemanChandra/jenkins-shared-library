def call(Map configMap) {

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
                        appVersion = readFile(file: 'version')
                        echo "App Version is: ${appVersion}"
                    }
                }
            }

            stage('Install Dependencies') {
                steps {
                    sh "pip3 install -r requirements.txt"
                }
            }

            stage('Build Image ECR') {
                steps {
                    script {
                        withAWS(region: 'us-east-1', credentials: 'aws-creds') {
                            sh """
                            aws ecr get-login-password --region us-east-1 | docker login --username AWS --password-stdin ${ACC_ID}.dkr.ecr.us-east-1.amazonaws.com
                            docker build -t ${ACC_ID}.dkr.ecr.us-east-1.amazonaws.com/${PROJECT}/${COMPONENT}:${appVersion} .
                            docker push ${ACC_ID}.dkr.ecr.us-east-1.amazonaws.com/${PROJECT}/${COMPONENT}:${appVersion}
                            """
                        }
                    }
                }
            }

            stage('Deploy Trigger Downstream Job') {
                steps {
                    script {
                        echo "Deploy Triggering downstream job"
                        build job: "../${COMPONENT}-deploy",
                            wait: false,
                            propagate: false,
                            parameters: [
                                string(name: 'appVersion', value: "${appVersion}"),
                                string(name: 'deploy_to', value: 'dev')
                            ]
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
}