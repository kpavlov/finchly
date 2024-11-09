package me.kpavlov.finchly

import io.github.cdimascio.dotenv.dotenv
import org.slf4j.LoggerFactory
import java.nio.file.Paths
import kotlin.io.path.isRegularFile

private val logger = LoggerFactory.getLogger(TestEnvironment.javaClass)

/**
 * Represents an abstract environment for testing purposes that can load environment variables
 * from a .env file.
 *
 * @property populateSystemProperties Determines whether to populate system properties
 * with the loaded environment variables.
 */
open class AbstractTestEnvironment(
    private val populateSystemProperties: Boolean = true,
) {
    private val dotenv =
        dotenv {
            val dotenvDir = Paths.get("${System.getProperty("user.dir")}/..")
            val dotenvFile = dotenvDir.resolve(".env")
            if (dotenvFile.isRegularFile()) {
                directory = dotenvDir.normalize().toString()
                logger.info("Loading .env file from $dotenvFile")
            }
            ignoreIfMissing = true
            ignoreIfMalformed = true
            systemProperties = populateSystemProperties
        }

    /**
     * Retrieves the value of an environment variable from the .env file.
     *
     * This method uses the Dotenv library to load the environment variable by its name.
     *
     * @param name The name of the environment variable to retrieve.
     * @return The value of the environment variable as a String, or null if the variable is not found.
     */
    open operator fun get(name: String): String = dotenv[name]

    /**
     * Retrieves the value of an environment variable from the .env file, with an optional default.
     *
     * This method looks for an environment variable with the specified name. If found, it returns
     * its value. If not found and a default value is provided, it returns the default value.
     *
     * @param name The name of the environment variable to retrieve.
     * @param defaultValue An optional default value to return if the environment variable is not found.
     * @return The value of the environment variable, or the default value if provided,
     * or null if the variable is not found and no default is provided.
     */
    open fun get(
        name: String,
        defaultValue: String? = null,
    ): String? =
        if (defaultValue != null) {
            dotenv.get(name, defaultValue)
        } else {
            dotenv[name]
        }
}

object TestEnvironment : AbstractTestEnvironment()
