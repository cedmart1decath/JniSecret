package io.github.cedmart1decath.jnisecret.task

import io.github.cedmart1decath.jnisecret.configuration.JniSecretConfiguration
import io.github.cedmart1decath.jnisecret.exceptions.NoConfigurationException
import io.github.cedmart1decath.jnisecret.utils.Config
import io.github.cedmart1decath.jnisecret.utils.CppUtils
import io.github.cedmart1decath.jnisecret.utils.GitIgnoreUtils
import org.gradle.api.DefaultTask
import org.gradle.api.tasks.*
import java.io.File

open class CreateCppTask: DefaultTask() {

    @Nested
    @Optional
    var configuration: JniSecretConfiguration? = null

    @Input
    @Optional
    var flavor: String? = null

    @OutputDirectory
    @Optional
    var outDir: File? = null

    @TaskAction
    fun createCppFile() {

        if(configuration == null) {
            throw NoConfigurationException()
        }
        configuration?.let {
            val content = buildCppContent(it)
            saveCppFile(content)
            setGitIgnore()
        }
    }

    private fun buildCppContent(configuration: JniSecretConfiguration): String {
        val packageName = CppUtils.transformPackageName(configuration.packageName)
        val className = CppUtils.transformClassName(configuration.className)
        val values = configuration
            .defaultConfig
            .secrets
            .map {
                it.key to it.value
            }
            .toMap()
            .let {
                val mutableMap = it.toMutableMap()
                configuration.productFlavors.findByName(flavor)?.secrets?.forEach { entry ->
                    mutableMap[entry.key] = entry.value
                }
                mutableMap
            }
            .toList()

        return CppUtils.getCppContent(packageName, className, values, configuration.storingType)
    }

    private fun saveCppFile(content: String) {

        outDir?.let {
            if (!it.exists()) {
                it.mkdirs()
            }

            File(it, Config.CPP_FILENAME).apply {
                writeText(content)
                createNewFile()
            }
        }
    }

    private fun setGitIgnore() {
        GitIgnoreUtils.addToProjectGitIgnore(
            project,
            "${Config.SRC_DIR}${Config.CPP_DIR}")
    }
}