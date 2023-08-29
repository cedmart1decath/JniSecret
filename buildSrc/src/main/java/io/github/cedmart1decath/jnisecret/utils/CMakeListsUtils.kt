package io.github.cedmart1decath.jnisecret.utils

object CMakeListsUtils {

    private const val cppDirPathHolder = "%cpp_dir_path_holder%"
    private const val cppFilenameHolder = "%cpp_file_name_holder%"
    private const val libNameHolder = "%lib_name%"

    private val cMakeListsContent = """
        cmake_minimum_required(VERSION 3.22.1)
        project($libNameHolder)
        add_library(${"$"}{CMAKE_PROJECT_NAME} SHARED $cppDirPathHolder/$cppFilenameHolder)
        find_library(log-lib log)
        target_link_libraries(${"$"}{CMAKE_PROJECT_NAME} ${"$"}{log-lib} ${"$"}{log})
    """.trimIndent()

    fun getFileContent(libName: String, cppDirPath: String, cppFilename: String): String {
        return cMakeListsContent
            .replace(libNameHolder, libName)
            .replace(cppDirPathHolder, cppDirPath)
            .replace(cppFilenameHolder, cppFilename)
    }

}