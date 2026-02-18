---
name: composite-build
description: Set up an Eventual Consistent Monorepo composite build between a producer and consumer repo. Use when the user wants to compose two repos so that one depends on the other from source, with breaking change detection, Gazelle BUILD generation, and bazel-diff target determination. Also use when the user says "add a new producer", "wire up a dependency", or "set up cross-repo build".
argument-hint: "[producer-repo] [consumer-repo]"
---

# Eventual Consistent Monorepo — Composite Build Setup

You are setting up a cross-repository composite build using the Eventual Consistent Monorepo pattern. This wires a **producer** repo (exposes a library) into a **consumer** repo (depends on it) so that:

- Both build from source — no published artifacts
- Breaking API changes are caught at compile time
- `bazel query` works across repo boundaries
- Gazelle can auto-generate BUILD files
- bazel-diff can determine affected targets

## Inputs

Ask the user for these if not provided via arguments:

1. **Producer repo** — the repo that exposes a library (e.g., `bazel-monorepo`)
2. **Consumer repo** — the repo that depends on the library (e.g., `ecmonorepo`)
3. **Producer target(s)** — which packages/targets to expose (e.g., `//library:library`)
4. **Integration type** — Bazel module (producer is Bazel) or source copy (producer is Gradle/Maven)

## Step-by-step Procedure

### Phase 1: Discover and Validate

1. Read the producer's `MODULE.bazel` (or `build.gradle`, `pom.xml`) to understand its build system and module name.
2. Read the consumer's `MODULE.bazel` to see existing `bazel_dep` and `local_path_override` entries.
3. Identify the Java packages the producer exposes (scan `src/main/java/` for package declarations).
4. Identify existing consumer code that imports or should import the producer's packages.

### Phase 2: Wire the Producer

**If the producer is already a Bazel repo:**

1. Ensure the producer target has `visibility = ["//visibility:public"]` in its BUILD file.
2. If the producer target doesn't exist yet, create a `BUILD` file with:
   ```python
   load("@rules_java//java:defs.bzl", "java_library")

   # gazelle:java_module_granularity module

   java_library(
       name = "<package-name>",
       srcs = [
           # List each .java file explicitly (not glob) for Gazelle compatibility
           "src/main/java/com/example/<pkg>/Foo.java",
       ],
       visibility = ["//visibility:public"],
   )
   ```
3. If the producer has tests, add a `java_test_suite`:
   ```python
   load("@contrib_rules_jvm//java:defs.bzl", "java_test_suite")

   java_test_suite(
       name = "<package-name>-tests",
       size = "small",
       srcs = ["src/test/java/..."],
       runner = "junit5",
       deps = [
           ":<package-name>",
           "@maven//:org_junit_jupiter_junit_jupiter_api",
       ],
       runtime_deps = [
           "@maven//:org_junit_jupiter_junit_jupiter_engine",
           "@maven//:org_junit_platform_junit_platform_launcher",
           "@maven//:org_junit_platform_junit_platform_reporting",
       ],
   )
   ```

**If the producer is Gradle/Maven (source copy):**

1. Create a new package in the consumer repo (e.g., `library/`).
2. Copy the producer's Java sources into `library/src/main/java/...`.
3. Create a `library/BUILD` with `java_library` targeting those sources.

### Phase 3: Wire the Consumer

1. **Update consumer `MODULE.bazel`** — add the producer as a Bzlmod dependency:
   ```python
   bazel_dep(name = "<producer-module-name>", version = "1.0.0")
   local_path_override(
       module_name = "<producer-module-name>",
       path = "../<producer-repo-dir>",
   )
   ```

2. **Add maven dependencies** if needed (JUnit 5, AssertJ, etc.):
   ```python
   bazel_dep(name = "contrib_rules_jvm", version = "0.32.0")
   bazel_dep(name = "rules_jvm_external", version = "6.7")

   maven = use_extension("@rules_jvm_external//:extensions.bzl", "maven")
   maven.install(
       artifacts = [
           "org.junit.jupiter:junit-jupiter-api:5.10.1",
           "org.junit.jupiter:junit-jupiter-engine:5.10.1",
           "org.junit.platform:junit-platform-commons:1.10.1",
           "org.junit.platform:junit-platform-engine:1.10.1",
           "org.junit.platform:junit-platform-launcher:1.10.1",
           "org.junit.platform:junit-platform-reporting:1.10.1",
           "org.opentest4j:opentest4j:1.3.0",
           "org.apiguardian:apiguardian-api:1.1.2",
           "org.assertj:assertj-core:3.25.3",
           "net.bytebuddy:byte-buddy:1.14.11",
       ],
       lock_file = "//:rules_jvm_external++maven+maven_install.json",
       repositories = ["https://repo1.maven.org/maven2"],
   )
   use_repo(maven, "maven")
   ```

3. **Create or update consumer BUILD** with the cross-repo dependency:
   ```python
   java_library(
       name = "consumer",
       srcs = ["src/main/java/..."],
       deps = ["@<producer-module-name>//<package>"],
   )

   java_test_suite(
       name = "consumer-tests",
       size = "small",
       srcs = ["src/test/java/..."],
       runner = "junit5",
       deps = [
           ":consumer",
           "@<producer-module-name>//<package>",  # keep
           "@maven//:org_junit_jupiter_junit_jupiter_api",
           "@maven//:org_assertj_assertj_core",
       ],
       runtime_deps = [
           "@maven//:org_junit_jupiter_junit_jupiter_engine",
           "@maven//:org_junit_platform_junit_platform_launcher",
           "@maven//:org_junit_platform_junit_platform_reporting",
       ],
   )
   ```

