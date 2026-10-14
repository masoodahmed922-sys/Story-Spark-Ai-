package com.example.ui.screens

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ReceiptLong
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.StorySparkViewModel
import com.example.ui.components.PaymentCheckoutSheet
import com.example.ui.components.TransactionHistoryDialog
import com.example.ui.theme.*

data class CheckoutItemData(
    val title: String,
    val price: Double,
    val period: String = "",
    val creditsToAdd: Int = 0,
    val newPlanName: String? = null
)

@Composable
fun PricingScreen(
    viewModel: StorySparkViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val currentPlan by viewModel.userPlan.collectAsState()
    val credits by viewModel.credits.collectAsState()
    val proPrice by viewModel.proMonthlyPrice.collectAsState()
    val creatorPrice by viewModel.creatorMonthlyPrice.collectAsState()
    val bundlePrice by viewModel.creditBundlePrice.collectAsState()
    val commercialTerms by viewModel.commercialTerms.collectAsState()
    val transactions by viewModel.transactions.collectAsState()

    var activeCheckoutItem by remember { mutableStateOf<CheckoutItemData?>(null) }
    var showTransactionHistory by remember { mutableStateOf(false) }
    var showVoucherDialog by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .testTag("pricing_screen"),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        // --- 1. HEADER ---
        item {
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = SparkPrimaryContainer
                ) {
                    Text(
                        text = "StorySpark SaaS Plans & Payment Options",
                        color = SparkOnPrimaryContainer,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                Text(
                    text = "Publish Professional Children's Books",
                    fontSize = 22.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.onSurface,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(6.dp))

                Text(
                    text = "Flexible payments for parents, teachers, KDP authors, and homeschoolers.",
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
            }
        }

        // --- 2. CURRENT BALANCE & BILLING SHORTCUTS ---
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = SparkSecondaryContainer)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "Current Credit Balance",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = SparkOnSecondaryContainer
                            )
                            Text(
                                text = "$credits Credits Available",
                                fontSize = 20.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = SparkOnSecondaryContainer
                            )
                            Text(
                                text = "Active Plan: $currentPlan",
                                fontSize = 12.sp,
                                color = SparkOnSecondaryContainer.copy(alpha = 0.8f)
                            )
                        }

                        Button(
                            onClick = {
                                activeCheckoutItem = CheckoutItemData(
                                    title = "25 AI Generation Credits",
                                    price = bundlePrice,
                                    creditsToAdd = 25
                                )
                            },
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = SparkSecondary)
                        ) {
                            Icon(Icons.Default.Stars, contentDescription = null, tint = Color.Black, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Buy Credits", color = Color.Black, fontWeight = FontWeight.Bold)
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))
                    HorizontalDivider(color = SparkOnSecondaryContainer.copy(alpha = 0.2f))
                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        TextButton(
                            onClick = { showTransactionHistory = true },
                            contentPadding = PaddingValues(0.dp)
                        ) {
                            Icon(Icons.AutoMirrored.Filled.ReceiptLong, contentDescription = null, modifier = Modifier.size(16.dp), tint = SparkOnSecondaryContainer)
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Billing History (${transactions.size})", fontSize = 12.sp, color = SparkOnSecondaryContainer, fontWeight = FontWeight.Bold)
                        }

                        TextButton(
                            onClick = { showVoucherDialog = true },
                            contentPadding = PaddingValues(0.dp)
                        ) {
                            Icon(Icons.Default.CardGiftcard, contentDescription = null, modifier = Modifier.size(16.dp), tint = SparkOnSecondaryContainer)
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Redeem Voucher", fontSize = 12.sp, color = SparkOnSecondaryContainer, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        // --- 3. SUPPORTED PAYMENT METHODS SHOWCASE ---
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text(
                        text = "Accepted Payment Methods",
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceAround
                    ) {
                        PaymentBadge("Google Pay", Icons.Default.AccountBalanceWallet, Color(0xFF0F172A))
                        PaymentBadge("Play Store", Icons.Default.Shop, Color(0xFF01875F))
                        PaymentBadge("Credit Card", Icons.Default.CreditCard, Color(0xFF1E40AF))
                        PaymentBadge("PayPal", Icons.Default.Payment, Color(0xFF0070BA))
                        PaymentBadge("School Grant", Icons.Default.CardGiftcard, Color(0xFF7C3AED))
                    }
                }
            }
        }

        // --- 4. THREE SUBSCRIPTION PLANS ---
        item {
            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                // Free Plan
                PlanCard(
                    planName = "Free Plan",
                    price = "$0",
                    period = "forever",
                    badge = "STARTER",
                    features = listOf(
                        "3 books per month",
                        "Up to 8 pages per book",
                        "Limited illustration generations",
                        "Standard templates",
                        "Watermarked digital preview"
                    ),
                    isCurrent = currentPlan == "Free Plan",
                    buttonText = if (currentPlan == "Free Plan") "Current Plan" else "Switch to Free",
                    onSelect = {
                        viewModel.upgradePlan("Free Plan")
                        Toast.makeText(context, "Switched to Free Plan", Toast.LENGTH_SHORT).show()
                    }
                )

                // Pro Plan
                PlanCard(
                    planName = "StorySpark Pro",
                    price = "$$proPrice",
                    period = "/ month",
                    badge = "MOST POPULAR",
                    isFeatured = true,
                    features = listOf(
                        "Unlimited story creation (fair-use limits)",
                        "Full 20-page storybooks",
                        "Premium templates & custom art styles",
                        "100 AI Generation Credits included monthly",
                        "No watermark on reader",
                        "High-res printable PDF export",
                        "Advanced child personalization & character bible",
                        "All AI writing and translation tools"
                    ),
                    isCurrent = currentPlan == "StorySpark Pro",
                    buttonText = if (currentPlan == "StorySpark Pro") "Active Plan" else "Subscribe to Pro ($$proPrice/mo)",
                    onSelect = {
                        activeCheckoutItem = CheckoutItemData(
                            title = "StorySpark Pro Subscription",
                            price = proPrice,
                            period = "/ month",
                            creditsToAdd = 100,
                            newPlanName = "StorySpark Pro"
                        )
                    }
                )

                // Creator Plan
                PlanCard(
                    planName = "Creator Plan",
                    price = "$$creatorPrice",
                    period = "/ month",
                    badge = "COMMERCIAL / KDP",
                    features = listOf(
                        "250 AI Generation Credits monthly",
                        "Commercial-use rights for Etsy & Amazon KDP",
                        "Priority background generation queue",
                        "Bulk story generation & multi-character consistency",
                        "All printable trim sizes (8.5x8.5, 8x10, A4, A5)",
                        "Custom author bio & back-cover branding",
                        "Direct multi-language translation"
                    ),
                    isCurrent = currentPlan == "Creator Plan",
                    buttonText = if (currentPlan == "Creator Plan") "Active Plan" else "Subscribe to Creator ($$creatorPrice/mo)",
                    onSelect = {
                        activeCheckoutItem = CheckoutItemData(
                            title = "StorySpark Creator Subscription",
                            price = creatorPrice,
                            period = "/ month",
                            creditsToAdd = 250,
                            newPlanName = "Creator Plan"
                        )
                    }
                )
            }
        }

        // --- 5. CREDIT TOP-UP PACKAGES ---
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Instant Generation Credit Packs",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Top up anytime without changing your monthly subscription plan.",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    CreditPackRowItem(
                        credits = 25,
                        price = "$${bundlePrice}",
                        badge = "Standard",
                        onBuy = {
                            activeCheckoutItem = CheckoutItemData(
                                title = "25 AI Generation Credits",
                                price = bundlePrice,
                                creditsToAdd = 25
                            )
                        }
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    CreditPackRowItem(
                        credits = 60,
                        price = "$9.99",
                        badge = "+10 Bonus Credits",
                        isPopular = true,
                        onBuy = {
                            activeCheckoutItem = CheckoutItemData(
                                title = "60 AI Generation Credits (+10 Bonus)",
                                price = 9.99,
                                creditsToAdd = 60
                            )
                        }
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    CreditPackRowItem(
                        credits = 150,
                        price = "$19.99",
                        badge = "+35 Best Value",
                        onBuy = {
                            activeCheckoutItem = CheckoutItemData(
                                title = "150 AI Generation Credits (+35 Best Value)",
                                price = 19.99,
                                creditsToAdd = 150
                            )
                        }
                    )
                }
            }
        }

        // --- 6. CREDIT COST BREAKDOWN ---
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Credit Usage Rates",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(10.dp))

                    CreditRateRow("Full Book Generation (Outline + Pages + Art)", "15 Credits")
                    CreditRateRow("Story Text Generation", "1 Credit")
                    CreditRateRow("Single Page Illustration", "2 Credits")
                    CreditRateRow("Custom Cover Art", "2 Credits")
                    CreditRateRow("AI Writing Enhancement / Translation", "1 Credit")
                }
            }
        }

        // --- 7. COMMERCIAL USE DISCLAIMER ---
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text(
                        text = "Commercial Use Notice",
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = commercialTerms,
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        lineHeight = 16.sp
                    )
                }
            }
        }
    }

    // Interactive Checkout BottomSheet
    if (activeCheckoutItem != null) {
        val item = activeCheckoutItem!!
        PaymentCheckoutSheet(
            itemTitle = item.title,
            price = item.price,
            period = item.period,
            creditsToAdd = item.creditsToAdd,
            newPlanName = item.newPlanName,
            viewModel = viewModel,
            onDismiss = { activeCheckoutItem = null },
            onSuccess = { receipt ->
                Toast.makeText(context, "Purchase complete! Receipt #$receipt", Toast.LENGTH_SHORT).show()
                activeCheckoutItem = null
            }
        )
    }

    // Transaction History Dialog
    if (showTransactionHistory) {
        TransactionHistoryDialog(
            viewModel = viewModel,
            onDismiss = { showTransactionHistory = false }
        )
    }

    // Voucher Redemption Dialog
    if (showVoucherDialog) {
        var code by remember { mutableStateOf("") }
        var resultMsg by remember { mutableStateOf<String?>(null) }

        AlertDialog(
            onDismissRequest = { showVoucherDialog = false },
            modifier = Modifier.imePadding(),
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.CardGiftcard, contentDescription = null, tint = SparkPrimary)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Redeem Grant or Voucher")
                }
            },
            text = {
                Column(
                    modifier = Modifier.imePadding(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "Enter your school grant or promotional code for instant credits & plan activation:",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    OutlinedTextField(
                        value = code,
                        onValueChange = { code = it },
                        placeholder = { Text("e.g. TEACHER2026, HOMESCHOOL, SPARKGIFT") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                    if (resultMsg != null) {
                        Text(
                            text = resultMsg!!,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (resultMsg!!.contains("Applied")) Color(0xFF047857) else Color.Red
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val (success, msg) = viewModel.redeemVoucher(code)
                        resultMsg = msg
                        if (success) {
                            Toast.makeText(context, "Voucher applied!", Toast.LENGTH_SHORT).show()
                        }
                    }
                ) {
                    Text("Redeem Code")
                }
            },
            dismissButton = {
                TextButton(onClick = { showVoucherDialog = false }) {
                    Text("Close")
                }
            }
        )
    }
}

@Composable
fun PaymentBadge(name: String, icon: androidx.compose.ui.graphics.vector.ImageVector, color: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(CircleShape)
                .background(color.copy(alpha = 0.1f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = name,
                tint = color,
                modifier = Modifier.size(20.dp)
            )
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(name, fontSize = 10.sp, fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.onSurface)
    }
}

@Composable
fun CreditPackRowItem(
    credits: Int,
    price: String,
    badge: String,
    isPopular: Boolean = false,
    onBuy: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .then(
                if (isPopular) Modifier.border(1.5.dp, SparkSecondary, RoundedCornerShape(10.dp)) else Modifier
            ),
        shape = RoundedCornerShape(10.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isPopular) SparkSecondaryContainer.copy(alpha = 0.25f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(SparkSecondaryContainer),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.Stars, contentDescription = null, tint = SparkSecondary, modifier = Modifier.size(20.dp))
                }
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    Text("$credits Credits", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    Text(badge, fontSize = 11.sp, color = SparkPrimary, fontWeight = FontWeight.SemiBold)
                }
            }

            Button(
                onClick = onBuy,
                shape = RoundedCornerShape(8.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isPopular) SparkSecondary else SparkPrimary
                )
            ) {
                Text(
                    price,
                    fontWeight = FontWeight.Bold,
                    color = if (isPopular) Color.Black else Color.White
                )
            }
        }
    }
}

