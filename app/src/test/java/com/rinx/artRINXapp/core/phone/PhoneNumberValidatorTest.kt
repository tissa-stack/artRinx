package com.rinx.artRINXapp.core.phone

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.rinx.artRINXapp.feature.auth.presentation.waitlist.CountryCode
import io.michaelrocks.libphonenumber.android.PhoneNumberUtil
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Proves that [PhoneNumberValidator] enforces each country's exact valid phone-number length — in
 * particular that over-length input (the reported 11-/12-digit India bug) is rejected. Runs on the JVM
 * via Robolectric (real Context + the libphonenumber AAR assets), so no emulator is needed.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class PhoneNumberValidatorTest {

    private val context = ApplicationProvider.getApplicationContext<Context>()
    private val validator = PhoneNumberValidator(context)
    private val util = PhoneNumberUtil.createInstance(context)

    private fun country(iso2: String) = CountryCode(
        flag = "",
        code = iso2,
        dialCode = "+" + util.getCountryCodeForRegion(iso2),
        name = iso2,
    )

    // National digits of the region's MOBILE example (validate() checks MOBILE length).
    private fun exampleMobileNational(iso2: String): String =
        util.getExampleNumberForType(iso2, io.michaelrocks.libphonenumber.android.PhoneNumberUtil.PhoneNumberType.MOBILE)
            .nationalNumber.toString()

    @Test
    fun `library mobile example numbers are accepted as OK`() {
        for (iso2 in listOf("IN", "US", "GB", "AE", "SG", "DE")) {
            assertEquals(
                "$iso2 mobile example should validate as OK",
                PhoneValidation.OK,
                validator.validate(country(iso2), exampleMobileNational(iso2)),
            )
        }
    }

    @Test
    fun `validation is length-only - arbitrary area code is accepted at the right length`() {
        // A 10-digit US number with a made-up area code must NOT be rejected: validation is length-based,
        // not carrier-pattern based. (This is the regression that rejected real US numbers.) The number
        // must not start with "1" (the US country code), which parse() would strip.
        val us = country("US")
        assertEquals(PhoneValidation.OK, validator.validate(us, "2125551234"))      // 10 digits → OK
        assertNotEquals(PhoneValidation.OK, validator.validate(us, "212555123"))    // 9 → too short
        assertNotEquals(PhoneValidation.OK, validator.validate(us, "21255512345"))  // 11 → too long
    }

    @Test
    fun `india rejects 11 and 12 digit numbers (the reported bug)`() {
        val india = country("IN")
        assertEquals(PhoneValidation.OK, validator.validate(india, "9876543210"))      // valid 10-digit mobile
        assertNotEquals(PhoneValidation.OK, validator.validate(india, "98765432109"))  // 11 digits
        assertNotEquals(PhoneValidation.OK, validator.validate(india, "987654321090")) // 12 digits
        assertEquals(PhoneValidation.EMPTY, validator.validate(india, ""))
    }

    @Test
    fun `maxNationalDigits matches each country's mobile length`() {
        assertEquals(10, validator.maxNationalDigits(country("US")))
        assertEquals(10, validator.maxNationalDigits(country("IN")))
        assertEquals(9, validator.maxNationalDigits(country("AE")))
        assertEquals(8, validator.maxNationalDigits(country("SG")))
        // Multi-length regions — assert a sane lower bound rather than an exact value.
        assertTrue(validator.maxNationalDigits(country("GB")) >= 10)
        assertTrue(validator.maxNationalDigits(country("DE")) >= 11)
    }

    @Test
    fun `countries list is complete with flag, dial code and name`() {
        val all = validator.countries()
        assertTrue("expected 200+ countries, got ${all.size}", all.size >= 200)
        assertTrue(
            "every country needs a flag, a dial code and a name",
            all.all { it.flag.isNotBlank() && it.dialCode.length >= 2 && it.name.isNotBlank() },
        )
    }
}
