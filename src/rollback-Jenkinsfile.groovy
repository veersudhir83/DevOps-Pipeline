#!/usr/bin/env groovy

/**
 * @ Maintainer Sudheer Veeravalli <veersudhir83@gmail.com>
 */

/* Only keep the 10 most recent builds. */
def projectProperties = [
	buildDiscarder(logRotator(artifactDaysToKeepStr: '10', artifactNumToKeepStr: '10', daysToKeepStr: '10', numToKeepStr: '10')),
	disableConcurrentBuilds()
]

properties(projectProperties)

try {
  node {
    def appName = "Application"  
    def artifactoryRepoName = 'DevOps'
    def workspaceRoot = env.WORKSPACE 
    def ROLLBACK_TO_BUILD = params.ROLLBACK_TO_BUILD

    stage('Download Config Files') {
      sh '''
         cp -r ./../Application/repo/Configuration_Files/ansible_files .
         rm ansible_files/appname-web.tar
      '''
      
    }
	
    stage('Download File') {
      sh '''
        cd ansible_files
        curl -uadmin:XXXXXXXXXX -O "http://localhost:8081/artifactory/generic-local/Applications/DevOps/${appName}/app/${ROLLBACK_TO_BUILD}/${appName}-web.tar"
      '''
    }
	
    stage('Perform Rollback') {
      dir('ansible_files/') {
        sh "ansible-playbook rollback-on-dev.yml -i hosts_file --extra-vars 'ROLLBACK_TO_BUILD=${ROLLBACK_TO_BUILD}'"
      }
    }
  }
} catch (exc) {
  error "Caught: ${exc}"
}
