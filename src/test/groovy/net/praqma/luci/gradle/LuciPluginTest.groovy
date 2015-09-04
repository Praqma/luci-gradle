package net.praqma.luci.gradle

import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.testfixtures.ProjectBuilder
import org.junit.Test


class LuciPluginTest {

    @Test
    public void testEmptyProject() {
        Project project = ProjectBuilder.builder().build()
        project.pluginManager.apply 'net.praqma.luci'

        project.luciInitialize()

        // Verify we have expected tasks
        assert project.systemCheck instanceof Task
        assert project.listMachineFactories instanceof Task

        // Verify we have expected machine factories
        assert project.luci.machineFactories.virtualBox != null
        assert project.luci.machineFactories.zetta != null

    }
}
