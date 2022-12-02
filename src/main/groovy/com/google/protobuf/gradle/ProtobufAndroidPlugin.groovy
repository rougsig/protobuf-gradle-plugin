/*
 * Original work copyright (c) 2015, Alex Antonov. All rights reserved.
 * Modified work copyright (c) 2015, Google Inc. All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice, this
 * list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 * this list of conditions and the following disclaimer in the documentation
 * and/or other materials provided with the distribution.
 *
 * 3. Neither the name of the copyright holder nor the names of its contributors
 * may be used to endorse or promote products derived from this software without
 * specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE
 * FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 * DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package com.google.protobuf.gradle

import com.android.build.gradle.BaseExtension
import com.android.build.gradle.api.BaseVariant
import com.android.builder.model.ProductFlavor
import com.android.builder.model.SourceProvider
import com.google.gradle.osdetector.OsDetectorPlugin
import com.google.protobuf.gradle.internal.AndroidSourceSetFacade
import com.google.protobuf.gradle.internal.DefaultProtoSourceSet
import com.google.protobuf.gradle.internal.ProjectExt
import com.google.protobuf.gradle.tasks.ProtoSourceSet
import groovy.transform.CompileStatic
import org.gradle.api.Action
import org.gradle.api.GradleException
import org.gradle.api.NamedDomainObjectContainer
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.artifacts.ArtifactView
import org.gradle.api.artifacts.Configuration
import org.gradle.api.artifacts.type.ArtifactTypeDefinition
import org.gradle.api.attributes.LibraryElements
import org.gradle.api.attributes.Usage
import org.gradle.api.file.CopySpec
import org.gradle.api.file.FileCollection
import org.gradle.api.internal.artifacts.ArtifactAttributes
import org.gradle.api.plugins.AppliedPlugin
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.SourceSet
import org.gradle.api.tasks.SourceSetContainer
import org.gradle.api.tasks.TaskProvider
import org.gradle.language.jvm.tasks.ProcessResources

/**
 * The main class for the protobuf plugin.
 */
@CompileStatic
class ProtobufAndroidPlugin implements Plugin<Project> {
  // any one of these plugins should be sufficient to proceed with applying this plugin
  private static final List<String> PREREQ_PLUGIN_OPTIONS = [
    'java',
    'java-library',
    'com.android.application',
    'com.android.feature',
    'com.android.library',
    'android',
    'android-library',
  ]

  private Project project
  private ProtobufExtension protobufExtension
  private boolean wasApplied = false

  void apply(final Project project) {
    if (GradleVersion.current() < GradleVersion.version("5.6")) {
      throw new GradleException(
        "Gradle version is ${project.gradle.gradleVersion}. Minimum supported version is 5.6")
    }

    this.protobufExtension = project.extensions.create("protobuf", ProtobufExtension, project)

    this.project = project

    // Provides the osdetector extension
    project.apply([plugin: com.google.gradle.osdetector.OsDetectorPlugin])

    // At least one of the prerequisite plugins must by applied before this plugin can be applied, so
    // we will use the PluginManager.withPlugin() callback mechanism to delay applying this plugin until
    // after that has been achieved. If project evaluation completes before one of the prerequisite plugins
    // has been applied then we will assume that none of prerequisite plugins were specified and we will
    // throw an Exception to alert the user of this configuration issue.
    Action<? super AppliedPlugin> applyWithPrerequisitePlugin = { AppliedPlugin prerequisitePlugin ->
      if (wasApplied) {
        project.logger.info('The com.google.protobuf plugin was already applied to the project: ' + project.path
          + ' and will not be applied again after plugin: ' + prerequisitePlugin.id)
      } else {
        wasApplied = true

        doApply()
      }
    }

    PREREQ_PLUGIN_OPTIONS.each { pluginName ->
      project.pluginManager.withPlugin(pluginName, applyWithPrerequisitePlugin)
    }

    project.afterEvaluate {
      if (!wasApplied) {
        throw new GradleException('The com.google.protobuf plugin could not be applied during project evaluation.'
          + ' The Java plugin or one of the Android plugins must be applied to the project first.')
      }
    }
  }

