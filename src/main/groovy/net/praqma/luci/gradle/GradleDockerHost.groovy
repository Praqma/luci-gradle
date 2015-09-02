package net.praqma.luci.gradle

import groovy.transform.CompileStatic
import net.praqma.luci.docker.DockerHost
import net.praqma.luci.docker.DockerHostImpl
import net.praqma.luci.docker.DockerMachineFactory
import net.praqma.luci.docker.net.praqma.luci.docker.hosts.DockerMachineHost

@CompileStatic
class GradleDockerHost {

    String name

    @Delegate
    DockerHost dockerHost

    GradleDockerHost(String name) {
        this.name = name
    }

    void dockerMachine(String name) {
        dockerHost = new DockerMachineHost(name)
    }

    void dockerMachine(Map map) {
        String machineName = map.name as String ?: name

        DockerMachineFactory factory = map.factory as DockerMachineFactory
        dockerHost = new DockerMachineHost(machineName, factory)
    }

    String toString() {
        return dockerHost ? dockerHost.toString() : super.toString()
    }
}
