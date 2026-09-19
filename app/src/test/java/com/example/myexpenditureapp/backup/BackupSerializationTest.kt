package com.example.myexpenditureapp.backup

import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import org.junit.Assert.*
import org.junit.Test
import java.math.BigDecimal

data class AccountBackupDto(
    val id: Long,
    val name: String,
    val type: String,
    val balance: String
)

data class BackupRootDto(
    val version: Int,
    val exportTimestamp: Long,
    val accounts: List<AccountBackupDto>
)

class BackupSerializationTest {

    private val moshi = Moshi.Builder()
        .add(KotlinJsonAdapterFactory())
        .build()

    @Test
    fun testJsonExportSerialization() {
        val root = BackupRootDto(
            version = 1,
            exportTimestamp = 1726700000000L,
            accounts = listOf(
                AccountBackupDto(id = 1L, name = "HDFC Savings", type = "Bank", balance = "25000.50")
            )
        )

        val adapter = moshi.adapter(BackupRootDto::class.java)
        val jsonString = adapter.toJson(root)

        assertTrue(jsonString.contains("HDFC Savings"))
        assertTrue(jsonString.contains("25000.50"))

        val deserialized = adapter.fromJson(jsonString)
        assertNotNull(deserialized)
        assertEquals(1, deserialized!!.version)
        assertEquals("HDFC Savings", deserialized.accounts[0].name)
        assertEquals(BigDecimal("25000.50"), BigDecimal(deserialized.accounts[0].balance))
    }
}
