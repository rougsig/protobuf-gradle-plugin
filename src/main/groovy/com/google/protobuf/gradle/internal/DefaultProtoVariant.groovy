package com.google.protobuf.gradle.internal

import com.google.protobuf.gradle.Utils
import com.google.protobuf.gradle.tasks.ProtoSourceSet
import com.google.protobuf.gradle.tasks.ProtoVariant
import groovy.transform.CompileStatic
import org.gradle.api.NamedDomainObjectContainer
import org.gradle.api.Project
import org.gradle.api.artifacts.Configuration
import org.gradle.api.file.DirectoryProperty

@CompileStatic
class DefaultProtoVariant implements ProtoVariant {
  private final String name
  private final ProtoSourceSet sources
  private final DirectoryProperty outputDir
  private final NamedDomainObjectContainer<Object> plugins
  private final NamedDomainObjectContainer<Object> builtins
  private final Configuration protobufConf
  private final Configuration compileProtoPathConf

  DefaultProtoVariant(String name, Project project) {
    this.name = name
    this.sources = new DefaultProtoSourceSet(name, project.objects)
    this.outputDir = project.objects.directoryProperty()
    this.plugins = project.objects.domainObjectContainer(Object)
    this.builtins = project.objects.domainObjectContainer(Object)

    this.protobufConf = project.configurations.create(Utils.getConfigName(name, 'protobuf'))
    this.protobufConf.visible = false
    this.protobufConf.transitive = true
    this.protobufConf.canBeConsumed = false
    this.protobufConf.canBeResolved = true

    this.compileProtoPathConf = project.configurations.create(Utils.getConfigName(name, 'compileProtoPath'))
  }

  @Override
  String getName() {
    return this.name
  }

  @Override
  ProtoSourceSet getSources() {
    return this.sources
  }

  @Override
  DirectoryProperty getOutputDir() {
    return this.outputDir
  }

  @Override
  NamedDomainObjectContainer<Object> getPlugins() {
    return this.plugins
  }

  @Override
  NamedDomainObjectContainer<Object> getBuiltins() {
    return this.builtins
  }

  @Override
  Configuration getProtobufConf() {
    return this.protobufConf
  }

  @Override
  Configuration getCompileProtoPathConf() {
    return this.compileProtoPathConf
  }
}
