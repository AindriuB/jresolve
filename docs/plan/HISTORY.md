# History

Append-only, newest first. See `docs/plan/HISTORY-INDEX.md` for a grep-first
index — do not load this file whole.

## 2026-09-09 — Resolver and builder land (task 07)

`jresolve-core` gains `api/`: `EntityResolver`, `CandidateRule`/`RuleDecision`,
`EntityResolverBuilder`, and the package-private `DefaultEntityResolver` that
wires 05's `field/`, 06's `scoring/` and `decision/` together — prepare the
source once per `resolve()`, compare fields in ascending cost tiers, run hard
`CandidateRule` vetoes between tiers with short-circuit, hand ranked
candidates to the decision engine. `EntityResolverBuilder.build()` rejects
every configuration error `§98` names, throwing
`EntityResolutionConfigurationException` with a message naming the field or
constraint. Union build on `main`: `mvn clean verify`, `BUILD SUCCESS`, 251
tests, exit 0 — exactly the 229-test baseline plus 22 new, no overlap.

**Cost:** Two attempts. Attempt 1 passed testing (250 tests) and was rejected
on review for three defects that were all the same shape: a guard that does
not guard. A D6 scale check validating an object with no runtime effect;
`.rule(null)` accepted and silently discarded while every other null failed
`build()`; `.required(fieldName)` setting a flag nothing in the library reads.
Attempt 2 closed two of the three in code and left the third open by design.

The D6 hole is real, open, and now documented rather than fixed:
`EntityResolverBuilder` validates the `DecisionThresholds` passed to
`.thresholds(...)` against the scorer's scale, but that object never reaches
the engine that actually decides — the engine decides using whatever
thresholds it was constructed with. A caller who passes one
`DecisionThresholds` instance to `new ThresholdDecisionEngine<>(...)` and a
different, scale-matching instance to `.thresholds(...)` gets a clean
`build()` and silently wrong decisions at `resolve()`. Both the tester and
the reviewer reproduced it concretely: an engine holding PROBABILITY
thresholds (0.9/0.5/0.2) with a scale-matching POINTS object passed to the
builder builds cleanly, and `resolve()` then returns MATCH by comparing a
10.0-point score against a 0.9 probability threshold.

It was not fixed inside `api/` because it cannot be. The reviewer checked the
obvious alternative — build the engine from the thresholds the builder just
validated — and refuted it: `MatchDecisionEngine` is an interface callers
must be able to implement themselves, `.decisionEngine(...)` is itself an
acceptance criterion, and `ThresholdDecisionEngine.thresholds` is private
with no getter. Fixing it means changing `decision/`, which task 07 does not
own, and widening `Owns` to fix it was explicitly ruled out. What shipped
instead is a Javadoc contract on `.thresholds(...)`: pass the exact same
instance used to construct the engine, plus a stated failure mode and why the
library cannot detect a violation. Both agents independently confirmed the
contract is sufficient — following it closes the hole entirely — but a
documented contract is weaker than a construction-time exception, and the
library still ships with a way to be silently wrong that only discipline
prevents. Recorded honestly in `PLAN.md`'s Known gaps for milestone 2:
`MatchDecisionEngine` should expose its scale so the check can inspect the
object that actually decides.

This is the third time this milestone the same failure mode has recurred
across independent probes: a byte scan for `C2 A0` proved the NBSP class
clean but was read as proving the invisible-character class was (task 03); a
probe of `getMargin()` on valid constructions could not reach the malformed
ones it was meant to guard (task 04); here, a tester confirming the D6
exception fires while the reviewer found it guards nothing it is wired to. In
every case the measurement was correct and the sentence describing it was
wider than the measurement. The counter is not "does the mechanism work" but
"is it wired to anything" — worth carrying into every future review round in
this project, not just this milestone's.

## 2026-09-09 — Field layer, scoring and decision land (tasks 05, 06)

