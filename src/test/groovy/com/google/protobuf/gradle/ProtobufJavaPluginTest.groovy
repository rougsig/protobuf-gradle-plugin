package com.google.protobuf.gradle

import groovy.transform.CompileDynamic
import org.gradle.api.Project
import org.gradle.testfixtures.ProjectBuilder
import org.gradle.testkit.runner.BuildResult
import org.gradle.testkit.runner.GradleRunner
import org.gradle.testkit.runner.TaskOutcome
import spock.lang.Specification
import spock.lang.Unroll

/**
 * Unit tests for normal java and kotlin functionality.
 */
@CompileDynamic
class ProtobufJavaPluginTest extends Specification {
  // Current supported version is Gradle 5+.
  private static final List<String> GRADLE_VERSIONS = ["7.6"]
  private static final List<String> KOTLIN_VERSIONS = ["1.7.21"]

  void "testApplying java and com.google.protobuf adds corresponding task to project"() {
    given: "a basic project with java and com.google.protobuf"
    Project project = setupBasicProject()

    when: "project evaluated"
    project.evaluate()

    then: "generate tasks added"
    assert project.tasks.generateProto instanceof GenerateProtoTask
    assert project.tasks.generateTestProto instanceof GenerateProtoTask

    assert project.tasks.extractIncludeProto instanceof ProtobufExtract
    assert project.tasks.extractIncludeTestProto instanceof ProtobufExtract
    assert project.tasks.extractProto instanceof ProtobufExtract
    assert project.tasks.extractTestProto instanceof ProtobufExtract
  }

  void "test generate proto task sources should include only *.proto files"() {
    given: "a project with readme file in proto source directory"
    Project project = setupBasicProject()
    project.file("src/main/proto").mkdirs()
    project.file("src/main/proto/messages.proto") << "syntax = \"proto3\";"
    project.file("src/main/proto/README.md") << "Hello World"

    when: "project evaluated"
    project.evaluate()

    then: "it contains only *.proto files"
    assert Objects.equals(
      project.tasks.generateProto.sourceDirs.asFileTree.files,
      [project.file("src/main/proto/messages.proto")] as Set<File>
    )
  }

  void "testCustom sourceSet should get its own GenerateProtoTask"() {
    given: "a basic project with java and com.google.protobuf"
    Project project = setupBasicProject()

    when: "adding custom sourceSet main2"
    project.sourceSets.create('main2')

    and: "project evaluated"
    project.evaluate()

    then: "tasks for main2 added"
    assert project.tasks.generateMain2Proto instanceof GenerateProtoTask

    assert project.tasks.extractIncludeMain2Proto instanceof ProtobufExtract
    assert project.tasks.extractMain2Proto instanceof ProtobufExtract
  }

  @Unroll
  void "testProject should be successfully executed (java-only project) [gradle #gradleVersion]"() {
    given: "project from testProject"
    File projectDir = ProtobufPluginTestHelper.projectBuilder('testProject')
        .copyDirs('testProjectBase', 'testProject')
        .build()

    when: "build is invoked"
    BuildResult result = ProtobufPluginTestHelper.getGradleRunner(
      projectDir,
      gradleVersion,
      "build"
    ).build()

    then: "it succeed"
    result.task(":build").outcome == TaskOutcome.SUCCESS
    ProtobufPluginTestHelper.verifyProjectDir(projectDir)

    where:
    gradleVersion << GRADLE_VERSIONS
  }

  @Unroll
  void "test generateTestProto should not execute :compileJava task (java-only project) [gradle #gradleVersion]"() {
    given: "project from testProject"
    File projectDir = ProtobufPluginTestHelper.projectBuilder('testProject')
      .copyDirs('testProjectBase', 'testProject')
      .build()

    when: "build is invoked"
    BuildResult result = ProtobufPluginTestHelper.getGradleRunner(
      projectDir,
      gradleVersion,
      "generateTestProto"
    ).build()

    then: "it succeed"
    result.task(":generateTestProto").outcome == TaskOutcome.SUCCESS
    assert !result.output.contains("Task :classes")
    assert !result.output.contains("Task :compileJava")

    where:
    gradleVersion << GRADLE_VERSIONS
  }

