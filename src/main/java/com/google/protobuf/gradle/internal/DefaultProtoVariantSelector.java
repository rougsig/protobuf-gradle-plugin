package com.google.protobuf.gradle.internal;

import com.google.protobuf.gradle.tasks.ProtoVariant;
import com.google.protobuf.gradle.tasks.ProtoVariantSelector;
import org.gradle.api.Action;
import org.gradle.api.NamedDomainObjectSet;

public class DefaultProtoVariantSelector implements ProtoVariantSelector {
    private final NamedDomainObjectSet<ProtoVariant> variants;

    public DefaultProtoVariantSelector(NamedDomainObjectSet<ProtoVariant> variants) {
        this.variants = variants;
    }

    @Override
    public DefaultProtoVariantSelector withBuildType(String buildType) {
        return new DefaultProtoVariantSelector(variants.matching((ProtoVariant variant) -> {
            return buildType.equals(variant.getBuildType());
        }));
    }

    @Override
    public DefaultProtoVariantSelector withFlavor(String flavour) {
        return new DefaultProtoVariantSelector(variants.matching((ProtoVariant variant) -> {
            return variant.getFlavors().contains(flavour);
        }));
    }

    @Override
    public DefaultProtoVariantSelector withNonTest() {
        return new DefaultProtoVariantSelector(variants.matching((ProtoVariant variant) -> {
            return !variant.getIsTest();
        }));
    }

    @Override
    public DefaultProtoVariantSelector withTest() {
        return new DefaultProtoVariantSelector(variants.matching((ProtoVariant variant) -> {
            return variant.getIsTest();
        }));
    }

    @Override
    public DefaultProtoVariantSelector withSourceSet(String sourceSet) {
        return new DefaultProtoVariantSelector(variants.matching((ProtoVariant variant) -> {
            return variant.getSourceSets().contains(sourceSet);
        }));
    }

    @Override
    public DefaultProtoVariantSelector withName(String name) {
        return new DefaultProtoVariantSelector(variants.matching((ProtoVariant variant) -> {
            return name.equals(variant.getName());
        }));
    }

    public void all(Action<ProtoVariant> action) {
        this.variants.all(action);
    }
}
