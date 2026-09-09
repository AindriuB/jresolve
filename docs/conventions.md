# Conventions

Fill this in per workspace. It is what `reviewer` checks against, so keep it to
rules that are actually checkable in a diff — a convention nobody can verify is
a preference, and it belongs in a conversation rather than in this file.

Sections marked *(kit)* below arrived with the agent kit. They are not project
guesses: each one was written after a specific failure cost a real workspace
attempts or lost work, and the failure is stated so the rule can be argued with
rather than obeyed blindly. Delete one only when you have decided it does not
apply here — not because it is long.

## Commits

`<task-id>: <imperative summary>` — for example `03: add IBAN checksum guard`.
Body only when the *why* is not obvious from the diff. No emoji.

*(kit)* `Co-Authored-By:` and `Claude-Session:` trailers are permitted on
agent-authored commits. The operating harness appends both automatically to
every commit; they are accurate provenance, not noise, and a rule the tooling
cannot follow costs a review round trip on every commit without improving the
history. A reviewer must not block a merge over their presence, and no existing
commit needs amending to strip them.

## Branches

`task/<id>-<slug>`, created by `.claude/scripts/wt-new.sh`. Never work directly
on the default branch.

## Naming

Standard Java: `PascalCase` types, `camelCase` members, `SCREAMING_SNAKE`
constants, package names all-lowercase with no underscores. Base package
`io.github.aindriub.jresolve`; a module owns exactly one subtree of it.

Interfaces are named for the role, not with an `I` prefix, and the default
implementation is `Default<Role>` — `FieldPipeline` / `DefaultFieldPipeline`.
Do not name a class `<Thing>Impl`.

No abbreviations except `id`, `url`, `api`, `jw` (Jaro-Winkler in local scope
only). Spell out `evidence`, `candidate`, `normalizer`, `probability`.

**`jresolve-core` may not name a domain concept.** No `name`, `address`,
`person`, `dob`, `irish` in any core type, member, package or Javadoc. This is
the boundary from `docs/architecture.md` and it is checkable by grep on the
diff.

## Errors

Configuration errors fail at `build()`, not at `resolve()`. Everything a builder
can check — duplicate field names, null extractors, thresholds on the wrong
scale, `m` or `u` outside `(0, 1]`, coefficients naming absent features — throws
`EntityResolutionConfigurationException` from `build()` with a message naming the
field and the constraint.

Comparators never throw on a null input. Null is data, and it maps to
`MISSING_ONE` or `MISSING_BOTH`. A `NullPointerException` out of a comparator is
a defect, not a caller error.

No silent catch. No exception swallowed to return a default score — a scorer
that cannot score returns a documented outcome or throws.

**No exception message, `toString()` or stack trace contains a field value.**
Messages name the field, the category and the constraint; never the data.

## Tests

JUnit 5 on the test classpath, AssertJ for assertions. Test names read as
sentences: `missingOnOneSideIsNotAConflict()`. One behaviour per test; a test
whose name needs "and" is two tests.

Every similarity metric carries its property tests: bounded in `[0, 1]`,
`similarity(x, x) == 1`, and **exactly** symmetric — `assertThat(sim(a,b))
.isEqualTo(sim(b,a))` with no tolerance, because an asymmetry in Jaro-Winkler or
Levenshtein is a bug and not a rounding artefact. Field comparators are not
required to be symmetric and each documents which it is.

Fellegi-Sunter and logistic-regression tests use hand-calculated expectations
with the arithmetic written in the test as a comment, so a failure says which
term moved. Never assert a score against a value produced by running the code.

Fixtures are synthetic. No real personal data in any file, ever — including
names taken from a public dataset of real people.

A character literal a reader can see in context may be written literally; one
that is invisible by definition must be written as an escape. Non-breaking
space, zero-width characters, and combining marks are invisible — write them
as `\u00A0`, `\u0301` and so on, or build them with an explicit API call
such as `Normalizer.normalize(..., Normalizer.Form.NFD)`, never by pasting
the raw character into source. The failure this prevents: an editor's
trim-whitespace or Unicode-normalizing pass silently rewrites the literal in
*both* the source under test and the test that checks it, so the behaviour it
exists to guard is lost while the suite stays green — nothing fails, because
both sides moved together. This was found five times in one wave in task 03:
two by review, a third by the implementer generalising the first two, and a
fifth by the next review pass. A byte scan for one specific sequence proved
only that one class was clean of it; it reads like proof the whole package is
clean and is not.

No sleeps, no ordering dependence between tests, no shared mutable static.

