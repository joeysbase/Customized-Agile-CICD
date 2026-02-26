# Week 1

**Team Name**: F-Team  
**Date**: Jan 27 - Feb 2, 2026  
**Team Members**: jason-te-sde, oooolky, joeysbase

# Completed tasks
    
| Task | Weight | Assignee                             | PR |
| ---- |--------|--------------------------------------| -- |
| [Issue #3: Design](https://github.com/CS7580-SEA-SP26/f-team/issues/3) | [M]    | jason-te-sde, oooolky, joeysbase     | [Dev](https://github.com/CS7580-SEA-SP26/f-team/pull/8) ✅ |
| [Issue #4: CLI verify logics](https://github.com/CS7580-SEA-SP26/f-team/issues/4) | [M]    | joeysbase                            | [Dev](https://github.com/CS7580-SEA-SP26/f-team/pull/8) ✅ |
| [Issue #5: CICD engine verification results and message functionality](https://github.com/CS7580-SEA-SP26/f-team/issues/5) | [M]    | joeysbase                            | [Dev](https://github.com/CS7580-SEA-SP26/f-team/pull/8) ✅ |
| [Issue #6: Pipeline configuration class](https://github.com/CS7580-SEA-SP26/f-team/issues/6) | [M]    | oooolky                              | [Dev](https://github.com/CS7580-SEA-SP26/f-team/pull/8) ✅ |
| [Issue #7: Test verify subcommand](https://github.com/CS7580-SEA-SP26/f-team/issues/7) | [S]    | joeysbase                                     | [Dev](https://github.com/CS7580-SEA-SP26/f-team/pull/8) ✅ |

# Carry over tasks

None - all planned tasks for Week 1 were completed successfully.

# What worked this week?

### 1. Design-First Approach Prevented Rework
We spent time on [Issue #3: Design](https://github.com/CS7580-SEA-SP26/f-team/issues/3) before jumping into code. This helped us:
- Agree on system architecture early
- Avoid conflicts during implementation
- Have a clear reference document for all team members

**Why it worked**: Having a shared understanding of the system structure meant fewer surprises and less rework.
## What worked this week?

### 1. Collaborative Design Process ([Issue #3](https://github.com/CS7580-SEA-SP26/f-team/issues/3))
The design document ([Issue #3](https://github.com/CS7580-SEA-SP26/f-team/issues/3)) benefited from three team members (jason-te-sde, oooolky, joeysbase) collaborating together. This multi-person approach ensured:
- Different perspectives were considered in the architecture design
- Design decisions were well-discussed and validated
- All team members had a shared understanding before implementation began
- Reduced risk of architectural conflicts during coding

**Why it worked**: Multiple reviewers caught potential design issues early, preventing costly rework later.

### 2. Clear Verification Logic Implementation ([Issue #4](https://github.com/CS7580-SEA-SP26/f-team/issues/4))
The CLI verify logic ([Issue #4](https://github.com/CS7580-SEA-SP26/f-team/issues/4)) was implemented with well-structured validation rules by joeysbase:
- Clean separation between syntax validation and semantic validation
- Each validation rule was isolated and testable
- Error messages were clear and actionable
- Code was easy to review and understand

**Why it worked**: Clear code structure made it easy for team members to understand and extend the verification logic.

### 3. Robust Error Reporting System ([Issue #5](https://github.com/CS7580-SEA-SP26/f-team/issues/5))
The verification results and message functionality ([Issue #5](https://github.com/CS7580-SEA-SP26/f-team/issues/5)) provided excellent user experience:
- Error messages included file name, line number, and column number
- Format followed industry standards for IDE integration
- Clear distinction between different types of errors
- Helpful context in error messages guided users to fix issues quickly

**Why it worked**: Well-formatted errors saved debugging time and improved the developer experience with our CLI tool.

### 4. Reusable Configuration Architecture ([Issue #6](https://github.com/CS7580-SEA-SP26/f-team/issues/6))
The Pipeline configuration class ([Issue #6](https://github.com/CS7580-SEA-SP26/f-team/issues/6)) implemented by oooolky created a solid foundation:
- Clean object model representing pipeline structure
- Reusable across different subcommands (verify, dryrun, run)
- Well-documented API made it easy for other team members to use
- Proper encapsulation prevented invalid configuration states

**Why it worked**: Strong abstraction layer allowed parallel development on other components without conflicts.

### 5. Comprehensive Test Coverage ([Issue #7](https://github.com/CS7580-SEA-SP26/f-team/issues/7)）
The test suite for verify subcommand ([Issue #7](https://github.com/CS7580-SEA-SP26/f-team/issues/7)) provided confidence in our implementation:
- Tests covered both valid and invalid YAML configurations
- Edge cases were identified and tested
- Test coverage exceeded 80% threshold
- Tests served as documentation for expected behavior

**Why it worked**: Comprehensive tests caught bugs early and gave the team confidence to refactor code when needed.


# Design updates

[initial-design.md](https://github.com/CS7580-SEA-SP26/f-team/blob/main/dev-docs/design/initial-design.md)

[tech-stack.md](https://github.com/CS7580-SEA-SP26/f-team/blob/main/dev-docs/design/tech-stack.md)

 