  @Unroll
  void "testProject should be successfully executed (configuration cache) [gradle #gradleVersion]"() {
    given: "project from testProject"
    File projectDir = ProtobufPluginTestHelper.projectBuilder('testProject')
      .copyDirs('testProjectBase', 'testProject')
      .build()
    // Limit max number of problems to catch regressions
    new File(projectDir, "gradle.properties").write('org.gradle.unsafe.configuration-cache.max-problems=42')

    and:
    GradleRunner runner = ProtobufPluginTestHelper.getGradleRunner(
      projectDir,
      gradleVersion,
      "build",
      "--configuration-cache"
    )

    when: "build is invoked"
    BuildResult result = runner.build()

    then: "it caches the task graph"
    result.output.contains("Calculating task graph")

    and: "it succeeds"
    result.task(":build").outcome == TaskOutcome.SUCCESS
    ProtobufPluginTestHelper.verifyProjectDir(projectDir)

    when: "build is invoked again"
    result = runner.build()

    then: "it reuses the task graph"
    result.output.contains("Reusing configuration cache")

    and: "it is up to date"
    result.task(":build").outcome == TaskOutcome.UP_TO_DATE
    ProtobufPluginTestHelper.verifyProjectDir(projectDir)

    where:
    gradleVersion << GRADLE_VERSIONS.takeRight(1)
  }

  @Unroll
  void "testProjectBuildTimeProto should be successfully executed [gradle #gradleVersion]"() {
    given: "project from testProjectGeneratedProto"
    File projectDir = ProtobufPluginTestHelper.projectBuilder('testProjectBuildTimeProto')
            .copyDirs('testProjectBuildTimeProto')
            .build()

    when: "build is invoked"
    BuildResult result = ProtobufPluginTestHelper.getGradleRunner(
      projectDir,
      gradleVersion,
      "build"
    ).build()

    then: "it succeed"
    result.task(":build").outcome == TaskOutcome.SUCCESS

    where:
    gradleVersion << GRADLE_VERSIONS
  }

  @Unroll
  void "testProjectKotlin (kotlin-only project) [gradle #gradleVersion, kotlin #kotlinVersion]"() {
    given: "project from testProjectKotlin overlaid on testProject"
    File projectDir = ProtobufPluginTestHelper.projectBuilder('testProjectKotlin')
        .copyDirs('testProjectBase', 'testProjectKotlin')
        .withKotlin(kotlinVersion)
        .build()

    when: "build is invoked"
    BuildResult result = ProtobufPluginTestHelper.getGradleRunner(
      projectDir,
      gradleVersion,
      "build"
    ).build()

    then: "it succeed"
    result.task(":build").outcome == TaskOutcome.SUCCESS
    ProtobufPluginTestHelper.verifyProjectDir(projectDir)

    where:
    gradleVersion << GRADLE_VERSIONS
    kotlinVersion << KOTLIN_VERSIONS
  }

  @Unroll
  void "testProjectJavaAndKotlin (java+kotlin project) [gradle #gradleVersion, kotlin #kotlinVersion]"() {
    given: "project from testProjecJavaAndKotlin overlaid on testProjectKotlin, testProject"
    File projectDir = ProtobufPluginTestHelper.projectBuilder('testProjectJavaAndKotlin')
        .copyDirs('testProjectBase', 'testProject', 'testProjectKotlin', 'testProjectJavaAndKotlin')
        .withKotlin(kotlinVersion)
        .build()

    when: "build is invoked"
    BuildResult result = ProtobufPluginTestHelper.getGradleRunner(
      projectDir,
      gradleVersion,
      "build"
    ).build()

    then: "it succeed"
    result.task(":build").outcome == TaskOutcome.SUCCESS
    ProtobufPluginTestHelper.verifyProjectDir(projectDir)

    where:
    gradleVersion << GRADLE_VERSIONS
    kotlinVersion << KOTLIN_VERSIONS
  }

