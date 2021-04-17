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
def PRODUCTION      = loadEnvironment('Production')
def STAGING_SANDBOX = loadEnvironment('Staging')
def UAT_SANDBOX     = loadEnvironment('UAT')
def QA_SANDBOX      = loadEnvironment('QA')
def SCRATCH_ORG = readJSON text: '{ "name": "scratch-org" }'
def SCRATCH_DEFINITION_FILE = "config/project-scratch-def.json"

// Static variables
// def BUILD_NOTIFICATION_EMAIL = 'someone@test.com'
// def SCHEDULE_JOBS_SCRIPT     = './scripts/schedule-jobs.apex'
// def POPULATE_CUSTOM_SETTINGS_SCRIPT = './scripts/populate-custom-settings.apex'

// Methods for commands
def installDependencies() {
    sh label: 'Installing npm dependencies', script: 'npm install'
}

def loadEnvironment(salesforceEnvironmentName) {
    return salesforceEnvironmentName
    //def environments = readJSON file: '${env.WORKSPACE}/sfdx-environments.json'
    //def sfdxEnvironments = readJSON file: ".//sfdx-environments.json"
    //File file = new File('sfdx-environments.json')
    // def sfdxEnvironments = jsonSlurper.parseText('{ "name": "John", "ID" : "1"}')


    // def jsonSlurper = new JsonSlurper()
    // def jsonData = jsonSlurper.parse(new File('sfdx-environments.json'))
    // def sfdxEnvironments = readJSON text: jsonData

    // def matchingEnvironment
    // for (Object env : sfdxEnvironments) {
    //     if (env.name == salesforceEnvironmentName) {
    //         matchingEnvironment = env
    //         break
    //     }
    // }
}

def getPackageDirectories() {
//     "packageDirectories": [
//     {
//       "path": "force-app",
//       "default": true
//     }
//   ]
}

def convertSourceToMdapiFormat() {

}

def authorizeEnvironment(salesforceEnvironment) {
    withCredentials([string(credentialsId: salesforceEnvironment, variable: 'sfdxAuthUrl')]) {
        sh label: 'Creating authorization file', script: 'echo "$sfdxAuthUrl" > ' + salesforceEnvironment
        sh label: 'Authorizing Salesforce environment: ' + salesforceEnvironment, script: 'sfdx force:auth:sfdxurl:store --sfdxurlfile=' + salesforceEnvironment + ' --setalias ' + salesforceEnvironment
        sh label: 'Purging authorization file', script: 'rm ' + salesforceEnvironment
    }
}

def createScratchOrg() {

}

def deploy(salesforceEnvironment, commitChanges, deployOnlyDiff) {
    try {
        def checkOnlyParam = commitChanges ? '' : ' --checkonly --testlevel RunLocalTests'
        def deployMessage  = commitChanges ? '. Deployment changes will be saved.' : '. Running check-only validation - deployment changes will not be saved.'
        echo 'Starting Salesforce deployment for environment: ' + salesforceEnvironment
        echo 'commitChanges is: ' + commitChanges + deployMessage
        echo 'deployOnlyDiff is: ' + deployOnlyDiff

        // When using SFDX's default timeout + multiple environments + multiple branches,
        // we've had issues with Jenkins jobs would continue running, waiting for a deployment result... that would never come :-(
        // Adding the --wait parameter with a longer time helps reduce/prevent this

        def deployCommand;
        if (deployOnlyDiff) {
            deployCommand = ''
        } else {
            deployCommand = 'sfdx force:source:deploy --verbose' + checkOnlyParam + ' --wait 1440 --sourcepath ./force-app/ --targetusername ' + salesforceEnvironment
        }

        sh label: 'Deploying Salesforce to ' + salesforceEnvironment, script: deployCommand
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
        sh label: 'Publishing Community Cloud site in ' + salesforceEnvironment, script: 'sfdx force:community:publish --name "SF Assessor Office"' + ' --targetusername ' + salesforceEnvironment
    }
}

def runApexScanner() {
    sh label: 'Running SFDX Scanner', script: 'sfdx scanner:run'
}

