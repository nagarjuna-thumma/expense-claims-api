package dev.nagarjuna.claims.audit

import dev.nagarjuna.claims.TestcontainersConfig
import dev.nagarjuna.claims.domain.ClaimService
import dev.nagarjuna.claims.security.CurrentUser
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.context.annotation.Import
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.test.annotation.DirtiesContext
import org.testcontainers.junit.jupiter.Testcontainers
import java.math.BigDecimal
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Checks the audit chain against a real Postgres: concurrent writers, the append-only trigger,
 * and detection of a row edited behind the application's back.
 */
@SpringBootTest
@Import(TestcontainersConfig::class)
@Testcontainers(disabledWithoutDocker = true)
@DirtiesContext
class AuditChainTest(
    @Autowired val claims: ClaimService,
    @Autowired val audit: AuditLog,
    @Autowired val jdbc: JdbcTemplate,
) {

    private val alice = CurrentUser("alice", "alice", isManager = false)
    private val bob = CurrentUser("bob", "bob", isManager = true)

    @Test
    fun `concurrent writers produce one unbroken chain`() {
        val pool = Executors.newFixedThreadPool(8)
        repeat(40) { i ->
            pool.submit {
                val claim = claims.submit(alice, "Train ticket $i", BigDecimal("10.00").add(BigDecimal(i)))
                if (i % 2 == 0) claims.decide(bob, claim.id, approve = i % 4 == 0)
            }
        }
        pool.shutdown()
        assertTrue(pool.awaitTermination(60, TimeUnit.SECONDS))

        val result = audit.verify()
        assertTrue(result.valid, result.problem)
        assertEquals(jdbc.queryForObject("SELECT max(seq) FROM claim_audit_log", Long::class.java), result.entries)
        assertEquals(result.entries, jdbc.queryForObject("SELECT count(*) FROM claim_audit_log", Long::class.java))
    }

    @Test
    fun `the database refuses to change or delete audit rows`() {
        claims.submit(alice, "Hotel", BigDecimal("180.00"))

        assertThrows<Exception> { jdbc.update("UPDATE claim_audit_log SET amount = 1") }
        assertThrows<Exception> { jdbc.update("DELETE FROM claim_audit_log") }
        assertThrows<Exception> { jdbc.execute("TRUNCATE claim_audit_log") }
    }

    @Test
    fun `an edit made behind the application's back is detected`() {
        val claim = claims.submit(alice, "Client dinner", BigDecimal("95.00"))
        claims.decide(bob, claim.id, approve = true)
        val approvedSeq = jdbc.queryForObject(
            "SELECT seq FROM claim_audit_log WHERE claim_id = ? AND action = 'APPROVED'", Long::class.java, claim.id)

        // Someone with direct database access turns off the trigger and raises the approved amount.
        tamper("UPDATE claim_audit_log SET amount = 9500.00 WHERE seq = $approvedSeq")
        try {
            val result = audit.verify()
            assertFalse(result.valid)
            assertEquals(approvedSeq, result.firstBadSeq)
            assertEquals("contents do not match the stored hash", result.problem)
        } finally {
            tamper("UPDATE claim_audit_log SET amount = 95.00 WHERE seq = $approvedSeq")
        }
        assertTrue(audit.verify().valid)
    }

    private fun tamper(sql: String) {
        jdbc.execute("ALTER TABLE claim_audit_log DISABLE TRIGGER claim_audit_log_no_update_delete")
        jdbc.execute(sql)
        jdbc.execute("ALTER TABLE claim_audit_log ENABLE TRIGGER claim_audit_log_no_update_delete")
    }
}
