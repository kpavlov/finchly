package me.kpavlov.finchly

import assertk.assertThat
import assertk.assertions.isEqualTo
import org.junit.jupiter.api.Test

internal class TestEnvironmentTest {
    private val env =
        AbstractTestEnvironment(
            dotEnvFileName = "test.env",
            dotEnvFileDir = "src/test/data",
        )

    @Test
    fun `Should read env variables`() {
        assertThat(env["HOME"]).isEqualTo(System.getProperty("user.home"))
    }

    @Test
    fun `Should read value from dotEnv file`() {
        assertThat(env["FOO"]).isEqualTo("bar")
    }

    @Test
    fun `Should fallback to provided default value`() {
        assertThat(env.get("NON_EXISTING", "foo")).isEqualTo("foo")
    }
}
