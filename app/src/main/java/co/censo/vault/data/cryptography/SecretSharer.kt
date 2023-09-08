package co.censo.vault.data.cryptography

import java.math.BigInteger

data class Point(val x: BigInteger, val y: BigInteger) {
    constructor(x: String, y: String) : this(BigInteger(x), BigInteger(y))
    constructor(x: BigInteger, y: String) : this(x, BigInteger(y))
    constructor(x: String, y: BigInteger) : this(BigInteger(x), y)
}

typealias Vector = Array<BigInteger>
typealias Matrix = Array<Vector>

val ORDER = BigInteger("13407807929942597099574024998205846127479365820592393377723561443721764030073546976801874298166903427690031858186486050853753882811946569946433649006083527")
val rnd = java.security.SecureRandom()

object SecretSharerUtils {
    fun randomFieldElement(order: BigInteger): BigInteger {
        var randomNumber: BigInteger
        BigInteger(order.bitLength(), rnd)
        do {
            randomNumber = BigInteger(order.bitLength(), rnd)
        } while (randomNumber >= order)
        return randomNumber
    }

    fun vandermonde(participants: List<BigInteger>, threshold: Int, order: BigInteger): Matrix {
        return Matrix(participants.size) { p ->
            Vector(threshold) { t ->
                participants[p].modPow(t.toBigInteger(), order)
            }
        }
    }

    fun dotProduct(matrix: Matrix, vector: Vector, order: BigInteger): Vector {
        val result = Vector(matrix.size) { BigInteger.ZERO }
        matrix.forEachIndexed { i, row ->
            row.forEachIndexed { j, value ->
                result[i] = (result[i] + (value * vector[j])).mod(order)
            }
        }
        return result
    }

    fun recoverSecret(shares: List<Point>, order: BigInteger): BigInteger {
        val van = vandermonde(shares.map { it.x }, shares.size, order)
        val(lu, p) = decomposeLUP(van, order)
        val inverse = invertLUP(lu, p, order)
        return addShares(shares.map { it.y }, inverse[0].asList(), order)
    }

    fun addShares(shares: List<BigInteger>, weights: List<BigInteger>, order: BigInteger): BigInteger {
        return shares.mapIndexed { i, s -> s * weights[i] }.reduce { a, b -> (a + b).mod(order) }
    }

    /*
     *  Division in integers modulus p means finding the inverse of the
     *  denominator modulo p and then multiplying the numerator by this
     *  inverse (Note: inverse of A is B such that A*B % p == 1). This can
     *  be computed via the extended Euclidean algorithm
     *  http://en.wikipedia.org/wiki/Modular_multiplicative_inverse#Computation
     */

    private fun divMod(numerator: BigInteger, denominator: BigInteger, order: BigInteger): BigInteger {
        val inverse = extendedGCD(denominator, order)
        return numerator * inverse
    }

    private fun extendedGCD(aIn: BigInteger, bIn: BigInteger): BigInteger {
        var a = BigInteger(1, aIn.toByteArray())
        var b = BigInteger(1, bIn.toByteArray())
        var x = BigInteger.ZERO
        var lastX = BigInteger.ONE
        var y = BigInteger.ONE
        var lastY = BigInteger.ZERO
        while (b != BigInteger.ZERO) {
            val quotient = a.div(b)
            a = b.also {
                b = a.mod(b)
            }
            lastX = x.also {
                x = lastX - quotient * x
            }
            lastY = y.also {
                y = lastY - quotient * x
            }
        }
        return lastX
    }

