load("@contrib_rules_jvm//java:defs.bzl", "java_test_suite")
load("@rules_java//java:defs.bzl", "java_binary", "java_library")

# gazelle:java_module_granularity module

java_library(
    name = "consumer",
    srcs = ["src/main/java/com/example/consumer/Application.java"],
    visibility = ["//visibility:public"],
    deps = [
        "@composite-monorepo//library",
    ],
)

java_binary(
    name = "app",
    main_class = "com.example.consumer.Application",
    visibility = ["//visibility:public"],
    runtime_deps = [":consumer"],
)

java_test_suite(
    name = "consumer-tests",
    size = "small",
    srcs = [
        "src/test/java/com/example/consumer/ApiContractTest.java",
        "src/test/java/com/example/consumer/UserServiceIntegrationTest.java",
    ],
    runner = "junit5",
    runtime_deps = [
        "@maven//:org_junit_jupiter_junit_jupiter_engine",
        "@maven//:org_junit_platform_junit_platform_launcher",
        "@maven//:org_junit_platform_junit_platform_reporting",
    ],
    deps = [
        ":consumer",  # keep
        "@composite-monorepo//library",  # keep
        "@maven//:org_assertj_assertj_core",
        "@maven//:org_junit_jupiter_junit_jupiter_api",
    ],
)

java_binary(
    name = "Application",
    main_class = "com.example.consumer.Application",
    visibility = ["//visibility:public"],
    runtime_deps = [":consumer"],
)
