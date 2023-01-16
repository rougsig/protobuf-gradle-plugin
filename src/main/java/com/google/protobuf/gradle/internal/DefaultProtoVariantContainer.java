package com.google.protobuf.gradle.internal;

import com.google.protobuf.gradle.tasks.ProtoVariant;
import com.google.protobuf.gradle.tasks.ProtoVariantContainer;
import org.gradle.api.internal.AbstractValidatingNamedDomainObjectContainer;
import org.gradle.api.internal.CollectionCallbackActionDecorator;
import org.gradle.api.model.ObjectFactory;
import org.gradle.api.reflect.TypeOf;
import org.gradle.internal.reflect.Instantiator;
import org.jetbrains.annotations.NotNull;

import javax.inject.Inject;

public class DefaultProtoVariantContainer extends AbstractValidatingNamedDomainObjectContainer<ProtoVariant> implements ProtoVariantContainer {
    private final ObjectFactory objects;
    private final Instantiator instantiator;
    private final CollectionCallbackActionDecorator collectionCallbackActionDecorator;

    @Inject
    public DefaultProtoVariantContainer(
        Instantiator instantiator,
        ObjectFactory objects,
        CollectionCallbackActionDecorator collectionCallbackActionDecorator
    ) {
        super(ProtoVariant.class, instantiator, ProtoVariant::getName, collectionCallbackActionDecorator);
        this.objects = objects;
        this.instantiator = instantiator;
        this.collectionCallbackActionDecorator = collectionCallbackActionDecorator;
    }

    @NotNull
    protected ProtoVariant doCreate(@NotNull String name) {
        return new DefaultProtoVariant(name, instantiator, collectionCallbackActionDecorator, objects);
    }

    @NotNull
    public TypeOf<?> getPublicType() {
        return TypeOf.typeOf(ProtoVariant.class);
    }
}
