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

import com.android.build.gradle.api.BaseVariant
import com.android.build.gradle.api.TestVariant
import com.android.build.gradle.api.UnitTestVariant
import com.android.builder.model.SourceProvider
import com.google.protobuf.gradle.internal.DefaultProtoSourceSet
import com.google.protobuf.gradle.internal.ProjectExt
import com.google.protobuf.gradle.tasks.ProtoSourceSet
import com.google.protobuf.gradle.tasks.ProtoVariant
import groovy.transform.CompileStatic
import groovy.transform.TypeChecked
import groovy.transform.TypeCheckingMode
import org.gradle.api.Action
import org.gradle.api.GradleException
import org.gradle.api.NamedDomainObjectContainer
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.artifacts.Configuration
import org.gradle.api.artifacts.type.ArtifactTypeDefinition
import org.gradle.api.attributes.LibraryElements
import org.gradle.api.attributes.Usage
import org.gradle.api.file.CopySpec
import org.gradle.api.file.FileCollection
import org.gradle.api.file.SourceDirectorySet
import org.gradle.api.internal.artifacts.ArtifactAttributes
import org.gradle.api.plugins.AppliedPlugin
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.SourceSet
import org.gradle.language.jvm.tasks.ProcessResources
import org.gradle.util.GradleVersion
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

/**
 * The main class for the protobuf plugin.
 */
@CompileStatic
class ProtobufPlugin implements Plugin<Project> {
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
      project.apply([plugin:com.google.gradle.osdetector.OsDetectorPlugin])

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

    @TypeChecked(TypeCheckingMode.SKIP) // Don't depend on AGP
    private void doApply() {
        boolean isAndroid = Utils.isAndroidProject(project)
        // Java projects will extract included protos from a 'compileProtoPath'
        // configuration of each source set, while Android projects will
        // extract included protos from {@code variant.compileConfiguration}
        // of each variant.
        Collection<Closure> postConfigure = []
        if (isAndroid) {
          project.android.sourceSets.configureEach { sourceSet ->
            ProtoSourceSet protoSourceSet = protobufExtension.sourceSets.create(sourceSet.name)
            addSourceSetExtension(sourceSet, protoSourceSet)
            Configuration protobufConfig = createProtobufConfiguration(protoSourceSet)
            setupExtractProtosTask(protoSourceSet, protobufConfig)
          }

          NamedDomainObjectContainer<ProtoSourceSet> variantSourceSets =
            project.objects.domainObjectContainer(ProtoSourceSet) { String name ->
              new DefaultProtoSourceSet(name, project.objects)
            }
          ProjectExt.forEachVariant(this.project) { BaseVariant variant ->
            addTasksForVariant(variant, variantSourceSets, postConfigure)
          }
        } else {
          project.sourceSets.configureEach { sourceSet ->
            ProtoSourceSet protoSourceSet = protobufExtension.sourceSets.create(sourceSet.name)
            addSourceSetExtension(sourceSet, protoSourceSet)
            Configuration protobufConfig = createProtobufConfiguration(protoSourceSet)
            Configuration compileProtoPath = createCompileProtoPathConfiguration(protoSourceSet)
            addTasksForSourceSet(sourceSet, protoSourceSet, protobufConfig, compileProtoPath, postConfigure)
          }
        }
        project.afterEvaluate {
          this.protobufExtension.configureTasks()
          // Disallow user configuration outside the config closures, because the operations just
          // after the doneConfig() loop over the generated outputs and will be out-of-date if
          // plugin output is added after this point.
          this.protobufExtension.generateProtoTasks.all().configureEach { it.doneConfig() }
          postConfigure.each { it.call() }
          // protoc and codegen plugin configuration may change through the protobuf{}
          // block. Only at this point the configuration has been finalized.
          project.protobuf.tools.resolve(project)
        }
    }

    /**
     * Creates a 'protobuf' configuration for the given source set. The build author can
     * configure dependencies for it. The extract-protos task of each source set will
     * extract protobuf files from dependencies in this configuration.
     */
    private Configuration createProtobufConfiguration(ProtoSourceSet protoSourceSet) {
      String protobufConfigName = Utils.getConfigName(protoSourceSet.name, 'protobuf')
      return project.configurations.create(protobufConfigName) { Configuration it ->
        it.visible = false
        it.transitive = true
      }
    }

