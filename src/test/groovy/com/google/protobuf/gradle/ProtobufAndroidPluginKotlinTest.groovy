package com.google.protobuf.gradle

import com.google.protobuf.gradle.version.AgpVersion
import com.google.protobuf.gradle.version.GradleVersion
import com.google.protobuf.gradle.version.KotlinVersion

import static com.google.protobuf.gradle.ProtobufPluginTestHelper.buildAndroidProject

import groovy.transform.CompileDynamic
import org.gradle.testkit.runner.BuildResult
import org.gradle.testkit.runner.TaskOutcome
import spock.lang.Specification
import spock.lang.Unroll

@CompileDynamic
class ProtobufAndroidPluginKotlinTest extends Specification {
  private static final List<String> GRADLE_VERSION = [GradleVersion.VERSION_5_6, GradleVersion.VERSION_6_5_MILESTONE_1, GradleVersion.VERSION_7_4_2]
  private static final List<String> ANDROID_PLUGIN_VERSION = [AgpVersion.VERSION_3_5_0, AgpVersion.VERSION_4_1_APLHA10, AgpVersion.VERSION_7_2_1]
  private static final List<String> KOTLIN_VERSION = [KotlinVersion.VERSION_1_3_20]

  /**
   * This test may take a significant amount of Gradle daemon Metaspace memory in some
   * Gradle + AGP versions. Try running it separately
   */
  @Unroll
  void "testProjectAndroidKotlin [android #agpVersion, gradle #gradleVersion, kotlin #kotlinVersion]"() {
    given: "project from testProject, testProjectLite & testProjectAndroid"
    File testProjectStaging = ProtobufPluginTestHelper.projectBuilder('testProject')
            .copyDirs('testProjectBase', 'testProject')
            .build()
    File testProjectAndroidStaging = ProtobufPluginTestHelper.projectBuilder('testProjectAndroid')
            .copyDirs('testProjectAndroidBase', 'testProjectAndroidKotlin')
            .build()
    File testProjectLiteStaging = ProtobufPluginTestHelper.projectBuilder('testProjectLite')
            .copyDirs('testProjectLite')
            .build()
    File mainProjectDir = ProtobufPluginTestHelper.projectBuilder('testProjectAndroidMain')
            .copySubProjects(testProjectStaging, testProjectLiteStaging, testProjectAndroidStaging)
            .withAndroidPlugin(agpVersion)
            .withKotlin(kotlinVersion)
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
    kotlinVersion << KOTLIN_VERSION + KOTLIN_VERSION + KOTLIN_VERSION
    }
}
