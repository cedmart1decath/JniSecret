package io.github.cedmart1decath.jnisecret

import com.android.build.gradle.api.BaseVariant
import com.android.build.gradle.internal.tasks.factory.dependsOn
import io.github.cedmart1decath.jnisecret.configuration.JniSecretConfiguration
import io.github.cedmart1decath.jnisecret.configuration.JniSecretEntries
import io.github.cedmart1decath.jnisecret.exceptions.NoExternalBuildException
import io.github.cedmart1decath.jnisecret.task.CreateCMakeListsTask
import io.github.cedmart1decath.jnisecret.task.CreateCppTask
import io.github.cedmart1decath.jnisecret.task.CreateJniInterfaceTask
import io.github.cedmart1decath.jnisecret.utils.Config
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.tasks.SourceTask
import org.gradle.api.tasks.TaskProvider
import java.io.File

class JniSecretPlugin : Plugin<Project> {

    companion object {
        const val EXTENSION_NAME = "jniSecret"
        const val CHECK_EXTERNAL_NATIVE_TASK = "verifyExternalNativeBuild"
        const val CLEAN_TASK = "cleanJniSecret"
    }

    override fun apply(project: Project) {


        //
        // Plugin configuration
        //

        val secrets = project.container(JniSecretEntries::class.java)
        val configuration = project.extensions.create(EXTENSION_NAME, JniSecretConfiguration::class.java, secrets)

        project.afterEvaluate {

            //
            // Task
            //

            val checkCmakeTask = checkCMakeTask(
                project,
                configuration
            )

            val cleanTask = cleanTask(project)

            project.android().applicationVariants().all { variant ->

                val flavorName = variant.name
                    .replace("Debug", "")
                    .replace("Release", "")

                val jniTask = jniTask(
                    project,
                    variant,
                    configuration,
                    flavorName
                )

                val cppTask = cppTask(
                    project,
                    variant,
                    configuration,
                    flavorName
                )

                val makeTask = makeTask(
                    project,
                    variant,
                    configuration
                )

                //
                // Task orchestration
                //

                val preBuildTaskName =
                    "pre${variant.name.replaceFirstChar(Char::titlecase)}Build"


                val preBuildTask = project.tasks.getByName(preBuildTaskName)

                if (configuration.generateCMake) {
                    // buildCmake -> preBuild
                    preBuildTask.dependsOn(makeTask)
                    // buildJniInterface -> buildCMake
                    makeTask.dependsOn(cppTask)

                } else {
                    // buildJniInterface -> preBuild
                   preBuildTask.dependsOn(cppTask)
                }
                // verifyExternalNativeBuild -> buildCpp
                cppTask.dependsOn(checkCmakeTask)

                jniTask.dependsOn(cppTask)
            }

            project.tasks.getByName("clean") { t ->
                t.dependsOn(cleanTask)
            }
        }
    }

    private fun cleanTask(project: Project): TaskProvider<Task> {
        return project.tasks.register(CLEAN_TASK) { t ->
            t.group = EXTENSION_NAME
            t.doFirst {
                val jniDir = File("${project.buildDir}/generated/source/JniSecret/")
                if (jniDir.exists()) jniDir.deleteRecursively()

                val cppDir = File("${project.projectDir}/${Config.SRC_DIR}${Config.CPP_DIR}")
                if (cppDir.exists()) cppDir.deleteRecursively()
            }
        }
    }

    private fun checkCMakeTask(project: Project,
                               configuration: JniSecretConfiguration): TaskProvider<Task> {
        return project.tasks.register(CHECK_EXTERNAL_NATIVE_TASK) { t ->
            t.group = EXTENSION_NAME
            t.doFirst {
                val cmakePath = project.android().externalNativeBuild.cmake.path
                if(configuration.generateCMake && cmakePath == null) {
                    throw NoExternalBuildException()
                }
            }
        }
    }

    private fun jniTask(project: Project,
                        variant: BaseVariant,
                        configuration: JniSecretConfiguration,
                        flavorName: String): TaskProvider<CreateJniInterfaceTask> {

        val outJniDir = File("${project.buildDir}/generated/source/JniSecret/${flavorName}/")

        val jniTask = project.tasks.register(
            "buildJniInterface${variant.name.replaceFirstChar(Char::titlecase)}",
            CreateJniInterfaceTask::class.java
        ) { t ->
            t.group = EXTENSION_NAME
            t.configuration = configuration
            t.flavor = flavorName
            t.outDir = outJniDir

            val ktFile = File("$outJniDir/${configuration.packageName.replace(".", "/")}/", "${configuration.className}.kt")
            if(!ktFile.exists()) {
                t.outputs.upToDateWhen { false }
            }

            t.doLast {
                variant.addJavaSourceFoldersToModel(outJniDir)
            }
        }

        variant.registerJavaGeneratingTask(
            jniTask,
            outJniDir
        )

        project.tasks.findByName("compile${variant.name.replaceFirstChar(Char::titlecase)}Kotlin")?.let { t ->
            val ktDir = File(outJniDir, configuration.packageName.replace(".", "/"))
            val srcSet = project.objects.sourceDirectorySet("JniSecret${variant.name}", "JniSecret${variant.name}").srcDir(ktDir)
            (t as? SourceTask)?.let {
                it.source(srcSet)
            }
        }

        return jniTask
    }

    private fun cppTask(
        project: Project,
        variant: BaseVariant,
        configuration: JniSecretConfiguration,
        flavorName: String
    ): TaskProvider<CreateCppTask> {

        val outCppDir = File("${project.projectDir}${Config.SRC_DIR}${Config.CPP_DIR}")

        return project.tasks.register(
            "buildCppFile${variant.name.replaceFirstChar(Char::titlecase)}",
            CreateCppTask::class.java
        ) { t ->

            t.group = EXTENSION_NAME
            t.configuration = configuration
            t.flavor = flavorName
            t.outDir = outCppDir

            if (!(File(outCppDir, Config.CPP_FILENAME).exists())) {
                t.outputs.upToDateWhen { false }
            }
        }
    }

    private fun makeTask(
        project: Project,
        variant: BaseVariant,
        configuration: JniSecretConfiguration
    ): TaskProvider<CreateCMakeListsTask> {
        val outCmakeDir = File("${project.projectDir}")

        return project.tasks.register(
            "buildCMake${variant.name.replaceFirstChar(Char::titlecase)}",
            CreateCMakeListsTask::class.java
        ) { t ->
            t.group = EXTENSION_NAME
            t.configuration = configuration
            t.outDir = outCmakeDir
        }
    }
}