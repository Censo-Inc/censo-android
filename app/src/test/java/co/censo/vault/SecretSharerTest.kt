package co.censo.vault

import co.censo.vault.data.cryptography.Matrix
import co.censo.vault.data.cryptography.ORDER
import co.censo.vault.data.cryptography.Point
import co.censo.vault.data.cryptography.SecretSharer
import co.censo.vault.data.cryptography.SecretSharerUtils
import co.censo.vault.data.cryptography.SecretSharerUtils.randomFieldElement
import org.junit.Assert.*
import java.math.BigInteger
import java.util.UUID
import kotlin.random.Random
import org.junit.Test

class SecretSharerTest {

    @Test
    fun `test secret sharer sequential small order`() {
        val rnd = java.security.SecureRandom()
        val order = 65537
        val secret = BigInteger(16, rnd)
        val secretSharer = SecretSharer(
            secret, 3, (1..6).map { BigInteger.valueOf(it.toLong()) }, order.toBigInteger()
        )
        assertEquals(
            secret,
            secretSharer.recoverSecret(
                listOf(
                    secretSharer.shards[0],
                    secretSharer.shards[1],
                    secretSharer.shards[2],
                )
            )
        )
    }

    @Test
    fun `test secret sharer one participant`() {
        val rnd = java.security.SecureRandom()
        val secret = BigInteger(ORDER.bitLength(), rnd)
        val secretSharer = SecretSharer(
            secret, 1, listOf(BigInteger.valueOf(1))
        )
        assertEquals(
            secret,
            secretSharer.recoverSecret(
                listOf(
                    secretSharer.shards[0],
                )
            )
        )
        assertEquals(
            secret,
            secretSharer.shards[0].y,
        )
    }

    @Test
    fun `test secret sharer two participant and 64 byte number`() {

        val secret = BigInteger(
            "a3a4a523f3fcd16ab61fb7eba989e7b4155a5f960eb30877a5a4fdeaa7b8fd8373eb765067c15c50803bd5d141fa1b1a43fc7415bc664d34d6b3ce14db67daee",
            16
        )
        val secretSharer = SecretSharer(
            secret, 2, listOf(BigInteger.valueOf(1), BigInteger.valueOf(2))
        )
        assertEquals(
            secret,
            secretSharer.recoverSecret(
                listOf(
                    secretSharer.shards[0],
                    secretSharer.shards[1],
                )
            )
        )
    }

    @Test
    fun `test secret sharer random`() {
        val rnd = java.security.SecureRandom()
        val secret = BigInteger(ORDER.bitLength(), rnd)
        val secretSharer = SecretSharer(
            secret, 3, (1..6).map { BigInteger.valueOf(Random.nextLong()) }
        )
        assertEquals(
            secret,
            secretSharer.recoverSecret(
                listOf(
                    secretSharer.shards[0], secretSharer.shards[1], secretSharer.shards[2]
                )
            )
        )
        assertEquals(
            secret,
            secretSharer.recoverSecret(
                listOf(
                    secretSharer.shards[2], secretSharer.shards[4], secretSharer.shards[5]
                )
            )
        )
        assertNotEquals(
            secret,
            secretSharer.recoverSecret(
                listOf(
                    secretSharer.shards[2],
                    secretSharer.shards[4],
                )
            )
        )
    }

    private fun assertMatrix(matrix: Matrix, vararg expected: Long) {
        var i = 0
        matrix.forEach { row ->
            row.forEach { value ->
                assertEquals(value, BigInteger.valueOf(expected[i]))
                i += 1
            }
        }
    }

    @Test
    fun `test matrix inversion`() {
        val secretSharer = SecretSharer(
            BigInteger.ONE,
            1,
            listOf(BigInteger.ONE),
            order = BigInteger.valueOf(65537)
        )
        val vandermonde =
            secretSharer.vandermonde(listOf(7, 8, 9, 10).map { BigInteger.valueOf(it.toLong()) }, 4)
        val (lu, p) = secretSharer.decomposeLUP(vandermonde)
        val inverse = secretSharer.invertLUP(lu, p)
        assertMatrix(
            inverse,
            120, 65222, 280, 65453,
            43651, 32880, 65434, 54646,
            32773, 65524, 32781, 65533,
            54614, 32769, 32768, 10923,
        )
    }
}

