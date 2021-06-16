// Jenkins build pipeline for Salesforce

// Import SFDX scripts
def SFDX_SCRIPTS

// Primary git branches
def MAIN_BRANCH    = 'main'
def UAT_BRANCH     = 'uat'
def DEVELOP_BRANCH = 'develop'

// Git branch prefixes
def RELEASE_PREFIX = 'release/*'
def HOTFIX_PREFIX  = 'hotfix/*'
def FEATURE_PREFIX = 'feature/*'
def BUGFIX_PREFIX  = 'bugfix/*'

// Salesforce environments (stored in Jenkins credentials)
def PRODUCTION       = 'Salesforce-Production'
def TRAINING_SANDBOX = 'Salesforce-Training'
def STAGING_SANDBOX  = 'Salesforce-Staging'
def UAT_SANDBOX      = 'Salesforce-UAT'
def QA_SANDBOX       = 'Salesforce-Production' //'Salesforce-QA' temp using prod org for testing

// Salesforce scratch org config
def SCRATCH_ORG_DEFINITION_FILE = './config/project-scratch-def.json'

// Static variables
// def BUILD_NOTIFICATION_EMAIL = 'someone@test.com'
def SCHEDULE_JOBS_SCRIPT = './scripts/deployment/schedule-jobs.apex'
def POPULATE_CUSTOM_SETTINGS_SCRIPT = './scripts/deployment/populate-custom-settings.apex'

