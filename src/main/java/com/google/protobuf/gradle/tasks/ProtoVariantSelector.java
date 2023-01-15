package com.google.protobuf.gradle.tasks;

public interface ProtoVariantSelector {
    ProtoVariantSelector withBuildType(final String buildType);

    ProtoVariantSelector withFlavor(final String flavour);

    ProtoVariantSelector withNonTest();

    ProtoVariantSelector withTest();

    ProtoVariantSelector withSourceSet(final String sourceSet);

    ProtoVariantSelector withName(final String name);
}
