#!/usr/bin/env groovy

/**
 * @ Maintainer sudheer veeravalli<veersudhir83@gmail.com>
 */

/* Only keep the 10 most recent builds. */
def projectProperties = [
        buildDiscarder(logRotator(artifactDaysToKeepStr: '5', artifactNumToKeepStr: '5', daysToKeepStr: '5', numToKeepStr: '5')),
        [$class: 'GithubProjectProperty', projectUrlStr: 'https://github.com/veersudhir83/addressbook-vaadin.git/']
        //,pipelineTriggers([pollSCM('H/10 * * * *')])
]

properties(projectProperties)

try {
    node {
        def mvnHome
        def antHome
        def artifactoryPublishInfo
        def artifactoryServer
        def isArchivalEnabled = params.ISARCHIVALENABLED
                // true // Enable if you want to archive files and configs to artifactory
        def isAnalysisEnabled = params.ISANALYSISENABLED
                // true // Enable if you want to analyze code with sonarqube
        def buildNumber = env.BUILD_NUMBER
        def workspaceRoot = env.WORKSPACE
        def artifactoryTempFolder = 'downloadsFromArtifactory' // name of the local temp folder where file(s) from artifactory get downloaded
        def sonarHome
        def SONAR_HOST_URL = 'http://localhost:9000'
        def appName = 'addressbook-vaadin' // application name currently in progress
        def appEnv  // application environment currently in progress
        def mavenArtifactName = 'addressbook.war'

        if (isArchivalEnabled) {
            // Artifactory server id configured in the jenkins along with credentials
            artifactoryServer = Artifactory.server 'ArtifactoryOSS-5.4.3'
        }

        // to download appConfig.json files from artifactory
        def downloadAppConfig = """{
            "files": [
                {
                    "pattern": "generic-local/Applications/${appName}/${appEnv}/appConfig.json",
                    "target": "${workspaceRoot}/${artifactoryTempFolder}/${appName}/${appEnv}/"
                }
            ]
        }"""

        def uploadAppConfigUnix = """{
            "files": [
                {
                    "pattern": "${workspaceRoot}/${artifactoryTempFolder}/${appName}/${appEnv}/appConfig.json",
                    "target": "generic-local/Applications/${appName}/${appEnv}/"
                }
            ]
        }"""

        def uploadAppConfigWindows = """{
            "files": [
                {
                    "pattern": "${workspaceRoot}\\${artifactoryTempFolder}\\${appName}\\${appEnv}\\appConfig.json",
                    "target": "generic-local/Applications/${appName}/${appEnv}/"
                }
            ]
        }"""

        def uploadMavenSpecUnix = """{
            "files": [
                {
                    "pattern": "${workspaceRoot}/${appName}/target/${mavenArtifactName}",
                    "target": "generic-local/Applications/${appName}/app/${buildNumber}/"
                }
            ]
        }"""

        def uploadMavenSpecWindows = """{
            "files": [
                {
                    "pattern": "${workspaceRoot}\\${appName}\\target\\${mavenArtifactName}",
                    "target": "generic-local/Applications/${appName}/app/${buildNumber}/"
                }
            ]
        }"""
        //artifactoryServer.upload(uploadSpec)

        stage('Tool Setup'){
            // ** NOTE: These tools must be configured in the jenkins global configuration.
            if (isUnix()) {
                mvnHome = tool name: 'mvn3', type: 'maven'
                antHome = tool name: 'ant1.9.6', type: 'ant'
            } else {
                mvnHome = tool name: 'mvn3.5', type: 'maven'
                antHome = tool name: 'ant1.9.6', type: 'ant'
            }
            if (isAnalysisEnabled) {
                sonarHome = tool name: 'sonar-scanner-3.0.3.778', type: 'hudson.plugins.sonar.SonarRunnerInstallation'
            }
        }

        stage('Checkout') {
            // Checkout codes from repository
            if (isUnix()) {
                sh "echo 'Running in Unix mode'"
                git url: 'https://github.com/veersudhir83/addressbook-vaadin.git',
                            branch: 'master'
                dir('downloadsFromArtifactory') {
                    sh "echo 'Created folder for artifactory downloads'"
                }
            } else {
                bat(/echo 'Running in windows mode' /)
                git url: 'file:///C:/Users/veers/DATA/PROJECTS/addressbook-vaadin/',
                            branch: 'master'
                dir('downloadsFromArtifactory') {
                    // created folder for artifactory
                }
            }
        }

        stage('Build') {
            echo 'Build in progress'
            try {
                if (isUnix()) {
                    sh "'${mvnHome}/bin/mvn' clean package"
                } else {
                    bat(/"${mvnHome}\bin\mvn" --batch-mode clean package/)
                }
            } catch (exc) {
                echo "Failure in antBuild process of Build stage: ${exc}"
            }
        }

        stage('Analysis') {
            if (isAnalysisEnabled) {
                echo 'Analysis in progress'
                if (isUnix()) {
                    sh "'${mvnHome}/bin/mvn' pmd:pmd sonar:sonar"
                } else {
                    sh "'${mvnHome}\\bin\\mvn' pmd:pmd sonar:sonar"
                }
            }
        }

        stage('Publish') {
            if (isArchivalEnabled) {
                echo 'Publish Artifacts & appConfig.json in progress'
                try {
                    if (isUnix()) {
                        if (fileExists('target/${mavenArtifactName}')) {
                            // upload artifactory and also publish build info
                            artifactoryPublishInfo = artifactoryServer.upload(uploadMavenSpecUnix)
                            artifactoryPublishInfo.retention maxBuilds: 5
                            // and publish build info to artifactory
                            artifactoryServer.publishBuildInfo(artifactoryPublishInfo)
                        } else {
                            error 'mavenPublish: Failed during file upload/publish to artifactory'
                        }
                    } else {
                        if (fileExists('target/${mavenArtifactName}')) {
                            // upload artifactory and also publish build info
                            artifactoryPublishInfo = artifactoryServer.upload(uploadMavenSpecWindows)
                            artifactoryPublishInfo.retention maxBuilds: 5
                            // and publish build info to artifactory
                            artifactoryServer.publishBuildInfo(artifactoryPublishInfo)
                        } else {
                            error 'mavenPublish: Failed during file upload/publish to artifactory'
                        }
                    }
                } catch (exc) {
                    echo "Failure in mavenPublish process of Publish stage: ${exc}"
                }
            }
        }

        stage('Deployment') {
            // TODO: Add code for deployment of both projects
            echo 'Deploy applications to docker/cloud instances'
            try {
                if (isUnix()) {
                    // TODO: Deployment to cloud foundry
                    /*wrap([$class: 'CloudFoundryCliBuildWrapper',
                              cloudFoundryCliVersion: 'Cloud Foundry CLI (built-in)',
                              apiEndpoint: 'https://api.system.aws-usw02-pr.ice.predix.io',
                              skipSslValidation: true,
                              credentialsId: '0b599508-6757-4c0c-be6e-c3367b89d7d5',
                              organization: 'sudheer.veeravalli@quest-global.com',
                              space: 'dev']) {

                            sh 'cf push'
                        }*/

                    // Deployment to docker containers devopsmaven-container*
                    // Commented out on purpose - Use with customization when needed
                    /*sh '''
                            docker ps -a | awk '{print $NF}' | grep -w devopsmaven* > temp.txt
                            sort temp.txt -o container_names_files.txt
                            while IFS='' read -r line || [[ -n "$line" ]]; do
                                echo "#############################"
                                STATUS=`docker inspect --format='{{json .State.Running}}' $line`
                                echo "Container Name is : $line and Status is $STATUS"

                                # copy war file to container
                                docker cp ./target/devops-web-maven.jar $line:/home/
                                echo "jar file is copied !!"
                            done < "container_names_files.txt"
                        '''*/
                } else {
                        // Do Something else
                }
            } catch(exc) {
                echo "Failure in Deployment stage: ${exc}"
            }
        }

        stage('Generate Reports') {
            junit '**/target/surefire-reports/*.xml'
        }

        stage('Finish & Cleanup') {
            // cleanWs() // cleanup workspace after build is complete
        }
    }
} catch (exc) {
    echo "Caught: ${exc}"
}