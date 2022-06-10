package com.google.protobuf.gradle

import com.google.protobuf.gradle.version.AgpVersion
import com.google.protobuf.gradle.version.GradleVersion

import static com.google.protobuf.gradle.ProtobufPluginTestHelper.buildAndroidProject
import static com.google.protobuf.gradle.ProtobufPluginTestHelper.getAndroidGradleRunner

import groovy.transform.CompileDynamic
import org.gradle.testkit.runner.BuildResult
import org.gradle.testkit.runner.GradleRunner
import org.gradle.testkit.runner.TaskOutcome
import spock.lang.Specification
import spock.lang.Unroll

/**
 * Unit tests for android related functionality.
 */
@CompileDynamic
class ProtobufAndroidPluginTest extends Specification {
  private static final List<String> GRADLE_VERSION = [GradleVersion.VERSION_5_6, GradleVersion.VERSION_6_5, GradleVersion.VERSION_6_8, GradleVersion.VERSION_7_4_2]
  private static final List<String> ANDROID_PLUGIN_VERSION = [AgpVersion.VERSION_3_5_0, AgpVersion.VERSION_4_1_0, AgpVersion.VERSION_4_2_APLHA10, AgpVersion.VERSION_7_2_1]

  @Unroll
  void "testProjectAndroid should be successfully executed [android #agpVersion, gradle #gradleVersion]"() {
    given: "project from testProject, testProjectLite & testProjectAndroid"
    File testProjectStaging = ProtobufPluginTestHelper.projectBuilder('testProject')
        .copyDirs('testProjectBase', 'testProject')
        .build()
    File testProjectAndroidStaging = ProtobufPluginTestHelper.projectBuilder('testProjectAndroid')
        .copyDirs('testProjectAndroidBase', 'testProjectAndroid')
        .build()
    File testProjectLiteStaging = ProtobufPluginTestHelper.projectBuilder('testProjectLite')
        .copyDirs('testProjectLite')
        .build()
    File mainProjectDir = ProtobufPluginTestHelper.projectBuilder('testProjectAndroidMain')
        .copySubProjects(testProjectStaging, testProjectLiteStaging, testProjectAndroidStaging)
        .withAndroidPlugin(agpVersion)
        .build()
    when: "build is invoked"
    BuildResult result = buildAndroidProject(
       mainProjectDir,
       gradleVersion,
       "testProjectAndroid:build"
    )

    then: "it succeed"
    result.task(":testProjectAndroid:build").outcome == TaskOutcome.SUCCESS

    where:
    agpVersion << ANDROID_PLUGIN_VERSION
    gradleVersion << GRADLE_VERSION
  }

  @Unroll
  void "testProjectAndroid succeeds with configuration cache [android #agpVersion, gradle #gradleVersion]"() {
    given: "project from testProject, testProjectLite & testProjectAndroid"
    File testProjectStaging = ProtobufPluginTestHelper.projectBuilder('testProject')
            .copyDirs('testProjectBase', 'testProject')
            .build()
    File testProjectAndroidStaging = ProtobufPluginTestHelper.projectBuilder('testProjectAndroid')
            .copyDirs('testProjectAndroidBase', 'testProjectAndroid')
            .build()
    File testProjectLiteStaging = ProtobufPluginTestHelper.projectBuilder('testProjectLite')
            .copyDirs('testProjectLite')
            .build()
    File mainProjectDir = ProtobufPluginTestHelper.projectBuilder('testProjectAndroidMain')
            .copySubProjects(testProjectStaging, testProjectLiteStaging, testProjectAndroidStaging)
            .withAndroidPlugin(agpVersion)
            .build()
    and:
    GradleRunner runner = getAndroidGradleRunner(
            mainProjectDir,
            gradleVersion,
            "testProjectAndroid:assembleDebug",
            "-Dorg.gradle.unsafe.configuration-cache=true"
    )
    when: "build is invoked"
    BuildResult result = runner.build()

    then: "it caches the task graph"
    result.output.contains("Calculating task graph")

    and: "it succeed"
    result.task(":testProjectAndroid:assembleDebug").outcome == TaskOutcome.SUCCESS

    when: "build is invoked again"
    result = runner.build()

    then: "it reuses the task graph"
    result.output.contains("Reusing configuration cache")

    and: "it is up to date"
    result.task(":testProjectAndroid:assembleDebug").outcome == TaskOutcome.UP_TO_DATE

    when: "clean is invoked, before a build"
    buildAndroidProject(
            mainProjectDir,
            gradleVersion,
            "testProjectAndroid:clean",
            "-Dorg.gradle.unsafe.configuration-cache=true"
    )
    result = runner.build()

    then: "it succeed"
    result.task(":testProjectAndroid:assembleDebug").outcome == TaskOutcome.SUCCESS

    where:
    agpVersion << [AgpVersion.VERSION_4_2_APLHA10]
    gradleVersion << [GradleVersion.VERSION_6_8]
  }

