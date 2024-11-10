package me.kpavlov.finchly

import assertk.assertThat
import assertk.assertions.isEqualTo
import org.junit.jupiter.api.Test

private val env =
    BaseTestEnvironment(
        dotEnvFileName = "test.env",
        dotEnvFileDir = "src/test/data",
    )

internal class BaseTestEnvironmentTest {
    @Test
    fun `Should read env variables`() {
        assertThat(env["HOME"]).isEqualTo(System.getProperty("user.home"))
    }

    @Test
    fun `Should prefer env variable`() {
        assertThat(env.get("HOME", "baz")).isEqualTo(System.getProperty("user.home"))
    }

    @Test
    fun `Should read value from dotEnv file`() {
        assertThat(env["FOO"]).isEqualTo("bar")
        assertThat(env.get("FOO", defaultValue = null)).isEqualTo("bar")
    }

    @Test
    fun `Should fallback to provided default value`() {
        assertThat(env.get("NON_EXISTING", "foo")).isEqualTo("foo")
    }
}
