pipeline {
    agent {
        label 'orange'
    }

    environment {
        LANG = 'en_US.UTF-8'
        LANGUAGE = 'en_US.UTF-8'
        LC_ALL = 'en_US.UTF-8'
    }

    stages {
        stage('Environment') {
            steps {
                script {
                    sh script: 'ls -a', label: 'Workspace'
                    sh script: 'chmod +x gradlew && ./gradlew --version', label: 'Gradle'
                    sh script: 'java -version', label: 'Java'
                    sh script: 'git --version', label: 'Git'
                }
            }
        }
        stage('Build') {
            steps {
                sh script: 'chmod +x gradlew && ./gradlew build', label: 'Build project and make jar'
                archiveArtifacts artifacts: '**/build/libs/*.jar', fingerprint: true
            }
        }
    }

    post {
        always {
            cleanWs()
        }
    }
}