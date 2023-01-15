package com.google.protobuf.gradle.internal;

import com.google.protobuf.gradle.tasks.DescriptorSetSpec;
import com.google.protobuf.gradle.tasks.GenerateProtoTaskSpec;
import com.google.protobuf.gradle.tasks.PluginSpec;
import org.gradle.api.Action;
import org.gradle.api.NamedDomainObjectContainer;
import org.gradle.api.NamedDomainObjectFactory;
import org.gradle.api.model.ObjectFactory;
import org.gradle.api.provider.Property;

public class DefaultGenerateProtoTaskSpec implements GenerateProtoTaskSpec {
    private final Property<String> outputSubDir;
    private final DescriptorSetSpec descriptorSet;
    private final NamedDomainObjectContainer<PluginSpec> plugins;
    private final NamedDomainObjectContainer<PluginSpec> builtins;

    DefaultGenerateProtoTaskSpec(ObjectFactory objects) {
        this.outputSubDir = objects.property(String.class);
        this.descriptorSet = new DefaultDescriptorSetSpec(objects);
        NamedDomainObjectFactory<PluginSpec> pluginSpecObjectFactory = (String name) -> new DefaultPluginSpec(name, objects);
        this.plugins = objects.domainObjectContainer(PluginSpec.class, pluginSpecObjectFactory);
        this.builtins = objects.domainObjectContainer(PluginSpec.class, pluginSpecObjectFactory);
    }

    @Override
    public Property<String> getOutputSubDir() {
        return this.outputSubDir;
    }

    @Override
    public DescriptorSetSpec getDescriptorSet() {
        return this.descriptorSet;
    }

    @Override
    public void descriptorSet(Action<DescriptorSetSpec> action) {
        action.execute(this.descriptorSet);
    }

    @Override
    public NamedDomainObjectContainer<PluginSpec> getPlugins() {
        return this.plugins;
    }

    @Override
    public void plugins(Action<NamedDomainObjectContainer<PluginSpec>> action) {
        action.execute(this.plugins);
    }

    @Override
    public NamedDomainObjectContainer<PluginSpec> getBuiltins() {
        return this.builtins;
    }

    @Override
    public void builtins(Action<NamedDomainObjectContainer<PluginSpec>> action) {
        action.execute(this.builtins);
    }
}
