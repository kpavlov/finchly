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
}

object TestEnvironment : AbstractTestEnvironment()
