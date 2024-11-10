package me.kpavlov.finchly

import io.github.cdimascio.dotenv.dotenv

/**
 * Represents an abstract environment for testing purposes that can load environment variables
 * from a .env file.
 *
 * @property populateSystemProperties Determines whether to populate system properties
 * with the loaded environment variables.
 */
open class AbstractTestEnvironment(
    private val populateSystemProperties: Boolean = true,
    private val dotEnvFileDir: String = "./",
    private val dotEnvFileName: String = ".env",
) {
    private val dotenv =
        dotenv {
            filename = dotEnvFileName
            directory = dotEnvFileDir
            ignoreIfMissing = true
            ignoreIfMalformed = true
            systemProperties = populateSystemProperties
        }

    /**
     * Retrieves the value of an environment variable.
     *
     * This method first attempts to fetch the value from the system environment variables.
     * If the variable is not present in the system environment,
     * it then looks for the variable in the `.env` file using the Dotenv library.
     *
     * @param name The name of the environment variable to retrieve.
     * @return The value of the environment variable as a String, or the value from the .env file if
     * the system environment does not contain the variable.
     */
    open operator fun get(name: String): String? {
        val systemEnv = System.getenv(name)
        if (systemEnv != null) {
            return systemEnv
        }
        return dotenv[name]
    }

    /**
     * Retrieves the value of an environment variable, or falls back to a default value if not found.
     *
     * This method first checks the system environment variables and returns the value if it exists.
     * If the variable is not found in the system environment, it looks for the variable in the
     * `.env` file via the Dotenv library.
     *
     * @param name The name of the environment variable to retrieve.
     * @param defaultValue The default value to return if the environment variable is not found.
     * Defaults to `null`.
     * @return The value of the environment variable as a String,
     * or the default value if the variable is not found.
     */
    open fun get(
        name: String,
        defaultValue: String? = null,
    ): String? {
        val systemEnv = System.getenv(name)
        if (systemEnv != null) {
            return systemEnv
        }

        return if (defaultValue != null) {
            dotenv.get(name, defaultValue)
        } else {
            dotenv[name]
        }
    }
}

object TestEnvironment : AbstractTestEnvironment()