  @Unroll
  void "testProjectLite should be successfully executed [gradle #gradleVersion]"() {
    given: "project from testProjectLite"
    File projectDir = ProtobufPluginTestHelper.projectBuilder('testProjectLite')
        .copyDirs('testProjectBase', 'testProjectLite')
        .build()

    when: "build is invoked"
    BuildResult result = ProtobufPluginTestHelper.getGradleRunner(
      projectDir,
      gradleVersion,
      "build"
    ).build()

    then: "it succeed"
    result.task(":build").outcome == TaskOutcome.SUCCESS

    where:
    gradleVersion << GRADLE_VERSIONS
  }

  @Unroll
  void "testProjectDependent should be successfully executed [gradle #gradleVersion]"() {
    given: "project from testProject & testProjectDependent"
    File testProjectStaging = ProtobufPluginTestHelper.projectBuilder('testProject')
        .copyDirs('testProjectBase', 'testProject')
        .build()
    File testProjectDependentStaging = ProtobufPluginTestHelper.projectBuilder('testProjectDependent')
        .copyDirs('testProjectDependent')
        .build()

    File mainProjectDir = ProtobufPluginTestHelper.projectBuilder('testProjectDependentMain')
        .copySubProjects(testProjectStaging, testProjectDependentStaging)
        .build()

    when: "build is invoked"
    BuildResult result = ProtobufPluginTestHelper.getGradleRunner(
      mainProjectDir,
      gradleVersion,
      "testProjectDependent:build"
    ).build()

    then: "it succeed"
    result.task(":testProjectDependent:build").outcome == TaskOutcome.SUCCESS

    where:
    gradleVersion << GRADLE_VERSIONS
  }

  @Unroll
  void "testProjectJavaLibrary should be successfully executed (java-only as a library) [gradle #gradleVersion]"() {
    given: "project from testProjectJavaLibrary"
    File projectDir = ProtobufPluginTestHelper.projectBuilder('testProjectJavaLibrary')
            .copyDirs('testProjectBase', 'testProjectJavaLibrary')
            .build()

    when: "build is invoked"
    BuildResult result = ProtobufPluginTestHelper.getGradleRunner(
      projectDir,
      gradleVersion,
      "build"
    ).build()

    then: "it succeed"
    result.task(":build").outcome == TaskOutcome.SUCCESS
    ProtobufPluginTestHelper.verifyProjectDir(projectDir)

    where:
    gradleVersion << GRADLE_VERSIONS
  }

  @Unroll
  void "testProjectDependentApp should be successfully executed [gradle #gradleVersion]"() {
    given: "project from testProject & testProjectDependent"
    File testProjectStaging = ProtobufPluginTestHelper.projectBuilder('testProjectJavaLibrary')
            .copyDirs('testProjectBase', 'testProjectJavaLibrary')
            .build()
    File testProjectDependentStaging = ProtobufPluginTestHelper.projectBuilder('testProjectDependentApp')
            .copyDirs('testProjectDependentApp')
            .build()

    File mainProjectDir = ProtobufPluginTestHelper.projectBuilder('testProjectDependentAppMain')
            .copySubProjects(testProjectStaging, testProjectDependentStaging)
            .build()

    when: "build is invoked"
    BuildResult result = ProtobufPluginTestHelper.getGradleRunner(
      mainProjectDir,
      gradleVersion,
      "testProjectDependentApp:build"
    ).build()

    then: "it succeed"
    result.task(":testProjectDependentApp:build").outcome == TaskOutcome.SUCCESS

    where:
    gradleVersion << GRADLE_VERSIONS
  }

