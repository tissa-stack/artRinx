package com.rinx.artRINXapp.core.phone

import android.content.Context
import android.telephony.TelephonyManager
import com.rinx.artRINXapp.feature.auth.presentation.waitlist.CountryCode
import com.rinx.artRINXapp.feature.auth.presentation.waitlist.CountryCodes
import dagger.hilt.android.qualifiers.ApplicationContext
import io.michaelrocks.libphonenumber.android.NumberParseException
import io.michaelrocks.libphonenumber.android.PhoneNumberUtil
import io.michaelrocks.libphonenumber.android.PhoneNumberUtil.PhoneNumberType
import io.michaelrocks.libphonenumber.android.PhoneNumberUtil.ValidationResult
import io.michaelrocks.libphonenumber.android.Phonenumber.PhoneNumber
import java.util.Locale
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton

/** Outcome of checking a national number's length/validity for a country. */
enum class PhoneValidation { EMPTY, OK, TOO_SHORT, TOO_LONG, INVALID }

/**
 * Inline error message for *clearly* wrong states only. Returns null while the field is empty, valid,
 * or merely incomplete (TOO_SHORT) so the user isn't nagged mid-typing — submit-enable still gates on
 * [PhoneValidation.OK].
 */
fun PhoneValidation.errorOrNull(): String? = when (this) {
    PhoneValidation.TOO_LONG -> "Phone number is too long."
    PhoneValidation.INVALID -> "Enter a valid phone number."
    else -> null
}

/** Max digits in an E.164 national significant number — the upper bound for any country's cap/probe. */
private const val MAX_E164_DIGITS = 15

/**
 * Offline source of truth for phone country data and per-country length validation, backed by
 * Google libphonenumber (Android port). Replaces the old hand-rolled `nationalNumberLength()` map and
 * the master-countries API for the phone picker — the metadata ships in the library's assets, so this
 * works with no network and covers every region.
 *
 * The underlying [PhoneNumberUtil] is created once (it's non-trivial to build) and the country list is
 * memoised for the app session.
 */
@Singleton
class PhoneNumberValidator @Inject constructor(
    @param:ApplicationContext private val context: Context,
) {
    private val util: PhoneNumberUtil by lazy { PhoneNumberUtil.createInstance(context) }

    @Volatile private var countryCache: List<CountryCode>? = null
    private val maxLenCache = ConcurrentHashMap<String, Int>()

    /**
     * Best guess of the device's country (ISO-3166 alpha-2, uppercase) for the default phone-code
     * selection: SIM country → network country → device locale, falling back to [CountryCodes.default].
     * SIM/network ISO need no runtime permission. Returns a valid 2-letter code or the US fallback.
     */
    fun deviceRegion(): String {
        val tm = context.getSystemService(Context.TELEPHONY_SERVICE) as? TelephonyManager
        val candidate = tm?.simCountryIso?.takeIf { it.isNotBlank() }
            ?: tm?.networkCountryIso?.takeIf { it.isNotBlank() }
            ?: context.resources.configuration.locales.takeIf { !it.isEmpty }?.get(0)?.country
        val region = candidate.orEmpty().uppercase()
        return region.takeIf { it.length == 2 && it.all(Char::isLetter) } ?: CountryCodes.default.code
    }

    /** Every supported region as a [CountryCode] (flag emoji + ISO2 + "+"-dial code + name), by name. */
    fun countries(): List<CountryCode> {
        countryCache?.let { return it }
        val list = util.supportedRegions
            .asSequence()
            .filter { it.length == 2 && it.all(Char::isLetter) }
            .map { region ->
                CountryCode(
                    flag = iso2ToFlagEmoji(region),
                    code = region,
                    dialCode = "+" + util.getCountryCodeForRegion(region),
                    name = Locale("", region).displayCountry.ifBlank { region },
                )
            }
            .sortedBy { it.name }
            .toList()
        return list.also { countryCache = it }
    }

    /**
     * Length/validity of [rawNational] (digits only, no dial code) for [country]. Uses libphonenumber's
     * possible-length check, so TOO_LONG is exactly that country's max-digit limit.
     */
    fun validate(country: CountryCode, rawNational: String): PhoneValidation {
        if (rawNational.isBlank()) return PhoneValidation.EMPTY
        return try {
            val number = util.parse(rawNational, country.code)
            // Length-only check scoped to MOBILE (the type users enter for SMS/OTP). This accepts any
            // correctly-sized number regardless of whether the area-code/prefix matches a real carrier
            // pattern (so a 10-digit US number with an arbitrary area code is fine), while still
            // rejecting wrong lengths (e.g. an 11-digit India number → TOO_LONG, mobile length is 10).
            when (possibleReason(country.code, number)) {
                ValidationResult.IS_POSSIBLE,
                ValidationResult.IS_POSSIBLE_LOCAL_ONLY -> PhoneValidation.OK
                ValidationResult.TOO_SHORT -> PhoneValidation.TOO_SHORT
                ValidationResult.TOO_LONG -> PhoneValidation.TOO_LONG
                else -> PhoneValidation.INVALID            // INVALID_LENGTH (gap) / INVALID_COUNTRY_CODE
            }
        } catch (e: NumberParseException) {
            PhoneValidation.INVALID
        }
    }

    /**
     * Largest national-number digit count accepted for [country] — used to hard-cap the input field so
     * the user can't type more than that many digits. Derived (and cached) via the public length-check
     * by probing all-9 strings for the first length that is TOO_LONG.
     */
    fun maxNationalDigits(country: CountryCode): Int = maxLenCache.getOrPut(country.code) {
        val region = country.code
        val useMobile = PhoneNumberType.MOBILE in util.getSupportedTypesForRegion(region)
        var max = MAX_E164_DIGITS
        for (len in 1..MAX_E164_DIGITS) {
            val number = try {
                util.parse("9".repeat(len), region)
            } catch (e: NumberParseException) {
                continue
            }
            val reason = if (useMobile) {
                util.isPossibleNumberForTypeWithReason(number, PhoneNumberType.MOBILE)
            } else {
                util.isPossibleNumberWithReason(number)
            }
            if (reason == ValidationResult.TOO_LONG) {
                max = len - 1
                break
            }
        }
        max.coerceIn(4, MAX_E164_DIGITS)
    }

    /** MOBILE length rules when the region has mobile numbering; else the overall possible-length check. */
    private fun possibleReason(region: String, number: PhoneNumber): ValidationResult =
        if (PhoneNumberType.MOBILE in util.getSupportedTypesForRegion(region)) {
            util.isPossibleNumberForTypeWithReason(number, PhoneNumberType.MOBILE)
        } else {
            util.isPossibleNumberWithReason(number)
        }
}

/**
 * The flag emoji for an ISO-3166 alpha-2 region code, built from the two regional-indicator symbols
 * (e.g. "IN" → 🇮🇳). Returns "" for anything that isn't a 2-letter code. No library ships flags; this
 * is the standard derivation.
 */
fun iso2ToFlagEmoji(iso2: String): String {
    if (iso2.length != 2 || !iso2.all { it.isLetter() }) return ""
    return buildString {
        iso2.uppercase().forEach { c -> appendCodePoint(0x1F1E6 + (c.code - 'A'.code)) }
    }
}
