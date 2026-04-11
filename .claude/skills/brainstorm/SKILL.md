---
name: brainstorm
description: Use this skill when the user wants to brainstorm, plan, or design a feature before implementing it. Helps validate ideas and produce a step-by-step implementation plan as a text file that an agent can later pick up.
version: 1.0.0
---

# Brainstorming Skill

## Core Rule

**Do not generate or modify any source code during a brainstorming session.** The goal is to think, validate, and plan — not to implement. Only write a plan document when the session is complete and the user asks for it.

## How to Conduct a Brainstorming Session

### 1. Understand the Goal

Start by making sure you fully understand what the user wants to achieve. Ask clarifying questions if the feature description is vague:
- What problem does this solve for the user?
- What is the expected end state?
- Are there any hard constraints (no new dependencies, specific tech, etc.)?

### 2. Read the Codebase First

Before proposing anything, read relevant parts of the existing codebase to understand:
- Current architecture and patterns in use
- Existing abstractions that could be reused or extended
- Potential conflicts or areas of risk

### 3. Explore and Validate Ideas

Think through the feature by raising and answering questions:
- What are the main design decisions to make?
- What are the tradeoffs of each approach?
- Which approach best fits the existing codebase style?
- What are the risks or unknowns?

Actively challenge assumptions. Point out if an idea won't work as expected, or if there's a simpler path.

### 4. Converge on a Plan

Once the user and you have talked through the options, converge on a concrete approach. The plan should be specific enough that another agent could implement it without needing to make major design decisions.

### 5. Write the Plan File

When the session is complete and the user confirms the plan, write a plan file to `.claude/plans/` in the project directory. Use a descriptive kebab-case filename, e.g. `.claude/plans/trade-booking-form.md`.

#### Plan File Structure

```markdown
# Feature: <name>

## Goal
One paragraph describing what this feature does and why.

## Key Decisions
- **Decision 1**: chosen approach and why (alternatives considered: ...)
- **Decision 2**: ...

## Implementation Steps
1. Step one — specific file or class to create/modify, what to do
2. Step two — ...
3. ...

## Out of Scope
Things explicitly not included in this plan.

## Open Questions
Any unknowns that the implementer will need to resolve.
```

Keep each implementation step actionable and concrete. Steps should name specific files, classes, or methods where known.

**Each step must be commit-sized**: small enough that it can be implemented, built, and manually
verified in one focused sitting. A step that touches one file or one logical concern is ideal.
Large steps must be split — for example, "implement the screen" is too big; "implement the
skeleton with routing and empty state", "implement the secondary toolbar", "implement the General
form" are each appropriately sized. The user will verify each step before the agent moves to the
next, so steps must produce something observable.