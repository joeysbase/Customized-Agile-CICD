# Week 6

> Feel in the week number (e.g., 1, 2, 3 etc.) for this report.


# Completed tasks

> List completed (DONE) tasks include
> 1. A link to the Issue
> 2. The total weight (points or T-shirt size) allocated to the issue

| Task                                                                                                                                 | Weight | Assignee | 
|--------------------------------------------------------------------------------------------------------------------------------------|--------|----------|
| [Issue #80: Implement allow failures support in pipeline execution](https://github.com/CS7580-SEA-SP26/f-team/issues/80)             | M      | oooolky  |
| [Issue #81: Add usage guide for evaluators in README](https://github.com/CS7580-SEA-SP26/f-team/issues/57) | S      | oooolky  |



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
### 1. Successfully implemented allow failures feature end-to-end
We implemented the failures key across YAML parsing, job execution, and report output.

* **Why it worked:** We extended the existing architecture (Job → RunWorker → DataStoreAgent) instead of redesigning it, which minimized code changes and made integration smoother.

### 2. Clear validation through real failure scenario testing
We tested pipelines with both blocking failures and allowed failures using actual failing commands (e.g., exit 1).
* **Why it worked:** This ensured that the behavior matched expectations—allowed failures did not stop the pipeline, while normal failures still triggered fail-fast—making our demo reliable and easy to explain.

# What did not work this week?

> In this section list part of the team's process that you believe did **not** work well. "Not Worked Well" means that the team found these actions to not have a good effect on the team's effectiveness. Try to
explain **why** these actions did not work well.
### 1. Late integration of failures field into report layer
The failures field was added to execution logic first and only later integrated into MongoDB and report output.

* **Why it didn’t work well:** This caused temporary inconsistencies between execution results and reports, leading to rework and additional debugging.

### 2. Iterations required to finalize report output format
We needed multiple updates to correctly include job-level details and the failures field in reports.
* **Why it didn’t work well:** We focused on execution functionality first and aligned with reporting requirements later, which increased the number of adjustments near the end of the sprint.

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
 
