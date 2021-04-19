# Advanced Salesforce SFDX Deployments with Jenkins

Jenkins is a great automation server to use for your Salesforce deployment pipeline. It's free ($0 and open source), can be installed on nearly any OS, and has a huge library of plugins. Salesforce even (has some examples of setting it up)[https://developer.salesforce.com/docs/atlas.en-us.sfdx_dev.meta/sfdx_dev/sfdx_dev_ci_jenkins.htm].

However, the examples on Salesforce's developer site are fairly basic examples of either deploying to a single sandbox or a single scratch. But in reality, many Salesforce projects need to deploy to several environments (for QA, user testing, and so on) - and Jenkins should handle automatically deploying to all environments as needed.

This repository is an example of how you can deploy to multiple Salesforce enviornments using Jenkins and SFDX. The metadata included in the `force-app` folder is just for demonstration purposes. It has example metadata of:

-   Custom Setting object
-   Apex schedulable class
-   Experience Cloud site

The deployment process itself is controlled by `Jenkinsfile` (stored in the repo's root directory). It uses `SFDX` commands to handle several common deployment steps:

| Deployment Step                           | Purpose                                                                                                                                                                                                                          |
| ----------------------------------------- | -------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| Continuous Integration using Scratch Orgs | For `feature/*` and `bugfix/*` branches, scratch orgs are automatically created & used to deploy all metadata, run Apex tests and store the results in Jenkins                                                                   |
| Sandbox Deployments                       | 4 sandboxes are used - but this approach scales well and it can easily be changed to use more (or fewer) environments, depending on your project's needs. (I've used this approach for automatically deploying to 12+ sandboxes) |
| Diff-only Prod Deployments                | Diff-only deployments to production using the [SFDX-Git-Delta](https://github.com/scolladon/sfdx-git-delta) plugin, including automatically generating & deploying `destructiveChanges.xml`                                      |
| Static Code Analysis                      | Run static code analysis on Apex code using Salesforce's [SFDX Scanner plugin](https://forcedotcom.github.io/sfdx-scanner)                                                                                                       |
| Upsert Custom Settings                    | Runs a post-deployment script to upsert `MyCustomSetting__c` custom setting                                                                                                                                                      |
| Upsert SObject data                       | Automatically upserts CSV data after the deployment                                                                                                                                                                              |
| Schedule Apex Jobs                        | Run a post-deployment script to (re-)schedule the Apex job `MySchedulableJob`                                                                                                                                                    |

## Important Files

If you want to leverage this same approach for your project, then you'll want to use these files:

-   [Jenkinsfile](Jenkinsfile) - this tells Jenkins how to run the deployment (using SFDX commands)
-   [sfdx-environments.json](sfdx-environments.json) - a custom JSON file that contains details about your Salesforce environments. This is used by Jenkins when deploying.
-   [sfdx-project.json](sfdx-project.json) - any metadata with the `packageDirectories` paths is deployed by Jenkins. Multiple directories/paths (shown below) are also supported:
    ```json
    "packageDirectories": [
        {
            "path": "force-app",
            "default": true
        },
        {
            "path": "another-force-app-directory"
        }
    ]
    ```
-   [package.json](package.json) - within this file (or your own version of the file), these package dependencies are needed
    ```json
    "scripts": {
        "install-sfdx-git-delta": "sfdx plugins:install sfdx-git-delta",
        "install-sfdx-scanner": "sfdx plugins:install @salesforce/sfdx-scanner"
    }
    "devDependencies": {
        "@salesforce/sfdx-scanner": "^2.8.0",
        "sfdx-git-delta": "^4.3.1"
    }
    ```

## Required Software/Tools

-   Jenkins server
-   Jenkins plugins
    -   Blue Ocean
    -   (Warnings Next Generation)[https://plugins.jenkins.io/warnings-ng/]
    -   (Pipeline Utility Steps)[https://plugins.jenkins.io/pipeline-utility-steps/]
-   SFDX (Salesforce CLI) installed on the Jenkins server/build agents

## Salesforce Environments

This repo uses these Salesforce environments for example purposes. You can add and remove environments as needed - the process itself scales well.
|Environment Name|Purpose|
--- | ---
|`Production`|qwerty
|`Staging`|asdf
|`UAT`|A sandbox used for user-acceptance testing
|`DataMig`|A sandbox used for data migration. Deployments to `UAT` and `DataMig` run in parallel.
|`QA`|A sandbox used for internal testing
|`Scratch Orgs`|When validating `feature` and `bugfix` branches (discussed below), a scratch org is created & used to validate that the metadata is deployable, and that all unit tests are passsing.

## Git Branches

Uses git flow naming conventions for git branches

### Git Named Branches

This repo uses the following named branches

-   `main` - your production metadata. In this repo, the [SFDX-Git-Delta](https://github.com/scolladon/sfdx-git-delta) plugin is used to only deploy changed metadata to production (instead of deploying the entire repo).
-   `uat` - your stable build branch, used for user-acceptance testing before deploying to production
-   `develop` - your development branch. Any `feature` or `bugfix` branches are created from `develop`

### Git Branch Prefixes

This repo also uses the following branch prefixes.

-   `release/*` - Release branches are used to verify the production deployment process by first deploying to a Staging sandbox. This sandbox should be a clone of the production environment.
-   `hotfix/*` - Hotfix branches are used when fixing critical production-level bugs.
-   `feature/*` - These are used when developing new features & enhancements.
-   `bugfix/*` - These are for non-critical bugs (i.e., the bug is not critical enough to warrant a production hotfix).
-   `devops/*` - These branches are used to run validation-only deployments in all environments (production and sandboxes). This is useful when you are making changes to Jenkinsfile and want to ensure that the deployment can run in all environments.
-   `revert/*` - Revert branches are used when a merged commit needs to be reverted.

## Configuring Salesforce Credentials for Jenkins

The SFDX Auth URL for each Salesforce environment must be stored in Jenkins's credentials manager. This lets Jenkins connect to your Salesforce orgs to run the deployments.

Now that you’ve created a Salesforce DX project, what’s next? Here are some documentation resources to get you started.

## How Do You Plan to Deploy Your Changes?

Do you want to deploy a set of changes, or create a self-contained application? Choose a [development model](https://developer.salesforce.com/tools/vscode/en/user-guide/development-models).

## Configure Your Salesforce DX Project

The `sfdx-project.json` file contains useful configuration information for your project. See [Salesforce DX Project Configuration](https://developer.salesforce.com/docs/atlas.en-us.sfdx_dev.meta/sfdx_dev/sfdx_dev_ws_config.htm) in the _Salesforce DX Developer Guide_ for details about this file.

## Read All About It

-   [Salesforce Extensions Documentation](https://developer.salesforce.com/tools/vscode/)
-   [Salesforce CLI Setup Guide](https://developer.salesforce.com/docs/atlas.en-us.sfdx_setup.meta/sfdx_setup/sfdx_setup_intro.htm)
-   [Salesforce DX Developer Guide](https://developer.salesforce.com/docs/atlas.en-us.sfdx_dev.meta/sfdx_dev/sfdx_dev_intro.htm)
-   [Salesforce CLI Command Reference](https://developer.salesforce.com/docs/atlas.en-us.sfdx_cli_reference.meta/sfdx_cli_reference/cli_reference.htm)
