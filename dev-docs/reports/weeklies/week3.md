# Week 3

> Feel in the week number (e.g., 1, 2, 3 etc.) for this report.


# Completed tasks

> List completed (DONE) tasks include
> 1. A link to the Issue
> 2. The total weight (points or T-shirt size) allocated to the issue

| Task                                                                                                                                      | Weight | Assignee  | 
|-------------------------------------------------------------------------------------------------------------------------------------------|--------|-----------|
| [Issue #40: WorkerManager provides an api to create a RunWorker](https://github.com/CS7580-SEA-SP26/f-team/issues/40)                     | S      | oooolky   |
| [Issue #43: GitAgent class for handling git related logics](https://github.com/CS7580-SEA-SP26/f-team/issues/43)                          | M      | oooolky   | 
| [Issue #41: RunWorker class to handle run related tasks](https://github.com/CS7580-SEA-SP26/f-team/issues/41)                             | M      | oooolky   |
| [Issue #44: Job class provides an api to execute a job](https://github.com/CS7580-SEA-SP26/f-team/issues/44)                              | M      | oooolky   |
| [Issue #39: RequestAgent provides an api to get run command results from the server](https://github.com/CS7580-SEA-SP26/f-team/issues/39) | S      | joeysbase |
| [Issue #38: RunSubcommand class for parsing user requirements of the run feature](https://github.com/CS7580-SEA-SP26/f-team/issues/38)    | S      | joeysbase   |
| [Issue #42: Redesign Job class to support parallel job executions](https://github.com/CS7580-SEA-SP26/f-team/issues/42)                   | S      | jason-te-sde  |



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
### 1. Built new features while intentionally leaving room for backward compatibility
We deliberately designed and implemented with future backward compatibility in mind, even when it added a small amount of upfront work.

* **Why it worked:** It prevented expensive refactoring or breaking changes later. The small extra effort now saves significant time and risk in future releases and customer upgrades.

### 2. Dynamically reassigned tasks based on each member's current workload
We adjusted task ownership mid-sprint according to who actually had capacity instead of sticking to the initial plan.

* **Why it worked:** It prevented bottlenecks, reduced individual overload, and utilized available bandwidth better. Overall throughput increased and team stress decreased noticeably.


# What did not work this week?

> In this section list part of the team's process that you believe did **not** work well. "Not Worked Well" means that the team found these actions to not have a good effect on the team's effectiveness. Try to
 explain **why** these actions did not work well.
### 1. Task decomposition still not granular enough to avoid overlap
We continued to have cases where the same class/file was modified or rewritten by two different people during the sprint.

* **Why it didn’t work well:** This created merge conflicts, duplicated effort, inconsistent implementations, and wasted time on discussions/rewrites. The lack of clear ownership boundaries reduced parallel work efficiency and increased review friction.

### 2. Task sizes remain too large and not consistently broken down
Many assigned tasks were still M+ in size without being split into smaller, clearly defined sub-tasks (ideally S-size) before starting.

* **Why it didn’t work well:** Large tasks led to longer feedback loops, delayed visibility of progress/risks, and made it harder to rebalance workload mid-sprint. People sometimes got stuck for days on one big item, hurting overall flow and team momentum.

### 3. Insufficient focus on future extensibility and high-concurrency in designs
Current architectural and component-level decisions still show limited consideration for scaling needs and extensibility points.

* **Why it didn’t work well:** We are building technical debt that will become painful when we need to support higher traffic, new features that touch the same modules, or plugin/customization scenarios. Late discovery of these gaps usually means larger refactorings and slower velocity in future sprints.

# Design updates

> If changes have been made to the overall design approach for the project, least the updates here. Link to documents (or updates to documents) that describe in detail what these changes are.

- [Issue #10: Redeisgn to support RESTful server and multi-threading](https://github.com/CS7580-SEA-SP26/f-team/issues/10)


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
 