    /**
     * Creates an internal 'compileProtoPath' configuration for the given source set that extends
     * compilation configurations as a bucket of dependencies with resources attribute.
     * The extract-include-protos task of each source set will extract protobuf files from
     * resolved dependencies in this configuration.
     *
     * <p> For Java projects only.
     * <p> This works around 'java-library' plugin not exposing resources to consumers for compilation.
     */
    private Configuration createCompileProtoPathConfiguration(ProtoSourceSet protoSourceSet) {
      String compileProtoConfigName = Utils.getConfigName(protoSourceSet.name, 'compileProtoPath')
      Configuration compileConfig =
              project.configurations.getByName(Utils.getConfigName(protoSourceSet.name, 'compileOnly'))
      Configuration implementationConfig =
              project.configurations.getByName(Utils.getConfigName(protoSourceSet.name, 'implementation'))
      return project.configurations.create(compileProtoConfigName) { Configuration it ->
          it.visible = false
          it.transitive = true
          it.extendsFrom = [compileConfig, implementationConfig]
          it.canBeConsumed = false
          it.getAttributes()
                // Variant attributes are not inherited. Setting it too loosely can
                // result in ambiguous variant selection errors.
                // CompileProtoPath only need proto files from dependency's resources.
                // LibraryElement "resources" is compatible with "jar" (if a "resources" variant is
                // not found, the "jar" variant will be used).
                .attribute(
                        LibraryElements.LIBRARY_ELEMENTS_ATTRIBUTE,
                        project.getObjects().named(LibraryElements, LibraryElements.RESOURCES))
                // Although variants with any usage has proto files, not setting usage attribute
                // can result in ambiguous variant selection if the producer provides multiple
                // variants with different usage attribute.
                // Preserve the usage attribute from CompileOnly and Implementation.
                .attribute(
                        Usage.USAGE_ATTRIBUTE,
                        project.getObjects().named(Usage, Usage.JAVA_RUNTIME))
      }
    }

    /**
     * Adds the proto extension to the SourceSet, e.g., it creates
     * sourceSets.main.proto and sourceSets.test.proto.
     */
    @TypeChecked(TypeCheckingMode.SKIP) // Don't depend on AGP
    private SourceDirectorySet addSourceSetExtension(Object sourceSet, ProtoSourceSet protoSourceSet) {
      String name = sourceSet.name
      SourceDirectorySet sds = protoSourceSet.proto
      sourceSet.extensions.add('proto', sds)
      sds.srcDir("src/${name}/proto")
      sds.include("**/*.proto")
      return sds
    }

    /**
     * Creates Protobuf tasks for a sourceSet in a Java project.
     */
    private void addTasksForSourceSet(
        SourceSet sourceSet, ProtoVariant protoVariant, Configuration protobufConfig,
        Configuration compileProtoPath, Collection<Closure> postConfigure) {
      Provider<ProtobufExtract> extractProtosTask = setupExtractProtosTask(
        protoVariant.sources,
        protobufConfig
      )

      Provider<ProtobufExtract> extractIncludeProtosTask = setupExtractIncludeProtosTask(
        protoVariant.sources,
        compileProtoPath
      )

      // Make protos in 'test' sourceSet able to import protos from the 'main' sourceSet.
      // Pass include proto files from main to test.
      if (protoVariant.isTest) {
        protoVariant.sources.includesFrom(protobufExtension.variants.getByName(SourceSet.MAIN_SOURCE_SET_NAME).sources)
      }

      protoVariant.sourceSets = [sourceSet.name] as Set<String>
      protoVariant.generateProtoTaskSpec.builtins.maybeCreate("java")
      Provider<GenerateProtoTask> generateProtoTask = addGenerateProtoTask(protoVariant)

      sourceSet.java.srcDirs(protoVariant.sources.output)

      // Include source proto files in the compiled archive, so that proto files from
      // dependent projects can import them.
      project.tasks.named(sourceSet.getTaskName('process', 'resources'), ProcessResources).configure {
        it.from(protoVariant.sources.proto) { CopySpec cs ->
          cs.include '**/*.proto'
        }
      }

      postConfigure.add {
        project.plugins.withId("eclipse") {
          // This is required because the intellij/eclipse plugin does not allow adding source directories
          // that do not exist. The intellij/eclipse config files should be valid from the start.
          generateProtoTask.get().getOutputSourceDirectories().each { File outputDir ->
            outputDir.mkdirs()
          }
        }

        project.plugins.withId("idea") {
          boolean isTest = Utils.isTest(sourceSet.name)
          protoVariant.sources.proto.srcDirs.each { File protoDir ->
            Utils.addToIdeSources(project, isTest, protoDir, false)
          }
          Utils.addToIdeSources(project, isTest, project.files(extractProtosTask).singleFile, true)
          Utils.addToIdeSources(project, isTest, project.files(extractIncludeProtosTask).singleFile, true)
          generateProtoTask.get().getOutputSourceDirectories().each { File outputDir ->
            Utils.addToIdeSources(project, isTest, outputDir, true)
          }
        }
      }
    }

