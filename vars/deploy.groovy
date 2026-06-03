def call (Map configMap){
    pipeline {
        agent {
            node {
                label 'Agent-1'
            }
        }

        environment {
            Course = 'Jenkins'
            appVersion = configMap.get("appVersion")
            deploy_to = configMap.get("deploy_to")
            ACC_ID = "634758830486"
            PROJECT = configMap.get("project")
            COMPONENT = configMap.get("component")
        }

        options {
            timeout(time: 60, unit: 'MINUTES')
            disableConcurrentBuilds()
        }

        stages {

            stage('Deploy') {
                steps {
                    script {
                        withAWS(region:'us-east-1',credentials:'aws-creds') {
                            sh """
                            set -e
                            aws eks update-kubeconfig --region us-east-1 --name ${PROJECT}-${deploy_to}
                            kubectl get nodes
                            sed -i "s/ImageVersion/${appVersion}/g" values.yaml
                            helm upgrade --install ${component} -f values-${deploy_to}.yaml -n ${PROJECT} --atomic --wait --timeout 10m .
                            """
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

}