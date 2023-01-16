package com.google.protobuf.gradle.internal;

import com.google.protobuf.gradle.tasks.PluginSpec;
import com.google.protobuf.gradle.tasks.PluginSpecContainer;
import org.gradle.api.internal.AbstractValidatingNamedDomainObjectContainer;
import org.gradle.api.internal.CollectionCallbackActionDecorator;
import org.gradle.api.model.ObjectFactory;
import org.gradle.api.reflect.TypeOf;
import org.gradle.internal.reflect.Instantiator;
import org.jetbrains.annotations.NotNull;

import javax.inject.Inject;

public class DefaultPluginSpecContainer extends AbstractValidatingNamedDomainObjectContainer<PluginSpec> implements PluginSpecContainer {
    private final ObjectFactory objects;

    @Inject
    public DefaultPluginSpecContainer(
        Instantiator instantiator,
        ObjectFactory objects,
        CollectionCallbackActionDecorator collectionCallbackActionDecorator
    ) {
        super(PluginSpec.class, instantiator, PluginSpec::getName, collectionCallbackActionDecorator);
        this.objects = objects;
    }

    @NotNull
    protected PluginSpec doCreate(@NotNull String name) {
        return new DefaultPluginSpec(name, objects);
    }

    @NotNull
    public TypeOf<?> getPublicType() {
        return TypeOf.typeOf(PluginSpec.class);
    }
}
