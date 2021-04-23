import groovy.json.JsonSlurper

// Generic commands
def runCommand(command) {
    if (Boolean.valueOf(env.UNIX)) {
        sh command
    } else {
        bat command
    }
}

// Local commands
def installDependencies() {
    echo 'Installing npm dependencies'
    runCommand('npm install')
}

def loadSfdxEnvironment(salesforceEnvironment) {
    echo 'loadSfdxEnvironment(salesforceEnvironment)==' + salesforceEnvironment
    def sfdxEnvironments = loadSfdxEnvironments()
    def environment = sfdxEnvironments[salesforceEnvironment]
    return environment
}

def loadSfdxEnvironments() {
    def jsonData = readFile(file: 'Jenkins.sfdx-environments.json')
    def sfdxEnvironments = new JsonSlurper().parseText(jsonData);

    def sfdxEnvironmentsByName = [:]
    for(sfdxEnvironment in sfdxEnvironments) {
        sfdxEnvironmentsByName[sfdxEnvironment.name] = sfdxEnvironment
    }

    echo 'sfdxEnvironmentsByName==' + sfdxEnvironmentsByName
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
    echo 'packageDirectories==' + packageDirectories
    return packageDirectories
}

def convertSourceToMdapiFormat() {
    def convertCommand = 'sfdx force:source:convert --rootdir ' + env.sfdxPackageDirectories + ' --outputdir ./mdapi/'
    runCommand(convertCommand)
}

def runApexScanner() {
    runCommand('sfdx scanner:run --target "./**/*cls" --format junit --outfile scanner-results.xml')
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

// Scratch org commands
def createScratchOrg(scratchOrgAlias, scratchDefinitionFile) {
    scratchOrgAlias = scratchOrgAlias.replace('/', '-')

    echo 'Creating scratch org alias: ' + scratchOrgAlias
    // TODO get the dev hub from Jenkins.sfdx-environments.json
    def createScratchOrgCommand = 'sfdx force:org:create --definitionfile ' + scratchDefinitionFile + ' --setalias ' + scratchOrgAlias + ' --durationdays 1'
    runCommand(createScratchOrgCommand)
}

def deleteScratchOrg(scratchOrgAlias) {
    scratchOrgAlias = scratchOrgAlias.replace('/', '-')

    echo 'Deleting scratch org alias: ' + scratchOrgAlias
    // TODO get the dev hub from Jenkins.sfdx-environments.json
    def deleteScratchOrgCommand = 'sfdx force:org:delete --noprompt --targetusername ' + scratchOrgAlias
    runCommand(deleteScratchOrgCommand)
}

def pushToScratchOrg(scratchOrgAlias) {
    scratchOrgAlias = scratchOrgAlias.replace('/', '-')

    def pushCommand = 'sfdx force:source:push --targetusername ' + scratchOrgAlias
    runCommand(pushCommand)
}

def runScratchOrgApexTests(scratchOrgAlias) {
    scratchOrgAlias = scratchOrgAlias.replace('/', '-')

    // try {
        def outputDirectory = './tests/' + scratchOrgAlias
        echo 'Executing Apex tests in ' + scratchOrgAlias
        runCommand('sfdx force:apex:test:run --testlevel RunLocalTests --outputdir ' + outputDirectory + ' --resultformat tap --targetusername ' + scratchOrgAlias)
    // } catch(Exception error) {
    //     // If any tests fail, SFDX throws an exception, which fails the build
    //     // We mark the build as unstable instead of failing it
    //     unstable('Apex test failure')
    // }
}

// Deploy commands
def deployToSalesforce(salesforceEnvironment, commitChanges, deployOnlyDiff) {
    try {
        def checkOnlyParam = commitChanges ? '' : ' --checkonly --testlevel RunLocalTests'
        def deployMessage  = commitChanges ? '. Deployment changes will be saved.' : '. Running check-only validation - deployment changes will not be saved.'
        def environmentDetails = loadSfdxEnvironment(salesforceEnvironment)
        echo 'Starting Salesforce deployment for environment: ' + salesforceEnvironment
        echo 'commitChanges is: ' + commitChanges + deployMessage
        echo 'deployOnlyDiff is: ' + deployOnlyDiff
        echo 'SFDX package directories: ' + env.sfdxPackageDirectories

        // When using SFDX's default timeout + multiple environments + multiple branches,
        // we've had issues with Jenkins jobs would continue running, waiting for a deployment result... that would never come :-(
        // Adding the --wait parameter with a longer time helps reduce/prevent this

        def deployCommand;
        if (environmentDetails.deployOnlyDiff) {
            runCommand('sfdx sgd:source:delta --to HEAD --from HEAD^ --output ./mdapi/ --generate-delta')
            runCommand('mv ./mdapi/destructiveChanges/destructiveChanges.xml ./mdapi/package/destructiveChangesPost.xml')
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

def deleteObsoleteFlowVersions(salesforceEnvironment) {
    runCommand('python ./scripts/deployment/delete-old-flow-versions --targetusername ' + salesforceEnvironment)
}

def publishCommunitySite(salesforceEnvironment, commitChanges, communitySiteName) {
    if(commitChanges) {
        echo 'Publishing Community Cloud site in ' + salesforceEnvironment
        def publishCommand = 'sfdx force:community:publish --name "SF Assessor Office"' + ' --targetusername ' + salesforceEnvironment
        runCommand(publishCommand)
    }
}

def runApexScript(salesforceEnvironment, apexCodeFile) {
    echo 'Executing Apex script in ' + salesforceEnvironment
    runCommand('sfdx force:apex:execute --apexcodefile ' + apexCodeFile + ' --targetusername ' + salesforceEnvironment)
}

def upsertCsvFiles(salesforceEnvironment) {//}, sobjectType, externalId) {
    //def csvFile = './config/data/' + sobjectType + '.csv'
    echo 'TODO Upserting data'
    //runCommand('sfdx force:data:bulk:upsert --sobjecttype ' + sobjectType + ' --externalid ' + externalId + ' --csvfile ' + csvFile + ' --targetusername ' + salesforceEnvironment)
}

return this