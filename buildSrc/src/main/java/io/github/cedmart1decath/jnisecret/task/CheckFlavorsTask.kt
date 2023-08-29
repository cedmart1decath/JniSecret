package io.github.cedmart1decath.jnisecret.task

import org.gradle.api.DefaultTask
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.TaskAction

open class CheckFlavorsTask: DefaultTask() {

    @Input
    var secretFlavors: List<String>? = null

    @TaskAction
    fun checkFlavors() {
        // Do nothing here
    }

}