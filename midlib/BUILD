load("@rules_java//java:defs.bzl", "java_library")

# gazelle:java_module_granularity module

java_library(
    name = "library",
    srcs = ["src/main/java/com/example/midlib/MidLibService.java"],
    visibility = ["//visibility:public"],
)

java_library(
    name = "midlib",
    srcs = ["src/main/java/com/example/midlib/MidLibService.java"],
    visibility = ["//:__subpackages__"],
)
