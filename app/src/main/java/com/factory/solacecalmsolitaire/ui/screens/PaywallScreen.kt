package com.factory.solacecalmsolitaire.ui.screens

import android.app.Activity
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.WorkspacePremium
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.factory.solacecalmsolitaire.R
import com.factory.solacecalmsolitaire.billing.BillingConnectionState
import com.factory.solacecalmsolitaire.billing.PremiumTier
import com.factory.solacecalmsolitaire.premium.PaywallTrigger
import com.factory.solacecalmsolitaire.premium.PremiumFeature
import com.factory.solacecalmsolitaire.viewmodel.PaywallViewModel

private fun headline(trigger: PaywallTrigger): Pair<String, String> = when (trigger) {
    PaywallTrigger.FIRST_LAUNCH -> "Welcome to Solace" to "Unlock every mode, theme, and statistic from the start."
    PaywallTrigger.DRAW_ONE_MODE -> "Draw One is a Premium mode" to "Upgrade to play the purist's challenge."
    PaywallTrigger.UNLIMITED_UNDO -> "You're out of free undos" to "Go premium for unlimited undo, every game."
    PaywallTrigger.AUTO_COMPLETE -> "Auto-Complete is Premium" to "Finish winnable games instantly with one tap."
    PaywallTrigger.ADVANCED_STATISTICS -> "Advanced stats are Premium" to "See your best time, best score, and streaks."
    PaywallTrigger.FELT_THEME -> "This felt theme is Premium" to "Unlock every table theme with Solace Premium."
    PaywallTrigger.SETTINGS_UPGRADE -> "Solace Premium" to "One upgrade, the whole game."
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PaywallScreen(
    trigger: PaywallTrigger,
    hapticsEnabled: Boolean,
    onClose: () -> Unit,
    viewModel: PaywallViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val message by viewModel.message.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val context = LocalContext.current
    val activity = context as? Activity
    val haptic = LocalHapticFeedback.current
    fun tapFeedback() {
        if (hapticsEnabled) haptic.performHapticFeedback(HapticFeedbackType.LongPress)
    }

    LaunchedEffect(message) {
        message?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.consumeMessage()
        }
    }

    LaunchedEffect(uiState.isPremium) {
        if (uiState.isPremium) viewModel.markInitialPaywallSeen()
    }

    Dialog(onDismissRequest = onClose, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Scaffold(
            snackbarHost = { SnackbarHost(snackbarHostState) { Snackbar(it) } },
            containerColor = MaterialTheme.colorScheme.background
        ) { padding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .statusBarsPadding()
                    .navigationBarsPadding()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 24.dp)
            ) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    IconButton(onClick = { tapFeedback(); onClose() }) {
                        Icon(Icons.Filled.Close, contentDescription = stringResource(R.string.cd_close))
                    }
                }

                val (title, subtitle) = headline(trigger)

                Icon(
                    imageVector = Icons.Filled.WorkspacePremium,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(48.dp)
                )
                Spacer(modifier = Modifier.height(12.dp))
                Text(title, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    subtitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.8f)
                )

                Spacer(modifier = Modifier.height(20.dp))

                PremiumFeature.premiumHighlights.forEach { feature ->
                    FeatureHighlightRow(title = feature.title, description = feature.description)
                }

                Spacer(modifier = Modifier.height(20.dp))

                when {
                    uiState.isPremium -> PremiumActiveCard(activeTier = uiState.activeTier, packageName = context.packageName)
                    else -> {
                        if (uiState.connectionState == BillingConnectionState.DISCONNECTED ||
                            uiState.connectionState == BillingConnectionState.UNAVAILABLE
                        ) {
                            ConnectionErrorBanner(onRetry = { tapFeedback(); viewModel.retryConnection() })
                            Spacer(modifier = Modifier.height(12.dp))
                        } else if (uiState.connectionState == BillingConnectionState.CONNECTING && uiState.productDetails.isEmpty()) {
                            ConnectingBanner()
                            Spacer(modifier = Modifier.height(12.dp))
                        }

                        val buttonsEnabled = activity != null && !uiState.isPurchasing
                        PremiumTier.entries.forEach { tier ->
                            TierButton(
                                tier = tier,
                                priceLabel = uiState.priceLabel(tier),
                                enabled = buttonsEnabled,
                                isLoading = uiState.isPurchasing,
                                onClick = { tapFeedback(); activity?.let { viewModel.purchase(it, tier) } }
                            )
                            Spacer(modifier = Modifier.height(10.dp))
                        }

                        Spacer(modifier = Modifier.height(8.dp))
                        TipRow(
                            priceLabel = uiState.tipPriceLabel,
                            enabled = buttonsEnabled,
                            onClick = { tapFeedback(); activity?.let { viewModel.purchaseTip(it) } }
                        )

                        Spacer(modifier = Modifier.height(16.dp))
                        TextButton(
                            onClick = { tapFeedback(); viewModel.restorePurchases() },
                            enabled = !uiState.isRestoring,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(if (uiState.isRestoring) "Restoring…" else "Restore purchases")
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))
                LegalLinksRow(context = context)
                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }
}

