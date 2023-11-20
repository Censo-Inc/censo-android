package co.censo.approver

import co.censo.shared.CensoLink.Companion.ACCESS_TYPE
import co.censo.shared.CensoLink.Companion.INVITE_TYPE
import co.censo.shared.parseLink
import junit.framework.TestCase.assertEquals
import junit.framework.TestCase.assertNull
import org.junit.Assert.assertThrows
import org.junit.Test

class CensoLinkTest {

    private val inviteId = "12345"
    private val partId = "78910"
    private val approvalId = "76123"

    private val v1InvitationLink = "censo://invite/$inviteId"
    private val v1AccessLink = "censo://access/$partId"
    private val v2AccessLink = "censo://access/v2/$partId/$approvalId"

    private val wrongTypeLink = "censo://other/$inviteId"


    @Test
    fun `test v1 invitation link`() {
        val censoLink = v1InvitationLink.parseLink()
        assertEquals(censoLink.type, INVITE_TYPE)
        assertEquals(censoLink.identifiers.mainId, inviteId)
        assertNull(censoLink.identifiers.approvalId)
    }

    @Test
    fun `test v1 access link`() {
        val censoLink = v1AccessLink.parseLink()
        assertEquals(censoLink.type, ACCESS_TYPE)
        assertEquals(censoLink.identifiers.mainId, partId)
        assertNull(censoLink.identifiers.approvalId)
    }

    @Test
    fun `test v2 access link`() {
        val censoLink = v2AccessLink.parseLink()
        assertEquals(censoLink.type, ACCESS_TYPE)
        assertEquals(censoLink.identifiers.mainId, partId)
        assertEquals(censoLink.identifiers.approvalId, approvalId)
    }

    @Test
    fun `test wrong type link`() {
        val exception: java.lang.Exception =
            assertThrows(Exception::class.java) {
                wrongTypeLink.parseLink()
            }

        val expectedMessage = "invalid link"
        val actualMessage = exception.message

        assertEquals(actualMessage, expectedMessage)
    }
}