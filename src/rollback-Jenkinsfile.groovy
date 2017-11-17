#!/usr/bin/env groovy

/**
 * @ Maintainer Sudheer Veeravalli <Sudheer.Veeravalli@quest-global.com>
 */

/* Only keep the 10 most recent builds. */
def projectProperties = [
	buildDiscarder(logRotator(artifactDaysToKeepStr: '10', artifactNumToKeepStr: '10', daysToKeepStr: '10', numToKeepStr: '10')),
	disableConcurrentBuilds()
]

properties(projectProperties)

try {
  node {
    def appName = "MediConnekt"  
    def artifactoryRepoName = 'Quest-DevOps'
    def workspaceRoot = env.WORKSPACE 
    def ROLLBACK_TO_BUILD = params.ROLLBACK_TO_BUILD

    stage('Download Config Files') {
      sh '''
         cp -r ./../MediConnekt-DEV/repo/Configuration_Files/ansible_files .
         rm ansible_files/mediconnekt-web.tar
      '''
      
    }
	
    stage('Download File') {
      sh '''
        cd ansible_files
        curl -uadmin:AP2FfJmveKDYuUnGqKfQrkYTmKS -O "http://10.1.151.88:8081/artifactory/generic-local/Applications/Quest-DevOps/MediConnekt/app/${ROLLBACK_TO_BUILD}/mediconnekt-web.tar"
      '''
    }
	
    stage('Perform Rollback') {
      dir('ansible_files/') {
        sh "ansible-playbook rollback-on-dev.yml -i mediconnekt_hosts --extra-vars 'ROLLBACK_TO_BUILD=${ROLLBACK_TO_BUILD}'"
      }
    }
  }
} catch (exc) {
  error "Caught: ${exc}"
}
