package com.google.protobuf.gradle;

import com.google.protobuf.gradle.internal.*;
import com.google.protobuf.gradle.tasks.*;
import org.gradle.api.Action;
import org.gradle.api.NamedDomainObjectContainer;
import org.gradle.api.Project;
import org.gradle.api.internal.CollectionCallbackActionDecorator;
import org.gradle.api.model.ObjectFactory;
import org.gradle.api.provider.Property;
import org.gradle.internal.reflect.Instantiator;

/**
 * Adds the protobuf {} block as a property of the project.
 */
public abstract class ProtobufExtension {
    private final ToolsLocator tools;

    private final NamedDomainObjectContainer<ProtoSourceSet> sourceSets;
    private final ProtoVariantContainer variants;

    private final String defaultGeneratedFilesBaseDir;

    public ProtobufExtension(
        Project project,
        ProtoVariantContainer protoVariantContainer
    ) {
        this.tools = new ToolsLocator(project);

        final ObjectFactory objects = project.getObjects();
        this.sourceSets = objects.domainObjectContainer(ProtoSourceSet.class, new ProtoSourceSetObjectFactory(objects));
        this.variants = protoVariantContainer;

        this.defaultGeneratedFilesBaseDir = project.getBuildDir() + "/generated/source/proto";
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
    public void protoc(Action<ExecutableLocator> configureAction) {
        configureAction.execute(tools.getProtoc());
    }

    /**
     * Locate the codegen plugin executables.
     * The closure will be manipulating a NamedDomainObjectContainer&lt;ExecutableLocator&gt;.
     */
    public void executables(Action<NamedDomainObjectContainer<ExecutableLocator>> configureAction) {
        configureAction.execute(tools.getExecutables());
    }

    public ProtoVariantSelector variantSelector() {
        return new DefaultProtoVariantSelector(variants);
    }

    public void variants(Action<GenerateProtoTaskSpec> configureAction) {
        variants(variantSelector(), configureAction);
    }

    public void variants(ProtoVariantSelector selector, Action<GenerateProtoTaskSpec> configureAction) {
        ((DefaultProtoVariantSelector) selector).all((ProtoVariant variant) -> {
            configureAction.execute(variant.getGenerateProtoTaskSpec());
        });
    }

    @Deprecated
    public void generateProtoTasks(Action<GenerateProtoTaskCollection> configureAction) {
        configureAction.execute(new GenerateProtoTaskCollection((DefaultProtoVariantSelector) variantSelector()));
    }
}
