package net.praqma.luci.gradle

import groovy.transform.Memoized
import net.praqma.luci.docker.DockerHost
import net.praqma.luci.docker.DockerHostImpl
import org.gradle.api.Project

class LuciExtension {

    private Project project

    LuciExtension(Project project) {
        this.project = project
    }

    @Memoized
    DockerHost getDefaultHost() {
        DockerHost host = null
        if (project.hasProperty('dockerMachine')) {
            String dockerMachine = project['dockerMachine']
            project.logger.lifecycle("Default dockerhost is '${dockerMachine}'")
            host = DockerHostImpl.fromDockerMachine(dockerMachine)
        } else {
            host = DockerHostImpl.default
        }
        return host
    }

}