`jresolve-core` gains `field/` (task 05) and `scoring/` + `decision/` (task
06). `field/`: `FieldPipeline` (the `prepare`/`compare` split), `FieldDefinition`,
`CostTiers`, and the two comparators the end-to-end test needs —
`ExactFieldComparator` and `SimilarityFieldComparator` with configurable
`SimilarityBands`. `scoring/`: `MatchScorer`, `RuleBasedScorer` (per-field,
per-category weights summing to a `Score` on `ScoreScale.POINTS` with ordered
`FieldContribution`s), `ScoringResult`. `decision/`: `DecisionThresholds` and
`ThresholdDecisionEngine`, ranking scored candidates and turning them into a
`MatchResult` via the legal-state set task 04 defined. Union build on `main`
after both merges: `mvn clean verify`, `BUILD SUCCESS`, 229 tests, exit 0 —
142 baseline + 53 from 05 + 34 from 06, exactly, no overlap. That settles a
discrepancy between task 06's implementer (175 tests) and tester (176) in
its own worktree: `scoring/` (11 + 9) plus `decision/` (5 + 9) sum to 34,
matching the tester's count and the union arithmetic. The implementer's 175
was an undercount by one; nothing in the union total is inconsistent with
34 new tests from 06.

**Cost:** Both tasks passed on the first attempt — the first wave in this
milestone to do so. Both briefs carried forward what the previous six
attempts (waves 1-2) cost: break something on purpose before reporting,
hand-derive expectations rather than asserting a value the code produced, and
state the legal set before enforcing anything. Neither implementer needed a
rejection to apply all three.

Two design claims moved from asserted to measured this wave, not just
implemented:

D2 (the prepare/compare split) is now demonstrated behaviourally, not just
inferred from the signature. Task 05's test built an invocation-counting
normalizer and drove ten candidate comparisons through a pipeline built on
it: `prepare` on the source value ran exactly once, `compare` never touched
the normalizer at all. That probe — count invocations on the object under
test, not on a mock that only records calls it was told to expect — is how
this claim should be re-verified once task 07 wires the resolver and the
same property has to hold across a whole `resolve()` call, not one pipeline.

Task 04's `MatchResult` legal set (7 of 24 combinations, closed in wave 2 by
enumerating the whole state space rather than patching named instances) met
its first real producer this wave. Task 06's reviewer walked every path
`ThresholdDecisionEngine` can take by reading the code; task 06's tester
independently drove eight scenarios through a probe of the built engine —
empty candidate list, a single candidate above/between/below the thresholds,
and the two-candidate match/review/no-match combinations. Neither found a
path that constructs an illegal combination. A constraint written in one
wave held against a producer written in the next, checked twice,
independently, without either side needing to consult the other's method.

Both implementers independently chose an inclusive-boundary operator
(`>=` to `>`) as their mutation-testing target, and in both cases a named
test caught it on the first try. Testers then swept every boundary rather
than trusting the one sampled mutant: three similarity bands in 05
(`VERY_HIGH`/`HIGH`/`MEDIUM` at 0.95/0.85/0.70) and three thresholds in 06
(`matchThreshold`, `reviewThreshold`, `minimumMargin`), each caught by
exactly one named test. This is the second wave running where a
boundary-operator flip has been the sharpest mutation-testing signal in the
codebase; it is where silent misclassification risk concentrates here.

Ownership of two open items got settled rather than left ambiguous:
`Score.algorithm`'s nullability is not task 07's to close (07 can only
reject a null at `build()`, which does not stop direct `Score` construction
elsewhere; the actual fix is a constructor guard in task 04's
`result/Score.java`) — moved from "task 07 should decide" to a milestone-2
item in `docs/plan/PLAN.md`'s Known gaps. D1's symmetric-case builder sugar
is task 07's, and task 05 confirmed `field/` needs no change to support it;
the concrete expression is recorded in
`docs/plan/tasks/07-resolver-and-builder.md` as a note, not an acceptance
criterion.

Five non-blocking items went into Known gaps rather than being fixed outside
their owning task's scope: `ExactFieldComparator`'s frequency key derives
from `toString()`, not `equals()`, which will misattribute D5's (milestone 2)
frequency adjustment for any `N` with value equality but a default
`toString`; `SimilarityBands.categoryFor` is package-private while its class
and getters are public, so an outside comparator can't reuse it and might
reimplement banding with different bounds; `ScoredCandidate` still permits a
null candidate, which would make `ThresholdDecisionEngine` throw from the
wrong package if the (currently nonexistent) producer ever made one;
`TokenSplitter` (flagged in three consecutive waves now, since task 02) still
has no production implementation; and two small test-hygiene notes — a
backwards comment in `FieldComparatorNullSafetyTest`, a redundant
`@SafeVarargs` in `ThresholdDecisionEngineTest`.

## 2026-09-09 — Normalization primitives and core value types land (tasks 03, 04)