    /**
     * Creates Protobuf tasks for a variant in an Android project.
     */
    @TypeChecked(TypeCheckingMode.SKIP) // Don't depend on AGP
    private void addTasksForVariant(
      Object variant,
      ProtoVariant protoVariant,
      Collection<Closure> postConfigure
    ) {
      Boolean isTestVariant = variant instanceof TestVariant || variant instanceof UnitTestVariant
      ProtoSourceSet variantSourceSet = protoVariant.sources

      // ExtractIncludeProto task, one per variant (compilation unit).
      // Proto definitions from an AAR dependencies are in its JAR resources.
      FileCollection classPathConfig = variant.compileConfiguration.incoming.artifactView {
        attributes.attribute(
          ArtifactAttributes.ARTIFACT_FORMAT,
          ArtifactTypeDefinition.JAR_TYPE
        )
      }.files

      // Make protos in 'test' variant able to import protos from the 'main' variant.
      // Pass include proto files from main to test.
      if (variant instanceof TestVariant || variant instanceof UnitTestVariant) {
        postConfigure.add {
          variantSourceSet.includesFrom(protobufExtension.sourceSets.getByName("main"))
          variantSourceSet.includesFrom(protobufExtension.sourceSets.getByName(variant.testedVariant.name))
        }
      }

      setupExtractIncludeProtosTask(variantSourceSet, classPathConfig)

      // GenerateProto task, one per variant (compilation unit).
      variant.sourceSets.each { SourceProvider sourceProvider ->
        variantSourceSet.extendsFrom(protobufExtension.sourceSets.getByName(sourceProvider.name))
      }

      protoVariant.isTest = isTestVariant
      protoVariant.flavors = variant.productFlavors.collect { it.name } as Set<String>
      protoVariant.buildType = variant.hasProperty("buildType") ? variant.buildType.name : null
      Provider<GenerateProtoTask> generateProtoTask = addGenerateProtoTask(protoVariant)

      if (project.android.hasProperty('libraryVariants')) {
          // Include source proto files in the compiled archive, so that proto files from
          // dependent projects can import them.
          variant.getProcessJavaResourcesProvider().configure {
            it.from(variantSourceSet.proto) {
              include '**/*.proto'
            }
          }
      }
      postConfigure.add {
        // This cannot be called once task execution has started.
        variant.registerJavaGeneratingTask(generateProtoTask.get(), generateProtoTask.get().outputSourceDirectories)

        project.plugins.withId("org.jetbrains.kotlin.android") {
          project.afterEvaluate {
            String compileKotlinTaskName = Utils.getKotlinAndroidCompileTaskName(project, variant.name)
            project.tasks.named(compileKotlinTaskName, KotlinCompile) { KotlinCompile task ->
              task.dependsOn(generateProtoTask)
              task.source(generateProtoTask.get().outputSourceDirectories)
            }
          }
        }
      }
    }