## Code comments *(kit)*

Short by default — a line or two. Length is earned, not assumed: only a
non-obvious trap, or a decision that would otherwise be re-litigated, buys more,
and even then keep it tight. Do not narrate what the code plainly does. Do not
restate the task file that produced the change. Never write a comment asserting
a result nobody observed — visual outcome, performance, behaviour — unless
someone actually looked.

The test for a long comment: would deleting it cost someone real time, or let
them reintroduce a bug? A comment recording that an awkward construct is a
deliberate workaround for a framework bug passes — without it, the next tidy-up
reintroduces the bug. A comment that runs tens of lines to say what one sentence
would does not.

One workspace shipped three "fixes" in a single milestone whose comments claimed
a visual result nobody had checked, and all three were wrong. A short comment has
less room to assert something unverified; that is not incidental to the brevity
rule, it is most of the reason for it.

## Documentation

Prose in complete sentences. Tables only for genuinely tabular content. Say what
changed and what it cost. Avoid "comprehensive", "robust", "seamlessly" — they
carry no information.

*(kit)* `scribe` commits what it writes before reporting back — `PLAN.md`,
`HISTORY.md` and its index, task-file retirements, any doc edit — rather than
leaving the working tree dirty for a later session to pick up or lose. This is
not optional cleanup: uncommitted scribe output has already gone missing, and a
retired task file once had to be reconstructed from a transcript. A `scribe`
turn that has not run `git commit` has not finished.

*(kit)* `docs/plan/tasks/retired/` is archive, not working set: exclude it from
default `rg` searches with an `.ignore` file, placed both at the repo root and
inside `docs/plan/` — some `rg` builds do not honour a parent-directory
`.ignore` when given a relative subdirectory path. Git still tracks every file
there and existing citations to a specific `retired/<file>.md` still resolve;
read one by explicit path when a citation points at it, and use `--no-ignore` to
search the archive itself.

## Pruning planning docs *(kit)*

Before deleting narrative from `PLAN.md` or any planning doc, verify coverage
mechanically: extract the set of task ids in the text you are about to delete,
compare it against the set surviving in `HISTORY.md` and `docs/plan/tasks/**`,
and state the residual explicitly. Do not delete on a header skim. One prune
claimed every deleted block had a `HISTORY.md` equivalent "verified by
cross-checking headers"; the claim was false and lost two close-out records,
recovered only by a later commit. The one-liner that does the comparison:

```
git show <commit>^:docs/plan/PLAN.md | grep -oE '\b[A-Z]?[0-9]{2,3}\b' | sort -u
```

against the same extraction over `HISTORY.md` and the task directories.

## Progress reporting *(kit)*

After each task closes — merged or otherwise resolved, not only at wave or
milestone boundaries — the orchestrator reports a markdown table of every task in
the current milestone or phase, including tasks not yet started. A table of only
completed work hides the point, which is to make remaining work visible at a
glance.

The table lists, at minimum, task id, a short description of what it owns, and
status. Status distinguishes at least complete/merged, in flight, and not
started; a task that is blocked or in review is reported as such rather than
collapsed into "in flight". The table reflects actual repository state — merged
branches, task files retired into `docs/plan/tasks/retired/` — not intent, and a
task is never shown as complete before it has merged and its gate has passed.

This is a reporting convention for the orchestrator's messages to the user. It
does not change what `PLAN.md` or `HISTORY.md` record: those remain `scribe`'s.

## Task-file baselines *(kit)*

A task file that states a test-count baseline ("467 existing tests still pass")
names the commit it was measured on. A `planner` re-measures the baseline when
the base branch has moved since that commit rather than copying a figure forward
from an earlier task file. Two task files in one wave once both cited a stale
commit; harmless that time because the arithmetic still reconciled, but it was
the third stale-figure incident in one project and worth catching before it
stops reconciling by luck.

## Task-file scoping *(kit)*

When a later wave will obviously consume a type this wave owns, the owning wave's
task file states every field the consumer will need — not only the fields its own
acceptance criteria exercise directly. Checkable in the task file itself: does its
`Owns`-list type carry the shape the next wave's task files already describe
wanting? Seen repeatedly — a state type missing `Equatable`, a row type missing
the two fields its view needed — each added under review rather than in the
original scope. Adding a field after the fact means a wave-2 task edits a file
wave 1 owns, exactly the contention disjoint file ownership exists to prevent. A
`planner` should ask, for every owned type, "what will the consumer already
planned in a later wave read off this" before freezing the wave-1 acceptance list.

## Acceptance-criteria discipline *(kit)*

