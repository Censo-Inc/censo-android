package com.android.billingclient.api

// DEBUG ONLY!!!
// Billing client classes have protected constructors, therefore accessing via
// google package in order to stub product and purchase for debug build
object BillingApiUtil {

    fun productDetails(json: String): ProductDetails {
        return ProductDetails(json)
    }

    fun purchase(json: String): Purchase {
        return Purchase(json, "test_signature")
    }

}