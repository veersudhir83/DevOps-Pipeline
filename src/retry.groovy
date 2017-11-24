//
// Coloured Messages: http://testerfenster.com/blog/jenkins-tutorials-add-color-to-console-output/
//
String boldGreenMessage(final String message) { return "\033[1;32m${message}\033[0m" }
String boldBlueMessage(final String message) { return "\033[1;34m${message}\033[0m" }
String boldRedMessage(final String message) { return "\033[1;31m${message}\033[0m" }
String boldYellowMessage(final String message) { return "\033[1;33m${message}\033[0m" }
String triplePrefixMessage(final Closure<String> colour, final String prefix, final String message) {
    def colouredPrefix = "${colour("${prefix}")}"
    def colouredMessage = "${colour("${message}")}"
    return "${colouredPrefix}\n${colouredPrefix} ${colouredMessage}\n${colouredPrefix}"
}
void successMessage(final String message) { ansiColor('xterm') { echo triplePrefixMessage(this.&boldGreenMessage, '[SUCCESS]', message) } }
void infoMessage(final String message) { ansiColor('xterm') { echo triplePrefixMessage(this.&boldBlueMessage, '[INFO]', message) } }
void warningMessage(final String message) { ansiColor('xterm') { echo triplePrefixMessage(this.&boldYellowMessage, '[WARNING]', message) } }
void errorMessage(final String message) { ansiColor('xterm') { echo triplePrefixMessage(this.&boldRedMessage, '[ERROR]', message) } }

//
// Retry, Continue or Abort on errors
//
public <R> R retryContinueOrAbort(final Closure<R> action, final int count = 0) {

    infoMessage "Trying action, attempt count is: ${count}"

    try {
        return action.call();
    } catch (final exception) {

        errorMessage exception.toString()

        def userChoice
        timeout(time: 30, unit: 'MINUTES') {
             userChoice = input(
                message: 'Something went wrong, what do you want to do next?',
                parameters: [
                    choice(
                        name: 'Next Action',
                        choices: ['Retry', 'Continue', 'Abort'].join('\n'),
                        description: 'Whats your next action?'
                    )
                ]
            )
        }

        switch (userChoice) {
            case 'Retry':
                warningMessage 'User has opted to try the action again.'
                return retryContinueOrAbort(action, count + 1)
            case 'Continue':
                warningMessage 'User has opted to continue past the action, they must have manually fixed things.'
                return null;
            default:
                errorMessage 'User has opted to abort the action'
                throw exception;
        }
    }
};

//
// Test Pipeline Script
//
pipeline {
    agent none
    stages {
        stage('Reach the bridge') {
            steps {
                infoMessage 'Stop. Who would cross the Bridge of Death must answer me these questions three, ere the other side he see.'
            }
        }
        stage('Answer a question') {
            steps {
                script {
                    favouriteColour = retryContinueOrAbort {
                        infoMessage 'What... is your favourite colour?'
                        error 'Blue. No, yel... [he is also thrown over the edge] auuuuuuuugh.'
                    }
                }
            }
        }
        stage('Get POM Version') {
            steps {
                successMessage 'You have made it past the bridge keeper!'
                script {
                    mavenVersion = retryContinueOrAbort {
                        "0.0.1-SNAPSHOT"
                    }
                }
            }
        }
        stage('Print Variables') {
            steps {
                successMessage "favouriteColour: ${favouriteColour}"
                successMessage "mavenVersion: ${mavenVersion}"
            }
        }
    }
}
