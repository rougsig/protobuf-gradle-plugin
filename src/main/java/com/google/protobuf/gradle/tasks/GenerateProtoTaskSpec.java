package com.google.protobuf.gradle.tasks;

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

    void descriptorSet(final Action<DescriptorSetSpec> action);

    @Nested
    NamedDomainObjectContainer<PluginSpec> getPlugins();

    void plugins(final Action<NamedDomainObjectContainer<PluginSpec>> action);

    @Nested
    NamedDomainObjectContainer<PluginSpec> getBuiltins();

    void builtins(final Action<NamedDomainObjectContainer<PluginSpec>> action);
}
