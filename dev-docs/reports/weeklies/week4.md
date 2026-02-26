# Week 3

> Feel in the week number (e.g., 1, 2, 3 etc.) for this report.


# Completed tasks

> List completed (DONE) tasks include
> 1. A link to the Issue
> 2. The total weight (points or T-shirt size) allocated to the issue

| Task                                                                                                                                      | Weight | Assignee  | 
|-------------------------------------------------------------------------------------------------------------------------------------------|--------|-----------|
| [Issue #56: ReportSubcommand class for processing report requirements](https://github.com/CS7580-SEA-SP26/f-team/issues/56)                     | S      | joeysbase   |
| [Issue #57: RequestAgent provides an api to get report results from the server](https://github.com/CS7580-SEA-SP26/f-team/issues/57)                          | S      | joeysbase   | 
| [Issue #58: WorkerManager provides an api to create report worker](https://github.com/CS7580-SEA-SP26/f-team/issues/58)                             | S      | oooolky   |
| [Issue #59: DataStoreAgent class for handling database relevant IO operations](https://github.com/CS7580-SEA-SP26/f-team/issues/59)                              | M      | oooolky   |
| [Issue #60: Design and create MongoDB schemas for report subcommand](https://github.com/CS7580-SEA-SP26/f-team/issues/60) | M      | oooolky |
| [Issue #61: ReportWorker class for handling report commmand relevant tasks](https://github.com/CS7580-SEA-SP26/f-team/issues/61)    | S      | oooolky   |
| [Issue #62: RunWorker should write execution data to the datastore](https://github.com/CS7580-SEA-SP26/f-team/issues/62)                   | S      | oooolky  |



# Carry over tasks

> List all issues that were planned for this week but did not get DONE
> Include
> 1. A link to the Issue
> 2. The total weight (points or T-shirt size) allocated to the issue
> 3. The team member assigned to the task. This has to be 1 person!

| Task | Weight | Assignee |
| ---- | ------ | -------- |
|      |        |          |




# What worked this week?

> In this section list part of the team's process that you believe worked well. "Worked Well" means helped the team be more efficient and/or effective. Try to explain **why** these actions worked well.
### 1. MongoDB integration and report subcommand were implemented end-to-end
We successfully implemented the MongoDB-backed report subcommand.

* **Why it worked:** We clearly separated responsibilities between RunWorker, DataStoreAgent, and ReportService. This modular design made debugging easier and allowed different team members to work on storage and rendering independently without too much interference.

### 2. We tested report functionality incrementally
Instead of implementing everything at once, we tested each level step-by-step.
* **Why it worked:** This incremental approach helped us quickly identify integration bugs (e.g., incorrect field names, null timestamps). It reduced debugging time and increased confidence before demo preparation.


# What did not work this week?

> In this section list part of the team's process that you believe did **not** work well. "Not Worked Well" means that the team found these actions to not have a good effect on the team's effectiveness. Try to
 explain **why** these actions did not work well.
### 1. Overlapping modifications in RunWorker and data layer
Multiple team members modified RunWorker, ReportStore, and later DataStoreAgent in parallel.

* **Why it didn’t work well:** This caused merge conflicts and temporary inconsistencies (e.g., having both in-memory ReportStore and Mongo-based DataStoreAgent). It slowed integration and required extra time to clean up duplicated logic.

### 2. Task size for Mongo integration was underestimated
The task “Design and create MongoDB schemas for report subcommand” initially looked simple, but it involved: schema design, mongo driver integration, environment setup, refactoring existing report logic, updating CLI output.
* **Why it didn’t work well:** The task was larger than expected and should have been broken down into smaller sub-tasks (schema design, persistence layer, report query logic, integration testing). This would have improved progress visibility and workload balancing.

### 3. Insufficient planning for execution order storage
Currently, stage order in report output is based on query sorting rather than execution order.

* **Why it didn’t work well:** We focused on persistence first and postponed ordering considerations. While the system works, this creates minor inconsistencies between execution flow and report display, which may require refactoring later.

# Design updates

> If changes have been made to the overall design approach for the project, least the updates here. Link to documents (or updates to documents) that describe in detail what these changes are.


> | Task | Points|
> | --- | --- | 
> | Issue are linked in the weekly report and point to the right issue on GitHub | 2 | 
> | Issues marked as DONE in the report are closed in GitHub | 2 | 
> | Issues marked as INCOMPLETE in the report are not closed in GitHub | 2 | 
> | Linked Issues have at least 1 linked PR | 4 | 
> | Linked Issues on GitHub have a clear title and description | 4 | 
> | Linked Issues on GitHub have 1 assignee | 2 | 
> | Linked Issues on GitHub have estimates | 2 | 
> | **TOTAL**  | **18** |
 
