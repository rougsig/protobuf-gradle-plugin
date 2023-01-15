package com.google.protobuf.gradle.internal;

import com.google.protobuf.gradle.tasks.ProtoVariant;
import org.gradle.api.NamedDomainObjectFactory;
import org.gradle.api.model.ObjectFactory;
import org.jetbrains.annotations.NotNull;

public class ProtoVariantObjectFactory implements NamedDomainObjectFactory<ProtoVariant> {
    private final ObjectFactory objects;

    public ProtoVariantObjectFactory(ObjectFactory objects) {
        this.objects = objects;
    }

    @NotNull
    @Override
    public ProtoVariant create(@NotNull String name) {
        return new DefaultProtoVariant(name, objects);
    }
}