@Composable
fun PlanCard(
    planName: String,
    price: String,
    period: String,
    badge: String,
    features: List<String>,
    isCurrent: Boolean,
    buttonText: String,
    isFeatured: Boolean = false,
    onSelect: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .then(
                if (isFeatured) Modifier.border(2.dp, SparkPrimary, RoundedCornerShape(16.dp)) else Modifier
            ),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isFeatured) SparkPrimaryContainer.copy(alpha = 0.25f) else MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = if (isFeatured) 4.dp else 2.dp)
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = planName,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = if (isFeatured) SparkPrimary else MaterialTheme.colorScheme.surfaceVariant
                ) {
                    Text(
                        text = badge,
                        color = if (isFeatured) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Row(verticalAlignment = Alignment.Bottom) {
                Text(
                    text = price,
                    fontSize = 28.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = period,
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 4.dp)
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            features.forEach { feat ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 3.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = null,
                        tint = if (isFeatured) SparkPrimary else Color(0xFF10B981),
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = feat,
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = onSelect,
                enabled = !isCurrent,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isFeatured) SparkPrimary else MaterialTheme.colorScheme.secondary
                )
            ) {
                Text(buttonText, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
fun CreditRateRow(operation: String, cost: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(operation, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(cost, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = SparkPrimary)
    }
}

