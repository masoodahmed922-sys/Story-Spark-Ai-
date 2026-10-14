package com.example.ui.components

import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.PaymentMethodType
import com.example.data.SavedPaymentMethod
import com.example.ui.StorySparkViewModel
import com.example.ui.theme.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PaymentCheckoutSheet(
    itemTitle: String,
    price: Double,
    period: String = "",
    creditsToAdd: Int = 0,
    newPlanName: String? = null,
    viewModel: StorySparkViewModel,
    onDismiss: () -> Unit,
    onSuccess: (String) -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val scrollState = rememberScrollState()

    var selectedMethod by remember { mutableStateOf(PaymentMethodType.GOOGLE_PAY) }
    var isProcessing by remember { mutableStateOf(false) }
    var processingStep by remember { mutableStateOf("Contacting payment gateway...") }
    var transactionReceipt by remember { mutableStateOf<String?>(null) }

    // Card Input States
    var cardholderName by remember { mutableStateOf(viewModel.currentUser.value?.name ?: "") }
    var cardNumber by remember { mutableStateOf("4532 •••• •••• 4821") }
    var cardExpiry by remember { mutableStateOf("08/28") }
    var cardCvc by remember { mutableStateOf("842") }
    var cardZip by remember { mutableStateOf("94043") }
    var saveCard by remember { mutableStateOf(true) }

    // Voucher State
    var voucherCode by remember { mutableStateOf("") }
    var voucherFeedback by remember { mutableStateOf<String?>(null) }
    var voucherApplied by remember { mutableStateOf(false) }

    val finalPrice = if (voucherApplied) 0.00 else price

    ModalBottomSheet(
        onDismissRequest = { if (!isProcessing) onDismiss() },
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
        containerColor = MaterialTheme.colorScheme.surface,
        modifier = Modifier.testTag("payment_checkout_sheet")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .imePadding()
                .padding(horizontal = 20.dp)
                .padding(bottom = 32.dp)
                .verticalScroll(scrollState)
        ) {
            // Header Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(SparkPrimaryContainer),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Lock,
                            contentDescription = null,
                            tint = SparkPrimary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "Secure Checkout",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "256-bit Encrypted • Google Play & Stripe Certified",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                IconButton(
                    onClick = { if (!isProcessing) onDismiss() },
                    enabled = !isProcessing
                ) {
                    Icon(Icons.Default.Close, contentDescription = "Close")
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            if (transactionReceipt != null) {
                // SUCCESS RECEIPT VIEW
                SuccessReceiptView(
                    itemTitle = itemTitle,
                    amount = finalPrice,
                    method = selectedMethod,
                    receiptNumber = transactionReceipt!!,
                    onDone = {
                        onSuccess(transactionReceipt!!)
                        onDismiss()
                    }
                )
            } else if (isProcessing) {
                // PROCESSING VIEW
                ProcessingView(stepText = processingStep)
            } else {
                // CHECKOUT FORM
                // --- 1. Order Summary Card ---
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = itemTitle,
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                if (creditsToAdd > 0) {
                                    Text(
                                        text = "+$creditsToAdd AI Generation Credits",
                                        fontSize = 12.sp,
                                        color = SparkSecondary,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                } else if (newPlanName != null) {
                                    Text(
                                        text = "Full SaaS Access: $newPlanName",
                                        fontSize = 12.sp,
                                        color = SparkPrimary,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                            }

                            Column(horizontalAlignment = Alignment.End) {
                                Text(
                                    text = if (voucherApplied) "$0.00" else "$$price",
                                    fontSize = 20.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                if (period.isNotBlank()) {
                                    Text(
                                        text = period,
                                        fontSize = 11.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }

                        if (voucherApplied) {
                            Spacer(modifier = Modifier.height(6.dp))
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = Color(0xFFD1FAE5)
                            ) {
                                Text(
                                    text = "✓ 100% Educational Grant Discount Applied",
                                    color = Color(0xFF065F46),
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // --- 2. Payment Method Selector ---
                Text(
                    text = "Select Payment Method:",
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(8.dp))

                val methods = listOf(
                    PaymentMethodType.GOOGLE_PAY,
                    PaymentMethodType.GOOGLE_PLAY_BILLING,
                    PaymentMethodType.CREDIT_CARD,
                    PaymentMethodType.PAYPAL,
                    PaymentMethodType.EDUCATOR_VOUCHER
                )

                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(methods) { method ->
                        FilterChip(
                            selected = selectedMethod == method,
                            onClick = { selectedMethod = method },
                            label = { Text(method.displayName, fontSize = 12.sp) },
                            leadingIcon = {
                                Icon(
                                    imageVector = getPaymentIcon(method),
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp),
                                    tint = if (selectedMethod == method) SparkPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // --- 3. Dynamic Method Content ---
                when (selectedMethod) {
                    PaymentMethodType.GOOGLE_PAY -> {
                        GooglePayMethodContent(accountEmail = viewModel.currentUser.value?.email)
                    }
                    PaymentMethodType.GOOGLE_PLAY_BILLING -> {
                        GooglePlayBillingContent(itemTitle = itemTitle, price = finalPrice)
                    }
                    PaymentMethodType.CREDIT_CARD -> {
                        CreditCardMethodContent(
                            name = cardholderName,
                            onNameChange = { cardholderName = it },
                            number = cardNumber,
                            onNumberChange = { cardNumber = it },
                            expiry = cardExpiry,
                            onExpiryChange = { cardExpiry = it },
                            cvc = cardCvc,
                            onCvcChange = { cardCvc = it },
                            zip = cardZip,
                            onZipChange = { cardZip = it },
                            saveCard = saveCard,
                            onSaveCardChange = { saveCard = it }
                        )
                    }
                    PaymentMethodType.PAYPAL -> {
                        PayPalMethodContent()
                    }
                    PaymentMethodType.EDUCATOR_VOUCHER -> {
                        VoucherMethodContent(
                            code = voucherCode,
                            onCodeChange = { voucherCode = it },
                            feedback = voucherFeedback,
                            onApply = {
                                val (ok, msg) = viewModel.redeemVoucher(voucherCode)
                                voucherFeedback = msg
                                voucherApplied = ok
                                if (ok) {
                                    Toast.makeText(context, "Voucher Applied!", Toast.LENGTH_SHORT).show()
                                }
                            }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // --- 4. Submit Payment Button ---
                Button(
                    onClick = {
                        isProcessing = true
                        coroutineScope.launch {
                            processingStep = "Authenticating with ${selectedMethod.displayName}..."
                            delay(600)
                            processingStep = "Encrypting transaction tokens..."
                            delay(600)
                            processingStep = "Authorizing $$finalPrice..."
                            delay(500)
                            processingStep = "Updating StorySpark account..."
                            delay(400)

                            val receipt = viewModel.processPayment(
                                itemTitle = itemTitle,
                                amount = finalPrice,
                                method = selectedMethod,
                                creditsToAdd = creditsToAdd,
                                newPlan = newPlanName
                            )

                            if (selectedMethod == PaymentMethodType.CREDIT_CARD && saveCard) {
                                viewModel.savePaymentMethod(
                                    SavedPaymentMethod(
                                        id = "PM_${System.currentTimeMillis()}",
                                        type = PaymentMethodType.CREDIT_CARD,
                                        label = "Card ending in ${cardNumber.takeLast(4)}",
                                        details = "Exp $cardExpiry",
                                        isDefault = false
                                    )
                                )
                            }

                            isProcessing = false
                            transactionReceipt = receipt
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                        .testTag("confirm_payment_button"),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = when (selectedMethod) {
                            PaymentMethodType.GOOGLE_PAY -> Color.Black
                            PaymentMethodType.GOOGLE_PLAY_BILLING -> Color(0xFF01875F)
                            PaymentMethodType.PAYPAL -> Color(0xFF0070BA)
                            else -> SparkPrimary
                        }
                    )
                ) {
                    Icon(
                        imageVector = getPaymentIcon(selectedMethod),
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = when (selectedMethod) {
                            PaymentMethodType.GOOGLE_PAY -> "Buy with GPay • $$finalPrice"
                            PaymentMethodType.GOOGLE_PLAY_BILLING -> "1-Tap Subscribe • $$finalPrice"
                            PaymentMethodType.PAYPAL -> "Pay with PayPal • $$finalPrice"
                            PaymentMethodType.EDUCATOR_VOUCHER -> if (voucherApplied) "Redeem Voucher (Free)" else "Apply & Activate"
                            else -> "Pay $$finalPrice with Card"
                        },
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                Text(
                    text = "🔒 Transactions are secure and encrypted. Cancel subscriptions anytime from Google Play or your Profile.",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

@Composable
fun GooglePayMethodContent(accountEmail: String? = null) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.AccountBalanceWallet, contentDescription = null, tint = SparkPrimary)
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text("Google Pay Account", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        if (!accountEmail.isNullOrBlank()) {
                            Text(accountEmail, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = Color.Black
                ) {
                    Text(
                        text = "GPay",
                        color = Color.White,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 11.sp,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
            Spacer(modifier = Modifier.height(10.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.CreditCard, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Default payment method: Chase Freedom Visa (•••• 4821)", fontSize = 12.sp)
            }
        }
    }
}

@Composable
fun GooglePlayBillingContent(itemTitle: String, price: Double) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF01875F).copy(alpha = 0.1f)),
        border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(Color(0xFF01875F)))
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Shop, contentDescription = null, tint = Color(0xFF01875F), modifier = Modifier.size(24.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Column {
                    Text("Google Play Billing System", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    Text("Managed via your Google Play account", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = "• Billed directly to your Google Play balance, carrier, or linked bank card.\n• Instant automatic receipt sent to your Play Store account.\n• Cancel or manage subscriptions in Play Store > Payments & Subscriptions.",
                fontSize = 12.sp,
                lineHeight = 16.sp,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

@Composable
fun CreditCardMethodContent(
    name: String,
    onNameChange: (String) -> Unit,
    number: String,
    onNumberChange: (String) -> Unit,
    expiry: String,
    onExpiryChange: (String) -> Unit,
    cvc: String,
    onCvcChange: (String) -> Unit,
    zip: String,
    onZipChange: (String) -> Unit,
    saveCard: Boolean,
    onSaveCardChange: (Boolean) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        OutlinedTextField(
            value = name,
            onValueChange = onNameChange,
            label = { Text("Cardholder Name") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            shape = RoundedCornerShape(8.dp)
        )

        OutlinedTextField(
            value = number,
            onValueChange = onNumberChange,
            label = { Text("Card Number") },
            trailingIcon = {
                Icon(Icons.Default.CreditCard, contentDescription = null, tint = SparkPrimary)
            },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            shape = RoundedCornerShape(8.dp)
        )

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(
                value = expiry,
                onValueChange = onExpiryChange,
                label = { Text("Expires (MM/YY)") },
                modifier = Modifier.weight(1f),
                singleLine = true,
                shape = RoundedCornerShape(8.dp)
            )
            OutlinedTextField(
                value = cvc,
                onValueChange = onCvcChange,
                label = { Text("CVV/CVC") },
                modifier = Modifier.weight(1f),
                singleLine = true,
                shape = RoundedCornerShape(8.dp)
            )
            OutlinedTextField(
                value = zip,
                onValueChange = onZipChange,
                label = { Text("ZIP") },
                modifier = Modifier.weight(1f),
                singleLine = true,
                shape = RoundedCornerShape(8.dp)
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Checkbox(checked = saveCard, onCheckedChange = onSaveCardChange)
            Spacer(modifier = Modifier.width(4.dp))
            Text("Save card securely for future book generation", fontSize = 12.sp)
        }
    }
}

@Composable
fun PayPalMethodContent() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF0070BA).copy(alpha = 0.08f))
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Payment, contentDescription = null, tint = Color(0xFF0070BA))
                Spacer(modifier = Modifier.width(8.dp))
                Column {
                    Text("PayPal Express Checkout", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    Text("Pay with PayPal balance, linked bank account, or Pay-in-4", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                "You will be redirected securely to authenticate and complete your purchase in 1 click.",
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun VoucherMethodContent(
    code: String,
    onCodeChange: (String) -> Unit,
    feedback: String?,
    onApply: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = SparkSecondaryContainer.copy(alpha = 0.4f))
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Text(
                text = "Educational Grant & Voucher Redemption",
                fontWeight = FontWeight.Bold,
                fontSize = 13.sp,
                color = SparkOnSecondaryContainer
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "For teachers, schools, homeschoolers, and library grant recipients.",
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = code,
                    onValueChange = onCodeChange,
                    placeholder = { Text("Try TEACHER2026 or HOMESCHOOL") },
                    modifier = Modifier.weight(1f),
                    singleLine = true,
                    shape = RoundedCornerShape(8.dp)
                )

                Button(
                    onClick = onApply,
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = SparkSecondary)
                ) {
                    Text("Apply", color = Color.Black, fontWeight = FontWeight.Bold)
                }
            }

            if (feedback != null) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = feedback,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = if (feedback.contains("Applied")) Color(0xFF047857) else Color.Red
                )
            }
        }
    }
}

@Composable
fun ProcessingView(stepText: String) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        CircularProgressIndicator(
            modifier = Modifier.size(52.dp),
            color = SparkPrimary,
            strokeWidth = 4.dp
        )
        Text(
            text = "Processing Payment...",
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = stepText,
            fontSize = 13.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
fun SuccessReceiptView(
    itemTitle: String,
    amount: Double,
    method: PaymentMethodType,
    receiptNumber: String,
    onDone: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Box(
            modifier = Modifier
                .size(60.dp)
                .clip(CircleShape)
                .background(Color(0xFFD1FAE5)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Check,
                contentDescription = null,
                tint = Color(0xFF047857),
                modifier = Modifier.size(36.dp)
            )
        }

        Text(
            text = "Payment Completed!",
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF047857)
        )

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                ReceiptRow("Item", itemTitle)
                ReceiptRow("Amount Paid", "$$amount")
                ReceiptRow("Payment Method", method.displayName)
                ReceiptRow("Receipt #", receiptNumber)
                ReceiptRow("Status", "Approved & Synchronized")
            }
        }

        Button(
            onClick = onDone,
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp),
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(containerColor = SparkPrimary)
        ) {
            Text("Done • Start Creating", fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
fun ReceiptRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 3.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, fontSize = 12.sp, fontWeight = FontWeight.Bold)
    }
}

private fun getPaymentIcon(type: PaymentMethodType) = when (type) {
    PaymentMethodType.GOOGLE_PAY -> Icons.Default.AccountBalanceWallet
    PaymentMethodType.GOOGLE_PLAY_BILLING -> Icons.Default.Shop
    PaymentMethodType.CREDIT_CARD -> Icons.Default.CreditCard
    PaymentMethodType.PAYPAL -> Icons.Default.Payment
    PaymentMethodType.EDUCATOR_VOUCHER -> Icons.Default.CardGiftcard
}
