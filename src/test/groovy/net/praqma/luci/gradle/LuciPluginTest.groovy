package net.praqma.luci.gradle

import net.praqma.luci.test.TestDockerHosts
import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.testfixtures.ProjectBuilder
import org.junit.Ignore
import org.junit.Test


class LuciPluginTest {

    @Test
    void testEmptyProject() {
        Project project = testProject()
        project.luciInitialize()

        // Verify we have expected tasks
        assert project.systemCheck instanceof Task
        assert project.listMachineFactories instanceof Task

        // Verify we have expected machine factories
        assert project.luci.machineFactories.virtualBox != null
        assert project.luci.machineFactories.zetta != null

        // There should be a hosts extension
        assert project.luci.hosts != null
    }

    @Test
    void testWithLucibox() {
        Project project = testProject()

        project.with {
            luci {
                boxes {
                    foo {
                        dockerHost = TestDockerHosts.primary
                        service('jenkins') {
                            // Add groovy files in the jenkinsInit.d dir to the jenkins init.d dir
                            initFiles fileTree(dir: 'jenkinsInit.d', include: '**/*.groovy')

                            // You can also specify files with a Gradle CopySpec
                            // See https://docs.gradle.org/current/userguide/working_with_files.html for some examples
                            initFiles {
                                // Copy all templates files from jenkinsInit.d
                                from 'jenkinsInit.d'
                                include '*.template'
                                // Change extension from .template to  .groovy
                                rename '(.*)\\.template', '$1\\.groovy'
                                // Insert the project name in templates
                                expand(name: name)
                            }

                            seedJob.with {
                                name = 'dummy'
                            }
                        }
                    }
                }
            }
        }

        project.luciInitialize()
        project.luci.boxes.foo.preStart()
        project.luci.boxes.foo.printInformation()

        // Verify we have expected tasks
        assert project.fooUp instanceof Task
        assert project.fooDown instanceof Task
        assert project.fooInfo instanceof Task
        assert project.fooDestroy instanceof Task

    }


    private Project testProject() {
        Project project = ProjectBuilder.builder().build()
        project.pluginManager.apply 'net.praqma.luci'

        return project
    }
}