4. **Pin maven deps** — run `bazel run @maven//:pin` to generate the lock file. If this fails with "Unable to determine lock file version", first create a minimal lock file:
   ```json
   {
     "dependency_tree_lock": {
       "__AUTOGENERATED_FILE_DO_NOT_MODIFY_THIS_FILE_MANUALLY": "..."
     }
   }
   ```
   Then re-run `bazel run @maven//:pin`.

### Phase 4: Configure Gazelle (if applicable)

1. **Add Gazelle deps** to `MODULE.bazel`:
   ```python
   bazel_dep(name = "rules_go", version = "0.60.0", repo_name = "io_bazel_rules_go")
   bazel_dep(name = "gazelle", version = "0.47.0")
   ```

2. **Add Gazelle config** to the consumer's root BUILD:
   ```python
   load("@gazelle//:def.bzl", "DEFAULT_LANGUAGES", "gazelle", "gazelle_binary")

   # gazelle:java_extension enabled
   # gazelle:java_maven_install_file rules_jvm_external++maven+maven_install.json
   # gazelle:exclude src

   gazelle(name = "gazelle", gazelle = ":gazelle_bin")
   gazelle_binary(
       name = "gazelle_bin",
       languages = DEFAULT_LANGUAGES + ["@contrib_rules_jvm//java/gazelle"],
   )
   ```

3. **Add cross-repo resolve directives** to the root BUILD for each producer package:
   ```python
   # gazelle:resolve java <java-package> @<module>//<bazel-package>
   ```
   Example:
   ```python
   # gazelle:resolve java com.example.library @composite-monorepo//library
   ```

4. **Add module granularity** to each subpackage BUILD:
   ```python
   # gazelle:java_module_granularity module
   ```

5. **Use explicit srcs** (not `glob()`) in BUILD files — Gazelle cannot merge with glob expressions.

6. **Verify idempotence**: Run `bazel run //:gazelle` and confirm no files change.

### Phase 5: Configure bazel-diff (Target Determination)

1. **Generate baseline hashes** (before any changes):
   ```bash
   bazel-diff generate-hashes \
     -w "$(pwd)" \
     -b "$(which bazel)" \
     --fineGrainedHashExternalRepos=@<producer-module>,@maven \
     baseline-hashes.json
   ```

2. **After making changes**, generate new hashes and diff:
   ```bash
   bazel-diff generate-hashes \
     -w "$(pwd)" \
     -b "$(which bazel)" \
     --fineGrainedHashExternalRepos=@<producer-module>,@maven \
     changed-hashes.json

   bazel-diff get-impacted-targets \
     -sh baseline-hashes.json \
     -fh changed-hashes.json \
     -o impacted-targets.txt
   ```

3. **Filter for test targets**:
   ```bash
   grep -E '(Test|test)' impacted-targets.txt
   ```

**CRITICAL**: The `@` prefix is **required** for Bzlmod repos in `fineGrainedHashExternalRepos`. Without it, bazel-diff silently produces empty results.

### Phase 6: Create CI Workflow

Create `.github/workflows/composite-build.yml` with two jobs:

**Job 1: happy-path**
- Checkout both repos (consumer + producer as sibling via `path: ../producer-repo`)
- Setup Java 21
- `bazel build //...`
- `bazel test //<consumer-pkg>:all --test_output=all`
- `bazel query 'rdeps(//..., @<module>//<pkg>)'`

**Job 2: breaking-change**
- Checkout both repos
- Verify happy path first
- Apply a breaking change to the producer (e.g., change a method signature via sed)
- Run `bazel test` and verify it fails
- Show affected targets via `bazel query`

### Phase 7: Verify End-to-End

Run these commands and confirm results:

```bash
# 1. Happy path
bazel build //...
bazel test //... --test_output=errors

# 2. Cross-repo query
bazel query 'rdeps(//..., @<module>//<pkg>)'
bazel query 'kind("test", rdeps(//..., @<module>//<pkg>))'

# 3. Breaking change detection
# Modify a method signature in the producer
bazel test //<consumer>:all  # Should fail to compile

# 4. Gazelle idempotence
bazel run //:gazelle  # Should produce no changes

# 5. bazel-diff target determination
# Generate hashes before/after, confirm impacted targets include consumer tests
```

## Common Pitfalls

| Problem | Solution |
|---------|----------|
| Maven lock file "Unable to determine version" | Bootstrap with a minimal JSON structure, then `bazel run @maven//:pin` |
| Gazelle "could not merge expression" on srcs | Use explicit file lists, not `glob()` |
| Gazelle creates unwanted subpackage BUILD files | Add `# gazelle:exclude src` to root BUILD |
| Gazelle can't resolve cross-repo imports | Add `# gazelle:resolve java <package> @module//target` |
| bazel-diff returns empty impacted targets | Use `@` prefix: `--fineGrainedHashExternalRepos=@module` |
| Stale bazel symlinks cause package errors | Remove `bazel-*` symlinks that point to non-existent paths |
| Test size warnings | Add `size = "small"` to `java_test_suite` |
| `actions/checkout` can't access sibling dir | Use `path: ../repo-name` in the checkout step |

## Reference Files

After setup, the key files in the consumer repo should be:

- `MODULE.bazel` — bazel_dep + local_path_override + maven
- `BUILD` — Gazelle config + resolve directives
- `<consumer-pkg>/BUILD` — java_library + java_test_suite with cross-repo deps
- `.github/workflows/composite-build.yml` — CI with happy-path + breaking-change jobs
- `rules_jvm_external++maven+maven_install.json` — Pinned maven deps
