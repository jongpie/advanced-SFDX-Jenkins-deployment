import groovy.json.JsonSlurper

def installDependencies() {
    echo 'Installing npm dependencies'
    runCommand('npm install')
}

def loadSfdxEnvironments() {
    def jsonData = readFile(file: 'sfdx-environments.json')
    def sfdxEnvironments = new JsonSlurper().parseText(jsonData);

    def sfdxEnvironmentsByName = [:]
    for(sfdxEnvironment in sfdxEnvironments) {
        sfdxEnvironmentsByName[sfdxEnvironment.name] = sfdxEnvironment
    }

    println('sfdxEnvironmentsByName==' + sfdxEnvironmentsByName)
    return sfdxEnvironmentsByName
}

def loadSfdxPackageDirectories() {
    def jsonData = readFile(file: 'sfdx-project.json')
    def sfdxProject = new JsonSlurper().parseText(jsonData);

    List<String> packageDirectoryPaths = []
    for(packageDirectory in sfdxProject.packageDirectories) {
        packageDirectoryPaths.add(packageDirectory.path)
    }

    def packageDirectories = packageDirectoryPaths.join(',')
    println('packageDirectories==' + packageDirectories)
    return packageDirectories
}

def convertSourceToMdapiFormat() {
    def convertCommand = 'sfdx force:source:convert --rootdir ' + env.sfdxPackageDirectories + ' --outputdir ./mdapi/'
    runCommand(convertCommand)
}

def runCommand(command) {
    if (Boolean.valueOf(env.UNIX)) {
        sh command
    } else {
        bat command
    }
}

def runLwcTests() {
    echo 'Running LWC tests'
    runCommand('npm test --coverage')
}

def authorizeEnvironment(salesforceEnvironment) {
    withCredentials([string(credentialsId: salesforceEnvironment, variable: 'sfdxAuthUrl')]) {
        def authCommand = 'sfdx force:auth:sfdxurl:store --sfdxurlfile=' + salesforceEnvironment + ' --setalias ' + salesforceEnvironment
        def deleteCommand;
        if (Boolean.valueOf(env.UNIX)) {
            deleteCommand = 'rm ' + salesforceEnvironment
        } else {
            deleteCommand = 'del ' + salesforceEnvironment
        }

        writeFile(file: salesforceEnvironment, text: sfdxAuthUrl, encoding: "UTF-8")
        runCommand(authCommand)
        runCommand(deleteCommand)
    }
}

def createScratchOrg() {
    runCommand('echo TODO make a scratch org!')
}

def deployToSalesforce(salesforceEnvironment, commitChanges, deployOnlyDiff) {
    try {
        def checkOnlyParam = commitChanges ? '' : ' --checkonly --testlevel RunLocalTests'
        def deployMessage  = commitChanges ? '. Deployment changes will be saved.' : '. Running check-only validation - deployment changes will not be saved.'
        echo 'Starting Salesforce deployment for environment: ' + salesforceEnvironment
        echo 'commitChanges is: ' + commitChanges + deployMessage
        echo 'deployOnlyDiff is: ' + deployOnlyDiff
        echo 'SFDX package directories: ' + env.sfdxPackageDirectories

        // When using SFDX's default timeout + multiple environments + multiple branches,
        // we've had issues with Jenkins jobs would continue running, waiting for a deployment result... that would never come :-(
        // Adding the --wait parameter with a longer time helps reduce/prevent this

        def deployCommand;
        if (deployOnlyDiff) {
            runCommand('sfdx sgd:source:delta --to HEAD --from HEAD^ --output ./mdapi/ --generate-delta')
            runCommand('mv ./mdapi/destructiveChanges/destructiveChanges.xml ./mdapi/package/destructiveChangesPre.xml')
            deployCommand = 'sfdx force:mdapi:deploy --verbose ' + checkOnlyParam + ' --wait 1440 --manifest ./mdapi/package/package.xml --targetusername ' + salesforceEnvironment
        } else {
            deployCommand = 'sfdx force:source:deploy --verbose' + checkOnlyParam + ' --wait 1440 --sourcepath ' + env.sfdxPackageDirectories + ' --targetusername ' + salesforceEnvironment
        }

        runCommand(deployCommand)
    } catch(Exception error) {
        if(commitChanges) {
            // If we're supposed to be committing changes and there's an error, throw the error
            throw error
        } else {
            // If we're running a check-only validation & it fails, mark the step as unstable
            unstable('Check-only deploy failure for Salesforce')
        }
    }
}

def publishCommunitySite(salesforceEnvironment, commitChanges, communitySiteName) {
    if(commitChanges) {
        echo 'Publishing Community Cloud site in ' + salesforceEnvironment
        def publishCommand = 'sfdx force:community:publish --name "SF Assessor Office"' + ' --targetusername ' + salesforceEnvironment
        runCommand(publishCommand)
    }
}

def runApexScanner() {
    runCommand('sfdx scanner:run --target "force-app" --engine "pmd" --format junit --outfile scanner/results.xml')
}

def runApexScript(salesforceEnvironment, apexCodeFile) {
    echo 'Executing Apex script in ' + salesforceEnvironment
    runCommand('sfdx force:apex:execute --apexcodefile ' + apexCodeFile + ' --targetusername ' + salesforceEnvironment)
}

def runApexTests(salesforceEnvironment) {
    try {
        def outputDirectory = './tests/' + salesforceEnvironment
        echo 'Executing Apex tests in ' + salesforceEnvironment
        runCommand('sfdx force:apex:test:run --testlevel RunLocalTests --outputdir ' + outputDirectory + ' --resultformat tap --targetusername ' + salesforceEnvironment)
    } catch(Exception error) {
        // If any tests fail, SFDX throws an exception, which fails the build
        // We mark the build as unstable instead of failing it
        unstable('Apex test failure')
    }
}

def loadCsvFile(salesforceEnvironment, sobjectType, externalId) {
    def csvFile = './config/data/' + sobjectType + '.csv'
    echo 'Upserting data'
    //runCommand('sfdx force:data:bulk:upsert --sobjecttype ' + sobjectType + ' --externalid ' + externalId + ' --csvfile ' + csvFile + ' --targetusername ' + salesforceEnvironment)
}

return this