  private void doApply(Collection<Closure> postConfigure) {
    BaseExtension androidExtension = project.extensions.getByType(BaseExtension)

    androidExtension.sourceSets.configureEach { Object sourceSet ->
      AndroidSourceSetFacade sourceSetFacade = new AndroidSourceSetFacade(sourceSet)
      ProtoSourceSet protoSourceSet = protobufExtension.sourceSets.create(sourceSetFacade.name)
      configureSourceSet(sourceSetFacade, protoSourceSet)

      // Sets up a task to extract protos from protobuf dependencies.
      // They are treated as sources and will be compiled.
      Configuration protobufConf = createProtobufConf(protoSourceSet)
      TaskProvider<ProtobufExtract> extractProtosTask = registerExtractProtosTask(
        protoSourceSet.getExtractProtoTaskName(),
        protoSourceSet.name,
        project.providers.provider { protobufConf as FileCollection },
        project.file("${project.buildDir}/extracted-protos/${protoSourceSet.name}")
      )
      protoSourceSet.proto.srcDir(extractProtosTask)
    }

    NamedDomainObjectContainer<ProtoSourceSet> mergedSourceSets =
      project.objects.domainObjectContainer(ProtoSourceSet) { String name ->
        new DefaultProtoSourceSet(name, project.objects) as ProtoSourceSet
      }
    ProjectExt.allVariants(project) { BaseVariant variant ->
      ProtoSourceSet mergedProtoSourceSet = mergedSourceSets.create(variant.name)
      variant.sourceSets.each { SourceProvider sourceProvider ->
        mergedProtoSourceSet.extendsFrom(protobufExtension.sourceSets.getByName(sourceProvider.name))
      }

      TaskProvider<ProtobufExtract> extractIncludeProtosTask = registerExtractProtosTask(
        mergedProtoSourceSet.getExtractIncludeProtoTaskName(),
        mergedProtoSourceSet.name,
        project.providers.provider {
          Configuration conf = createCompileProtoPathConf(mergedProtoSourceSet)
          configureProtoPathConfExtendsFrom(project, protobufExtension, conf, variant.sourceSets)
          configureCompileProtoPathConfAttrs(conf, variant)
          return getIncomingJarFromConf(conf)
        },
        project.file("${project.buildDir}/extracted-include-protos/${mergedProtoSourceSet.name}")
      )
      mergedProtoSourceSet.includeProtoDirs.srcDir(extractIncludeProtosTask)

      TaskProvider<GenerateProtoTask> generateProtoTask = registerGenerateProtoTask(mergedProtoSourceSet)
      mergedProtoSourceSet.output
        .srcDir(generateProtoTask.map { GenerateProtoTask task -> task.outputSourceDirectories })

      BaseVariant testedVariant = getTestVariant(variant)
      if (testedVariant != null) {
        postConfigure.add {
          mergedProtoSourceSet.includesFrom(protobufExtension.sourceSets.getByName("main"))
          mergedProtoSourceSet.includesFrom(mergedSourceSets.getByName(testedVariant.name))
        }
      }

      generateProtoTask.configure({ GenerateProtoTask task ->
        task.setVariant(variant, Utils.isTest(variant.name))
        task.setBuildType(variant.buildType.name)
        task.setFlavors(variant.productFlavors.collect { ProductFlavor flavor -> flavor.name })
        task.doneInitializing()
      } as Action<GenerateProtoTask>)

      // Include source proto files in the compiled archive, so that proto files from
      // dependent projects can import them.
      addProtoSourcesToAar(variant, mergedProtoSourceSet)

      postConfigure.add {
        variant.registerJavaGeneratingTask(generateProtoTask.get(), generateProtoTask.get().outputSourceDirectories)
        configureAndroidKotlinCompileTasks(project, variant, mergedProtoSourceSet)
      }
    }
  }

  /**
   * Creates a 'protobuf' configuration for the given source set. The build author can
   * configure dependencies for it. The extract-protos task of each source set will
   * extract protobuf files from dependencies in this configuration.
   */
  private Configuration createProtobufConf(ProtoSourceSet protoSourceSet) {
    String confName = protoSourceSet.getProtobufConfName()
    Configuration conf = project.configurations.create(confName)

    conf.visible = false
    conf.transitive = true
    conf.canBeConsumed = false
    conf.canBeResolved = true

    return conf
  }

