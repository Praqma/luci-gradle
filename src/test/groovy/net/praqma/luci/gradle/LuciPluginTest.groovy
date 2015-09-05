package net.praqma.luci.gradle

import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.testfixtures.ProjectBuilder
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

                    }
                }
            }
        }

        project.luciInitialize()

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
