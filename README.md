# bazel-app

A standalone Bazel hello world application.

## Prerequisites

- [Bazel](https://bazel.build/) 9.0.0 (managed via `.bazelversion`)
- Java 21

## Run

```bash
bazel run //:app
```

## Build

```bash
bazel build //:app
```

The compiled output will be at `bazel-bin/app.jar`.