pipeline {
    agent any
    stages {
        stage('Load Dependencies') {
            steps {
                script {
                    SFDX_SCRIPTS = load 'Jenkins.sfdx-scripts.groovy'
                    SFDX_SCRIPTS.installDependencies()
                    env.sfdxEnvironments = SFDX_SCRIPTS.loadSfdxEnvironments()
                    env.sfdxPackageDirectories = SFDX_SCRIPTS.loadSfdxPackageDirectories()
                }
            }
        }
        // stage('Run Apex Scanner') {
        //     when  { anyOf { branch FEATURE_PREFIX; branch BUGFIX_PREFIX } }
        //     steps {
        //         script {
        //             SFDX_SCRIPTS.runApexScanner()
        //         }
        //     }
        // }
        stage('Run LWC Tests') {
            when  { anyOf { branch FEATURE_PREFIX; branch BUGFIX_PREFIX } }
            steps {
                script {
                    SFDX_SCRIPTS.runLwcTests()
                }
            }
        }
        stage('Convert Source to MDAPI') {
            when { anyOf { branch DEVELOP_BRANCH; branch UAT_BRANCH; branch MAIN_BRANCH } }
            steps {
                script {
                    SFDX_SCRIPTS.convertSourceToMdapiFormat()
                }
            }
        }
        stage('Deploy to Salesforce') {
            parallel {
                stage('1. Production') {
                    when  { anyOf { branch RELEASE_PREFIX; branch MAIN_BRANCH } }
                    steps {
                        script {
                            SFDX_SCRIPTS.authorizeEnvironment(PRODUCTION)
                            SFDX_SCRIPTS.deployToSalesforce(PRODUCTION, env.BRANCH_NAME == MAIN_BRANCH, true)
                            // SFDX_SCRIPTS.deleteObsoleteFlowVersions(PRODUCTION)
                        }
                    }
                }
                stage('2. Training') {
                    when  { branch MAIN_BRANCH }
                    steps {
                        script {
                            SFDX_SCRIPTS.authorizeEnvironment(TRAINING_SANDBOX)
                            SFDX_SCRIPTS.deployToSalesforce(TRAINING_SANDBOX, env.BRANCH_NAME == MAIN_BRANCH, false)
                            // SFDX_SCRIPTS.deleteObsoleteFlowVersions(TRAINING_SANDBOX)
                        }
                    }
                }
                stage('3. Staging') {
                    when  { anyOf { branch HOTFIX_PREFIX; branch RELEASE_PREFIX } }
                    steps {
                        script {
                            SFDX_SCRIPTS.authorizeEnvironment(STAGING_SANDBOX)
                            SFDX_SCRIPTS.deployToSalesforce(STAGING_SANDBOX, env.BRANCH_NAME == RELEASE_PREFIX, false)
                            // SFDX_SCRIPTS.deleteObsoleteFlowVersions(STAGING_SANDBOX)
                        }
                    }
                }
                stage('4. UAT') {
                    when  { anyOf { branch DEVELOP_BRANCH; branch UAT_BRANCH } }
                    steps {
                        script {
                            SFDX_SCRIPTS.authorizeEnvironment(UAT_SANDBOX)
                            SFDX_SCRIPTS.deployToSalesforce(UAT_SANDBOX, env.BRANCH_NAME == UAT_BRANCH, false)
                            // SFDX_SCRIPTS.deleteObsoleteFlowVersions(UAT_SANDBOX)
                        }
                    }
                }
                stage('5. QA') {
                    when  { anyOf { branch FEATURE_PREFIX; branch BUGFIX_PREFIX; branch DEVELOP_BRANCH } }
                    steps {
                        script {
                            SFDX_SCRIPTS.authorizeEnvironment(QA_SANDBOX)
                            SFDX_SCRIPTS.deployToSalesforce(QA_SANDBOX, env.BRANCH_NAME == DEVELOP_BRANCH, false)
                            // SFDX_SCRIPTS.deleteObsoleteFlowVersions(QA_SANDBOX)
                        }
                    }
                }
                // stage('Scratch Org') {
                //     when  { anyOf { branch FEATURE_PREFIX; branch BUGFIX_PREFIX; } }
                //     steps {
                //         script {
                //             SFDX_SCRIPTS.createScratchOrg(env.BRANCH_NAME, SCRATCH_ORG_DEFINITION_FILE)
                //             SFDX_SCRIPTS.pushToScratchOrg(env.BRANCH_NAME)
                //             SFDX_SCRIPTS.runScratchOrgApexTests(env.BRANCH_NAME)
                //             SFDX_SCRIPTS.deleteScratchOrg(env.BRANCH_NAME)
                //         }
                //     }
                // }
            }
        }
        stage('Upsert Custom Settings') {
            when     { anyOf { branch FEATURE_PREFIX; branch DEVELOP_BRANCH; branch UAT_BRANCH } }
            parallel {
                stage('1. Production') {
                    when  { branch MAIN_BRANCH }
                    steps {
                        script {
                            SFDX_SCRIPTS.runApexScript(STAGING_SANDBOX, POPULATE_CUSTOM_SETTINGS_SCRIPT)
                        }
                    }
                }
                stage('2. Training') {
                    when  { branch MAIN_BRANCH }
                    steps {
                        script {
                            SFDX_SCRIPTS.runApexScript(TRAINING_SANDBOX, POPULATE_CUSTOM_SETTINGS_SCRIPT)
                        }
                    }
                }
                stage('3. Staging') {
                    when  { anyOf { branch HOTFIX_PREFIX; branch RELEASE_PREFIX } }
                    steps {
                        script {
                            SFDX_SCRIPTS.runApexScript(STAGING_SANDBOX, POPULATE_CUSTOM_SETTINGS_SCRIPT)
                        }
                    }
                }
                stage('4. UAT') {
                    when  { branch UAT_BRANCH }
                    steps {
                        script {
                            SFDX_SCRIPTS.runApexScript(UAT_SANDBOX, POPULATE_CUSTOM_SETTINGS_SCRIPT)
                        }
                    }
                }
                stage('5. QA') {
                    when  { anyOf { branch FEATURE_PREFIX; branch BUGFIX_PREFIX; branch DEVELOP_BRANCH } }
                    steps {
                        script {
                            SFDX_SCRIPTS.runApexScript(QA_SANDBOX, POPULATE_CUSTOM_SETTINGS_SCRIPT)
                        }
                    }
                }
            }
        }
        stage('Upsert CSV Data') {
            when     { anyOf { branch FEATURE_PREFIX; branch DEVELOP_BRANCH; branch UAT_BRANCH } }
            parallel {
                stage('1. Production') {
                    when  { branch MAIN_BRANCH }
                    steps {
                        script {
                            SFDX_SCRIPTS.upsertCsvFiles(PRODUCTION)
                        }
                    }
                }
                stage('2. Training') {
                    when  { branch MAIN_BRANCH }
                    steps {
                        script {
                            SFDX_SCRIPTS.upsertCsvFiles(TRAINING_SANDBOX)
                        }
                    }
                }
                stage('3. Staging') {
                    when  { anyOf { branch HOTFIX_PREFIX; branch RELEASE_PREFIX } }
                    steps {
                        script {
                            SFDX_SCRIPTS.upsertCsvFiles(STAGING_SANDBOX)
                        }
                    }
                }
                stage('4. UAT') {
                    when  { branch UAT_BRANCH }
                    steps {
                        script {
                            SFDX_SCRIPTS.upsertCsvFiles(UAT_SANDBOX)
                        }
                    }
                }
                stage('5. QA') {
                    when  { anyOf { branch FEATURE_PREFIX; branch BUGFIX_PREFIX; branch DEVELOP_BRANCH } }
                    steps {
                        script {
                            SFDX_SCRIPTS.upsertCsvFiles(QA_SANDBOX)
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
                            SFDX_SCRIPTS.runApexScript(STAGING_SANDBOX, SCHEDULE_JOBS_SCRIPT)
                        }
                    }
                }
                stage('2. Training') {
                    when  { branch MAIN_BRANCH }
                    steps {
                        script {
                            SFDX_SCRIPTS.runApexScript(TRAINING_SANDBOX, SCHEDULE_JOBS_SCRIPT)
                        }
                    }
                }
                stage('3. Staging') {
                    when  { anyOf { branch HOTFIX_PREFIX; branch RELEASE_PREFIX } }
                    steps {
                        script {
                            SFDX_SCRIPTS.runApexScript(STAGING_SANDBOX, SCHEDULE_JOBS_SCRIPT)
                        }
                    }
                }
                stage('4. UAT') {
                    when  { branch UAT_BRANCH }
                    steps {
                        script {
                            SFDX_SCRIPTS.runApexScript(UAT_SANDBOX, SCHEDULE_JOBS_SCRIPT)
                        }
                    }
                }
                stage('5. QA') {
                    when  { branch DEVELOP_BRANCH }
                    steps {
                        script {
                            SFDX_SCRIPTS.runApexScript(QA_SANDBOX, SCHEDULE_JOBS_SCRIPT)
                        }
                    }
                }
            }
        }
    }
    post {
        always {
            junit allowEmptyResults: true, testResults: 'tests/**/*.xml'
            // recordIssues enabledForFailure: true, tool: pmdParser(pattern: 'scanner-results.xml')
        }
        success {
            cleanWs()
        }
    }
}