  private TaskProvider<ProtobufExtract> registerExtractProtosTask(
    String taskName,
    String sourceName,
    Provider<FileCollection> extractFrom,
    File outputDir
  ) {
    return project.tasks.register(taskName, ProtobufExtract) { ProtobufExtract task ->
      FileCollection conf = extractFrom.get()
      task.description = "Extracts proto files/dependencies specified by '$sourceName' source"
      task.destDir.set(outputDir)
      task.inputFiles.from(conf)
    }
  }

  private FileCollection getIncomingJarFromConf(Configuration conf) {
    return conf.incoming.artifactView { ArtifactView.ViewConfiguration viewConf ->
      viewConf.attributes.attribute(
        ArtifactAttributes.ARTIFACT_FORMAT,
        ArtifactTypeDefinition.JAR_TYPE
      )
    }.files
  }

  // Creates an internal 'compileProtoPath' configuration for the given source set that extends
  // compilation configurations as a bucket of dependencies with resources attribute.
  // The extract-include-protos task of each source set will extract protobuf files from
  // resolved dependencies in this configuration.
  //
  // <p> For Java projects only.
  // <p> This works around 'java-library' plugin not exposing resources to consumers for compilation.
  private Configuration createCompileProtoPathConf(ProtoSourceSet protoSourceSet) {
    String confName = protoSourceSet.getCompileProtoPathConfName()
    Configuration conf = project.configurations.create(confName)

    conf.visible = false
    conf.transitive = true
    conf.canBeConsumed = false
    conf.canBeResolved = true

    return conf
  }

  /**
   * Adds the proto extension to the SourceSet, e.g., it creates
   * sourceSets.main.proto and sourceSets.test.proto.
   */
  private void configureSourceSet(AndroidSourceSetFacade sourceSetFacade, ProtoSourceSet protoSourceSet) {
    sourceSetFacade.extensions.add("proto", protoSourceSet.proto)
    protoSourceSet.proto.srcDir("src/${protoSourceSet.name}/proto")
    protoSourceSet.proto.include("**/*.proto")
  }

  static BaseVariant getTestVariant(BaseVariant variant) {
    return variant instanceof TestVariant || variant instanceof UnitTestVariant ? variant.testedVariant : null
  }

  static void configureCompileProtoPathConfAttrs(Configuration conf, BaseVariant variant) {
    AttributeContainer confAttrs = conf.attributes
    variant.compileConfiguration.attributes.keySet().each { Attribute<Object> attr ->
      Object attrValue = variant.compileConfiguration.attributes.getAttribute(attr)
      confAttrs.attribute(attr, attrValue)
    }
  }

  static void addProtoSourcesToAar(BaseVariant variant, ProtoSourceSet protoSourceSet) {
    variant.getProcessJavaResourcesProvider().configure { ProcessResources task ->
      task.from(protoSourceSet.proto) { CopySpec cs ->
        cs.include('**/*.proto')
      }
    }
  }

  static void configureAndroidKotlinCompileTasks(Project project, BaseVariant variant, ProtoSourceSet protoSourceSet) {
    project.plugins.withId("org.jetbrains.kotlin.android") {
      project.afterEvaluate {
        String compileKotlinTaskName = Utils.getKotlinAndroidCompileTaskName(project, variant.name)
        project.tasks.named(compileKotlinTaskName, SourceTask) { SourceTask task ->
          task.source(protoSourceSet.output)
        }
      }
    }
  }

  static void configureProtoPathConfExtendsFrom(
    Project project,
    ProtobufExtension protobufExtension,
    Configuration conf,
    Collection<SourceProvider> sourceSets
  ) {
    sourceSets.each { SourceProvider sourceProvider ->
      ProtoSourceSet protoSourceSet = protobufExtension.sourceSets.getByName(sourceProvider.name)

      String compileOnlyConfName = protoSourceSet.getCompileOnlyConfName()
      Configuration compileOnlyConf = project.configurations.getByName(compileOnlyConfName)
      String implementationConfName = protoSourceSet.getImplementationConfName()
      Configuration implementationConf = project.configurations.getByName(implementationConfName)
      conf.extendsFrom(compileOnlyConf, implementationConf)
    }
  }
}
