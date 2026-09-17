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
- ~~Measured before planning: `mvn -pl jresolve-core javadoc:javadoc` currently emits **zero** warnings.~~ **Amended during implementation: that measurement was invalid.** It ran the javadoc goal with no doclint configured, so it measured the lenient default rather than the gate. Under `doclint=all` core emits **100** warnings — all of them `no @param`, `no @return`, `no comment`, i.e. the `missing` group. None is a broken reference. The gate is therefore configured `all,-missing`: documentation *completeness* is a policy this project has never adopted and is not what a stale reference is. Under that setting the baseline genuinely is clean, including test sources and private members.

## Acceptance
- [ ] `maven-javadoc-plugin` runs as part of `verify` with doclint enabled, and a doclint violation **fails** the build rather than warning.
- [ ] Both modules are covered, not only core.
- [ ] **Scope amended during implementation.** The default javadoc scope is `protected`, and `javadoc-no-fork` reads main sources only — so as first configured the gate would have caught the motivating defect on *neither* count, since it lived on a test class and on a member javadoc never looks at. `show=private` and a second `test-javadoc-no-fork` execution are required, and each must be demonstrated separately: the executions run in POM order, so the first failure hides the second.
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
- Rewriting existing Javadoc, and adopting a documentation-completeness policy. The 100 `missing` warnings are real but are a different decision, and one nobody has made.
- Prose-level or semantic checks of any kind.
