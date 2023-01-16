package com.google.protobuf.gradle.tasks;

import groovy.transform.Internal;
import org.gradle.api.Action;
import org.gradle.api.NamedDomainObjectContainer;
import org.gradle.api.provider.Property;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.Nested;

public interface GenerateProtoTaskSpec {
    @Input
    Property<String> getOutputSubDir();

    @Nested
    DescriptorSetSpec getDescriptorSet();

    @Internal
    void descriptorSet(final Action<DescriptorSetSpec> action);

    /**
     * Returns the container of protoc plugins.
     */
    @Nested
    NamedDomainObjectContainer<PluginSpec> getPlugins();

    /**
     * Configures the protoc plugins in a closure, which will be maniuplating a
     * NamedDomainObjectContainer&lt;PluginOptions&gt;.
     */
    @Internal
    void plugins(final Action<NamedDomainObjectContainer<PluginSpec>> action);

    /**
     * Returns the container of protoc builtins.
     */
    @Nested
    NamedDomainObjectContainer<PluginSpec> getBuiltins();

    /**
     * Configures the protoc builtins in a closure, which will be manipulating a
     * NamedDomainObjectContainer&lt;PluginOptions&gt;.
     */
    @Internal
    void builtins(final Action<NamedDomainObjectContainer<PluginSpec>> action);
}
