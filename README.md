# Customized Agile CICD

# 1. Overview
While CI/CD systems like the ones supported by GitHub and GitLab are useful, we want something custom made that addresses some of our needs discovered while using existing CI/CD systems for class projects. 

Users define how the cicd would run on a project by using a pipeline configuration file
- The configuration file resides in /.pipeline and it's a yaml file
- Insied a pipeline configuration file, there are stages and jobs
- The cicd system will execute the jobs in the order defined in the pipeline file unless *needs* keyword specifies prerequisites
  
Cli client grabs pipeline file from the local and send it to the server
- Depends on the user's requirement, one of verify, dryrun, run, and report service will be called
- Execution info will be recorded to the database

Cli client display execution info
- Any error and execution message will be return to the client

High Priority Features
- Local execution of the CI/CD. The system must be able to allow developers to run a CI/CD pipeline locally. To run locally all components of the CI/CD system (services, data stores etc.) must be initialized and running locally. No changes to the repositories CI/CD configuration should be needed to run locally.

- All CI/CD configuration for a repository must reside within the repo itself in folders/files. The CI/CD configuration must be committed to the repository so that we can store and track configuration files.

- The system will only consider what is already checked in the Git repository (either the local repository or a remote repository). Any local changes that have not been committed are not to be considered by the CI/CD system.

- There must be a command line client (CLI) that developers can use to run pipelines locally. This same command line client must be able to be used on server machines and/or be part of scripts.

# 2. High Level Design
The system adopts Microservices architecture and the agile development cycle.
- Verify service handles pipeline configuration file validations
- Dryrun services handles dryrunning a pipeline configuration file
- Run service handles the execution of a ppipeline file and write to the database
- Report service gather execution info from the database
```
      Client
        ↓
   Load Balancer
        ↓
----------------------------------
| Verify | Dryrun | Run | Report |
----------------------------------
        ↓
Database + Cache
```