  @Unroll
  void "testProjectAndroidDependent [android #agpVersion, gradle #gradleVersion, kotlin #kotlinVersion]"() {
    given: "project from testProjectAndroidLibrary, testProjectAndroid"
    File testProjectStaging = ProtobufPluginTestHelper.projectBuilder('testProject')
            .copyDirs('testProjectBase', 'testProject')
            .build()
    File testProjectLibraryStaging = ProtobufPluginTestHelper.projectBuilder('testProjectAndroidLibrary')
            .copyDirs('testProjectAndroidLibrary')
            .build()
    File testProjectAndroidStaging = ProtobufPluginTestHelper.projectBuilder('testProjectAndroid')
            .copyDirs('testProjectAndroidDependentBase', 'testProjectAndroid')
            .build()
    File mainProjectDir = ProtobufPluginTestHelper.projectBuilder('testProjectAndroidDependentMain')
            .copySubProjects(testProjectStaging, testProjectLibraryStaging, testProjectAndroidStaging)
            .withAndroidPlugin(agpVersion)
            .build()
    when: "build is invoked"
    BuildResult result = buildAndroidProject(
            mainProjectDir,
            gradleVersion,
            "testProjectAndroid:build"
    )

    then: "it succeed"
    result.task(":testProjectAndroid:build").outcome == TaskOutcome.SUCCESS

    where:
    agpVersion << ANDROID_PLUGIN_VERSION
    gradleVersion << GRADLE_VERSION
  }

  @Unroll
  void "testProjectAndroid tests build without warnings [android #agpVersion, gradle #gradleVersion]"() {
    given: "project from testProject, testProjectLite & testProjectAndroid"
    File testProjectStaging = ProtobufPluginTestHelper.projectBuilder('testProject')
            .copyDirs('testProjectBase', 'testProject')
            .build()
    File testProjectAndroidStaging = ProtobufPluginTestHelper.projectBuilder('testProjectAndroid')
            .copyDirs('testProjectAndroidBase', 'testProjectAndroid')
            .build()
    File testProjectLiteStaging = ProtobufPluginTestHelper.projectBuilder('testProjectLite')
            .copyDirs('testProjectLite')
            .build()
    File mainProjectDir = ProtobufPluginTestHelper.projectBuilder('testProjectAndroidMain')
            .copySubProjects(testProjectStaging, testProjectLiteStaging, testProjectAndroidStaging)
            .withAndroidPlugin(agpVersion)
            .build()
    when: "build is invoked"
    BuildResult result = buildAndroidProject(
            mainProjectDir,
            gradleVersion,
            "testProjectAndroid:assembleAndroidTest"
    )

    then: "it succeed"
    result.task(":testProjectAndroid:assembleAndroidTest").outcome == TaskOutcome.SUCCESS

    and: "does not contain warnings about proto location"
    !result.output.contains("This makes you vulnerable to https://github.com/google/protobuf-gradle-plugin/issues/248")

    where:
    agpVersion << [AgpVersion.VERSION_4_2_APLHA10]
    gradleVersion << [GradleVersion.VERSION_6_8]
  }
}