    // below inspired by https://en.wikipedia.org/wiki/LU_decomposition#C_code_example
    /*
     * Decomposes matrix into an LUP factorization, returning LU as a single matrix,
     * and P as a vector
     */
    fun decomposeLUP(matrix: Matrix, order: BigInteger): Pair<Matrix, Vector> {
        val n = matrix.size
        matrix.forEach { row -> require(row.size == n) { "Matrix must be square" } }
        val lu = matrix.clone()
        // Unit permutation matrix, p[i] initialized with i
        val p = Vector(n) { i -> BigInteger.valueOf(i.toLong()) }

        (0 until n).forEach { i ->
            var maxA = BigInteger.ZERO
            var iMax = i
            (i until n).forEach { k ->
                val absA = matrix[k][i].abs()
                if (absA > maxA) {
                    maxA = absA
                    iMax = k
                }
            }

            require(maxA > BigInteger.ZERO) { "Matrix is degenerate" }

            if (iMax != i) {
                // pivoting P
                p[i] = p[iMax].also {
                    p[iMax] = p[i]
                }

                // pivoting rows of A
                lu[i] = lu[iMax].also {
                    lu[iMax] = lu[i]
                }
            }

            (i + 1 until n).forEach { j ->
                lu[j][i] = divMod(lu[j][i], lu[i][i], order).mod(order)
                (i + 1 until n).forEach { k ->
                    lu[j][k] = (lu[j][k] - lu[j][i] * lu[i][k]).mod(order)
                }
            }
        }

        return Pair(lu, p)
    }

    /*
     * Takes an LU matrix and a P permutation vector, returns the inverse matrix
     */
    fun invertLUP(lu: Matrix, p: Vector, order: BigInteger): Matrix {
        val n = lu.size
        val inverse = Matrix(n) { Vector(n) { BigInteger.ZERO } }
        (0 until n).forEach { j ->
            (0 until n).forEach { i ->
                inverse[i][j] = if (p[i].toInt() == j) BigInteger.ONE else BigInteger.ZERO
                (0 until i).forEach { k ->
                    inverse[i][j] = inverse[i][j] - lu[i][k] * inverse[k][j]
                    inverse[i][j] = inverse[i][j].mod(order)
                }
            }
            ((n - 1) downTo 0).forEach { i ->
                ((i + 1) until n).forEach { k ->
                    inverse[i][j] = inverse[i][j] - lu[i][k] * inverse[k][j]
                    inverse[i][j] = inverse[i][j].mod(order)
                }
                inverse[i][j] = divMod(inverse[i][j], lu[i][i], order)
                inverse[i][j] = inverse[i][j].mod(order)
            }
        }
        return inverse
    }
}

class SecretSharer(val secret: BigInteger, val threshold: Int, val participants: List<BigInteger>, val order: BigInteger = ORDER) {
    val shards : List<Point> = getShares(participants, threshold, secret)

    fun vandermonde(participants: List<BigInteger>, threshold: Int) = SecretSharerUtils.vandermonde(participants, threshold, order)
    fun dotProduct(matrix: Matrix, vector: Vector) = SecretSharerUtils.dotProduct(matrix, vector, order)
    fun invertLUP(lu: Matrix, p: Vector) = SecretSharerUtils.invertLUP(lu, p, order)
    fun decomposeLUP(matrix: Matrix) = SecretSharerUtils.decomposeLUP(matrix, order)
    fun addShards(shares: List<BigInteger>, weights: List<BigInteger>) = SecretSharerUtils.addShares(shares, weights, order)
    fun recoverSecret(shares: List<Point>) = SecretSharerUtils.recoverSecret(shares, order)

    fun getShares(participants: List<BigInteger>, threshold: Int, secret: BigInteger): List<Point> {
        if (threshold > participants.size) {
            throw Exception("secret would be irrecoverable.")
        }
        val vec = Vector(threshold) { if (it == 0) secret else SecretSharerUtils.randomFieldElement(order) }
        val rowShares = dotProduct(vandermonde(participants, threshold), vec)
        return participants.mapIndexed { i, p ->
            Point(p, rowShares[i])
        }
    }

    fun getReshares(newParticipants: List<BigInteger>, newThreshold: Int): List<List<Point>> {
        // generate the reshares from the first threshold participants
        return participants.take(threshold).indices.map { i ->
            getShares(newParticipants, newThreshold, shards[i].y)
        }
    }
}