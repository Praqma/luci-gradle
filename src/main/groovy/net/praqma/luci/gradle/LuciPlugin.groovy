package net.praqma.luci.gradle

import groovyx.gpars.GParsPool
import net.praqma.luci.docker.DockerHost
import net.praqma.luci.docker.hosts.DockerMachineFactory
import net.praqma.luci.model.JenkinsModel
import net.praqma.luci.model.LuciboxModel
import net.praqma.luci.utils.ClasspathResources
import net.praqma.luci.utils.ExternalCommand
import net.praqma.luci.utils.LuciSettings
import net.praqma.luci.utils.SystemCheck
import org.gradle.api.GradleException
import org.gradle.api.NamedDomainObjectContainer
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.file.CopySpec
import org.gradle.api.tasks.TaskContainer

class LuciPlugin implements Plugin<Project> {

    @Override
    void apply(Project project) {
        project.apply(plugin: 'base')

        // Define directory where resources are extracted from jar
        ClasspathResources.extractedResoucesDir = new File("${project.buildDir}/extractedResources")

        enhanceJenkinsModel(project)

        project.extensions.create('luci', LuciExtension, project)

        def boxes = project.container(LuciboxModel)
        def hosts = project.container(GradleDockerHost)
        def factories = project.container(DockerMachineFactory)

        project.luci.extensions.boxes = boxes
        project.luci.extensions.hosts = hosts
        project.luci.extensions.machineFactories = factories


        setupFactories(factories)

        // Make luciInitialize available as extra property. This is
        // used in unit tests
        project.ext.luciInitialize = { luciInitialize(project) }

        project.afterEvaluate {
            luciInitialize(project)
        }

    }

    void setupFactories(NamedDomainObjectContainer<DockerMachineFactory> factories) {
        // Complete VirtualBox factory
        factories.create('virtualBox') { driver ='virtualbox' }

        // Some meaningful defaults for Zetta.io
        factories.create('zetta') {
            driver = 'openstack'

            addProperty 'username', 'openstack-username'
            addProperty 'password', 'openstack-password'
            addProperty 'domainId', 'openstack-domain-id'
            addProperty 'tenantName', 'openstack-tenant-name'


            options 'openstack-flavor-id': '6',
                    'openstack-image-id': 'd0a89aa8-9644-408d-a023-4dcc1148ca01',
                    'openstack-floatingip-pool': 'Public',
                    'openstack-ssh-user': 'ubuntu',
                    'openstack-net-name': 'Private',
                    'openstack-sec-groups': 'default,DockerAPI',
                    'openstack-auth-url': 'https://identity.api.zetta.io/v3',
                    'openstack-region': 'no-osl1',
                    'openstack-tenant-name': '${lookup("zetta.tenantName", "Standard")}',
                    'openstack-domain-id': '${lookup("zetta.domainId")}',
                    'openstack-username': '${lookup("zetta.username")}',
                    'openstack-password': '${lookup("zetta.password")}'
        }
    }

    /**
     * Initialize the project as a Luci project.
     * <p>
     * This method must be call after the project is evaluated (i.e. when all configuration is done).
     * It will create Luci specific tasks, and other initialization related to Luci.
     */
    void luciInitialize(Project project) {
        project.luci.machineFactories.each { DockerMachineFactory f ->
            f.bindings.settings = LuciSettings.instance
            f.bindings.project = project
            f.bindings.luci = project.luci
            // Define lookup function. Looking in System properties, project project and Luci settings
            // TODO consider order of lookup
            f.bindings.lookup = { key, defaultValue = null ->
                f.properties[key] ?: System.properties[key] ?:
                        project.properties[key] ?: LuciSettings.instance[key] ?:
                                defaultValue ?: {
                            throw new GradleException("Property '${key}' not defined")
                        }()
            }
        }

        createTasks(project)
    }

    private void createTasks(Project project) {
        TaskContainer tasks = project.tasks

        // General Luci tasks
        tasks.create('systemCheck') {
            group 'luci'
            description "Check the systems fitness for playing wiht Luci"

            doLast {
                new SystemCheck(new PrintWriter(System.out)).perform()

                println "Docker host: ${project.luci.defaultHost}"
            }
        }

        tasks.create('listMachineFactories') {
            group 'luci'
            description "List all defined Docker Machine factories"

            doLast {
                String header = "Defined Machine Factories"
                println "\n${header}\n${'=' * header.length()}\n"

                project.luci.machineFactories.each { DockerMachineFactory factory ->
                    String cmdLine
                    try {
                        List<String> list = factory.commandLine('<name>')
                        ExternalCommand ec = new ExternalCommand()
                        ec.sensitiveData = factory.sensitiveData()
                        cmdLine = ec.formatCmdForLogging(list)
                    } catch (Exception e) {
                        cmdLine = "error: ${e.message}"
                    }
                    println "  ${factory.name} : ${cmdLine}"
                }
            }
        }

        tasks.create('listAllHosts') {
            group 'luci'
            description 'List all defined Docker hosts'

            doLast {
                String header = "Defined Docker Hosts"
                println "\n${header}\n${'=' * header.length()}\n"

                project.luci.hosts.each { DockerHost host ->
                    println "  ${host.name} : ${host} - ${host.status}"
                }
            }
        }

        tasks.create('initializeAllHosts') {
            group 'luci'
            description 'Initialize all defined Docker hosts'
            dependsOn tasks.listAllHosts

            doLast {
                GParsPool.withPool {
                    project.luci.hosts.eachParallel { DockerHost host ->
                        host.initialize()
                    }
                }
            }
        }

        // Box specific tasks
        project.luci.boxes.each { LuciboxModel box ->
            if (box.dockerHost == null) {
                box.dockerHost =  project.luci.defaultHost
            }
            box.initialize(project.file("${project.buildDir}/luciboxes/${box.name}"))
            // Task to generate docker-compose yaml and other things needed
            // to star the lucibox
            String taskNamePrefix = box.name
            String taskGroup = "lucibox ${box.name.capitalize()}"

            tasks.create("${taskNamePrefix}Up") {
                group taskGroup
                description "Bring up '${box.name}'"
                doFirst {
                    box.bringUp()
                }
            }

            tasks.create("${taskNamePrefix}Down") {
                group taskGroup
                description "Take down '${box.name}'"
                doFirst {
                    box.takeDown()
                }
            }

            tasks.create("${taskNamePrefix}Destroy") {
                group taskGroup
                description "Destroy '${box.name}', delete all containers includeing data containers"
                doFirst {
                    box.destroy()
                }
            }

            tasks.create("${taskNamePrefix}Info") {
                group taskGroup
                description "Miscellaneous information about '${box.name}'"
                doFirst {
                    box.printInformation()
                }
            }
        }
    }

    /**
     * Enhance the JenkinsModel (that doesn't know about Gradle)
     * with some Gradle friendly methods
     *
     * @param project
     */
    private void enhanceJenkinsModel(Project project) {
        JenkinsModel.metaClass.initFiles = { Closure copySpec ->
            JenkinsModel model = delegate
            // A closure that will use the copySpec to copy/create the files and them
            // add them as java.util.File objects to initFiles of the JenkinsModel
            Closure action = {
                CopySpec spec = project.copySpec(copySpec)
                File workDir = new File(project.buildDir, "luciboxes/${model.box.name}/initFiles${new Random().nextInt()}${System.nanoTime()}")
                workDir.mkdirs()
                project.copy {
                    into workDir
                    with spec
                }
                model.initFiles project.fileTree(dir: workDir)
            }
            model.addPreStartAction(action)
        }
    }

}
