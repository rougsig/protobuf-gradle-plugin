/*
 * Copyright (c) 2022, Google Inc. All rights reserved.
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
package com.google.protobuf.gradle.internal

import com.google.protobuf.gradle.tasks.ProtoSourceSet
import com.google.protobuf.gradle.tasks.ProtoVariant
import groovy.transform.CompileStatic
import org.gradle.api.NamedDomainObjectContainer
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.model.ObjectFactory

@CompileStatic
class DefaultProtoVariant implements ProtoVariant {
  private final String name
  private final ProtoSourceSet sources
  private final DirectoryProperty outputDir
  private final NamedDomainObjectContainer<Object> plugins
  private final NamedDomainObjectContainer<Object> builtins

  DefaultProtoVariant(String name, ObjectFactory objects) {
    this.name = name
    this.sources = new DefaultProtoSourceSet("${name}Sources", objects)
    this.outputDir = objects.directoryProperty()
    this.plugins = objects.domainObjectContainer(Object)
    this.builtins = objects.domainObjectContainer(Object)
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
}
