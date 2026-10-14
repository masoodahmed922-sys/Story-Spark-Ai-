package com.example.ui.components

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.TransactionRecord
import com.example.ui.StorySparkViewModel
import com.example.ui.theme.*
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun TransactionHistoryDialog(
    viewModel: StorySparkViewModel,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val transactions by viewModel.transactions.collectAsState()
    val subDetails by viewModel.subscriptionDetails.collectAsState()

    var showCancelSubDialog by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.AutoMirrored.Filled.ReceiptLong, contentDescription = null, tint = SparkPrimary)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Billing & Payment History", fontSize = 17.sp, fontWeight = FontWeight.Bold)
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 420.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Subscription Status Card
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Active Plan: ${subDetails.planName}",
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp
                            )
                            Surface(
                                shape = RoundedCornerShape(4.dp),
                                color = if (subDetails.autoRenew) Color(0xFFD1FAE5) else Color(0xFFFEF3C7)
                            ) {
                                Text(
                                    text = subDetails.status,
                                    color = if (subDetails.autoRenew) Color(0xFF047857) else Color(0xFFB45309),
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }

                        if (subDetails.planName != "Free Plan") {
                            val renewDate = SimpleDateFormat("MMM d, yyyy", Locale.getDefault()).format(Date(subDetails.nextRenewalDate))
                            Text(
                                text = if (subDetails.autoRenew) "Next renewal: $renewDate ($${subDetails.price}/mo)" else "Access expires on $renewDate",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )

                            if (subDetails.autoRenew) {
                                Spacer(modifier = Modifier.height(6.dp))
                                TextButton(
                                    onClick = { showCancelSubDialog = true },
                                    contentPadding = PaddingValues(0.dp)
                                ) {
                                    Text("Cancel Auto-Renewal", fontSize = 11.sp, color = Color.Red)
                                }
                            }
                        }
                    }
                }

                Text(
                    text = "Past Transactions (${transactions.size})",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold
                )

                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(transactions) { txn ->
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(8.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(10.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(
                                        modifier = Modifier
                                            .size(32.dp)
                                            .clip(CircleShape)
                                            .background(SparkPrimaryContainer),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            Icons.Default.Check,
                                            contentDescription = null,
                                            tint = SparkPrimary,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Column {
                                        Text(txn.title, fontWeight = FontWeight.SemiBold, fontSize = 12.sp)
                                        val dateStr = SimpleDateFormat("MMM d, yyyy", Locale.getDefault()).format(Date(txn.timestamp))
                                        Text("${txn.paymentMethod.displayName} • $dateStr", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        Text(txn.receiptNumber, fontSize = 9.sp, color = SparkPrimary)
                                    }
                                }

                                Text(
                                    text = if (txn.amount == 0.0) "FREE" else "$${txn.amount}",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = onDismiss) {
                Text("Close")
            }
        }
    )

    if (showCancelSubDialog) {
        AlertDialog(
            onDismissRequest = { showCancelSubDialog = false },
            title = { Text("Cancel Auto-Renewal?") },
            text = {
                Text(
                    "Your plan benefits will remain active until the end of the current billing cycle. You will not be charged again.",
                    fontSize = 13.sp
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.cancelSubscription()
                        showCancelSubDialog = false
                        Toast.makeText(context, "Subscription auto-renewal canceled.", Toast.LENGTH_SHORT).show()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color.Red)
                ) {
                    Text("Confirm Cancellation")
                }
            },
            dismissButton = {
                TextButton(onClick = { showCancelSubDialog = false }) {
                    Text("Keep Active")
                }
            }
        )
    }
}