  @Unroll
  void "testProjectCustomProtoDir should be successfully executed [gradle #gradleVersion]"() {
    given: "project from testProjectCustomProtoDir"
    File projectDir = ProtobufPluginTestHelper.projectBuilder('testProjectCustomProtoDir')
        .copyDirs('testProjectCustomProtoDir')
        .build()

    when: "build is invoked"
    BuildResult result = ProtobufPluginTestHelper.getGradleRunner(
      projectDir,
      gradleVersion,
      "build"
    ).build()

    then: "it succeed"
    result.task(":build").outcome == TaskOutcome.SUCCESS

    where:
    gradleVersion << GRADLE_VERSIONS
  }

  @Unroll
  void "test proto generation is not up-to-date on dependency changes [gradle #gradleVersion]"() {
    given: "project from testProject"
    File projectDir = ProtobufPluginTestHelper.projectBuilder('testProject')
            .copyDirs('testProjectBase', 'testProject')
            .build()

    when: "build is invoked"
    BuildResult result = ProtobufPluginTestHelper.getGradleRunner(
      projectDir,
      gradleVersion,
      "build"
    ).build()

    then: "it succeeds"
    result.task(":build").outcome == TaskOutcome.SUCCESS

    when: "protoc artifact is changed and build runs again"
    new File(projectDir, "build.gradle")
            .append("""
              protobuf {
                protoc {
                  artifact = 'com.google.protobuf:protoc:3.0.2'
                }
              }""")
    result = ProtobufPluginTestHelper.getGradleRunner(
      projectDir,
      gradleVersion,
      "build"
    ).build()

    then: "generateProto is not UP_TO_DATE"
    result.task(":generateProto").outcome == TaskOutcome.SUCCESS

    when: "plugin artifact is changed and build runs again"
    new File(projectDir, "build.gradle")
            .append("""
              protobuf {
                plugins {
                  grpc {
                    artifact = 'io.grpc:protoc-gen-grpc-java:1.0.3'
                  }
                }
              }""")
    result = ProtobufPluginTestHelper.getGradleRunner(
      projectDir,
      gradleVersion,
      "build"
    ).build()

    then: "generateProto is not UP_TO_DATE"
    result.task(":generateGrpcProto").outcome == TaskOutcome.SUCCESS

    where:
    gradleVersion << GRADLE_VERSIONS
  }

  @Unroll
  void "test proto generation is not up-to-date on path changes [gradle #gradleVersion]"() {
    given: "project from testProject"
    File projectDir = ProtobufPluginTestHelper.projectBuilder('testProject')
            .copyDirs('testProjectBase', 'testProject')
            .build()

    when: "protoc path is set and build is invoked"
    File buildGradleFile = new File(projectDir, "build.gradle")
    buildGradleFile.append("""
        configurations {
          protoc
        }

        dependencies {
          protoc "com.google.protobuf:protoc:3.0.0:\$project.osdetector.classifier@exe"
        }

        protobuf {
          protoc {
            path = "\$configurations.protoc.singleFile"
          }
        }""")
    BuildResult result = ProtobufPluginTestHelper.getGradleRunner(
      projectDir,
      gradleVersion,
      "build"
    ).build()

    then: "it succeeds"
    result.task(":generateProto").outcome == TaskOutcome.SUCCESS

    when: "protoc path is changed and build runs again"
    buildGradleFile.text = buildGradleFile.text.replace("com.google.protobuf:protoc:3.0.0",
        "com.google.protobuf:protoc:3.0.2")
    result = ProtobufPluginTestHelper.getGradleRunner(
      projectDir,
      gradleVersion,
      "build"
    ).build()

    then: "generateProto is not UP_TO_DATE"
    result.task(":generateProto").outcome == TaskOutcome.SUCCESS

    where:
    gradleVersion << GRADLE_VERSIONS
  }

