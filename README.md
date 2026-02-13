# ecmonorepo

A standalone Bazel hello world application that demonstrates cross-repository dependencies using Bzlmod.

## Prerequisites

- [Bazel](https://bazel.build/) 9.0.0 (managed via `.bazelversion`)
- Java 21
- The [bazel-monorepo](https://github.com/pravasna30-dev/bazel-monorepo) repository cloned as a sibling directory (see [Repository Layout](#repository-layout))

## Repository Layout

This project expects the following directory structure:

```
parent-directory/
  ecmonorepo/          # This repository
  bazel-monorepo/     # https://github.com/pravasna30-dev/bazel-monorepo
```

Clone both repositories side by side:

```bash
git clone https://github.com/pravasna30-dev/ecmonorepo.git
git clone https://github.com/pravasna30-dev/bazel-monorepo.git
```

## Run

```bash
bazel run //:app
```

## Build

```bash
bazel build //:app
```

The compiled output will be at `bazel-bin/app.jar`.

## Cross-Repository Dependency: low-level-1

This project depends on the `low-level-1` module from `bazel-monorepo`. Below is a step-by-step guide on how this was wired up, which you can follow to add similar cross-repo Bazel dependencies.

### Step 1: Declare the dependency in MODULE.bazel

Add a `bazel_dep` for the external module, then use `local_path_override` to point Bazel to the local checkout:

```python
# MODULE.bazel

bazel_dep(name = "composite-monorepo", version = "1.0.0")
local_path_override(
    module_name = "composite-monorepo",
    path = "../bazel-monorepo",
)
```

- `composite-monorepo` is the module name declared in `bazel-monorepo/MODULE.bazel`
- `path` is a relative path from this repository's root to the other repository

### Step 2: Add the target to deps in your BUILD file

Reference the external target using `@module-name//package:target` syntax:

```python
# BUILD

load("@rules_java//java:defs.bzl", "java_binary")

java_binary(
    name = "app",
    srcs = glob(["src/main/java/com/example/app/*.java"]),
    main_class = "com.example.app.App",
    deps = [
        "@composite-monorepo//low-level-1:LowLevelOne",
    ],
)
```

- `@composite-monorepo` refers to the module declared in Step 1
- `//low-level-1:LowLevelOne` is the `java_library` target inside that module
- The target must have `visibility = ["//visibility:public"]` in the source repo

### Step 3: Import and use in your Java code

```java
import com.acme.arc.dep.test.LowOneMain;

public class App {
    public static void main(String[] args) {
        LowOneMain.say(); // prints "Low-level-1"
    }
}
```

### How it works

- `local_path_override` tells Bzlmod to resolve the module from a local directory instead of the Bazel Central Registry
- Bazel builds `low-level-1` from source — no pre-built JARs needed
- Changes to `low-level-1` in `bazel-monorepo` are picked up immediately on the next build
- The relative path `../bazel-monorepo` keeps things portable across machines (no absolute paths)

### Finding the module name and target

To find the module name of another Bazel repository, look at its `MODULE.bazel`:

```python
# bazel-monorepo/MODULE.bazel
module(
    name = "composite-monorepo",  # <-- this is the module name
    version = "1.0.0",
)
```

To find available targets, run from the other repository:

```bash
cd ../bazel-monorepo
bazel query //low-level-1:all
```

### Querying dependencies

To see the full dependency graph of this app:

```bash
bazel query 'deps(//:app)'
```
