package io.reflekt.plugin.analysis.models

import io.reflekt.plugin.analysis.processor.FileId
import io.reflekt.plugin.analysis.processor.source.invokes.*
import io.reflekt.plugin.analysis.processor.source.uses.*
import io.reflekt.plugin.analysis.psi.function.toFunctionInfo
import io.reflekt.plugin.analysis.serialization.*
import io.reflekt.plugin.analysis.serialization.SerializationUtils.toKotlinType
import io.reflekt.plugin.analysis.serialization.SerializationUtils.toSerializableKotlinType

import org.jetbrains.kotlin.descriptors.impl.ModuleDescriptorImpl
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.types.KotlinType
import org.jetbrains.kotlin.types.Variance

import kotlinx.serialization.Serializable

typealias ClassOrObjectInvokes = MutableSet<SupertypesToAnnotations>
typealias FunctionInvokes = MutableSet<SignatureToAnnotations>
typealias SerializableFunctionInvokes = MutableSet<SerializableSignatureToAnnotations>

typealias TypeUses<K, V> = MutableMap<K, MutableList<V>>
typealias ClassOrObjectUses = TypeUses<SupertypesToAnnotations, KtClassOrObject>
typealias FunctionUses = TypeUses<SignatureToAnnotations, KtNamedFunction>

typealias IrClassOrObjectUses = TypeUses<SupertypesToAnnotations, String>
typealias IrFunctionUses = TypeUses<SignatureToAnnotations, IrFunctionInfo>

typealias BaseInvokesProcessors = Set<BaseInvokesProcessor<*>>

/**
 * If the function [withAnnotations] is called without supertypes then [supertypes] is setOf(Any::class::qualifiedName)
 * If the function [withSupertypes] is called without annotations then [annotations] is empty
 * @property supertypes
 * @property annotations
 */
@Serializable
data class SupertypesToAnnotations(
    val supertypes: Set<String> = emptySet(),
    val annotations: Set<String> = emptySet(),
)

/**
 * @property fqName
 * @property arguments
 * @property returnType
 * @property receiverType
 */
@Serializable
data class SerializableKotlinType(
    val fqName: String,
    val arguments: List<SerializableTypeProjection> = emptyList(),
    val returnType: String,
    val receiverType: SerializableKotlinType?,
)

/**
 * @property fqName
 * @property isStarProjection
 * @property projectionKind
 */
@Serializable
data class SerializableTypeProjection(
    val fqName: String,
    val isStarProjection: Boolean,
    val projectionKind: Variance,
)

/**
 * @property signature
 * @property annotations// kotlin.FunctionN< ... >
 */
@Serializable
data class SerializableSignatureToAnnotations(
    var signature: SerializableKotlinType?, val annotations: Set<String> = emptySet(),
)

/**
 * @property signature
 * @property annotations// kotlin.FunctionN< ... >
 */
data class SignatureToAnnotations(
    var signature: KotlinType?, val annotations: Set<String> = emptySet(),
)

/**
 * @property objects
 * @property classes
 * @property functions
 */
@Serializable
data class SerializableReflektInvokes(
    val objects: HashMap<FileId, ClassOrObjectInvokes> = HashMap(),
    val classes: HashMap<FileId, ClassOrObjectInvokes> = HashMap(),
    val functions: HashMap<FileId, SerializableFunctionInvokes> = HashMap(),
)

/**
 * @property invokes
 * @property packages
 */
@Serializable
data class SerializableReflektInvokesWithPackages(
    val invokes: SerializableReflektInvokes,
    val packages: Set<String>,
) {
    fun toReflektInvokesWithPackages(module: ModuleDescriptorImpl) =
        ReflektInvokesWithPackages(
            invokes = ReflektInvokes(
                objects = invokes.objects,
                classes = invokes.classes,
                functions = invokes.functions.mapValues { fileToInvokes ->
                    fileToInvokes.value.map {
                        SignatureToAnnotations(
                            annotations = it.annotations,
                            signature = it.signature?.toKotlinType(module),
                        )
                    }.toMutableSet()
                } as HashMap,
            ),
            packages = packages,
        )
}

/**
 * @property invokes
 * @property packages
 */
data class ReflektInvokesWithPackages(
    val invokes: ReflektInvokes,
    val packages: Set<String>,
) {
    fun toSerializableReflektInvokesWithPackages() =
        SerializableReflektInvokesWithPackages(
            invokes = invokes.toSerializableReflektInvokes(),
            packages = packages,
        )
}

/**
 * @property objects
 * @property classes
 * @property functions
 */