data class Shard(
    val sid: Sid,
    val pid: Pid,
    val threshold: Int,
    val shard: BigInteger,
    val revision: Revision,
    val email: String,
    val parentShards: List<Shard>? = null
)

class ShardStore {
    val shards: MutableSet<Shard> = mutableSetOf()

    fun getShards(
        sid: Set<Sid>? = null,
        pid: Set<Pid>? = null,
        email: Set<String>? = null,
        revision: Set<Revision>? = null,
        isReshare: Boolean? = null,
        parentPid: Pid? = null
    ): List<Shard> {
        return shards.filter { shard ->
            (
                    sid?.let {
                        sid.contains(shard.sid)
                    } ?: true
                    ) &&
                    (
                            pid?.let {
                                pid.contains(shard.pid)
                            } ?: true
                            ) &&
                    (
                            email?.let {
                                email.contains(shard.email)
                            } ?: true
                            ) &&
                    (
                            revision?.let {
                                revision.contains(shard.revision)
                            } ?: true
                            ) &&
                    (
                            when (isReshare) {
                                true -> (shard.parentShards?.size ?: 0) > 0
                                false -> (shard.parentShards?.size ?: 0) == 0
                                else -> true
                            }
                            ) &&
                    (
                            parentPid?.let {
                                (shard.parentShards ?: listOf()).firstOrNull()?.pid == it
                            } ?: true
                            )
        }
    }

    fun replaceShare(policy: Policy, email: String, parentShards: List<Shard>, shard: BigInteger) {
        val existingShards = getShards(
            revision = setOf(policy.revision),
            pid = setOf(parentShards.first().pid),
            email = setOf(email),
            parentPid = if (parentShards.size > 1) parentShards[1].pid else null
        )
        assertEquals(1, existingShards.size)
        shards.remove(existingShards[0])
        shards.add(
            Shard(
                existingShards[0].sid,
                parentShards.first().pid,
                policy.threshold,
                shard,
                policy.revision,
                email,
                parentShards.slice(1 until parentShards.size)
            )
        )
    }
}

typealias Key = String
typealias Sid = String
typealias Pid = BigInteger
typealias Revision = String

data class User(val email: String, val pids: MutableList<Pid>)

data class Device(val user: User, val key: Key, val pid: Pid)

data class Policy(val approvers: List<Device>, val threshold: Int, val revision: Revision) {
    val participants = approvers.map { it.pid }
    fun shareSecret(secret: BigInteger) = SecretSharer(secret, threshold, participants).shards
    fun reshareShard(shard: Shard, policy: Policy) =
        SecretSharer(shard.shard, policy.threshold, policy.participants).shards
}

data class Org(
    val shardStore: ShardStore,
    val orgRecoveryKey1: Key,
    val orgRecoveryKey2: Key,
    val orgRecoveryKey3: Key
) {
    private val pids = (1..3).map { randomFieldElement(ORDER) }
    fun shareSecret(secret: BigInteger) = SecretSharer(secret, 2, pids).shards
    fun recoverShards(policy: Policy, which: Int, adminPid: Pid) = shardStore.getShards(
        revision = setOf(policy.revision), pid = setOf(pids[which]), parentPid = adminPid
    )

    fun recoverSeed(user: User, which: Int) = shardStore.getShards(
        email = setOf(user.email), pid = setOf(pids[which]), isReshare = false
    )[0]
}

class MobileApp(val user: User, val org: Org, val shardStore: ShardStore) {
    private val rnd = java.security.SecureRandom()
    var rootSeed = BigInteger(ORDER.bitLength(), rnd)
    val deviceKey = "dev-${UUID.randomUUID()}"
    var participantId = randomFieldElement(ORDER)
    val device = Device(user, deviceKey, participantId)