`jresolve-core` gains two more packages. `normalization/` (task 03): a
`StringNormalizer` functional interface plus stages for Unicode form, case
folding, combining-mark removal, apostrophe variants, punctuation and
whitespace, composed by an immutable `CompositeNormalizer`. `evidence/` and
`result/` (task 04): `ComparisonCategory` (interned, not an enum),
`FieldEvidence`/`DefaultFieldEvidence`/`MatchEvidence`, and
`Score`/`ScoreScale`/`Decision`/`MatchResult`/`ScoredCandidate`/`FieldContribution`
— the immutable vocabulary tasks 05-07 build against. Union build on `main`
after both merges: `mvn clean verify`, `BUILD SUCCESS`, 142 tests across 21
test classes, exit 0.

**Cost:** Both tasks took three attempts, and both converged the same way —
production code was correct at attempt 1 in each case; every rejection was
about a state that could not fail while the suite stayed green.

03's defect was a non-breaking space written as a raw invisible character one
line above an ordinary space, visually identical to it. Any editor's
trim-whitespace pass would silently rewrite both the production literal and
the test's own literal to a plain space, so `WhitespaceNormalizer` would stop
folding NBSP and the test would keep passing — degrading together, not apart.
Found five times across the wave: twice by review, a third by the implementer
generalising the first two, a fourth (a combining acute, `CC 81`, in
`UnicodeFormNormalizerTest`) by the next review pass after attempt 2's byte
scan for `C2 A0` correctly found zero NBSP instances and incorrectly read as
proof the whole invisible-character class was clean, and closed on attempt 3
by enumerating every non-ASCII code point in both owned trees with a verdict
per entry rather than scanning for one more sequence. That enumeration, and
the rule it now backs in `docs/conventions.md#tests`, is the artifact that
outlives the fix.

04's defect was that `MatchResult` permitted states that contradicted its own
documented contract: `isMatch()` true with `getMatch()` null, or the reverse,
or a second-best score with no best score to be second to. Attempt 2 closed
the named instances one at a time and reopened a sibling each time — reject
`NO_MATCH` carrying a candidate, and `MATCH` with a null match was still
legal; guard `getMargin()`'s NPE, and the `IllegalStateException` it threw
instead still misdescribed its own cause. Attempt 3 stopped patching cases and
wrote down the legal set instead: 7 of 24 combinations of (decision, match,
score, secondBestScore), enforced exactly in the constructor, with a
24-combination matrix test asserting accepted equals legal in both
directions. Recorded as an amendment to `docs/design-decisions.md#d12` and in
`docs/architecture.md` under "The type model" — 05, 06 and 07 all consume
`MatchResult` and need the set, not just the current absence of a bug.

The lesson that generalizes past this wave: fixing the instances a review
names produces another review; fixing the class ends it. A green suite is
evidence about the tests, not the code, until something is broken on purpose
and observed to fail — attempt 3 in both tasks is the only attempt that left
behind an artifact that stays true on its own (the byte enumeration, the
matrix test) rather than one more instance that happened to be found.

Three items raised in review and left open because no task owns the file:
`JaroWinklerSimilarity`'s two constructor guards (from task 02) still have no
automated test; `MatchEvidence.toString()`'s `evidence == null` branch is now
dead code since the constructor rejects null values; and
`UnicodeFormNormalizerTest:28` feeds a precomposed literal to an NFD test,
which passes today but proves nothing under an NFD-normalizing editor pass.
Recorded in `docs/plan/PLAN.md` under "Known gaps" rather than fixed by this
close-out, per the retired task files' scope. `Score.algorithm` (task 04) is
unvalidated and may be null; task 06 is expected to stamp it in practice, and
a note to that effect is in `docs/plan/tasks/06-scoring-and-decision.md`.

## 2026-09-09 — Similarity metrics land (task 02)

`jresolve-core` now has a `comparison/` package: `SimilarityMetric`, and three
implementations — `JaroWinklerSimilarity`, `LevenshteinSimilarity` and
`TokenSimilarity` (a delegate-plus-splitter best-match-mean over tokens). All
three are pure functions of two strings, treat `null` as empty, and are bounded
in `[0,1]` and exactly symmetric, each backed by a property test over a
20-plus-pair synthetic corpus. A `TokenSplitter` functional interface also
landed, used today only by a test lambda.

