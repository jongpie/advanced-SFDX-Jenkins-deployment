// Jenkins build pipeline for Salesforce

import groovy.json.JsonSlurper

// Primary git branches
def MAIN_BRANCH           = 'main'
def UAT_BRANCH            = 'uat'
def DEVELOP_BRANCH        = 'develop'

// Git branch prefixes
def RELEASE_PREFIX = 'release/*'
def HOTFIX_PREFIX  = 'hotfix/*'
def FEATURE_PREFIX = 'feature/*'
def BUGFIX_PREFIX  = 'bugfix/*'

// Salesforce environments (stored in Jenkins credentials)
def PRODUCTION      = 'Salesforce-Production'
def STAGING_SANDBOX = 'Salesforce-Staging'
def UAT_SANDBOX     = 'Salesforce-UAT'
def DATAMIG_SANDBOX = 'Salesforce-DataMig'
def QA_SANDBOX      = 'Salesforce-Production' //'Salesforce-QA' temp using prod org for testing
def SCRATCH_ORG     = 'Salesforce-Scratch'
def SCRATCH_DEFINITION_FILE = "config/project-scratch-def.json"

// Static variables
// def BUILD_NOTIFICATION_EMAIL = 'someone@test.com'
def SCHEDULE_JOBS_SCRIPT     = './scripts/schedule-jobs.apex'
def POPULATE_CUSTOM_SETTINGS_SCRIPT = './scripts/populate-custom-settings.apex'

// Methods for commands
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
    def convertCommand = 'sfdx force:source:convert --rootdir ' + env.sfdxPackageDirectories + ' --outputdir mdapi/src'
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
    echo 'TODO'
    //runCommand('sfdx force:lightning:lwc:test:run')
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
            // runCommand('sfdx sgd:source:delta --to HEAD --from HEAD^ --output . --generate-delta')
            deployCommand = ''
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
    echo 'TODO'
    //runCommand('sfdx scanner:run --target "force-app" --engine "pmd" --format junit --outfile scanner/results.xml')
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

