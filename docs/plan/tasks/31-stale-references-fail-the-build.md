# 31 — A stale cross-file reference fails the build

**Repo:** .
**Depends on:** nothing
**Owns:**
- pom.xml

## Goal
A `{@link}` to a type that no longer exists breaks the build instead of rotting
in a comment.

## Context
- docs/plan/PLAN.md — "Raised in milestone 5, wave 1", fourth bullet: task 27's rename left a false claim in a neighbouring file's Javadoc, and nothing in the build would have caught it.
- docs/architecture.md — the no-dependency boundary. A **build plugin** is not a library dependency and does not breach it; nothing here is added to the artifact's classpath. Say so where the plugin is configured, because the next reader will ask.
- Measured before planning: `mvn -pl jresolve-core javadoc:javadoc` currently emits **zero** warnings, so this gate constrains what comes next rather than demanding a cleanup first.

## Acceptance
- [ ] `maven-javadoc-plugin` runs as part of `verify` with doclint enabled, and a doclint violation **fails** the build rather than warning.
- [ ] Both modules are covered, not only core.
- [ ] **Demonstrated, not assumed.** Introduce a broken `{@link}` locally, show the build fails, remove it, show the build passes. Put both outcomes in the commit body — a gate nobody has seen fail is a gate nobody knows works.
- [ ] `mvn clean verify` passes on the unmodified tree, with the same test count as before: this task adds no tests.
- [ ] The POM comment states what this gate does and does not catch. It catches a reference to something that no longer exists. It does **not** catch a comment that describes neighbouring code wrongly while naming nothing — which is precisely the defect that raised this gap.

## A judgement this task owes
This gate does not catch the defect that motivated it. Task 27's false claim
named no type; it was prose about what a neighbouring file's fixtures looked
like, and doclint is indifferent to it.

Say so plainly in the commit body, and say whether the gate is still worth
having. If the honest answer is that it narrows a different problem than the
one raised, that belongs in `PLAN.md` as a gap that remains open rather than
being quietly counted as closed.

## Out of scope
- Publishing javadoc, attaching a javadoc jar, or anything to do with release.
- Rewriting existing Javadoc. There is nothing to fix: the baseline is clean.
- Prose-level or semantic checks of any kind.
