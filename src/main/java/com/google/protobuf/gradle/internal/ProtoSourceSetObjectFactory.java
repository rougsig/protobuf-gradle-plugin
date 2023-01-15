package com.google.protobuf.gradle.internal;

import com.google.protobuf.gradle.tasks.ProtoSourceSet;
import org.gradle.api.NamedDomainObjectFactory;
import org.gradle.api.model.ObjectFactory;
import org.jetbrains.annotations.NotNull;

public class ProtoSourceSetObjectFactory implements NamedDomainObjectFactory<ProtoSourceSet> {
    private final ObjectFactory objects;

    public ProtoSourceSetObjectFactory(ObjectFactory objects) {
        this.objects = objects;
    }

    @NotNull
    @Override
    public ProtoSourceSet create(@NotNull String name) {
        return new DefaultProtoSourceSet(name, objects);
    }
}
