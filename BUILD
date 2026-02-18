load("@gazelle//:def.bzl", "DEFAULT_LANGUAGES", "gazelle", "gazelle_binary")
load("@rules_java//java:defs.bzl", "java_binary")

# gazelle:java_extension enabled
# gazelle:java_maven_install_file rules_jvm_external++maven+maven_install.json
# gazelle:resolve java org.junit.jupiter.api @maven//:org_junit_jupiter_junit_jupiter_api
# gazelle:resolve java org.assertj.core.api @maven//:org_assertj_assertj_core
# gazelle:resolve java com.example.library @composite-monorepo//library
# gazelle:resolve java com.acme.arc.dep.test @composite-monorepo//low-level-1
# gazelle:exclude src

gazelle(
    name = "gazelle",
    gazelle = ":gazelle_bin",
)

gazelle_binary(
    name = "gazelle_bin",
    languages = DEFAULT_LANGUAGES + [
        "@contrib_rules_jvm//java/gazelle",
    ],
)

java_binary(
    name = "app",
    srcs = ["src/main/java/com/example/app/App.java"],  # keep
    main_class = "com.example.app.App",
    deps = [
        "//library",
        "//midlib:library",
        "@composite-monorepo//low-level-1",
    ],
)
