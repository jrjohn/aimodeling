package com.example.arcana.verification

import org.gradle.api.Plugin
import org.gradle.api.Project

/**
 * Gradle plugin that adds architecture verification tasks to the project
 */
class ArchitectureVerificationPlugin : Plugin<Project> {

    override fun apply(project: Project) {
        // Register verification task
        val verifyTask = project.tasks.register("verifyArchitecture", ArchitectureVerificationTask::class.java)

        // Register report generation task
        val reportTask = project.tasks.register("generateArchitectureReport", ArchitectureReportTask::class.java)

        // Hook into afterEvaluate to ensure tasks are available
        project.afterEvaluate {
            // Make verification depend on tests (runs after tests complete)
            project.tasks.findByName("test")?.let { testTask ->
                verifyTask.configure {
                    dependsOn(testTask)
                }
                // Also make test task finalized by verification
                testTask.finalizedBy(verifyTask)
            }

            // Hook into check and build tasks
            project.tasks.findByName("check")?.let { checkTask ->
                checkTask.dependsOn(verifyTask)
            }

            project.tasks.findByName("build")?.let { buildTask ->
                buildTask.dependsOn(verifyTask)
            }

            // Make report generation run after every build task
            project.tasks.matching { it.name.contains("assemble") || it.name == "build" }.forEach { buildTask ->
                buildTask.finalizedBy(reportTask)
            }
        }

        project.logger.lifecycle("✅ Architecture Verification Plugin applied")
    }
}