pipeline {
    agent any
    stages {
        // stage('Install NPM Dependencies') {
        //     steps { installDependencies() }
        // }
        stage('Load SFDX Config') {
            steps {
                script {
                    env.sfdxEnvironments = loadSfdxEnvironments()
                    env.sfdxPackageDirectories = loadSfdxPackageDirectories()
                }
                echo "${env.sfdxEnvironments}"
                echo "${env.sfdxPackageDirectories}"
            }
        }
        stage('Run Apex Scanner') {
            when  { anyOf { branch FEATURE_PREFIX; branch BUGFIX_PREFIX } }
            steps { runApexScanner() }
        }
        stage('Run LWC Tests') {
            when  { anyOf { branch FEATURE_PREFIX; branch BUGFIX_PREFIX } }
            steps { runLwcTests() }
        }
        stage('Convert Source to MDAPI') {
            when { anyOf {branch FEATURE_PREFIX; branch DEVELOP_BRANCH; branch UAT_BRANCH; branch MAIN_BRANCH } }
            steps { convertSourceToMdapiFormat() }
        }
        stage('Deploy to Salesforce') {
            parallel {
                stage('1. Production') {
                    when  { anyOf { branch RELEASE_PREFIX; branch MAIN_BRANCH } }
                    steps {
                        authorizeEnvironment(PRODUCTION)
                        deployToSalesforce(PRODUCTION, env.BRANCH_NAME == MAIN_BRANCH, true)
                        publishCommunitySite(PRODUCTION, env.BRANCH_NAME == MAIN_BRANCH, 'My_Community_Site1')
                    }
                }
                stage('2. Staging') {
                    when  { anyOf { branch HOTFIX_PREFIX; branch RELEASE_PREFIX } }
                    steps {
                        authorizeEnvironment(STAGING_SANDBOX)
                        deployToSalesforce(STAGING_SANDBOX, env.BRANCH_NAME == RELEASE_PREFIX, false)
                        publishCommunitySite(STAGING_SANDBOX, env.BRANCH_NAME == RELEASE_PREFIX, 'My_Community_Site1')
                    }
                }
                stage('3. UAT') {
                    // Run a check-only validation when the git branch is 'develop'
                    // Run a deployment when the git branch is 'uat'
                    when  { anyOf { branch DEVELOP_BRANCH; branch UAT_BRANCH } }
                    steps {
                        authorizeEnvironment(UAT_SANDBOX)
                        deployToSalesforce(UAT_SANDBOX, env.BRANCH_NAME == UAT_BRANCH, false)
                        publishCommunitySite(UAT_SANDBOX, env.BRANCH_NAME == UAT_BRANCH, 'My_Community_Site1')
                    }
                }
                stage('4. DataMig') {
                    // Run a check-only validation when the git branch is 'develop'
                    // Run a deployment when the git branch is 'uat'
                    when  { anyOf { branch DEVELOP_BRANCH; branch UAT_BRANCH } }
                    steps {
                        authorizeEnvironment(DATAMIG_SANDBOX)
                        deployToSalesforce(DATAMIG_SANDBOX, env.BRANCH_NAME == UAT_BRANCH, false)
                        publishCommunitySite(DATAMIG_SANDBOX, env.BRANCH_NAME == UAT_BRANCH, 'My_Community_Site1')
                    }
                }
                stage('5. QA') {
                    when  { anyOf {branch FEATURE_PREFIX; branch DEVELOP_BRANCH } }
                    steps {
                        authorizeEnvironment(QA_SANDBOX)
                        deployToSalesforce(QA_SANDBOX, env.BRANCH_NAME == DEVELOP_BRANCH, false)
                        //publishCommunitySite(QA_SANDBOX, env.BRANCH_NAME == DEVELOP_BRANCH, 'My_Community_Site1')
                    }
                }
                stage('Scratch Org') {
                    when  { anyOf { branch BUGFIX_PREFIX; } }
                    steps {
                        createScratchOrg()
                        // authorizeEnvironment(PRODUCTION)
                        // deployToSalesforce(PRODUCTION, false, false)
                        // //publishCommunitySite(SCRATCH_ORG, env.BRANCH_NAME == DEVELOP_BRANCH, 'My_Community_Site1')
                        // runApexTests(SCRATCH_ORG)
                    }
                }
            }
        }
        stage('Upsert Custom Settings') {
            when     { anyOf { branch FEATURE_PREFIX; branch DEVELOP_BRANCH; branch UAT_BRANCH } }
            parallel {
                stage('1. Production') {
                    when  { branch MAIN_BRANCH }
                    steps {
                        runApexScript(STAGING_SANDBOX, POPULATE_CUSTOM_SETTINGS_SCRIPT)
                    }
                }
                stage('2. Staging') {
                    when  { branch HOTFIX_PREFIX }
                    steps {
                        runApexScript(STAGING_SANDBOX, POPULATE_CUSTOM_SETTINGS_SCRIPT)
                    }
                }
                stage('3. UAT') {
                    when  { branch UAT_BRANCH }
                    steps {
                        runApexScript(UAT_SANDBOX, POPULATE_CUSTOM_SETTINGS_SCRIPT)
                    }
                }
                stage('4. DataMig') {
                    when  { branch UAT_BRANCH }
                    steps {
                        runApexScript(DATAMIG_SANDBOX, POPULATE_CUSTOM_SETTINGS_SCRIPT)
                    }
                }
                stage('5. QA') {
                    when  { anyOf {branch FEATURE_PREFIX; branch DEVELOP_BRANCH } }
                    steps {
                        runApexScript(QA_SANDBOX, POPULATE_CUSTOM_SETTINGS_SCRIPT)
                    }
                }
                stage('Scratch Org') {
                    when  { branch DEVELOP_BRANCH }
                    steps {
                        runApexScript(SCRATCH_ORG, POPULATE_CUSTOM_SETTINGS_SCRIPT)
                    }
                }
            }
        }
        stage('Upsert Config Data') {
            when     { anyOf { branch FEATURE_PREFIX; branch DEVELOP_BRANCH; branch UAT_BRANCH } }
            parallel {
                stage('1. Production') {
                    when  { branch MAIN_BRANCH }
                    steps { loadCsvFile(PRODUCTION, 'User', 'MyExternalId__c') }
                }
                stage('2. Staging') {
                    when  { branch HOTFIX_PREFIX }
                    steps { loadCsvFile(STAGING_SANDBOX, 'User', 'MyExternalId__c') }
                }
                stage('3. UAT') {
                    when  { branch UAT_BRANCH }
                    steps { loadCsvFile(UAT_SANDBOX, 'User', 'MyExternalId__c') }
                }
                stage('4. DataMig') {
                    when  { branch UAT_BRANCH }
                    steps { loadCsvFile(DATAMIG_SANDBOX, 'User', 'MyExternalId__c') }
                }
                stage('5. QA') {
                    when  { anyOf {branch FEATURE_PREFIX; branch DEVELOP_BRANCH } }
                    steps { loadCsvFile(QA_SANDBOX, 'User', 'MyExternalId__c') }
                }
                stage('Scratch Org') {
                    when  { branch DEVELOP_BRANCH }
                    steps { loadCsvFile(SCRATCH_ORG, 'User', 'MyExternalId__c') }
                }
            }
        }
        stage('Schedule Apex Jobs') {
            when     { anyOf { branch FEATURE_PREFIX; branch DEVELOP_BRANCH; branch UAT_BRANCH } }
            parallel {
                stage('1. Production') {
                    when  { branch MAIN_BRANCH }
                    steps {
                        runApexScript(STAGING_SANDBOX, SCHEDULE_JOBS_SCRIPT)
                    }
                }
                stage('2. Staging') {
                    when  { branch HOTFIX_PREFIX }
                    steps {
                        runApexScript(STAGING_SANDBOX, SCHEDULE_JOBS_SCRIPT)
                    }
                }
                stage('3. UAT') {
                    when  { branch UAT_BRANCH }
                    steps {
                        runApexScript(UAT_SANDBOX, SCHEDULE_JOBS_SCRIPT)
                    }
                }
                stage('4. DataMig') {
                    when  { branch UAT_BRANCH }
                    steps {
                        runApexScript(DATAMIG_SANDBOX, SCHEDULE_JOBS_SCRIPT)
                    }
                }
                stage('5. QA') {
                    when  { anyOf {branch FEATURE_PREFIX; branch DEVELOP_BRANCH } }
                    steps {
                        runApexScript(QA_SANDBOX, SCHEDULE_JOBS_SCRIPT)
                    }
                }
                stage('Scratch Org') {
                    when  { branch DEVELOP_BRANCH }
                    steps {
                        runApexScript(SCRATCH_ORG, SCHEDULE_JOBS_SCRIPT)
                    }
                }
            }
        }
    }
    post {
        always {
            junit allowEmptyResults: true, testResults: 'tests/**/*.xml'

            //recordIssues enabledForFailure: true, tool: pmdParser(pattern: '**/target/pmd.xml')

            // cleanWs()
        }
    //     // success {
    //     //     emailext to : "${BUILD_NOTIFICATION_EMAIL}",
    //     //         subject : 'SUCCESS: $PROJECT_NAME - #$BUILD_NUMBER',
    //     //         body    : 'Build Successful $PROJECT_NAME - #$BUILD_NUMBER'
    //     // }
    //     // unstable {
    //     //     emailext to : "${BUILD_NOTIFICATION_EMAIL}",
    //     //         subject : 'UNSTABLE: $PROJECT_NAME - #$BUILD_NUMBER',
    //     //         body    : 'Check console output at $BUILD_URL to view the results. <br/> Last Changes: ${CHANGES} <br/> Last 100 lines of logs <br/> ${BUILD_LOG, maxLines=100, escapeHtml=false}'
    //     // }
    //     // failure {
    //     //     emailext to : "${BUILD_NOTIFICATION_EMAIL};${env.FAILURE_NOTIFICATION_EMAIL}",
    //     //         subject : 'FAILED: $PROJECT_NAME - #$BUILD_NUMBER',
    //     //         body    : 'Check console output at $BUILD_URL to view the results. <br/> Last Changes:  ${CHANGES} <br/> Last 100 lines of logs <br/> ${BUILD_LOG, maxLines=100, escapeHtml=false}'
    //     // }
    }
}
