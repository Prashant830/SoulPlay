package com.souljoy.soulmasti.ui.shop

import androidx.activity.ComponentActivity
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.android.billingclient.api.ProductDetails
import com.souljoy.soulmasti.R
import com.souljoy.soulmasti.ui.common.SoulplayCenterAlignedTopBar
import com.souljoy.soulmasti.data.billing.InAppCoinProducts
import org.koin.androidx.compose.koinViewModel

private fun ProductDetails.formattedOneTimePrice(): String =
    oneTimePurchaseOfferDetails?.formattedPrice ?: "—"

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GoldShopScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    vm: GoldShopViewModel = koinViewModel(),
) {
    val activity = LocalContext.current as ComponentActivity
    val state by vm.uiState.collectAsStateWithLifecycle()
    val coinBalance by vm.coinBalance.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(Unit) {
        vm.refreshProducts()
    }

    LaunchedEffect(state.userMessage) {
        val msg = state.userMessage ?: return@LaunchedEffect
        snackbarHostState.showSnackbar(msg)
        vm.consumeUserMessage()
    }

    val screenWidthDp = LocalConfiguration.current.screenWidthDp
    val isCompactWidth = screenWidthDp < 400

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            SoulplayCenterAlignedTopBar(
                title = stringResource(R.string.gold_shop_title),
                onBack = onBack,
                backContentDescription = stringResource(R.string.gold_shop_back),
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = if (isCompactWidth) 10.dp else 16.dp),
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant)
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = stringResource(R.string.gold_shop_balance_label),
                    style = MaterialTheme.typography.titleSmall,
                )
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(text = "\uD83E\uDE99", fontSize = 20.sp)
                    Text(
                        text = coinBalance?.toString() ?: "…",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(
                        Brush.horizontalGradient(
                            listOf(Color(0xFFFFB74D), Color(0xFFFF7043), Color(0xFFE64A19)),
                        ),
                    )
                    .padding(14.dp),
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        text = stringResource(R.string.gold_shop_promo_dates),
                        style = MaterialTheme.typography.labelSmall,
                        color = Color(0xFF5D4037),
                    )
                    Text(
                        text = stringResource(R.string.gold_shop_promo_headline),
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.ExtraBold),
                        color = Color(0xFF3E2723),
                    )
                    Text(
                        text = stringResource(R.string.gold_shop_promo_sub),
                        style = MaterialTheme.typography.labelSmall,
                        color = Color(0xFF5D4037),
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            when {
                state.loading -> {
                    Box(modifier = Modifier.fillMaxWidth().weight(1f), contentAlignment = Alignment.Center) {
                        Text(stringResource(R.string.gold_shop_loading), style = MaterialTheme.typography.bodyLarge)
                    }
                }
                state.billingError != null -> {
                    Text(
                        text = state.billingError.orEmpty(),
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
                else -> {
                    // Always 3 columns so layout matches every screen size; cards scale text to avoid wraps.
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(3),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        contentPadding = PaddingValues(bottom = 16.dp),
                        modifier = Modifier.weight(1f),
                    ) {
                        items(state.products, key = { it.productId }) { details ->
                            CoinPackCard(
                                productDetails = details,
                                compact = isCompactWidth,
                                onBuy = { vm.launchPurchase(activity, details) },
                            )
                        }
                    }
                }
            }

            Column(
                modifier = Modifier.padding(bottom = 12.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Text(
                    text = stringResource(R.string.gold_shop_footer_1),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    lineHeight = 16.sp,
                )
                Text(
                    text = stringResource(R.string.gold_shop_footer_2),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    lineHeight = 16.sp,
                )
                Text(
                    text = stringResource(R.string.gold_shop_footer_3),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    lineHeight = 16.sp,
                )
            }
        }
    }
}

@Composable
private fun CoinPackCard(
    productDetails: ProductDetails,
    compact: Boolean,
    onBuy: () -> Unit,
) {
    val coins = InAppCoinProducts.coinsFromProductDetails(productDetails) ?: 0L
    val bonus = InAppCoinProducts.bonusLabelForProductId(productDetails.productId)
    val price = productDetails.formattedOneTimePrice()
    val tierEmoji = when (productDetails.productId) {
        InAppCoinProducts.COIN_1 -> "\uD83E\uDE99"
        InAppCoinProducts.COIN_2 -> "\uD83E\uDE99\uD83E\uDE99"
        InAppCoinProducts.COIN_3 -> "\uD83E\uDE99\uD83E\uDE99\uD83E\uDE99"
        InAppCoinProducts.COIN_4 -> "💰"
        InAppCoinProducts.COIN_5 -> "💰💰"
        else -> "🏆"
    }

    val pad = if (compact) 6.dp else 8.dp
    val titleStyle = if (compact) {
        MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold)
    } else {
        MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
    }
    val priceFontSize = if (compact) 10.sp else 12.sp

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = if (compact) 168.dp else 176.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(
                Brush.verticalGradient(
                    listOf(Color(0xFFFF8A50), Color(0xFFFF5722)),
                ),
            )
            .padding(pad),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween,
    ) {
        if (bonus != null) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 2.dp),
                contentAlignment = Alignment.CenterStart,
            ) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color(0xFFFFEB3B))
                        .padding(horizontal = 4.dp, vertical = 2.dp),
                ) {
                    Text(
                        text = bonus,
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = if (compact) 9.sp else 11.sp),
                        color = Color(0xFF5D4037),
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }
        // Emoji + gold label centered (fixed height so layout works inside LazyVerticalGrid cells)
        val centerBlockHeight = if (compact) 84.dp else 92.dp
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(centerBlockHeight),
            contentAlignment = Alignment.Center,
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Text(
                    text = tierEmoji,
                    fontSize = if (compact) 22.sp else 28.sp,
                    textAlign = TextAlign.Center,
                )
                Text(
                    text = "$coins ${stringResource(R.string.gold_shop_gold_suffix)}",
                    style = titleStyle,
                    color = Color.White,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    textAlign = TextAlign.Center,
                    lineHeight = 16.sp,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
        Button(
            onClick = onBuy,
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = Color.White,
                contentColor = Color(0xFFE64A19),
            ),
            modifier = Modifier.fillMaxWidth(),
            contentPadding = PaddingValues(horizontal = 4.dp, vertical = 6.dp),
        ) {
            Text(
                text = price,
                fontWeight = FontWeight.Bold,
                fontSize = priceFontSize,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}
