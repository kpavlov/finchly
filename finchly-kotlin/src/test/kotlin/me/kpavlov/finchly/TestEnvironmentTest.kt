package me.kpavlov.finchly

import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isNull
import org.junit.jupiter.api.Test

internal class TestEnvironmentTest {
    private val env = TestEnvironment

    @Test
    fun `Should read env variables`() {
        assertThat(env["HOME"]).isEqualTo(System.getenv("HOME"))
    }

    @Test
    fun `Should read value from dotEnv file`() {
        assertThat(env["FOO"]).isNull()
        assertThat(env.get("FOO", defaultValue = null)).isNull()
    }

    @Test
    fun `Should fallback to provided default value`() {
        assertThat(env.get("NON_EXISTING", "foo")).isEqualTo("foo")
    }
}
