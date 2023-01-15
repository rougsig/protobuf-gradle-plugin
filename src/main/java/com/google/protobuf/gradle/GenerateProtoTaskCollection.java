package com.google.protobuf.gradle;

import com.google.protobuf.gradle.internal.DefaultProtoVariantSelector;
import com.google.protobuf.gradle.tasks.GenerateProtoTaskSpec;
import com.google.protobuf.gradle.tasks.ProtoVariant;
import org.gradle.api.Action;

@Deprecated
public class GenerateProtoTaskCollection {
    private final DefaultProtoVariantSelector selector;

    public GenerateProtoTaskCollection(final DefaultProtoVariantSelector selector) {
        this.selector = selector;
    }

    @Deprecated
    public void all(final Action<GenerateProtoTaskSpec> configureAction) {
        selector.all((final ProtoVariant variant) -> {
            configureAction.execute(variant.getGenerateProtoTaskSpec());
        });
    }

    @Deprecated
    public void ofSourceSet(
        final String sourceSet,
        final Action<GenerateProtoTaskSpec> configureAction
    ) {
        selector.withSourceSet(sourceSet).all((final ProtoVariant variant) -> {
            configureAction.execute(variant.getGenerateProtoTaskSpec());
        });
    }

    @Deprecated
    public void ofFlavor(
        final String flavor,
        final Action<GenerateProtoTaskSpec> configureAction
    ) {
        selector.withFlavor(flavor).all((final ProtoVariant variant) -> {
            configureAction.execute(variant.getGenerateProtoTaskSpec());
        });
    }

    @Deprecated
    public void ofBuildType(
        final String buildType,
        final Action<GenerateProtoTaskSpec> configureAction
    ) {
        selector.withBuildType(buildType).all((final ProtoVariant variant) -> {
            configureAction.execute(variant.getGenerateProtoTaskSpec());
        });
    }

    @Deprecated
    public void ofVariant(
        final String variantName,
        final Action<GenerateProtoTaskSpec> configureAction
    ) {
        selector.withName(variantName).all((final ProtoVariant variant) -> {
            configureAction.execute(variant.getGenerateProtoTaskSpec());
        });
    }

    @Deprecated
    public void ofNonTest(final Action<GenerateProtoTaskSpec> configureAction) {
        selector.withNonTest().all((final ProtoVariant variant) -> {
            configureAction.execute(variant.getGenerateProtoTaskSpec());
        });
    }

    @Deprecated
    public void ofTest(final Action<GenerateProtoTaskSpec> configureAction) {
        selector.withTest().all((final ProtoVariant variant) -> {
            configureAction.execute(variant.getGenerateProtoTaskSpec());
        });
    }
}
