package co.censo.censo.util

/**
 * Used to set System properties to the JVM to indicate if unit tests are running
 *
 * Main use case is to allow for direct control of UI state in VMs while unit tests are running
 */
object TestUtil {
    const val TEST_MODE = "TEST_MODE"
    const val TEST_MODE_TRUE = "TRUE"
}