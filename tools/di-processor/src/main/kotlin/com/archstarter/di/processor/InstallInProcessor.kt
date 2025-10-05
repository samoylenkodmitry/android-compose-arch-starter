package com.archstarter.di.processor

import com.archstarter.core.di.InstallInAppComponent
import com.archstarter.core.di.InstallInScreenComponent
import com.archstarter.core.di.InstallInSubscreenComponent
import com.google.devtools.ksp.processing.CodeGenerator
import com.google.devtools.ksp.processing.Dependencies
import com.google.devtools.ksp.processing.KSPLogger
import com.google.devtools.ksp.processing.Resolver
import com.google.devtools.ksp.processing.SymbolProcessor
import com.google.devtools.ksp.processing.SymbolProcessorEnvironment
import com.google.devtools.ksp.processing.SymbolProcessorProvider
import com.google.devtools.ksp.symbol.KSAnnotated
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.FileSpec
import com.squareup.kotlinpoet.TypeSpec
import com.squareup.kotlinpoet.ksp.writeTo
import java.io.File

private const val GENERATED_PACKAGE = "com.archstarter.core.di.generated"
private const val OPTION_MODULE_NAME = "com.archstarter.di.moduleName"
private const val OPTION_METADATA_DIR = "com.archstarter.di.metadataDir"
private const val OPTION_GENERATE_AGGREGATES = "com.archstarter.di.generateAggregates"

private enum class InstallInScope(
    val annotation: String,
    val interfaceName: String,
    val metadataFolder: String,
) {
    APP(
        annotation = InstallInAppComponent::class.qualifiedName!!,
        interfaceName = "GeneratedAppBindings",
        metadataFolder = "app",
    ),
    SCREEN(
        annotation = InstallInScreenComponent::class.qualifiedName!!,
        interfaceName = "GeneratedScreenBindings",
        metadataFolder = "screen",
    ),
    SUBSCREEN(
        annotation = InstallInSubscreenComponent::class.qualifiedName!!,
        interfaceName = "GeneratedSubscreenBindings",
        metadataFolder = "subscreen",
    ),
}

class InstallInProcessor(
    private val codeGenerator: CodeGenerator,
    private val logger: KSPLogger,
    private val moduleName: String,
    private val metadataDir: File,
    private val generateAggregates: Boolean,
) : SymbolProcessor {

    private val sanitizedModuleName: String = moduleName
        .replace(':', '_')
        .replace('/', '_')
        .removePrefix("_")

    private var generated = false

    override fun process(resolver: Resolver): List<KSAnnotated> {
        if (generated) return emptyList()

        metadataDir.mkdirs()

        InstallInScope.entries.forEach { scope ->
            val declarations = resolver
                .getSymbolsWithAnnotation(scope.annotation)
                .filterIsInstance<KSClassDeclaration>()
                .toList()

            writeMetadata(scope, declarations)

            if (generateAggregates) {
                val contributions = buildSet {
                    addAll(readMetadata(scope))
                    declarations.mapNotNullTo(this) { it.qualifiedName?.asString() }
                }
                generateBindingInterface(scope, contributions, declarations)
            }
        }

        generated = true
        return emptyList()
    }

    private fun writeMetadata(scope: InstallInScope, declarations: List<KSClassDeclaration>) {
        val scopeDir = metadataDir.resolve(scope.metadataFolder)
        val metadataFile = scopeDir.resolve("$sanitizedModuleName.txt")
        if (declarations.isEmpty()) {
            if (metadataFile.exists()) {
                metadataFile.delete()
            }
            return
        }

        if (!scopeDir.exists() && !scopeDir.mkdirs()) {
            logger.error("Failed to create metadata directory: ${scopeDir.absolutePath}")
            return
        }

        metadataFile.printWriter().use { writer ->
            declarations
                .mapNotNull { it.qualifiedName?.asString() }
                .sorted()
                .forEach { writer.println(it) }
        }
    }

    private fun readMetadata(scope: InstallInScope): Set<String> {
        val scopeDir = metadataDir.resolve(scope.metadataFolder)
        if (!scopeDir.exists()) return emptySet()

        return scopeDir
            .listFiles()
            ?.filter { it.isFile }
            ?.flatMap { file ->
                file
                    .readLines()
                    .map { it.trim() }
                    .filter { it.isNotEmpty() }
            }
            ?.toSet()
            ?: emptySet()
    }

    private fun generateBindingInterface(
        scope: InstallInScope,
        contributions: Set<String>,
        declarations: List<KSClassDeclaration>,
    ) {
        val typeSpecBuilder = TypeSpec.interfaceBuilder(scope.interfaceName)
        contributions
            .sorted()
            .map { ClassName.bestGuess(it) }
            .forEach { className -> typeSpecBuilder.addSuperinterface(className) }

        val typeSpec = typeSpecBuilder.build()
        val fileSpec = FileSpec
            .builder(GENERATED_PACKAGE, scope.interfaceName)
            .addType(typeSpec)
            .build()

        val dependencies = Dependencies(
            aggregating = true,
            *declarations.mapNotNull { it.containingFile }.toTypedArray(),
        )

        try {
            fileSpec.writeTo(codeGenerator, dependencies)
        } catch (e: Exception) {
            logger.error("Failed to generate ${scope.interfaceName}: ${e.message}")
            throw e
        }
    }
}

class InstallInProcessorProvider : SymbolProcessorProvider {
    override fun create(environment: SymbolProcessorEnvironment): SymbolProcessor {
        val moduleName = environment.options[OPTION_MODULE_NAME]
            ?: error("Missing KSP option '$OPTION_MODULE_NAME'")
        val metadataDirPath = environment.options[OPTION_METADATA_DIR]
            ?: error("Missing KSP option '$OPTION_METADATA_DIR'")
        val generateAggregates = environment.options[OPTION_GENERATE_AGGREGATES]?.toBooleanStrictOrNull() ?: false

        return InstallInProcessor(
            codeGenerator = environment.codeGenerator,
            logger = environment.logger,
            moduleName = moduleName,
            metadataDir = File(metadataDirPath),
            generateAggregates = generateAggregates,
        )
    }
}