  @Unroll
  void "test proto extraction is up-to-date for testProject when changing java sources [gradle #gradleVersion]"() {
    given: "project from testProject"
    File projectDir = ProtobufPluginTestHelper.projectBuilder('testProject')
            .copyDirs('testProjectBase', 'testProject')
            .build()

    when: "build is invoked"
    BuildResult result = ProtobufPluginTestHelper.getGradleRunner(
      projectDir,
      gradleVersion,
      "build"
    ).build()

    then: "it succeed"
    result.task(":build").outcome == TaskOutcome.SUCCESS

    when: "Java class is added and build runs again"
    new File(projectDir, "src/main/java/Bar.java").write("public class Bar {}")
    result = ProtobufPluginTestHelper.getGradleRunner(
      projectDir,
      gradleVersion,
      "build"
    ).build()

    then: "extract include test protos is up to date because it ignores classpath changes"
    result.task(":extractIncludeTestProto").outcome == TaskOutcome.UP_TO_DATE

    when: "proto file is added"
    new File(projectDir, "empty_proto.proto").write("syntax = \"proto3\";")
    new File(projectDir, "build.gradle")
            .append("\n dependencies { implementation files ('empty_proto.proto') } ")
    result = ProtobufPluginTestHelper.getGradleRunner(
      projectDir,
      gradleVersion,
      "build"
    ).build()

    then: "extract include protos is not up to date"
    result.task(":extractIncludeProto").outcome == TaskOutcome.SUCCESS
    result.task(":extractIncludeTestProto").outcome == TaskOutcome.SUCCESS

    where:
    gradleVersion << GRADLE_VERSIONS
  }

  void "test generateCmds should split commands when limit exceeded"() {
    given: "a cmd length limit and two proto files"

    List<String> baseCmd = ["protoc"]
    List<File> protoFiles = [ new File("short.proto"), new File("long_proto_name.proto") ]
    int cmdLengthLimit = 32

    when: "the commands are generated"

    List<List<String>> cmds = GenerateProtoTask.generateCmds(baseCmd, protoFiles, cmdLengthLimit)

    then: "it splits appropriately"
    cmds.size() == 2 && cmds[0] == ["protoc", "short.proto"] && cmds[1] == ["protoc", "long_proto_name.proto"]
  }

  void "test generateCmds should not split commands when under limit"() {
    given: "a cmd length limit and two proto files"

    List<String> baseCmd = ["protoc"]
    List<File> protoFiles = [ new File("short.proto"), new File("long_proto_name.proto") ]
    int cmdLengthLimit = 64

    when: "the commands are generated"

    List<List<String>> cmds = GenerateProtoTask.generateCmds(baseCmd, protoFiles, cmdLengthLimit)

    then: "it splits appropriately"
    cmds.size() == 1 && cmds[0] == ["protoc", "short.proto", "long_proto_name.proto"]
  }

  void "test generateCmds should not return commands when no protos are given"() {
    given: "a cmd length limit and no proto files"

    List<String> baseCmd = ["protoc"]
    List<File> protoFiles = []
    int cmdLengthLimit = 32

    when: "the commands are generated"

    List<List<String>> cmds = GenerateProtoTask.generateCmds(baseCmd, protoFiles, cmdLengthLimit)

    then: "it returns no commands"
    cmds.isEmpty()
  }

  void "test getCmdLengthLimit returns correct limit for Windows"() {
    given: "Windows OS"

    String os = "windows"

    when: "the command length limit is queried"

    int limit = GenerateProtoTask.getCmdLengthLimit(os)

    then: "it returns the XP limit"
    limit == GenerateProtoTask.WINDOWS_CMD_LENGTH_LIMIT
  }

  void "test getCmdLengthLimit returns correct limit for non-Windows OS"() {
    given: "MacOS X at major version 10"

    String os = "Mac OS X"

    when: "the command length limit is queried"

    int limit = GenerateProtoTask.getCmdLengthLimit(os)

    then: "it returns default limit"
    limit == GenerateProtoTask.DEFAULT_CMD_LENGTH_LIMIT
  }

  private Project setupBasicProject() {
    Project project = ProjectBuilder.builder().build()
    project.apply plugin:'java'
    project.apply plugin:'com.google.protobuf'
    return project
  }
}
