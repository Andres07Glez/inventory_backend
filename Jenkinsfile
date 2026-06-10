pipeline {
    agent any
    tools {
        jdk 'Java17'
    }


    stages {
        stage('Descargar Código') {
            steps {
                checkout scm
            }
        }

        stage('Construir y Probar') {
            steps {
                sh 'chmod +x ./gradlew'
                sh './gradlew clean build jacocoTestReport'
            }
            post {
                always {
                    junit 'build/test-results/test/*.xml'
                }
            }
        }

        stage('Análisis SonarQube') {
            steps {
                // Debe ser exactamente el nombre que ponemos en Manage Jenkins > System
                withSonarQubeEnv('sonarqube') {
                    sh './gradlew sonar -Dsonar.gradle.skipCompile=true'
                }
            }
        }

        stage('Quality Gate') {
            steps {
                timeout(time: 5, unit: 'MINUTES') {
                    // Ahora esto funcionará porque withSonarQubeEnv preparó el terreno
                    waitForQualityGate abortPipeline: true
                }
            }
        }
    }

    post {
        success {
            echo 'Pipeline completado exitosamente.'
        }
        failure {
            echo 'El pipeline falló. Revisar logs.'
        }
    }
}