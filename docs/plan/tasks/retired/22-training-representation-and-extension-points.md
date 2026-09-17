# 22 — Training representation and the extension points

**Repo:** .
**Depends on:** none
**Owns:**
- jresolve-core/src/main/java/io/github/aindriub/jresolve/scoring/LabelledMatchExample.java
- jresolve-core/src/main/java/io/github/aindriub/jresolve/scoring/UnlabelledMatchExample.java
- jresolve-core/src/main/java/io/github/aindriub/jresolve/scoring/FeatureExtractor.java
- jresolve-core/src/main/java/io/github/aindriub/jresolve/scoring/ProbabilityModel.java
- jresolve-core/src/test/java/io/github/aindriub/jresolve/scoring/MatchExampleTest.java
- jresolve-core/src/test/java/io/github/aindriub/jresolve/scoring/ExtensionPointsTest.java

## Goal
Ship the shapes that keep the future open, and nothing that pretends to
implement it.

Two separate obligations. D16 says §93's labelled-only training example is not
enough, because `m` is normally estimated by EM over *unlabelled* pairs — so
the representation needs an unlabelled form or the standard estimation path is
blocked before it starts. D17 keeps logistic regression behind v1 but requires
`FeatureExtractor` and `ProbabilityModel` to ship unimplemented, so §107's
extension requirement holds.

## Context
- docs/design-decisions.md#d16 — why a labelled-only example blocks EM, and that models are built rather than deserialized.
- docs/design-decisions.md#d17 — logistic regression and the JMH harness move behind v1; the two interfaces still ship.
- docs/spec/original-design.md §93 — the original `MatchTrainingExample`, features plus a boolean label. Read that section only.
- docs/spec/original-design.md §107 — future models must be addable without changing `FieldDefinition`, `FieldPipeline` or `MatchEvidence`. Read that section only.
- jresolve-core/src/main/java/io/github/aindriub/jresolve/evidence/MatchEvidence.java — what a model actually consumes.

## Acceptance
- [ ] `LabelledMatchExample` carries a feature map and a boolean label; `UnlabelledMatchExample` carries the feature map alone. Both are final, immutable, and defensively copy the map.
- [ ] Both reject a null or null-containing feature map; neither permits mutation after construction, asserted by a test that mutates the source map afterwards and shows the example unchanged.
- [ ] The Javadoc on the unlabelled form states why it exists: `m` is normally estimated by EM over unlabelled pairs, and a representation that can only express labelled data blocks the standard estimation path. Without that sentence the type looks like a redundant cousin of the labelled one.
- [ ] The renaming away from §93's `MatchTrainingExample` is deliberate — two types, neither of which is "the" training example — and a Javadoc line records that, so a reader holding the spec is not left hunting for a class that no longer exists under that name.
- [ ] `FeatureExtractor` turns `MatchEvidence` into a feature map; `ProbabilityModel` turns a feature map into a probability. Both are interfaces, both are documented as extension points with **no implementation shipping in this milestone**, and their Javadoc says so explicitly rather than leaving a reader to wonder where the implementation went.
- [ ] `ProbabilityModel`'s Javadoc states that a probability from such a model is a probability *under that model*, and that production confidence still depends on representative training data and calibration (§94). It points at `docs/calibration.md`.
- [ ] A test implements both interfaces with a trivial stand-in and asserts they compose — evidence to features to probability — proving the extension point is usable rather than merely declared. The stand-in lives in the test and nothing in main sources implements either interface.
- [ ] A test asserts `FeatureExtractor` consumes `MatchEvidence` unchanged: §107 requires a future model be addable without changing `FieldDefinition`, `FieldPipeline` or `MatchEvidence`, and the test names that requirement.
- [ ] No type, member or Javadoc word in these files names a person, name, address, date of birth or country; fixtures are neutral tokens.

## Out of scope
- Any implementation of `ProbabilityModel` — logistic regression is explicitly behind v1 (D17).
- Training, fitting or EM. The library defines the representation; estimation happens outside it (D16).
- Serialization of examples or models (D16 forbids the core dependency).
- Feature engineering: what makes a good feature is a consumer's problem and `docs/calibration.md`'s subject, not a core type's.
