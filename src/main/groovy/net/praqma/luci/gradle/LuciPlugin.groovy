package net.praqma.luci.gradle

import net.praqma.luci.docker.hosts.DockerMachineFactory
import net.praqma.luci.model.JenkinsModel
import net.praqma.luci.model.LuciboxModel
import net.praqma.luci.utils.ClasspathResources
import net.praqma.luci.utils.SystemCheck
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

        extendJenkinsModel(project)

        project.extensions.create('luci', LuciExtension, project)

        def boxes = project.container(LuciboxModel)
        def hosts = project.container(GradleDockerHost)
        def factories = project.container(DockerMachineFactory)

        project.luci.extensions.boxes = boxes
        project.luci.extensions.hosts = hosts
        project.luci.extensions.machineFactories = factories

        project.afterEvaluate {
            createTasks(project)
        }

    }

    void createTasks(Project project) {
        TaskContainer tasks = project.tasks

        // General Luci tasks
        tasks.create('luciSystemCheck') {
            group 'luci'
            description "Check the systems fitness for playing wiht Luci"

            doFirst {
                new SystemCheck(new PrintWriter(System.out)).perform()

                println "Docker host: ${project.luci.defaultHost}"
            }
        }

        // Box specific tasks
        project.luci.boxes.each { LuciboxModel box ->
            if (box.dockerHost == null) {
                box.dockerHost =  project.luci.defaultHost
            }
            box.initialize()
            // Task to generate docker-compose yaml and other things needed
            // to star the lucibox
            String taskNamePrefix = "luci${box.name.capitalize()}"
            String taskGroup = "lucibox ${box.name.capitalize()}"

            tasks.create("${taskNamePrefix}Up") {
                group taskGroup
                description "Bring up '${box.name}'"
                doFirst {
                    File dir = project.file("${project.buildDir}/luciboxes/${box.name}")
                    box.bringUp(dir)
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
                    File dir = project.file("${project.buildDir}/luciboxes/${box.name}")
                    box.printInformation(dir)
                }
            }
        }
    }

    /**
     * Extend the JenkinsModel (that doesn't know about Gradle)
     * with some Gradle friendly methods
     *
     * @param project
     */
    private void extendJenkinsModel(Project project) {
        JenkinsModel.metaClass.initFiles = { Closure copySpec ->
            JenkinsModel model = delegate
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
