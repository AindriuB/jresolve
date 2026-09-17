# 23 — `docs/calibration.md`

**Repo:** .
**Depends on:** 21
**Owns:**
- docs/calibration.md

## Goal
D11 does not merely recommend this file — it names it as where the
conditional-independence assumption is stated and where it fails. Three other
tasks' Javadoc points at it. Without it those pointers dangle and the
assumption lives only in scattered class comments.

## A role question this task raises, and does not settle
`CLAUDE.md` rule 7 says **only `scribe` writes docs**. This task writes one.

It is listed as a task rather than folded into the record pass because the
file is a design artefact D11 requires, not a record of work done — its
content is decided by whoever understands the model, and it must exist before
the milestone can claim D5 is honoured. But that is an argument, not a
ruling. Whoever picks this up should either confirm `scribe` owns it and hand
it over, or note the exception in the commit body. Do not write it silently
and leave the rule looking broken.

## Context
- docs/design-decisions.md#d11 — the assumption, the household counterexample, and this file named as its home.
- docs/design-decisions.md#d5 — where `m` and `u` are expected to come from, and why a posterior from prior odds is conditional on the model's own assumptions.
- docs/design-decisions.md#d16 — `m` is normally estimated by EM over unlabelled pairs; the file must say which estimation method each field of a model is expected to come from.
- docs/spec/original-design.md §94 — the distinctions the library must keep apart: similarity, likelihood ratio, raw score, probability, calibrated probability. Read that section only.
- docs/spec/original-design.md §5 and §34, via docs/design-decisions.md#d18 — a similarity is not a probability, and blocking is not proof of identity. Kept verbatim as rules; this file should not soften them.
- docs/conventions.md — house style for prose.

## Acceptance
- [ ] The file states the conditional-independence assumption plainly: Fellegi-Sunter assumes fields are conditionally independent given match status.
- [ ] It names where that fails, with a worked explanation rather than an assertion — two fields that co-vary within a household are not independent, so a model over both double counts the shared signal and is systematically overconfident. **This file may name domain concepts**; the core-vocabulary rule binds `jresolve-core`, not `docs/`.
- [ ] It states what the library does about it: nothing automatic, but the model supports declaring two fields as one composite comparison, and the scorer's Javadoc repeats the assumption. The honest position is that the library cannot fix this and must not hide it.
- [ ] It says where each part of a model is expected to come from: `u` from a corpus frequency, `m` from labelled data or EM over unlabelled pairs, prior odds from the consumer's own base rate. A reader configuring a model should be able to tell which numbers they can measure and which they must estimate.
- [ ] It keeps §94's five terms apart — similarity, likelihood ratio, raw score, probability, calibrated probability — with one sentence each on what the library will and will not give them.
- [ ] It states that a posterior computed from prior odds is conditional on the model's own assumptions and is not evidence of empirical calibration.
- [ ] Every pointer aimed at this file resolves: the Javadoc in tasks 20, 21 and 22 cites it, and the sections those citations imply exist under headings a reader can find. This task owns only `docs/calibration.md`, so a citation that points somewhere this file cannot sensibly provide is **reported, not fixed** — editing the citing class belongs to the task that owns it.
- [ ] It says what it is *not*: not a tutorial on estimating a model, not a claim that any shipped default is calibrated. The library ships no calibrated model, and the file says so.
- [ ] No real personal data, and no worked example built from one.

## Out of scope
- Prescribing an estimation procedure in detail. Name the methods and their inputs; implementing or tutorialising them is outside the library and outside this file.
- Recommending specific `m`/`u` values. Any number that looked authoritative here would be copied into production by someone who should have measured it.
- Restating `docs/design-decisions.md`. Cite it; do not duplicate it.
