package com.google.protobuf.gradle;

import com.google.protobuf.gradle.internal.DefaultProtoVariantSelector;
import com.google.protobuf.gradle.tasks.GenerateProtoTaskSpec;
import com.google.protobuf.gradle.tasks.ProtoVariant;
import org.gradle.api.Action;

@Deprecated
public class GenerateProtoTaskCollection {
    private final DefaultProtoVariantSelector selector;

    public GenerateProtoTaskCollection(DefaultProtoVariantSelector selector) {
        this.selector = selector;
    }

    @Deprecated
    public void all(GenerateProtoTaskSpecAction configureAction) {
        selector.all((ProtoVariant variant) -> {
            configureAction.execute(variant.getGenerateProtoTaskSpec());
        });
    }

    @Deprecated
    public void ofSourceSet(
        String sourceSet,
        GenerateProtoTaskSpecAction configureAction
    ) {
        selector.withSourceSet(sourceSet).all((ProtoVariant variant) -> {
            configureAction.execute(variant.getGenerateProtoTaskSpec());
        });
    }

    @Deprecated
    public void ofFlavor(
        String flavor,
        GenerateProtoTaskSpecAction configureAction
    ) {
        selector.withFlavor(flavor).all((ProtoVariant variant) -> {
            configureAction.execute(variant.getGenerateProtoTaskSpec());
        });
    }

    @Deprecated
    public void ofBuildType(
        String buildType,
        GenerateProtoTaskSpecAction configureAction
    ) {
        selector.withBuildType(buildType).all((ProtoVariant variant) -> {
            configureAction.execute(variant.getGenerateProtoTaskSpec());
        });
    }

    @Deprecated
    public void ofVariant(
        String variantName,
        GenerateProtoTaskSpecAction configureAction
    ) {
        selector.withName(variantName).all((ProtoVariant variant) -> {
            configureAction.execute(variant.getGenerateProtoTaskSpec());
        });
    }

    @Deprecated
    public void ofNonTest(GenerateProtoTaskSpecAction configureAction) {
        selector.withNonTest().all((ProtoVariant variant) -> {
            configureAction.execute(variant.getGenerateProtoTaskSpec());
        });
    }

    @Deprecated
    public void ofTest(GenerateProtoTaskSpecAction configureAction) {
        selector.withTest().all((ProtoVariant variant) -> {
            configureAction.execute(variant.getGenerateProtoTaskSpec());
        });
    }
}
