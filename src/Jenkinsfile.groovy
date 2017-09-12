#!/usr/bin/env groovy

/**
 * @ Maintainer sudheer veeravalli<veersudhir83@gmail.com>
 */

/* Only keep the 10 most recent builds. */
def projectProperties = [
        buildDiscarder(logRotator(artifactDaysToKeepStr: '5', artifactNumToKeepStr: '5', daysToKeepStr: '5', numToKeepStr: '5')),
        [$class: 'GithubProjectProperty', projectUrlStr: 'https://github.com/veersudhir83/DevOps-Pipeline.git/']
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
        def antProjectName = 'devops-web'
        def mavenProjectName = 'devops-web-maven'
        def buildNumber = env.BUILD_NUMBER
        def workspaceRoot = env.WORKSPACE
        def artifactoryTempFolder = 'downloadsFromArtifactory' // name of the local temp folder where file(s) from artifactory get downloaded
        def sonarHome
        def SONAR_HOST_URL = 'http://localhost:9000'
        def appName // application name currently in progress
        def appEnv  // application environment currently in progress

        if (isArchivalEnabled) {
            // Artifactory server id configured in the jenkins along with credentials
            artifactoryServer = Artifactory.server 'ArtifactoryOSS-5.4.3'
        }

        // to download appConfig.json files from artifactory
        def downloadAppConfigUnix = """{
            "files": [
                {
                    "pattern": "generic-local/Applications/Quest-Pipeline/${appName}/${appEnv}/appConfig.json",
                    "target": "${workspaceRoot}/${artifactoryTempFolder}/${appName}/${appEnv}/"
                }
            ]
        }"""

        def downloadAppConfigWindows = """{
            "files": [
                {
                    "pattern": "generic-local/Applications/Quest-Pipeline/${appName}/${appEnv}/appConfig.json",
                    "target": "${workspaceRoot}/${artifactoryTempFolder}/${appName}/${appEnv}/"
                }
            ]
        }"""

        def uploadAppConfigUnix = """{
            "files": [
                {
                    "pattern": "${workspaceRoot}/${artifactoryTempFolder}/${appName}/${appEnv}/appConfig.json",
                    "target": "generic-local/Applications/Quest-Pipeline/${appName}/${appEnv}/"
                }
            ]
        }"""

        def uploadAppConfigWindows = """{
            "files": [
                {
                    "pattern": "${workspaceRoot}\\${artifactoryTempFolder}\\${appName}\\${appEnv}\\appConfig.json",
                    "target": "generic-local/Applications/Quest-Pipeline/${appName}/${appEnv}/"
                }
            ]
        }"""

        // to upload files to artifactory
        def uploadAntSpecUnix = """{
            "files": [
                {
                    "pattern": "${workspaceRoot}/devops-web/dist/devops-web.war",
                    "target": "generic-local/Applications/Quest-Pipeline/devops-web/app/${buildNumber}/"
                }
            ]
        }"""

        def uploadMavenSpecUnix = """{
            "files": [
                {
                    "pattern": "${workspaceRoot}/devops-web-maven/target/devops-web-maven.jar",
                    "target": "generic-local/Applications/Quest-Pipeline/devops-web-maven/app/${buildNumber}/"
                }
            ]
        }"""

        def uploadAntSpecWindows = """{
            "files": [
                {
                    "pattern": "${workspaceRoot}\\devops-web\\dist\\devops-web.war",
                    "target": "generic-local/Applications/Quest-Pipeline/devops-web/app/${buildNumber}/"
                }
            ]
        }"""

        def uploadMavenSpecWindows = """{
            "files": [
                {
                    "pattern": "${workspaceRoot}\\devops-web-maven\\target\\devops-web-maven.jar",
                    "target": "generic-local/Applications/Quest-Pipeline/devops-web-maven/app/${buildNumber}/"
                }
            ]
        }"""
        //artifactoryServer.upload(uploadSpec)

        stage('Checkout') {
            // Checkout codes from repository
            if (isUnix()) {
                sh "echo 'Running in Unix mode'"
                dir('devops-web') {
                    git url: 'https://github.com/veersudhir83/devops-web.git',
                            branch: 'master'
                }
                dir('devops-web-maven') {
                    git url: 'https://github.com/veersudhir83/devops-web-maven.git',
                            branch: 'master'
                }
                dir('downloadsFromArtifactory') {
                    sh "echo 'Created folder for artifactory downloads'"
                }
            } else {
                bat(/echo 'Running in windows mode' /)
                dir('devops-web') {
                    git url: 'file:///C:/Users/veers/DATA/PROJECTS/devops-web/',
                            branch: 'master'
                }
                dir('devops-web-maven') {
                    git url: 'file:///C:/Users/veers/DATA/PROJECTS/devops-web-maven/',
                            branch: 'master'
                }
                dir('downloadsFromArtifactory') {
                    // created folder for artifactory
                }
            }
        }

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

        stage('Build') {
            echo 'Build in progress'
            parallel antBuild: {
                try {
                    if (isUnix()) {
                        dir('devops-web/') {
                            sh "'${antHome}/bin/ant' "
                        }
                    } else {
                        dir('devops-web\\') {
                            bat(/"${antHome}\bin\ant" /)
                        }
                    }
                } catch (exc) {
                    echo "Failure in antBuild process of Build stage: ${exc}"
                }
            }, mavenBuild: {
                try {
                    if (isUnix()) {
                        dir('devops-web-maven/') {
                            sh "'${mvnHome}/bin/mvn' clean package javadoc:javadoc"
                        }
                    } else {
                        dir('devops-web-maven\\') {
                            bat(/"${mvnHome}\bin\mvn" --batch-mode clean package javadoc:javadoc/)
                        }
                    }
                } catch (exc) {
                    echo "Failure in antBuild process of Build stage: ${exc}"
                }
            },
            failFast: true
        }

        stage('Analysis') {
            if (isAnalysisEnabled) {
                echo 'Analysis in progress'
                parallel antAnalysis: {
                    if (isUnix()) {
                        dir('devops-web/') {
                            sh "${sonarHome}/bin/sonar-scanner " +
                                    "-Dsonar.host.url=${SONAR_HOST_URL} " +
                                    "-Dsonar.projectKey=${antProjectName} " +
                                    "-Dsonar.projectName=${antProjectName} " +
                                    "-Dsonar.projectVersion=${buildNumber} " +
                                    "-Dsonar.java.source=1.8 " +
                                    "-Dsonar.sources=./src " +
                                    "-Dsonar.java.libraries=./lib "
                        }
                    } else {
                        dir('devops-web\\') {
                            // TODO: Need to fix this logic for ant sonar analysis using windows
                            /*sh "${sonarHome}\\bin\\sonar-scanner " +
                                    "-Dsonar.host.url=${SONAR_HOST_URL} " +
                                    "-Dsonar.projectKey=${antProjectName} " +
                                    "-Dsonar.projectName=${antProjectName} " +
                                    "-Dsonar.projectVersion=${buildNumber} " +
                                    "-Dsonar.java.source=1.8 " +
                                    "-Dsonar.sources=./src " +
                                    "-Dsonar.java.libraries=./lib "*/
                        }
                    }
                }, mavenAnalysis: {
                    if (isUnix()) {
                        dir('devops-web-maven/') {
                            sh "'${mvnHome}/bin/mvn' sonar:sonar"
                        }
                    } else {
                        dir('devops-web-maven\\') {
                            sh "'${mvnHome}\\bin\\mvn' sonar:sonar"
                        }
                    }
                }, failFast: true
            }
        }

        stage('Publish') {
            if (isArchivalEnabled) {
                echo 'Publish Artifacts & appConfig.json in progress'
                parallel antPublish: {
                    try {
                        if (isUnix()) {
                            dir('devops-web/') {
                                if (fileExists('dist/devops-web.war')) {
                                    // upload artifactory and also publish build info
                                    artifactoryPublishInfo = artifactoryServer.upload(uploadAntSpecUnix)
                                    artifactoryPublishInfo.retention maxBuilds: 5
                                    // and publish build info to artifactory
                                    artifactoryServer.publishBuildInfo(artifactoryPublishInfo)
                                } else {
                                    error 'antPublish: Failed during file upload/publish to artifactory'
                                }
                            }
                        } else {
                            dir('devops-web\\') {
                                if (fileExists('dist\\devops-web.war')) {
                                    // upload artifactory and also publish build info
                                    artifactoryPublishInfo = artifactoryServer.upload(uploadAntSpecWindows)
                                    artifactoryPublishInfo.retention maxBuilds: 5
                                    // and publish build info to artifactory
                                } else {
                                    error 'antPublish: Failed during file upload/publish to artifactory'
                                }
                            }
                        }
                    } catch (exc) {
                        echo "Failure in antPublish process of Publish stage: ${exc}"
                    }
                }, mavenPublish: {
                    try {
                        if (isUnix()) {
                            dir('devops-web-maven/') {
                                sh "cp ./target/devops-web-maven*.jar ./target/devops-web-maven.jar"
                                if (fileExists('target/devops-web-maven.jar')) {
                                    // upload artifactory and also publish build info
                                    artifactoryPublishInfo = artifactoryServer.upload(uploadMavenSpecUnix)
                                    artifactoryPublishInfo.retention maxBuilds: 5
                                    // and publish build info to artifactory
                                    artifactoryServer.publishBuildInfo(artifactoryPublishInfo)
                                } else {
                                    error 'mavenPublish: Failed during file upload/publish to artifactory'
                                }
                            }
                        } else {
                            dir('devops-web-maven\\') {
                                bat(/copy .\\target\\devops-web-maven*.jar .\\target\\devops-web-maven.jar/)
                                if (fileExists('target/devops-web-maven.jar')) {
                                    // upload artifactory and also publish build info
                                    artifactoryPublishInfo = artifactoryServer.upload(uploadMavenSpecWindows)
                                    artifactoryPublishInfo.retention maxBuilds: 5
                                    // and publish build info to artifactory
                                    artifactoryServer.publishBuildInfo(artifactoryPublishInfo)
                                } else {
                                    error 'mavenPublish: Failed during file upload/publish to artifactory'
                                }
                            }
                        }
                    } catch (exc) {
                        echo "Failure in mavenPublish process of Publish stage: ${exc}"
                    }
                },failFast: true
            }
        }

        stage('Deployment') {
            // TODO: Add code for deployment of both projects
            echo 'Deploy applications to docker/cloud instances'
            try {
                parallel antDeploy: {
                    if (isUnix()) {
                        dir('devops-web/') {
                            // Do something
                        }
                    } else {
                        dir('devops-web\\') {
                            // Do Something else
                        }
                    }
                }, mavenDeploy: {
                    if (isUnix()) {
                        dir('devops-web-maven/') {
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
                        }
                    } else {
                        dir('devops-web-maven\\') {
                            // Do Something else
                        }
                    }
                }
            } catch(exc) {
                echo "Failure in Deployment stage: ${exc}"
            }
        }

        stage('Generate Reports') {
            junit '**/target/surefire-reports/TEST-*.xml'
        }

        stage('Finish & Cleanup') {
            // cleanWs() // cleanup workspace after build is complete
        }
    }
} catch (exc) {
    echo "Caught: ${exc}"
}