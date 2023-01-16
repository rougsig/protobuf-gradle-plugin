package com.google.protobuf.gradle.internal;

import com.google.protobuf.gradle.tasks.DescriptorSetSpec;
import com.google.protobuf.gradle.tasks.GenerateProtoTaskSpec;
import com.google.protobuf.gradle.tasks.PluginSpec;
import com.google.protobuf.gradle.tasks.PluginSpecContainer;
import org.gradle.api.Action;
import org.gradle.api.NamedDomainObjectContainer;
import org.gradle.api.NamedDomainObjectFactory;
import org.gradle.api.internal.CollectionCallbackActionDecorator;
import org.gradle.api.model.ObjectFactory;
import org.gradle.api.provider.Property;
import org.gradle.internal.reflect.Instantiator;

public class DefaultGenerateProtoTaskSpec implements GenerateProtoTaskSpec {
    private final Property<String> outputSubDir;
    private final DescriptorSetSpec descriptorSet;
    private final PluginSpecContainer plugins;
    private final PluginSpecContainer builtins;

    public DefaultGenerateProtoTaskSpec(
        Instantiator instantiator,
        CollectionCallbackActionDecorator collectionCallbackActionDecorator,
        ObjectFactory objects
    ) {
        this.outputSubDir = objects.property(String.class);
        this.descriptorSet = new DefaultDescriptorSetSpec(objects);
        this.plugins = instantiator.newInstance(DefaultPluginSpecContainer.class, instantiator, objects, collectionCallbackActionDecorator);
        this.builtins = instantiator.newInstance(DefaultPluginSpecContainer.class, instantiator, objects, collectionCallbackActionDecorator);
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
    public PluginSpecContainer getPlugins() {
        return this.plugins;
    }

    @Override
    public void plugins(Action<PluginSpecContainer> action) {
        action.execute(this.plugins);
    }

    @Override
    public PluginSpecContainer getBuiltins() {
        return this.builtins;
    }

    @Override
    public void builtins(Action<PluginSpecContainer> action) {
        action.execute(this.builtins);
    }
}
