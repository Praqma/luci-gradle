# LUCI Understands Continuous Integration

## Running LUCI

### Clone LUCI

From https://github.com/Praqma/luci.git clone the gradle branch

### Build Images

Luci provides a number of images. The intent is they will be push the  Docker hub, but currently you have to build them on the target Docker host.
You build the images with gradle task 'luciBuildAllImages'. So execute:

./gradlew luciBuildAllImages

Important: When you pull  be sure to rebuild images, there might be changes. 

### Spefifying Docker host for the Lucibox

As default the Lucibox will be created on the Docker host specified by the environment variables (DOCKER_HOST etc).
It is possible to specify a Docker machine to create the Lucibox on. A default Docker machine can be
specified by the Gradle Project Property 'dockerMachine'. E.g. with '-PdockerMachine=MyLuciHost' on the command line,
see http://mrhaki.blogspot.dk/2010/09/gradle-goodness-different-ways-to-set.html for other possibilities.

### Check your environment

You must have Java installed to execute the Gradle script.

Execute './gradlew luciCheckSystem' to make Luci check if your system is ready for Luci. Make sure you have the
required tools installed, and your version isn't too old.

### Start and stop a Luci box

In the build.gradle file a few example Luciboxes are defined. If you want to spin up 'demo' you execute
'''./gradlew luciDemoUp'''

The script prints a URL where you can reach the Lucibox. The page shows links to the enabled services.
Note that Jenkins is rather slow to start and you might get a "503 Service not available" in the beginning.

If you change the configuration you apply the changes by spinning it up again.

To kill it you stop the containers.

### Gradle Implementation notes

The Luci Gradle plugin should be distributed as a standalone plugin, so you can make any number of Luci Gradle configuration files. But currently you build the plugin whenever you use Luci, that is making the development much more efficient.

### Dependencies

You must have the following installed on the box where you execute the gradle script:
* Java
* Docker
* Docker compose: Must be 1.4.0 or newer
* Docker machine: I'm not sure about this one, but it should be possible ot make it work without