    /**
     * Adds a task to run protoc and compile all proto source files for a sourceSet or variant.
     *
     * @param sourceSetOrVariantName the name of the sourceSet (Java) or
     * variant (Android) that this task will run for.
     *
     * @param sourceSets the sourceSets that contains the proto files to be
     * compiled. For Java it's the sourceSet that sourceSetOrVariantName stands
     * for; for Android it's the collection of sourceSets that the variant includes.
     */
    private Provider<GenerateProtoTask> addGenerateProtoTask(
      ProtoVariant protoVariant
    ) {
      String variantName = protoVariant.name
      String taskName = 'generate' + Utils.getSourceSetSubstringForTaskNames(variantName) + 'Proto'
      String defaultGeneratedFilesBaseDir = protobufExtension.defaultGeneratedFilesBaseDir
      Provider<String> generatedFilesBaseDirProvider = protobufExtension.generatedFilesBaseDir
      Provider<GenerateProtoTask> task = project.tasks.register(taskName, GenerateProtoTask) {
        CopyActionFacade copyActionFacade = CopyActionFacade.Loader.create(it.project, it.objectFactory)
        it.spec.set(protoVariant.generateProtoTaskSpec)
        it.description = "Compiles Proto source for '${variantName}'".toString()
        it.outputBaseDir = project.providers.provider {
          "${defaultGeneratedFilesBaseDir}/${variantName}".toString()
        }
        it.addSourceDirs(protoVariant.sources.proto)
        it.addIncludeDir(protoVariant.sources.proto.sourceDirectories)
        it.addIncludeDir(protoVariant.sources.includeProtoDirs)
        it.doLast { task ->
          String generatedFilesBaseDir = generatedFilesBaseDirProvider.get()
          if (generatedFilesBaseDir == defaultGeneratedFilesBaseDir) {
            return
          }
          // Purposefully don't wire this up to outputs, as it can be mixed with other files.
          copyActionFacade.copy { CopySpec spec ->
            spec.includeEmptyDirs = false
            spec.from(it.outputBaseDir)
            spec.into("${generatedFilesBaseDir}/${variantName}")
          }
        }
      }
      protoVariant.sources.output.from(task.map { GenerateProtoTask it -> it.outputSourceDirectories })
      return task
    }

    /**
     * Sets up a task to extract protos from protobuf dependencies. They are
     * treated as sources and will be compiled.
     *
     * <p>This task is per-sourceSet, for both Java and Android. In Android a
     * variant may have multiple sourceSets, each of these sourceSets will have
     * its own extraction task.
     */
    private Provider<ProtobufExtract> setupExtractProtosTask(
      ProtoSourceSet protoSourceSet,
      Configuration protobufConfig
    ) {
      String sourceSetName = protoSourceSet.name
      String taskName = getExtractProtosTaskName(sourceSetName)
      Provider<ProtobufExtract> task = project.tasks.register(taskName, ProtobufExtract) {
        it.description = "Extracts proto files/dependencies specified by 'protobuf' configuration"
        it.destDir.set(getExtractedProtosDir(sourceSetName) as File)
        it.inputFiles.from(protobufConfig)
      }
      protoSourceSet.proto.srcDir(task)
      return task
    }

    private String getExtractProtosTaskName(String sourceSetName) {
      return 'extract' + Utils.getSourceSetSubstringForTaskNames(sourceSetName) + 'Proto'
    }

    /**
     * Sets up a task to extract protos from compile dependencies of a sourceSet, Those are needed
     * for imports in proto files, but they won't be compiled since they have already been compiled
     * in their own projects or artifacts.
     *
     * <p>This task is per-sourceSet for both Java and per variant for Android.
     */
    private Provider<ProtobufExtract> setupExtractIncludeProtosTask(
        ProtoSourceSet protoSourceSet,
        FileCollection archives
    ) {
      String taskName = 'extractInclude' + Utils.getSourceSetSubstringForTaskNames(protoSourceSet.name) + 'Proto'
      Provider<ProtobufExtract> task = project.tasks.register(taskName, ProtobufExtract) {
        it.description = "Extracts proto files from compile dependencies for includes"
        it.destDir.set(getExtractedIncludeProtosDir(protoSourceSet.name) as File)
        it.inputFiles.from(archives)
      }
      protoSourceSet.includeProtoDirs.from(task)
      return task
    }

    private String getExtractedIncludeProtosDir(String sourceSetName) {
      return "${project.buildDir}/extracted-include-protos/${sourceSetName}"
    }

    private String getExtractedProtosDir(String sourceSetName) {
      return "${project.buildDir}/extracted-protos/${sourceSetName}"
    }
}
