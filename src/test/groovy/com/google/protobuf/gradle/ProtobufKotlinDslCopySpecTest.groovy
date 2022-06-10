package com.google.protobuf.gradle

import com.google.protobuf.gradle.version.GradleVersion
import groovy.transform.CompileDynamic
import org.gradle.testkit.runner.BuildResult
import org.gradle.testkit.runner.GradleRunner
import org.gradle.testkit.runner.TaskOutcome
import spock.lang.Specification
import spock.lang.Unroll

/**
 * Unit test confirming copy spec is explicitly defined for gradle7+ compliance.
 */
@CompileDynamic
class ProtobufKotlinDslCopySpecTest extends Specification {
  private static final List<String> GRADLE_VERSIONS = [GradleVersion.VERSION_5_6, GradleVersion.VERSION_6_0, GradleVersion.VERSION_6_7_1, GradleVersion.VERSION_7_0]

  @Unroll
  void "testProjectKotlinDslCopySpec should declare explicit copy spec [gradle #gradleVersion]"() {
    given: "project from testProjectKotlinDslCopySpec"
    File projectDir = ProtobufPluginTestHelper.projectBuilder('testProjectKotlinDslCopySpec')
            .copyDirs('testProjectKotlinDslCopySpec')
            .build()

    when: "build is invoked"
    BuildResult result = GradleRunner.create()
            .withProjectDir(projectDir)
            .withArguments('test', 'build', '--stacktrace')
            .withPluginClasspath()
            .withGradleVersion(gradleVersion)
            .forwardStdOutput(new OutputStreamWriter(System.out))
            .forwardStdError(new OutputStreamWriter(System.err))
            .build()

    then: "it succeed"

    result.task(":test").outcome == TaskOutcome.SUCCESS

    verifyProjectDir(projectDir)

    where:
    gradleVersion << GRADLE_VERSIONS
  }

  private static void verifyProjectDir(File projectDir) {
    File generatedSrcDir = new File(projectDir.path, "build/generated/source/proto/main/java")
    List<File> fileList = []
    generatedSrcDir.eachFileRecurse { file ->
      if (file.path.endsWith('.java')) {
        fileList.add (file)
      }
    }
    assert fileList.size > 0
  }

}
