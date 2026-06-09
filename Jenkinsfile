pipeline {
    agent any
    tools {
        jdk 'Java17'
    }

    environment {
        SONAR_TOKEN = credentials('sonarqube-token') // nombre del credential en Jenkins
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
                // `build` ya incluye compilación + tests + jacocoTestReport (por el finalizedBy)
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
                // skipCompile=true: usa los .class del stage anterior, no recompila
                sh './gradlew sonar -Dsonar.token=$SONAR_TOKEN'
            }
        }

        stage('Quality Gate') {
            steps {
                timeout(time: 5, unit: 'MINUTES') {
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