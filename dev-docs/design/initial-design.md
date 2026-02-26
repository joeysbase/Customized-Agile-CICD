# High Level Architecture Design
In general, there are three components:
- A CLI client for handling user logics
- A CI/CD engine for handling CI/CD logics
- Data storages for storing data

The engine and data storage can either run locally or remotely. 

The interaction between the engine and different data storage should rely on different driver implementing the same interface which supports comprehensive data manipulations.

---
![high-level-architecture](./images/high-level.drawio.svg)

---
# Component Design
## Cli client
### verify
Verify workflow: 
- A user call verify subcommand and pass a configuration file path. 
- The verify subcommand read the file into string and pass it to the server agent. 
- The agent distribute a worker request and the file string to a server. 
- A WorkerManager inside the server creates a VerifyWorker and monitors its status. 
- Return work results (messages) to the client and write to the DB when the work is done.

![verify workflow](./images/verify/verify-workflow.drawio.svg)

![sequence diagram](./images/verify/verify-sequence-diagram.drawio.svg)



There are classes involved: 

![verify subcommand classes](./images/verify/verify-related-classes.drawio.svg)

### dryrun
Dryrun workflow:
- A user call dry subcommand and pass a configuration file path. 
- The verify subcommand read the file into string and pass it to the server agent. 
- The agent distribute a worker request and the file string to a server. 
- A WorkerManager inside the server creates a VerifyWorker and monitors its status. 
- Return work results (messages) to the client and write to the DB when the work is done.

![verify workflow](./images/dryrun/dryrun-workflow.drawio.svg)

![sequence diagram](./images/dryrun/dryrun-sequence-diagram.drawio.svg)



There are classes involved: 

![verify subcommand classes](./images/dryrun/dryrun-related-classes.drawio.svg)

### run
Dryrun workflow:
- A user call dry subcommand and pass a configuration file path. 
- The verify subcommand read the file into string and pass it to the server agent. 
- The agent distribute a worker request and the file string to a server. 
- A WorkerManager inside the server creates a VerifyWorker and monitors its status. 
- Return work results (messages) to the client and write to the DB when the work is done.

![verify workflow](./images/run/run-workflow.drawio.svg)

![sequence diagram](./images/run/run-sequence-diagram.drawio.svg)



There are classes involved: 

![verify subcommand classes](./images/run/run-related-classes.drawio.svg)

### report
Dryrun workflow:
- A user call dry subcommand and pass a configuration file path. 
- The verify subcommand read the file into string and pass it to the server agent. 
- The agent distribute a worker request and the file string to a server. 
- A WorkerManager inside the server creates a VerifyWorker and monitors its status. 
- Return work results (messages) to the client and write to the DB when the work is done.

![verify workflow](./images/report/report-workflow.drawio.svg)

![sequence diagram](./images/report/report-sequence-diagram.drawio.svg)



There are classes involved: 

![verify subcommand classes](./images/report/report-related-classes.drawio.svg)

---

## Server agent

## Database