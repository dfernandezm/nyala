#!/usr/bin/env groovy

pipeline {
    agent any

    stages {
                
        stage('Build & Test') {
             
                steps {
                    echo 'Building TrexSubs...'
                    sh "./gradlew clean test fatJar"
                }
        }
        
        stage('Build & Push Docker Images') {
            steps {
                script {
                        gitCommit = sh(returnStdout: true, script: 'git rev-parse HEAD').trim()
                        commitHash = gitCommit.take(7)
                }
                echo 'Building Docker Image...'
                sh """
                       # this command deletes the docker config if it exists, if not silently ignores it. This is needed to avoid different configurations of Docker between Morpheus API
                       # and scoring ways of building Docker images. There should be a commons way of doing this in the future 
                       rm -f -- /var/jenkins_home/.docker/config.json
                    """
                sh "./gradlew clean pushImage -PdockerTag=${env.CURRENT_VERSION}-${commitHash}"

            }
        }
    }
}