Four rules, all evidenced by one task that took six attempts to repair a test
suite and whose cause was the task file each time, not the implementers.

**A criterion must be checkable by a measurement already taken at least once.**
That task's acceptance list opened with "the full suite passes", written when no
such run had ever completed; five attempts went on discovering that the blocker
lived in another task's file before a sixth could take the measurement the
criterion assumed. If nobody has watched the instrument produce a reading, the
first deliverable is a completing run, filed as its own task — not a criterion
riding on top of one that has never happened.

**A criterion may not name a file outside the task's `Owns`, except to require it
unchanged.** Three of that task's criteria pointed at files it did not own, each
costing a full attempt to discover. If closing a criterion needs an edit outside
`Owns`, the criterion belongs to the task that owns the file: widen `Owns` when
the plan is written, or put the criterion where the file already is.

**A task's criteria freeze when its first attempt starts.** New work found
mid-flight is filed as a successor task and named in the close-out, not folded
into the task already in flight. That task kept absorbing scope across six
attempts, so "done" moved every time an attempt got close to it, and a task whose
definition of done moves after each attempt cannot converge.

**Do not run two tasks that drive the same exclusive resource at once** — one
simulator, one device, one bound port. Attempt 4 measured a 0% flake rate with
exclusive access on a case three earlier attempts had called flaky; contention had
been resolving a build artefact from one run into a sibling worktree. Re-measure
any historical flake rate before trusting it rather than inheriting a number taken
under contention.

## Reviewer isolation *(kit)*

A reviewer reads from git objects only — `git diff`, `git show`, `git log` —
never the working tree, because another agent may be actively mutating it. A
reviewer and a tester were once run against the same worktree at the same time;
the tester's brief required temporarily mutating a source file to prove a guard
non-vacuous, the reviewer read the working tree mid-mutation, and reported a
phantom defect that existed at no commit. The reviewer had already been told to
use `git diff`/`git show` and checked the working tree anyway, so state both
halves: do not schedule a reviewer and a tester against the same worktree at the
same time when the tester's brief includes a mutation-based non-vacuity proof —
serialise them, or have the reviewer work from an explicit `git show` of the tip.
Mutation-based proofs are standard practice under the discipline above, so this
collision recurs unless both halves are stated.

## Background tasks in subagent turns *(kit)*

Subagents do not receive background-task completion notifications. An implementer
that starts a long build or test run in the background and ends its turn saying
"waiting for job X" is never woken — it stalls rather than resumes. Run long
commands in the foreground and block, or poll within the same turn; never yield a
turn expecting to be resumed later. This has also retrospectively explained
stalled runs previously written off as environment hangs.

## Verification environment *(kit)*

Before trusting any test failure against a service-backed suite, confirm the
service is actually accepting connections for *this* run — an explicit client
connect, not just a container reporting healthy. One wave lost significant time
to three environment faults that produced red results indistinguishable from code
failures until traced by hand. A container can restart healthy while never
re-binding a host port already held by something else, and a healthcheck does not
catch that.

## What never goes in a file

Credentials, tokens, keys, real personal data, and pasted log dumps.

## Logs *(kit)*

If an agent needs to write a log file — verification output, replay traces — it
goes in `logs/` at the repo root, never loose at the top level. `logs/` and
`*.log` are gitignored: logs are scratch, not an artifact to commit.

## Java 8 *(project)*

The library targets Java 8 and the build enforces it, but a violation caught by
`mvn verify` has already cost a review round trip. Do not write, in main sources:

`List.of` / `Set.of` / `Map.of` / `.copyOf`, `Optional.isEmpty`, `String.isBlank`
/ `.repeat` / `.strip`, `Stream.toList`, `var`, records, sealed types, switch
expressions, text blocks, `instanceof` patterns.

Use `Arrays.asList`, `Collections.singletonList`, `Collections.emptyList`,
`Collectors.toList`, and explicit types. Wrap what you expose:
`Collections.unmodifiableList(new ArrayList<>(input))` — defensively copied,
because an unmodifiable view over a caller's list is not immutable.

`--release 8` is only available from JDK 9, so the build runs on JDK 17 pinned by
maven-toolchains. Building on JDK 8 is unsupported.

## Immutability and threads *(project)*

Everything reachable from a built `EntityResolver` is immutable and safe for
concurrent `resolve()` calls. `final` fields, no setters, defensive copies in
constructors, no lazy initialisation without a documented memory-model argument.

No mutable static state. No static cache holding field values — that is both a
thread-safety problem and a privacy one.
