// Jenkins build pipeline for Salesforce

// Import SFDX scripts
def PROJECT_SCRIPTS

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

pipeline {
    agent any
    stages {
        stage('Load Dependencies') {
            steps {
                script {
                    PROJECT_SCRIPTS = load 'Jenkinsfile.scripts.groovy'
                    PROJECT_SCRIPTS.installDependencies()
                    env.sfdxEnvironments = PROJECT_SCRIPTS.loadSfdxEnvironments()
                    env.sfdxPackageDirectories = PROJECT_SCRIPTS.loadSfdxPackageDirectories()
                }
            }
        }
        stage('Run Apex Scanner') {
            when  { anyOf { branch FEATURE_PREFIX; branch BUGFIX_PREFIX } }
            steps {
                script {
                    PROJECT_SCRIPTS.runApexScanner()
                }
            }
        }
        stage('Run LWC Tests') {
            when  { anyOf { branch FEATURE_PREFIX; branch BUGFIX_PREFIX } }
            steps {
                script {
                    runLwcTests()
                }
            }
        }
        stage('Convert Source to MDAPI') {
            when { anyOf {branch FEATURE_PREFIX; branch DEVELOP_BRANCH; branch UAT_BRANCH; branch MAIN_BRANCH } }
            steps {
                script {
                    convertSourceToMdapiFormat()
                }
            }
        }
        stage('Deploy to Salesforce') {
            parallel {
                stage('1. Production') {
                    when  { anyOf { branch RELEASE_PREFIX; branch MAIN_BRANCH } }
                    steps {
                        script {
                            authorizeEnvironment(PRODUCTION)
                            deployToSalesforce(PRODUCTION, env.BRANCH_NAME == MAIN_BRANCH, true)
                        }
                    }
                }
                stage('2. Staging') {
                    when  { anyOf { branch HOTFIX_PREFIX; branch RELEASE_PREFIX } }
                    steps {
                        script {
                            authorizeEnvironment(STAGING_SANDBOX)
                            deployToSalesforce(STAGING_SANDBOX, e
                        }nv.BRANCH_NAME == RELEASE_PREFIX, false)
                    }
                }
                stage('3. UAT') {
                    // Run a check-only validation when the git branch is 'develop'
                    // Run a deployment when the git branch is 'uat'
                    when  { anyOf { branch DEVELOP_BRANCH; branch UAT_BRANCH } }
                    steps {
                        script {
                            authorizeEnvironment(UAT_SANDBOX)
                            deployToSalesforce(UAT_SANDBOX, env.BRANCH_NAME == UAT_BRANCH, false)
                        }
                    }
                }
                stage('4. DataMig') {
                    // Run a check-only validation when the git branch is 'develop'
                    // Run a deployment when the git branch is 'uat'
                    when  { anyOf { branch DEVELOP_BRANCH; branch UAT_BRANCH } }
                    steps {
                        script {
                            authorizeEnvironment(DATAMIG_SANDBOX)
                            deployToSalesforce(DATAMIG_SANDBOX, env.BRANCH_NAME == UAT_BRANCH, false)
                        }
                    }
                }
                stage('5. QA') {
                    when  { anyOf {branch FEATURE_PREFIX; branch DEVELOP_BRANCH } }
                    steps {
                        script {
                            authorizeEnvironment(QA_SANDBOX)
                            deployToSalesforce(QA_SANDBOX, env.BRANCH_NAME == DEVELOP_BRANCH, false)
                        }
                    }
                }
                stage('Scratch Org') {
                    when  { anyOf { branch BUGFIX_PREFIX; } }
                    steps {
                        script {
                            createScratchOrg()
                            // authorizeEnvironment(PRODUCTION)
                            // deployToSalesforce(PRODUCTION, false, false)
                            // runApexTests(SCRATCH_ORG)
                        }
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
                        script {
                            runApexScript(STAGING_SANDBOX, POPULATE_CUSTOM_SETTINGS_SCRIPT)
                        }
                    }
                }
                stage('2. Staging') {
                    when  { branch HOTFIX_PREFIX }
                    steps {
                        script {
                            runApexScript(STAGING_SANDBOX, POPULATE_CUSTOM_SETTINGS_SCRIPT)
                        }
                    }
                }
                stage('3. UAT') {
                    when  { branch UAT_BRANCH }
                    steps {
                        script {
                            runApexScript(UAT_SANDBOX, POPULATE_CUSTOM_SETTINGS_SCRIPT)
                        }
                    }
                }
                stage('4. DataMig') {
                    when  { branch UAT_BRANCH }
                    steps {
                        script {
                            runApexScript(DATAMIG_SANDBOX, POPULATE_CUSTOM_SETTINGS_SCRIPT)
                        }
                    }
                }
                stage('5. QA') {
                    when  { anyOf {branch FEATURE_PREFIX; branch DEVELOP_BRANCH } }
                    steps {
                        script {
                            runApexScript(QA_SANDBOX, POPULATE_CUSTOM_SETTINGS_SCRIPT)
                        }
                    }
                }
                stage('Scratch Org') {
                    when  { branch DEVELOP_BRANCH }
                    steps {
                        script {
                            runApexScript(SCRATCH_ORG, POPULATE_CUSTOM_SETTINGS_SCRIPT)
                        }
                    }
                }
            }
        }
        stage('Upsert Config Data') {
            when     { anyOf { branch FEATURE_PREFIX; branch DEVELOP_BRANCH; branch UAT_BRANCH } }
            parallel {
                stage('1. Production') {
                    when  { branch MAIN_BRANCH }
                    steps {
                        script {
                            loadCsvFile(PRODUCTION, 'User', 'MyExternalId__c')
                        }
                    }
                }
                stage('2. Staging') {
                    when  { branch HOTFIX_PREFIX }
                    steps {
                        script {
                            loadCsvFile(STAGING_SANDBOX, 'User', 'MyExternalId__c')
                        }
                    }
                }
                stage('3. UAT') {
                    when  { branch UAT_BRANCH }
                    steps {
                        script {
                            loadCsvFile(UAT_SANDBOX, 'User', 'MyExternalId__c')
                        }
                    }
                }
                stage('4. DataMig') {
                    when  { branch UAT_BRANCH }
                    steps {
                        script {
                            loadCsvFile(DATAMIG_SANDBOX, 'User', 'MyExternalId__c')
                        }
                    }
                }
                stage('5. QA') {
                    when  { anyOf {branch FEATURE_PREFIX; branch DEVELOP_BRANCH } }
                    steps {
                        script {
                            loadCsvFile(QA_SANDBOX, 'User', 'MyExternalId__c')
                        }
                    }
                }
                stage('Scratch Org') {
                    when  { branch DEVELOP_BRANCH }
                    steps {
                        script {
                            loadCsvFile(SCRATCH_ORG, 'User', 'MyExternalId__c')
                        }
                    }
                }
            }
        }
        stage('Schedule Apex Jobs') {
            when     { anyOf { branch FEATURE_PREFIX; branch DEVELOP_BRANCH; branch UAT_BRANCH } }
            parallel {
                stage('1. Production') {
                    when  { branch MAIN_BRANCH }
                    steps {
                        script {
                            runApexScript(STAGING_SANDBOX, SCHEDULE_JOBS_SCRIPT)
                        }
                    }
                }
                stage('2. Staging') {
                    when  { branch HOTFIX_PREFIX }
                    steps {
                        script {
                            runApexScript(STAGING_SANDBOX, SCHEDULE_JOBS_SCRIPT)
                        }
                    }
                }
                stage('3. UAT') {
                    when  { branch UAT_BRANCH }
                    steps {
                        script {
                            runApexScript(UAT_SANDBOX, SCHEDULE_JOBS_SCRIPT)
                        }
                    }
                }
                stage('4. DataMig') {
                    when  { branch UAT_BRANCH }
                    steps {
                        script {
                            runApexScript(DATAMIG_SANDBOX, SCHEDULE_JOBS_SCRIPT)
                        }
                    }
                }
                stage('5. QA') {
                    when  { anyOf {branch FEATURE_PREFIX; branch DEVELOP_BRANCH } }
                    steps {
                        script {
                            runApexScript(QA_SANDBOX, SCHEDULE_JOBS_SCRIPT)
                        }
                    }
                }
                stage('Scratch Org') {
                    when  { branch DEVELOP_BRANCH }
                    steps {
                        script {
                            runApexScript(SCRATCH_ORG, SCHEDULE_JOBS_SCRIPT)
                        }
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
