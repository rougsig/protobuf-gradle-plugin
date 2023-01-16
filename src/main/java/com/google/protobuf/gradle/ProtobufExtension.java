package com.google.protobuf.gradle;

import com.google.protobuf.gradle.internal.DefaultProtoVariantSelector;
import com.google.protobuf.gradle.internal.ProtoSourceSetObjectFactory;
import com.google.protobuf.gradle.internal.ProtoVariantObjectFactory;
import com.google.protobuf.gradle.tasks.GenerateProtoTaskSpec;
import com.google.protobuf.gradle.tasks.ProtoSourceSet;
import com.google.protobuf.gradle.tasks.ProtoVariant;
import com.google.protobuf.gradle.tasks.ProtoVariantSelector;
import org.gradle.api.Action;
import org.gradle.api.NamedDomainObjectContainer;
import org.gradle.api.Project;
import org.gradle.api.model.ObjectFactory;
import org.gradle.api.provider.Property;

/**
 * Adds the protobuf {} block as a property of the project.
 */
public abstract class ProtobufExtension {
    private final ToolsLocator tools;

    private final NamedDomainObjectContainer<ProtoSourceSet> sourceSets;
    private final NamedDomainObjectContainer<ProtoVariant> variants;

    private final String defaultGeneratedFilesBaseDir;

    public ProtobufExtension(final Project project) {
        this.tools = new ToolsLocator(project);

        final ObjectFactory objects = project.getObjects();
        this.sourceSets = objects.domainObjectContainer(ProtoSourceSet.class, new ProtoSourceSetObjectFactory(objects));
        this.variants = objects.domainObjectContainer(ProtoVariant.class, new ProtoVariantObjectFactory(objects));

        this.defaultGeneratedFilesBaseDir = "${project.buildDir}/generated/source/proto";
        this.getGeneratedFilesBaseDir().convention(defaultGeneratedFilesBaseDir);
    }

    /**
     * The base directory of generated files.
     * The default is: "${project.buildDir}/generated/source/proto".
     */
    public abstract Property<String> getGeneratedFilesBaseDir();

    public String getDefaultGeneratedFilesBaseDir() {
        return defaultGeneratedFilesBaseDir;
    }

    public NamedDomainObjectContainer<ProtoSourceSet> getSourceSets() {
        return sourceSets;
    }

    public NamedDomainObjectContainer<ProtoVariant> getVariants() {
        return variants;
    }

    public ToolsLocator getTools() {
        return tools;
    }

    //===========================================================================
    // Configuration methods
    //===========================================================================

    /**
     * Locates the protoc executable.
     * The closure will be manipulating an ExecutableLocator.
     */
    public void protoc(final Action<ExecutableLocator> configureAction) {
        configureAction.execute(tools.getProtoc());
    }

    /**
     * Locate the codegen plugin executables.
     * The closure will be manipulating a NamedDomainObjectContainer&lt;ExecutableLocator&gt;.
     */
    public void plugins(final Action<NamedDomainObjectContainer<ExecutableLocator>> configureAction) {
        configureAction.execute(tools.getPlugins());
    }

    public ProtoVariantSelector variantSelector() {
        return new DefaultProtoVariantSelector(variants);
    }

    public void variants(final Action<GenerateProtoTaskSpec> configureAction) {
        variants(variantSelector(), configureAction);
    }

    public void variants(
        final ProtoVariantSelector selector,
        final Action<GenerateProtoTaskSpec> configureAction
    ) {
        ((DefaultProtoVariantSelector) selector).all((final ProtoVariant variant) -> {
            configureAction.execute(variant.getGenerateProtoTaskSpec());
        });
    }

    @Deprecated
    public void generateProtoTasks(Action<GenerateProtoTaskCollection> configureAction) {
        configureAction.execute(new GenerateProtoTaskCollection((DefaultProtoVariantSelector) variantSelector()));
    }
}