data class ReflektInvokes(
    val objects: HashMap<FileId, ClassOrObjectInvokes> = HashMap(),
    val classes: HashMap<FileId, ClassOrObjectInvokes> = HashMap(),
    val functions: HashMap<FileId, FunctionInvokes> = HashMap(),
) {
    fun isEmpty() = objects.isEmpty() && classes.isEmpty() && functions.isEmpty()

    fun isNotEmpty() = !isEmpty()

    fun toSerializableReflektInvokes(): SerializableReflektInvokes =
        SerializableReflektInvokes(
            objects = objects,
            classes = classes,
            functions = functions.mapValues { fileToInvokes ->
                fileToInvokes.value.map {
                    SerializableSignatureToAnnotations(
                        annotations = it.annotations,
                        signature = it.signature?.toSerializableKotlinType(),
                    )
                }.toMutableSet()
            } as HashMap,
        )

    fun merge(second: ReflektInvokes): ReflektInvokes = ReflektInvokes(
        objects = this.objects.merge(second.objects),
        classes = this.classes.merge(second.classes),
        functions = this.functions.merge(second.functions),
    )

    @Suppress("TYPE_ALIAS", "IDENTIFIER_LENGTH")
    private fun <V> HashMap<FileId, MutableSet<V>>.merge(second: HashMap<FileId, MutableSet<V>>): HashMap<FileId, MutableSet<V>> =
        this.also { second.forEach { (k, v) -> this.getOrPut(k) { v } } }
    companion object {
        fun createByProcessors(processors: BaseInvokesProcessors) = ReflektInvokes(
            objects = processors.mapNotNull { it as? ObjectInvokesProcessor }.first().fileToInvokes,
            classes = processors.mapNotNull { it as? ClassInvokesProcessor }.first().fileToInvokes,
            functions = processors.mapNotNull { it as? FunctionInvokesProcessor }.first().fileToInvokes,
        )
    }
}

/**
 * Stores enough information to generate function reference IR
 *
 * @property fqName
 * @property receiverFqName
 * @property isObjectReceiver
 */
data class IrFunctionInfo(
    val fqName: String,
    val receiverFqName: String?,
    val isObjectReceiver: Boolean,
)

/**
 * Store a set of qualified names that match the conditions for each item from [ReflektInvokes]
 *
 * @property objects
 * @property classes
 * @property functions
 */
data class ReflektUses(
    val objects: HashMap<FileId, ClassOrObjectUses> = HashMap(),
    val classes: HashMap<FileId, ClassOrObjectUses> = HashMap(),
    val functions: HashMap<FileId, FunctionUses> = HashMap(),
) {
    companion object {
        fun createByProcessors(processors: Set<BaseUsesProcessor<*>>) = ReflektUses(
            objects = processors.mapNotNull { it as? ObjectUsesProcessor }.first().fileToUses,
            classes = processors.mapNotNull { it as? ClassUsesProcessor }.first().fileToUses,
            functions = processors.mapNotNull { it as? FunctionUsesProcessor }.first().fileToUses,
        )
    }
}

/**
 * @property objects
 * @property classes
 * @property functions
 */
data class IrReflektUses(
    val objects: IrClassOrObjectUses = HashMap(),
    val classes: IrClassOrObjectUses = HashMap(),
    val functions: IrFunctionUses = HashMap(),
) {
    fun merge(second: IrReflektUses): IrReflektUses = IrReflektUses(
        objects = this.objects.merge(second.objects),
        classes = this.classes.merge(second.classes),
        functions = this.functions.merge(second.functions),
    )

    @Suppress("IDENTIFIER_LENGTH")
    private fun <K, V> TypeUses<K, V>.merge(second: TypeUses<K, V>): TypeUses<K, V> = this.also { second.forEach { (k, v) -> this.getOrPut(k) { v } } }
    companion object {
        @Suppress("IDENTIFIER_LENGTH")
        fun fromReflektUses(uses: ReflektUses, binding: BindingContext) = IrReflektUses(
            objects = HashMap(uses.objects.flatten().mapValues { (_, v) -> v.map { it.fqName!!.toString() }.toMutableList() }),
            classes = HashMap(uses.classes.flatten().mapValues { (_, v) -> v.map { it.fqName!!.toString() }.toMutableList() }),
            functions = HashMap(uses.functions.flatten().mapValues { (_, v) -> v.map { it.toFunctionInfo(binding) }.toMutableList() }),
        )
    }
}

fun ClassOrObjectUses.toSupertypesToFqNamesMap() = this.map { it.key.supertypes to it.value.mapNotNull { it.fqName?.toString() } }.toMap()

@Suppress("IDENTIFIER_LENGTH", "TYPE_ALIAS")
fun <T, V : KtElement> HashMap<FileId, TypeUses<T, V>>.flatten(): TypeUses<T, V> {
    val uses: TypeUses<T, V> = HashMap()
    this.forEach { (_, typeUses) ->
        typeUses.forEach { (k, v) ->
            uses.getOrPut(k) { mutableListOf() }.addAll(v)
        }
    }
    return uses
}
