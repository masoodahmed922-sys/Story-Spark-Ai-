package com.example.data

enum class PaymentMethodType(val displayName: String, val iconName: String) {
    GOOGLE_PAY("Google Pay", "gpay"),
    GOOGLE_PLAY_BILLING("Google Play Billing", "play_billing"),
    CREDIT_CARD("Credit / Debit Card", "credit_card"),
    PAYPAL("PayPal", "paypal"),
    EDUCATOR_VOUCHER("School / Grant Code", "voucher")
}

data class TransactionRecord(
    val id: String,
    val title: String,
    val amount: Double,
    val currency: String = "USD",
    val timestamp: Long = System.currentTimeMillis(),
    val paymentMethod: PaymentMethodType,
    val status: String = "Completed",
    val receiptNumber: String,
    val description: String
)

data class SavedPaymentMethod(
    val id: String,
    val type: PaymentMethodType,
    val label: String,
    val details: String,
    val isDefault: Boolean = false
)

data class SubscriptionDetails(
    val planName: String,
    val price: Double,
    val billingCycle: String = "Monthly",
    val nextRenewalDate: Long = System.currentTimeMillis() + 30L * 24 * 60 * 60 * 1000,
    val autoRenew: Boolean = true,
    val status: String = "Active"
)
