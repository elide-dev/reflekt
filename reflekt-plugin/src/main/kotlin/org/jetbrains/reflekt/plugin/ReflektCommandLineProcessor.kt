package org.jetbrains.reflekt.plugin

import org.jetbrains.reflekt.plugin.utils.Keys
import org.jetbrains.reflekt.util.Util.DEPENDENCY_JAR_OPTION_INFO
import org.jetbrains.reflekt.util.Util.ENABLED_OPTION_INFO
import org.jetbrains.reflekt.util.Util.LIBRARY_TO_INTROSPECT
import org.jetbrains.reflekt.util.Util.OUTPUT_DIR_OPTION_INFO
import org.jetbrains.reflekt.util.Util.PLUGIN_ID
import org.jetbrains.reflekt.util.Util.REFLEKT_META_FILE_OPTION_INFO
import org.jetbrains.reflekt.util.Util.REFLEKT_META_FILE_PATH
import org.jetbrains.reflekt.util.Util.SAVE_METADATA_OPTION_INFO

import com.google.auto.service.AutoService
import org.jetbrains.kotlin.compiler.plugin.*
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.config.CompilerConfigurationKey

import java.io.File

/**
 * A class for handling command line arguments and transfer them into the kotlin compiler plugin
 *
 * @property pluginId the compiler plugin id. Just needs to be consistent
 *  with the key for ReflektSubPlugin#getCompilerPluginId from the gradle plugin.
 * @property pluginOptions the collection of the command line options for the kotlin compiler plugin.
 *  Should match up with the options we return from the ReflektSubPlugin in the gradle plugin.
 *  Should also have matching when branches for each name in the [processOption] function
 */
@AutoService(CommandLineProcessor::class)
class ReflektCommandLineProcessor : CommandLineProcessor {
    override val pluginId: String = PLUGIN_ID
    override val pluginOptions: Collection<CliOption> =
        listOf(
            ENABLED_OPTION,
            DEPENDENCY_JAR_OPTION,
            REFLEKT_META_FILE_OPTION,
            OUTPUT_DIR_OPTION,
            SAVE_METADATA_OPTION,
            REFLEKT_META_FILE_PATH_OPTION,
            LIBRARY_TO_INTROSPECT_OPTION,
        )

    /**
     * Process the compiler plugin command line options and put them to the CompilerConfiguration
     *
     * @param option the current command line option that should be handled
     * @param value of the current option, should be converted into necessary type
     * @param configuration the kotlin compiler configuration that should be updated,
     *  the options should be added into the configuration
     */
    override fun processOption(
        option: AbstractCliOption,
        value: String,
        configuration: CompilerConfiguration,
    ) = when (option) {
        ENABLED_OPTION -> configuration.put(Keys.ENABLED, value.toBoolean())
        DEPENDENCY_JAR_OPTION -> configuration.addToList(Keys.DEPENDENCY_JARS, File(value))
        REFLEKT_META_FILE_OPTION -> configuration.addToList(Keys.REFLEKT_META_FILES, File(value))
        OUTPUT_DIR_OPTION -> configuration.put(Keys.OUTPUT_DIR, File(value))
        SAVE_METADATA_OPTION -> configuration.put(Keys.TO_SAVE_METADATA, value.toBoolean())
        REFLEKT_META_FILE_PATH_OPTION -> configuration.put(Keys.REFLEKT_META_PATH, value)
        LIBRARY_TO_INTROSPECT_OPTION -> configuration.addToList(Keys.LIBRARY_TO_INTROSPECT, value)
        else -> error("Unexpected config option ${option.optionName}")
    }

    /**
     * Add a new [value] into a list of values with the [configurationKey] into the kotlin compiler configuration.
     * If the configuration does not have this key, create an empty list and put the [value]
     *
     * @param configurationKey the key of the command line option (should be a list)
     * @param value new value that should be added
     */
    @Suppress("TYPE_ALIAS")
    private fun <T> CompilerConfiguration.addToList(configurationKey: CompilerConfigurationKey<List<T>>, value: T) {
        val values = get(configurationKey) ?: emptyList()
        put(configurationKey, values + value)
    }

    /**
     * The possible kotlin compiler command line options
     *
     * @property ENABLED_OPTION indicates if the plugin is enabled
     * @property DEPENDENCY_JAR_OPTION stores the libraries jars absolute
     *  path that included in the current project as a compileClasspath configuration
     * @property REFLEKT_META_FILE_OPTION stores the absolute file's path from the library with Reflekt meta information
     * @property OUTPUT_DIR_OPTION stores a relative path for generated files (e.g. ReflektImpl.kt)
     * @property SAVE_METADATA_OPTION indicates whether to save Reflekt usages into META-INF
     * @property REFLEKT_META_FILE_PATH_OPTION stores the relative path to the ReflektMeta file
     *  in the resources directory in the src folder in the project
     * @property LIBRARY_TO_INTROSPECT_OPTION stores names of the libraries' for introspection
     *  (each library should be included in the project by a configuration that can be resolved in the compile-time)
     *  TODO: delete this option since we can indicate these libraries by check if the ReflektMeta file exist
     */
    companion object {
        val ENABLED_OPTION =
            CliOption(
                optionName = ENABLED_OPTION_INFO.name,
                valueDescription = ENABLED_OPTION_INFO.valueDescription,
                description = ENABLED_OPTION_INFO.description,
            )
        val DEPENDENCY_JAR_OPTION =
            CliOption(
                optionName = DEPENDENCY_JAR_OPTION_INFO.name,
                valueDescription = DEPENDENCY_JAR_OPTION_INFO.valueDescription,
                description = DEPENDENCY_JAR_OPTION_INFO.description,
                allowMultipleOccurrences = true,
                required = false,
            )
        val REFLEKT_META_FILE_OPTION =
            CliOption(
                optionName = REFLEKT_META_FILE_OPTION_INFO.name,
                valueDescription = REFLEKT_META_FILE_OPTION_INFO.valueDescription,
                description = REFLEKT_META_FILE_OPTION_INFO.description,
                allowMultipleOccurrences = true,
                required = false,
            )
        val OUTPUT_DIR_OPTION =
            CliOption(
                optionName = OUTPUT_DIR_OPTION_INFO.name,
                valueDescription = OUTPUT_DIR_OPTION_INFO.valueDescription,
                description = OUTPUT_DIR_OPTION_INFO.description,
                required = false,
                allowMultipleOccurrences = false,
            )
        val SAVE_METADATA_OPTION =
            CliOption(
                optionName = SAVE_METADATA_OPTION_INFO.name,
                valueDescription = SAVE_METADATA_OPTION_INFO.valueDescription,
                description = SAVE_METADATA_OPTION_INFO.description,
                required = false,
                allowMultipleOccurrences = false,
            )
        val REFLEKT_META_FILE_PATH_OPTION =
            CliOption(
                optionName = REFLEKT_META_FILE_PATH.name,
                valueDescription = REFLEKT_META_FILE_PATH.valueDescription,
                description = REFLEKT_META_FILE_PATH.description,
                required = false,
                allowMultipleOccurrences = false,
            )
        val LIBRARY_TO_INTROSPECT_OPTION =
            CliOption(
                optionName = LIBRARY_TO_INTROSPECT.name,
                valueDescription = LIBRARY_TO_INTROSPECT.valueDescription,
                description = LIBRARY_TO_INTROSPECT.description,
                required = false,
                allowMultipleOccurrences = true,
            )
    }
}