    init {
        user.pids.add(participantId)
    }

    fun shareToAdmins(policy: Policy): Sid {
        val shareId = "sid-share-${UUID.randomUUID()}"
        val orgShareId = "$shareId-org"
        shardStore.shards.addAll(
            policy.shareSecret(rootSeed).map { point ->
                Shard(shareId, point.x, policy.threshold, point.y, policy.revision, user.email)
            }.toList() + org.shareSecret(rootSeed).map { orgPoint ->
                Shard(orgShareId, orgPoint.x, 2, orgPoint.y, policy.revision, user.email)
            }
        )
        return shareId
    }

    fun approvePolicyRevision(oldPolicy: Policy, newPolicy: Policy): List<Sid> {
        return shardStore.getShards(
            revision = setOf(oldPolicy.revision),
            pid = setOf(participantId)
        ).map { shard ->
            val shareId = "sid-reshare-${UUID.randomUUID()}"
            shardStore.shards.addAll(
                oldPolicy.reshareShard(shard, newPolicy).flatMap { point ->
                    Shard(
                        shareId,
                        point.x,
                        newPolicy.threshold,
                        point.y,
                        newPolicy.revision,
                        shard.email,
                        listOf(shard) + (shard.parentShards ?: emptyList())
                    ).let {
                        val orgShareId = "$shareId-org"
                        listOf(it) + org.shareSecret(point.y).map { orgPoint ->
                            Shard(
                                orgShareId,
                                orgPoint.x,
                                2,
                                orgPoint.y,
                                newPolicy.revision,
                                shard.email,
                                listOf(it, shard) + (shard.parentShards ?: emptyList())
                            )
                        }
                    }
                }.toList()
            )
            shareId
        }
    }

    fun approveDeviceRecovery(policy: Policy, app: MobileApp) =
        shardStore.getShards(
            revision = setOf(policy.revision),
            email = setOf(app.user.email),
            pid = setOf(participantId)
        )

    fun recoverSeed(shards: List<Shard>) {
        var remainingShardGroups = shards.groupBy { it.sid }
        while (remainingShardGroups.size > 1) {
            remainingShardGroups = remainingShardGroups.flatMap {
                if (it.value[0].parentShards == null) {
                    it.value
                } else {
                    val exemplar = it.value[0].parentShards!![0]
                    listOf(
                        Shard(
                            exemplar.sid,
                            exemplar.pid,
                            exemplar.threshold,
                            SecretSharerUtils.recoverSecret(
                                it.value.map { s -> Point(s.pid, s.shard) },
                                ORDER
                            ),
                            exemplar.revision,
                            exemplar.email,
                            exemplar.parentShards
                        )
                    )
                }
            }.groupBy { it.sid }
        }
        rootSeed = SecretSharerUtils.recoverSecret(
            remainingShardGroups.values.first().map { s -> Point(s.pid, s.shard) },
            ORDER
        )
    }

    fun recoverSeedAndShards(
        policy: Policy,
        org1Shards: List<Shard>,
        org2Shards: List<Shard>,
        org1Seed: Shard,
        org2Seed: Shard
    ) {
        rootSeed = SecretSharerUtils.recoverSecret(
            listOf(org1Seed, org2Seed).map { Point(it.pid, it.shard) },
            ORDER
        )
        val allShards = org1Shards + org2Shards
        assert(allShards.map { it.parentShards!![0].pid }.toSet().size == 1)
        participantId = allShards[0].parentShards!![0].pid
        allShards.groupBy { it.sid }.map {
            val shard = SecretSharerUtils.recoverSecret(
                it.value.map { s -> Point(s.pid, s.shard) },
                ORDER
            )
            shardStore.replaceShare(policy, it.value[0].email, it.value[0].parentShards!!, shard)
        }
    }
}
