// Specify whether or not to build platform by default
def buildDefinitions = [ 'windows' : true , 'macos' : true , 'ubuntu' : true ]

// Keep track of builds that fail
def failedBuilds = [:]

stage('Build Customisation') {
    try {
        // Wait limited amount of time for user input
        timeout(time: 30, unit: 'SECONDS') {

            // Update the build definitions based on user input
            buildDefinitions = input(
                message: 'Toggle which builds to run (Abort will use default)',

                // Use custom global function to generate boolean input parameters based on a map
                // Sets default value to value in input map
                parameters: generateInputBoolParams( buildDefinitions )
            )
        }

    // Continue pipeline if user input not provided within time limit
    } catch ( error ) {
        echo 'Using default pipeline configuration...'
    }

    // Check that at least one build platform is selected
    if ( !mapContainsTrue( buildDefinitions ) ) {
        error 'No builds selected, aborting pipeline'
    }
}

stage('Conditional Build') {
    parallel (
        'Windows' : {
            // Prevent a build failure from terminating the pipeline after this stage
            try {
                // Check if windows build is set to run
                if ( buildDefinitions['windows'] ) {

                    node('windows') {
                        checkout(scm)
                        bat 'build.bat default-windows'
                    }
                } else {
                    echo 'Build was disabled by user'
                }

            // Catch an error in the build
            } catch ( error ) {
                // Make note that the build failed
                failedBuilds['windows'] = true

                // Set the pipeline status as failure
                currentBuild.result = 'FAILURE'
            }
        },

        'MacOS' : {
            try {
                if ( buildDefinitions['macos'] ) {
                    node('macos') {
                        checkout(scm)
                        sh './build.sh default-macos'
                    }
                } else {
                    echo 'Build was disabled by user'
                }
            } catch ( error ) {
                failedBuilds['macos'] = true
                currentBuild.result = 'FAILURE'
            }
        },

        'Ubuntu' : {
            try {
                if ( buildDefinitions['ubuntu'] ) {
                    node('ubuntu') {
                        checkout(scm)
                        sh './build.sh default-ubuntu'
                    }
                } else {
                    echo 'Build was disabled by user'
                }
                error 'test error'
            } catch ( error ) {
                failedBuilds['ubuntu'] = true
                currentBuild.result = 'FAILURE'
            }
        }
    )

    // Check if any builds have been marked as failed
    if ( mapContainsTrue( failedBuilds ) ) {

        // Remove failed builds from the original map of enabled builds
        def updatedBuildDefinitions = subtractMap( buildDefinitions, failedBuilds )

        // Check that there are builds left to run
        if ( mapContainsTrue( updatedBuildDefinitions ) ) {

            // Update the original build map
            buildDefinitions = updatedBuildDefinitions

            // Lists the failed builds and asks whether to continue or abort the pipeline
            timeout(time: 30, unit: 'SECONDS') {
                input(
                    message: 'Builds failed ' + getKeyset( failedBuilds ) + ', do you want to continue the pipeline and skip failed builds?'
                )
            }
        } else {
            // Throw an error to terminate the pipeline if no builds are left to run
            error 'No builds left to run'
        }
    }
}

stage('Conditional Downstream') {
    parallel (
        'Windows' : {
            if ( buildDefinitions['windows'] ) {
                echo 'You chose to run the windows build!'
            } else {
                echo 'The windows build was skipped'
            }
        },

        'MacOS' : {
            if ( buildDefinitions['macos'] ) {
                echo 'You chose to run the macos build!'
            } else {
                echo 'The macos build was skipped'
            }
        },

        'Ubuntu' : {
            if ( buildDefinitions['ubuntu'] ) {
                echo 'You chose to run the ubuntu build!'
            } else {
                echo 'The ubuntu build was skipped'
            }
        }
    )
}



// subtractMap.groovy
def call ( map1, map2 ) {
    return map1 - map2
}

// mapContainsTrue.groovy
boolean call ( array ) {
    for ( entry in array ) {
        if ( entry.value == true ) {
            isBuildConfigValid = true
            return true
        } else {
            return false
        }
    }
}

// getKeyset.groovy
def call ( map ) {
    return map.keySet() as String[]
}

// generateInputBoolParams.groovy
def call ( array ) {
    def parameterList = []
    for ( item in array ) {
        parameterList.add( booleanParam(defaultValue: item.value, name: item.key) )
    }
    return parameterList
}