package io.github.cedmart1decath.jnisecret.task

import io.github.cedmart1decath.jnisecret.configuration.JniSecretConfiguration
import io.github.cedmart1decath.jnisecret.exceptions.NoConfigurationException
import io.github.cedmart1decath.jnisecret.utils.CMakeListsUtils
import io.github.cedmart1decath.jnisecret.utils.Config
import io.github.cedmart1decath.jnisecret.utils.GitIgnoreUtils
import org.gradle.api.DefaultTask
import org.gradle.api.tasks.Nested
import org.gradle.api.tasks.Optional
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.TaskAction
import java.io.File

open class CreateCMakeListsTask: DefaultTask() {

    @Nested
    @Optional
    var configuration: JniSecretConfiguration? = null

    @OutputDirectory
    @Optional
    var outDir: File? = null

    @TaskAction
    fun createCMakeLists() {
        val safeConfiguration = configuration ?: throw NoConfigurationException()
        val content = CMakeListsUtils
            .getFileContent(
                safeConfiguration.className,
                "${project.projectDir}${Config.SRC_DIR}${Config.CPP_DIR}",
                Config.CPP_FILENAME
            )

        File(outDir, Config.CMAKE_FILENAME).apply {
            writeText(content)
            createNewFile()
        }

        GitIgnoreUtils.addToProjectGitIgnore(
            project,
            GitIgnoreUtils.GITIGNORE_CMAKELISTS)

    }

}