def runApexScript(salesforceEnvironment, apexCodeFile) {
    sh label: 'Executing Apex script in ' + salesforceEnvironment, script: 'sfdx force:apex:execute --apexcodefile ' + apexCodeFile + ' --targetusername ' + salesforceEnvironment
}

def runApexTests(salesforceEnvironment) {
    try {
        def outputDirectory = './tests/' + salesforceEnvironment
        sh label: 'Executing Apex tests in ' + salesforceEnvironment, script: 'sfdx force:apex:test:run --testlevel RunLocalTests --outputdir ' + outputDirectory + ' --resultformat tap --targetusername ' + salesforceEnvironment
    } catch(Exception error) {
        // If any tests fail, SFDX throws an exception, which fails the build
        // We mark the build as unstable instead of failing it
        unstable('Apex test failure')
    }
}

def loadCsvFile(salesforceEnvironment, sobjectType, externalId) {
    def csvFile = './config/data/' + sobjectType + '.csv'
    sh label: 'Upserting data', script: 'sfdx force:data:bulk:upsert --sobjecttype ' + sobjectType + ' --externalid ' + externalId + ' --csvfile ' + csvFile + ' --targetusername ' + salesforceEnvironment
}

pipeline {
    agent any
    stages {
        // stage('Install NPM Dependencies') {
        //     steps { installDependencies() }
        // }
        stage('Convert Source to MDAPI') {
            when { branch DEVELOP_BRANCH; branch UAT_BRANCH; branch MAIN_BRANCH }
            steps { convertSourceToMdapiFormat() }
        }
        stage('Deploy to Salesforce') {
            parallel {
                stage('1. Production') {
                    when  { anyOf { branch RELEASE_PREFIX; branch MAIN_BRANCH } }
                    steps {
                        authorizeEnvironment(PRODUCTION)
                        deploy(PRODUCTION, env.BRANCH_NAME == MAIN_BRANCH)
                        publishCommunitySite(PRODUCTION, env.BRANCH_NAME == MAIN_BRANCH, 'My_Community_Site1')
                    }
                }
                stage('2. Staging') {
                    when  { anyOf { branch HOTFIX_PREFIX; branch RELEASE_PREFIX } }
                    steps {
                        authorizeEnvironment(STAGING_SANDBOX)
                        deploy(STAGING_SANDBOX, env.BRANCH_NAME == RELEASE_PREFIX)
                        publishCommunitySite(STAGING_SANDBOX, env.BRANCH_NAME == RELEASE_PREFIX, 'My_Community_Site1')
                    }
                }
                stage('3. UAT') {
                    // Run a check-only validation when the git branch is 'develop'
                    // Run a deployment when the git branch is 'uat'
                    when  { anyOf { branch DEVELOP_BRANCH; branch UAT_BRANCH } }
                    steps {
                        authorizeEnvironment(UAT_SANDBOX)
                        deploy(UAT_SANDBOX, env.BRANCH_NAME == UAT_BRANCH)
                        publishCommunitySite(UAT_SANDBOX, env.BRANCH_NAME == UAT_BRANCH, 'My_Community_Site1')
                    }
                }
                stage('4. QA') {
                    when  { branch DEVELOP_BRANCH }
                    steps {
                        authorizeEnvironment(QA_SANDBOX)
                        deploy(QA_SANDBOX, env.BRANCH_NAME == DEVELOP_BRANCH)
                        publishCommunitySite(QA_SANDBOX, env.BRANCH_NAME == DEVELOP_BRANCH, 'My_Community_Site1')
                    }
                }
                stage('Scratch Org') {
                    when  { anyOf { branch FEATURE_PREFIX; branch BUGFIX_PREFIX; } }
                    steps {
                        authorizeEnvironment(SCRATCH_ORG)
                        deploy(SCRATCH_ORG, env.BRANCH_NAME == DEVELOP_BRANCH)
                        publishCommunitySite(SCRATCH_ORG, env.BRANCH_NAME == DEVELOP_BRANCH, 'My_Community_Site1')
                    }
                }
            }
        }
        stage('Load Config Data') {
            when     { anyOf { branch DEVELOP_BRANCH; branch UAT_BRANCH } }
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
                stage('4. QA') {
                    when  { branch DEVELOP_BRANCH }
                    steps { loadCsvFile(QA_SANDBOX, 'User', 'MyExternalId__c') }
                }
                stage('5. Dev') {
                    when  { branch DEVELOP_BRANCH }
                    steps { loadCsvFile(SCRATCH_ORG, 'User', 'MyExternalId__c') }
                }
            }
        }
        stage('Schedule Apex Jobs') {
            when     { anyOf { branch DEVELOP_BRANCH; branch UAT_BRANCH } }
            parallel {
                stage('1. Production') {
                    when  { branch MAIN_BRANCH }
                    steps {
                        runApexScript(STAGING_SANDBOX, SCHEDULE_JOBS_SCRIPT)
                        runApexScript(STAGING_SANDBOX, POPULATE_CUSTOM_SETTINGS_SCRIPT)
                    }
                }
                stage('2. Staging') {
                    when  { branch HOTFIX_PREFIX }
                    steps {
                        runApexScript(STAGING_SANDBOX, SCHEDULE_JOBS_SCRIPT)
                        runApexScript(STAGING_SANDBOX, POPULATE_CUSTOM_SETTINGS_SCRIPT)
                    }
                }
                stage('3. UAT') {
                    when  { branch UAT_BRANCH }
                    steps {
                        runApexScript(UAT_SANDBOX, SCHEDULE_JOBS_SCRIPT)
                        runApexScript(UAT_SANDBOX, POPULATE_CUSTOM_SETTINGS_SCRIPT)
                    }
                }
                stage('4. QA') {
                    when  { branch DEVELOP_BRANCH }
                    steps {
                        runApexScript(QA_SANDBOX, SCHEDULE_JOBS_SCRIPT)
                        runApexScript(QA_SANDBOX, POPULATE_CUSTOM_SETTINGS_SCRIPT)
                    }
                }
                stage('Scratch Org') {
                    when  { branch DEVELOP_BRANCH }
                    steps {
                        runApexScript(SCRATCH_ORG, SCHEDULE_JOBS_SCRIPT)
                        runApexScript(SCRATCH_ORG, POPULATE_CUSTOM_SETTINGS_SCRIPT)
                    }
                }
            }
        }
        stage('Get Test Coverage') {
            when  { anyOf { branch FEATURE_PREFIX; branch BUGFIX_PREFIX } }
            steps { runApexTests(SCRATCH_ORG) }
        }
        stage('Run Apex Scanner') {
            when  { anyOf { branch FEATURE_PREFIX; branch BUGFIX_PREFIX } }
            steps { runApexScanner() }
        }
    }
    post {
        always {
            junit allowEmptyResults: true, testResults: 'tests/**/*.xml'
            cleanWs()
        }
        // success {
        //     emailext to : "${BUILD_NOTIFICATION_EMAIL}",
        //         subject : 'SUCCESS: $PROJECT_NAME - #$BUILD_NUMBER',
        //         body    : 'Build Successful $PROJECT_NAME - #$BUILD_NUMBER'
        // }
        // unstable {
        //     emailext to : "${BUILD_NOTIFICATION_EMAIL}",
        //         subject : 'UNSTABLE: $PROJECT_NAME - #$BUILD_NUMBER',
        //         body    : 'Check console output at $BUILD_URL to view the results. <br/> Last Changes: ${CHANGES} <br/> Last 100 lines of logs <br/> ${BUILD_LOG, maxLines=100, escapeHtml=false}'
        // }
        // failure {
        //     emailext to : "${BUILD_NOTIFICATION_EMAIL};${env.FAILURE_NOTIFICATION_EMAIL}",
        //         subject : 'FAILED: $PROJECT_NAME - #$BUILD_NUMBER',
        //         body    : 'Check console output at $BUILD_URL to view the results. <br/> Last Changes:  ${CHANGES} <br/> Last 100 lines of logs <br/> ${BUILD_LOG, maxLines=100, escapeHtml=false}'
        // }
    }
}
