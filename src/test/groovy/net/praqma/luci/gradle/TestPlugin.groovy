package net.praqma.luci.gradle

import net.praqma.luci.docker.Images
import org.junit.Test


class TestPlugin {


    @Test
    void test() {
        Images x = Images.DATA
        println x
    }
}