**Cost:** Took two attempts. Attempt 1 tested PASS — 29 tests, a full mutation
battery, hand-computed values agreeing — and was rejected in review anyway:
`TokenSimilarityTest.weightsBestMatchesBySideTokenCounts` derived its expected
value by calling `JaroWinklerSimilarity` at test time and then asserting the
code against its own output, so it could not detect a regression in the
delegate it was supposed to be exercising. Attempt 2 rewrote it against a
hand-derived expectation — a token pair where Jaro-Winkler is exactly 0.0
because no characters fall within the match window — and proved the fix by
perturbing the delegate and watching the test fail where it had previously
passed. The general lesson recurred in both 03 and 04 the same wave and is
worth restating here: a green suite is evidence about the tests, not about the
code, until something has been broken on purpose and observed to fail.

`TokenSplitter` was not named in the task file but was accepted after a
cross-branch check against 03's concurrent work: `normalization/` is
`String`-to-`String` throughout with no tokenising abstraction to collide
with. Nothing in 02's main sources implements `TokenSplitter` yet — only a
test lambda does — so task 05 is where a second splitter could be invented
independently; noted directly in `docs/plan/tasks/05-field-layer.md` so it
isn't rediscovered.

Two constructor guards added to `JaroWinklerSimilarity` in attempt 2 (prefix
scale bounds) have no automated test covering them — the tester verified them
by hand with a scratch class — so deleting either guard would survive the
suite unnoticed; worth a property or unit test in a later pass. `mvn clean
verify` now runs roughly 9-13 seconds from clean, up from 7-8 at task 01's
close.

## 2026-09-09 — Maven multi-module build skeleton lands (task 01)

The repo now has a real reactor build: a root `pom` with `jresolve-core` and
`jresolve-profiles-ie` modules, a JDK 17 toolchain requirement, `maven.compiler.release=8`,
JUnit 5 + AssertJ on the test classpath via root `dependencyManagement`, and
`animal-sniffer-maven-plugin` checking both modules against `java18` in
`verify`. `jresolve-core` has no compile/runtime dependency; `jresolve-profiles-ie`
depends on it and holds only an empty `io.github.aindriub.jresolve.profiles.ie`
package. `BuildSmokeTest` proves JUnit 5 and AssertJ are wired. `mvn clean
verify` runs green from a clean checkout in roughly 7-8 seconds, which is the
baseline for later tasks — expect it to grow.

**Cost:** Took two attempts. Attempt 1 tested PASS but was rejected in review:
it left animal-sniffer's `animal.sniffer.skip` user property live, so `mvn
verify -Danimal.sniffer.skip=true` silently bypassed the java18 check — the
exact gate this task existed to install. A `-D` on the command line can
override a POM value expressed through the property's own expression, but
cannot override an explicit `<skip>false</skip>` written directly in the
plugin's `<configuration>`; that is the fix, and it is the only way to make
the gate genuinely non-skippable. Attempt 1 also pinned
`<vendor>openjdk</vendor>` on the toolchain requirement, which would fail
against a `toolchains.xml` reading `temurin` or `zulu` (what a second
developer is likely to have installed) — dropped in attempt 2. The diff
between attempts was four lines.

Proving animal-sniffer non-vacuous needed `maven.compiler.release` temporarily
raised from 8 to 11: at `release=8`, javac's own `--release` check rejects a
post-Java-8 API before the sniffer ever runs, so the two gates are redundant
for this class of violation. Both are kept anyway — `release=8` is what
someone loosens to unblock themselves in a hurry, and the sniffer is what
catches them doing it when they do.

Also measured and worth not re-deriving: breaking the `toolchains.xml` version
makes the build fail with "Cannot find matching toolchain definitions" rather
than silently falling back to whichever JDK is running Maven (25 here); and
`mvn -pl jresolve-core dependency:list -DincludeScope=runtime` resolves
nothing, confirming core's JDK-only constraint by measurement rather than
inspection.

The build needs a JDK 17 toolchain that is not part of the repo — Temurin
17.0.20.1 at `C:\Program Files\Eclipse Adoptium\jdk-17.0.20.101-hotspot` on
this machine, with `C:\Users\aindr\.m2\toolchains.xml` pointing at it. Anyone
else building this needs their own `toolchains.xml`; documented in
`docs/architecture.md#building`. Maven 3.8.6 itself runs on JDK 25 — only
compilation is pinned to 17.
