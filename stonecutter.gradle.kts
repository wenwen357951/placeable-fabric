// Stonecutter root controller script.
//
// This script runs in the *root* project context. Its responsibilities are:
//   1. Declare which Stonecutter target is "active" for the current Gradle
//      invocation.
//   2. Register a top-level `chiseledBuild` aggregator that runs `build`
//      against every declared version. CI relies on this task name.
//
// Per-version build logic lives in `build.gradle.kts` (the centralScript),
// which is evaluated once per chiseled subproject with the per-version
// `versions/<mc>/gradle.properties` already merged into the project
// properties.
//
// Stonecutter automatically generates `Set active project to <mc>` tasks that
// switch the active version persistently (they rewrite the literal in this
// file).
//
// The multi-version surface is intentionally small. Cross-version source
// differences live inside Java files as `//? if` comments, NOT in branched
// build scripts.

plugins {
    id("dev.kikugie.stonecutter")
}

// Active version — must match one of the versions declared in
// `settings.gradle.kts` inside `stonecutter { create(rootProject) {
// versions(...) } }`.
stonecutter active "1.21.5"

// Aggregator task — invokes `build` across every chiseled subproject.
// Uses Stonecutter's lazy task collection (`stonecutter.tasks.named`) so
// subproject task instances are never resolved eagerly (which would defeat
// configuration avoidance and break with Gradle 9+ task realization rules).
//
// CI consumes this single task name to produce the full version matrix in
// one run.
tasks.register("chiseledBuild") {
    group = "build"
    description = "Runs `build` for every Stonecutter-registered Minecraft version."
    dependsOn(stonecutter.tasks.named("build"))
}
