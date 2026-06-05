pipeline {
    agent any

    stages {
        stage('Descargar Código') {
            steps {
                checkout scm
            }
        }

        stage('Construir y Probar (Unit Tests)') {
            steps {
                sh 'chmod +x ./gradlew'
                // Compila y genera el reporte XML de JaCoCo
                sh './gradlew clean build jacocoTestReport'
            }
        }

        stage('Análisis de Calidad (SonarQube)') {
            steps {
                // Usamos el nombre del contenedor y el token que acabas de generar
                sh './gradlew sonar -Dsonar.host.url=http://sonarqube_server:9000 -Dsonar.token=sqa_e32c67bb4d56e723b8d1ad5fb10aac9db54eadd1'
            }
        }
        stage('Quality Gate') {
                    steps {
                        timeout(time: 5, unit: 'MINUTES') {
                            // Jenkins se pausa aquí esperando la respuesta del webhook de SonarQube
                            waitForQualityGate abortPipeline: true
                        }
                    }
                }
    }

    post {
        always {
            junit 'build/test-results/test/*.xml'
        }
    }
}