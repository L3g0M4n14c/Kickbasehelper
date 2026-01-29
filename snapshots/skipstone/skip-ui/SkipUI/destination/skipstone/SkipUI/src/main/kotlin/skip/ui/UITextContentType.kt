package skip.ui

import skip.lib.*

// Copyright 2023–2025 Skip
// SPDX-License-Identifier: LGPL-3.0-only WITH LGPL-3.0-linking-exception

class UITextContentType: RawRepresentable<Int> {
    override val rawValue: Int

    constructor(rawValue: Int) {
        this.rawValue = rawValue
    }

    internal val _contentType: androidx.compose.ui.autofill.ContentType?
        get() {
            // https://developer.android.com/reference/kotlin/androidx/compose/ui/autofill/ContentType#summary
            when (this) {
                UITextContentType.name -> return androidx.compose.ui.autofill.ContentType.PersonFullName
                UITextContentType.namePrefix -> return androidx.compose.ui.autofill.ContentType.PersonNamePrefix
                UITextContentType.givenName -> return androidx.compose.ui.autofill.ContentType.PersonFirstName
                UITextContentType.middleName -> return androidx.compose.ui.autofill.ContentType.PersonMiddleName
                UITextContentType.familyName -> return androidx.compose.ui.autofill.ContentType.PersonLastName
                UITextContentType.nameSuffix -> return androidx.compose.ui.autofill.ContentType.PersonNameSuffix
                UITextContentType.nickname -> return null
                UITextContentType.jobTitle -> return null
                UITextContentType.organizationName -> return null
                UITextContentType.location -> return null
                UITextContentType.fullStreetAddress -> return androidx.compose.ui.autofill.ContentType.PostalAddress
                UITextContentType.streetAddressLine1 -> return androidx.compose.ui.autofill.ContentType.AddressStreet
                UITextContentType.streetAddressLine2 -> return null
                UITextContentType.addressCity -> return androidx.compose.ui.autofill.ContentType.AddressLocality
                UITextContentType.addressState -> return androidx.compose.ui.autofill.ContentType.AddressRegion
                UITextContentType.addressCityAndState -> return null
                UITextContentType.sublocality -> return null
                UITextContentType.countryName -> return androidx.compose.ui.autofill.ContentType.AddressCountry
                UITextContentType.postalCode -> return androidx.compose.ui.autofill.ContentType.PostalCode
                UITextContentType.telephoneNumber -> return androidx.compose.ui.autofill.ContentType.PhoneNumber
                UITextContentType.emailAddress -> return androidx.compose.ui.autofill.ContentType.EmailAddress
                UITextContentType.URL -> return null
                UITextContentType.creditCardNumber -> return androidx.compose.ui.autofill.ContentType.CreditCardNumber
                UITextContentType.username -> return androidx.compose.ui.autofill.ContentType.Username
                UITextContentType.password -> return androidx.compose.ui.autofill.ContentType.Password
                UITextContentType.newPassword -> return androidx.compose.ui.autofill.ContentType.NewPassword
                UITextContentType.oneTimeCode -> return androidx.compose.ui.autofill.ContentType.SmsOtpCode
                UITextContentType.shipmentTrackingNumber -> return null
                UITextContentType.flightNumber -> return null
                UITextContentType.dateTime -> return null
                UITextContentType.birthdate -> return androidx.compose.ui.autofill.ContentType.BirthDateFull
                UITextContentType.birthdateDay -> return androidx.compose.ui.autofill.ContentType.BirthDateDay
                UITextContentType.birthdateMonth -> return androidx.compose.ui.autofill.ContentType.BirthDateMonth
                UITextContentType.birthdateYear -> return androidx.compose.ui.autofill.ContentType.BirthDateYear
                UITextContentType.creditCardSecurityCode -> return androidx.compose.ui.autofill.ContentType.CreditCardSecurityCode
                UITextContentType.creditCardName -> return null
                UITextContentType.creditCardGivenName -> return androidx.compose.ui.autofill.ContentType.PersonFirstName
                UITextContentType.creditCardMiddleName -> return androidx.compose.ui.autofill.ContentType.PersonMiddleName
                UITextContentType.creditCardFamilyName -> return androidx.compose.ui.autofill.ContentType.PersonLastName
                UITextContentType.creditCardExpiration -> return androidx.compose.ui.autofill.ContentType.CreditCardExpirationDate
                UITextContentType.creditCardExpirationMonth -> return androidx.compose.ui.autofill.ContentType.CreditCardExpirationMonth
                UITextContentType.creditCardExpirationYear -> return androidx.compose.ui.autofill.ContentType.CreditCardExpirationYear
                UITextContentType.creditCardType -> return null
                else -> return null
            }
        }

    override fun equals(other: Any?): Boolean {
        if (other !is UITextContentType) return false
        return rawValue == other.rawValue
    }

    @androidx.annotation.Keep
    companion object {

        // Warning: these values are used in bridging

        val name = UITextContentType(rawValue = 0) // Not allowed as a Kotlin enum case name
        val namePrefix = UITextContentType(rawValue = 1)
        val givenName = UITextContentType(rawValue = 2)
        val middleName = UITextContentType(rawValue = 3)
        val familyName = UITextContentType(rawValue = 4)
        val nameSuffix = UITextContentType(rawValue = 5)
        val nickname = UITextContentType(rawValue = 6)
        val jobTitle = UITextContentType(rawValue = 7)
        val organizationName = UITextContentType(rawValue = 8)
        val location = UITextContentType(rawValue = 9)
        val fullStreetAddress = UITextContentType(rawValue = 10)
        val streetAddressLine1 = UITextContentType(rawValue = 11)
        val streetAddressLine2 = UITextContentType(rawValue = 12)
        val addressCity = UITextContentType(rawValue = 13)
        val addressState = UITextContentType(rawValue = 14)
        val addressCityAndState = UITextContentType(rawValue = 15)
        val sublocality = UITextContentType(rawValue = 16)
        val countryName = UITextContentType(rawValue = 17)
        val postalCode = UITextContentType(rawValue = 18)
        val telephoneNumber = UITextContentType(rawValue = 19)
        val emailAddress = UITextContentType(rawValue = 20)
        val URL = UITextContentType(rawValue = 21)
        val creditCardNumber = UITextContentType(rawValue = 22)
        val username = UITextContentType(rawValue = 23)
        val password = UITextContentType(rawValue = 24)
        val newPassword = UITextContentType(rawValue = 25)
        val oneTimeCode = UITextContentType(rawValue = 26)
        val shipmentTrackingNumber = UITextContentType(rawValue = 27)
        val flightNumber = UITextContentType(rawValue = 28)
        val dateTime = UITextContentType(rawValue = 29)
        val birthdate = UITextContentType(rawValue = 30)
        val birthdateDay = UITextContentType(rawValue = 31)
        val birthdateMonth = UITextContentType(rawValue = 32)
        val birthdateYear = UITextContentType(rawValue = 33)
        val creditCardSecurityCode = UITextContentType(rawValue = 34)
        val creditCardName = UITextContentType(rawValue = 35)
        val creditCardGivenName = UITextContentType(rawValue = 36)
        val creditCardMiddleName = UITextContentType(rawValue = 37)
        val creditCardFamilyName = UITextContentType(rawValue = 38)
        val creditCardExpiration = UITextContentType(rawValue = 39)
        val creditCardExpirationMonth = UITextContentType(rawValue = 40)
        val creditCardExpirationYear = UITextContentType(rawValue = 41)
        val creditCardType = UITextContentType(rawValue = 42)
    }
}

