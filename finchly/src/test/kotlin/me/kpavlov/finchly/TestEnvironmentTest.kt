package me.kpavlov.finchly

import assertk.assertThat
import assertk.assertions.isEqualTo
import org.junit.jupiter.api.Test

internal class TestEnvironmentTest {
    private val env = TestEnvironment

    @Test
    fun `Should read env variables`() {
        assertThat(env["HOME"]).isEqualTo(System.getProperty("user.home"))
    }
}