@Composable
private fun FeatureHighlightRow(title: String, description: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        verticalAlignment = Alignment.Top
    ) {
        Icon(
            imageVector = Icons.Filled.CheckCircle,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(top = 2.dp).size(18.dp)
        )
        Spacer(modifier = Modifier.width(12.dp))
        Column {
            Text(title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
            Text(
                description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f)
            )
        }
    }
}

@Composable
private fun TierButton(
    tier: PremiumTier,
    priceLabel: String,
    enabled: Boolean,
    isLoading: Boolean,
    onClick: () -> Unit
) {
    val isBestValue = tier == PremiumTier.YEARLY
    val label = when (tier) {
        PremiumTier.WEEKLY -> "Weekly"
        PremiumTier.MONTHLY -> "Monthly"
        PremiumTier.YEARLY -> "Yearly"
        PremiumTier.LIFETIME -> "Lifetime"
    }
    val description = label + (if (isBestValue) ", best value" else "") + ", $priceLabel"
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .border(
                width = if (isBestValue) 2.dp else 1.dp,
                color = if (isBestValue) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onBackground.copy(alpha = 0.2f),
                shape = RoundedCornerShape(12.dp)
            )
            .clickable(enabled = enabled, onClick = onClick)
            .semantics(mergeDescendants = true) { contentDescription = description }
            .padding(16.dp)
    ) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(label, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    if (isBestValue) {
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            "BEST VALUE",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onPrimary,
                            modifier = Modifier
                                .background(MaterialTheme.colorScheme.primary, RoundedCornerShape(4.dp))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }
                if (tier == PremiumTier.LIFETIME) {
                    Text(
                        "Pay once, own it forever",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f)
                    )
                }
            }
            if (isLoading) {
                CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
            } else {
                Text(
                    text = if (tier.isSubscription) "$priceLabel${tier.fallbackPeriod}" else priceLabel,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
private fun TipRow(priceLabel: String, enabled: Boolean, onClick: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text("Enjoying Solace?", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
            Text(
                "Leave a small tip to support development.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f)
            )
        }
        Spacer(modifier = Modifier.width(12.dp))
        OutlinedButton(onClick = onClick, enabled = enabled) {
            Text(priceLabel)
        }
    }
}

@Composable
private fun PremiumActiveCard(activeTier: PremiumTier?, packageName: String) {
    val context = LocalContext.current
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(12.dp))
            .padding(16.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Filled.CheckCircle, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            Spacer(modifier = Modifier.width(8.dp))
            Text("You're Premium", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        }
        if (activeTier != null && activeTier.isSubscription) {
            Spacer(modifier = Modifier.height(8.dp))
            TextButton(onClick = {
                val uri = Uri.parse(
                    "https://play.google.com/store/account/subscriptions?sku=${activeTier.productId}&package=$packageName"
                )
                context.startActivity(Intent(Intent.ACTION_VIEW, uri))
            }) {
                Text("Manage subscription")
            }
        }
    }
}

@Composable
private fun ConnectionErrorBanner(onRetry: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.error.copy(alpha = 0.15f), RoundedCornerShape(8.dp))
            .padding(12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            "No connection to Google Play. Prices shown may be out of date.",
            style = MaterialTheme.typography.bodySmall,
            modifier = Modifier.weight(1f)
        )
        TextButton(onClick = onRetry) { Text("Retry") }
    }
}

@Composable
private fun ConnectingBanner() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(8.dp))
            .padding(12.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        CircularProgressIndicator(modifier = Modifier.size(14.dp), strokeWidth = 2.dp)
        Spacer(modifier = Modifier.width(8.dp))
        Text(stringResource(R.string.paywall_connecting), style = MaterialTheme.typography.bodySmall)
    }
}

@Composable
private fun LegalLinksRow(context: android.content.Context) {
    val termsUrl = stringResource(R.string.legal_terms_url)
    val privacyUrl = stringResource(R.string.legal_privacy_url)
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
        Text(
            "Terms of Service",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f),
            modifier = Modifier.clickable(onClickLabel = "Opens Terms of Service in browser") {
                context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(termsUrl)))
            }
        )
        Text(
            "  •  ",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
        )
        Text(
            "Privacy Policy",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f),
            modifier = Modifier.clickable(onClickLabel = "Opens Privacy Policy in browser") {
                context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(privacyUrl)))
            }
        )
    }
}
