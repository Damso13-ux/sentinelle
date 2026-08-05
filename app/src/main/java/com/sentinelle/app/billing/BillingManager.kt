package com.sentinelle.app.billing

import android.app.Activity
import android.content.Context
import android.util.Log
import com.android.billingclient.api.AcknowledgePurchaseParams
import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.BillingClientStateListener
import com.android.billingclient.api.BillingFlowParams
import com.android.billingclient.api.BillingResult
import com.android.billingclient.api.PendingPurchasesParams
import com.android.billingclient.api.Purchase
import com.android.billingclient.api.PurchasesUpdatedListener
import com.android.billingclient.api.QueryProductDetailsParams
import com.android.billingclient.api.QueryPurchasesParams
import com.sentinelle.app.util.PreferencesManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/**
 * Wraps Google Play Billing for the single "Sentinelle Pro" one-time
 * unlock. There are no consumables and no subscriptions — one
 * non-consumable product ([PRO_PRODUCT_ID]) unlocks every Pro feature at
 * once. This class's job is only to keep the local entitlement flag
 * (PreferencesManager.setProUnlocked) in sync with what Play actually
 * records; gated screens should read that flag, not talk to this class
 * directly, so they never block on a network round-trip.
 *
 * PRO_PRODUCT_ID has no corresponding product in Play Console yet — the
 * merchant/payment profile is still pending verification as of this
 * writing. Nothing purchases anything for real until that product exists
 * there with this exact ID.
 */
class BillingManager(
    private val context: Context,
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val purchasesUpdatedListener =
        PurchasesUpdatedListener { billingResult, purchases ->
            when {
                billingResult.responseCode == BillingClient.BillingResponseCode.OK && purchases != null -> {
                    purchases.forEach { handlePurchase(it) }
                }
                billingResult.responseCode == BillingClient.BillingResponseCode.USER_CANCELED -> {
                    // Nothing to do — not an error, just a closed dialog.
                }
                else -> Log.e(TAG, "Purchase update error: ${billingResult.debugMessage}")
            }
        }

    private val billingClient =
        BillingClient
            .newBuilder(context)
            .setListener(purchasesUpdatedListener)
            .enablePendingPurchases(
                PendingPurchasesParams
                    .newBuilder()
                    .enableOneTimeProducts()
                    .build(),
            ).enableAutoServiceReconnection()
            .build()

    /**
     * Connects to Play and, once ready, re-checks Play's purchase records
     * so a refund or a restore on a new device is reflected locally —
     * never assume the cached preference is still correct just because it
     * was true before. Call this once per screen/session that cares about
     * entitlement (e.g. on Settings screen start), not just after a
     * purchase.
     */
    fun startConnection(onReady: () -> Unit = {}) {
        billingClient.startConnection(
            object : BillingClientStateListener {
                override fun onBillingSetupFinished(billingResult: BillingResult) {
                    if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                        restorePurchases()
                        onReady()
                    } else {
                        Log.e(TAG, "Billing setup failed: ${billingResult.debugMessage}")
                    }
                }

                override fun onBillingServiceDisconnected() {
                    // enableAutoServiceReconnection() handles retry/backoff —
                    // nothing to do here.
                }
            },
        )
    }

    private fun restorePurchases() {
        val params =
            QueryPurchasesParams
                .newBuilder()
                .setProductType(BillingClient.ProductType.INAPP)
                .build()

        billingClient.queryPurchasesAsync(params) { billingResult, purchases ->
            if (billingResult.responseCode != BillingClient.BillingResponseCode.OK) return@queryPurchasesAsync

            val stillOwnsPro =
                purchases.any { purchase ->
                    purchase.products.contains(PRO_PRODUCT_ID) &&
                        purchase.purchaseState == Purchase.PurchaseState.PURCHASED
                }
            purchases.forEach { handlePurchase(it) }
            scope.launch { PreferencesManager.setProUnlocked(context, stillOwnsPro) }
        }
    }

    /** Looks up the Pro product and opens Play's purchase sheet for it. */
    fun launchPurchaseFlow(
        activity: Activity,
        onError: (String) -> Unit = {},
    ) {
        val productList =
            listOf(
                QueryProductDetailsParams.Product
                    .newBuilder()
                    .setProductId(PRO_PRODUCT_ID)
                    .setProductType(BillingClient.ProductType.INAPP)
                    .build(),
            )
        val params = QueryProductDetailsParams.newBuilder().setProductList(productList).build()

        billingClient.queryProductDetailsAsync(params) { billingResult, result ->
            val productDetails = result.productDetailsList.firstOrNull()
            if (billingResult.responseCode != BillingClient.BillingResponseCode.OK || productDetails == null) {
                Log.e(TAG, "Product lookup failed: ${billingResult.debugMessage}")
                onError("Achat indisponible pour le moment.")
                return@queryProductDetailsAsync
            }

            val productDetailsParamsList =
                listOf(
                    BillingFlowParams.ProductDetailsParams
                        .newBuilder()
                        .setProductDetails(productDetails)
                        .build(),
                )
            val flowParams =
                BillingFlowParams
                    .newBuilder()
                    .setProductDetailsParamsList(productDetailsParamsList)
                    .build()
            billingClient.launchBillingFlow(activity, flowParams)
        }
    }

    private fun handlePurchase(purchase: Purchase) {
        if (purchase.purchaseState != Purchase.PurchaseState.PURCHASED) return
        if (!purchase.products.contains(PRO_PRODUCT_ID)) return

        scope.launch {
            PreferencesManager.setProUnlocked(context, true)
        }

        // Non-consumables must be acknowledged within 3 days or Play
        // auto-refunds them — do this every time, acknowledgePurchase() is
        // a no-op (OK response) if already acknowledged.
        if (!purchase.isAcknowledged) {
            val ackParams =
                AcknowledgePurchaseParams
                    .newBuilder()
                    .setPurchaseToken(purchase.purchaseToken)
                    .build()
            billingClient.acknowledgePurchase(ackParams) { billingResult ->
                if (billingResult.responseCode != BillingClient.BillingResponseCode.OK) {
                    Log.e(TAG, "Acknowledge failed: ${billingResult.debugMessage}")
                }
            }
        }
    }

    fun endConnection() {
        billingClient.endConnection()
    }

    companion object {
        private const val TAG = "BillingManager"

        // Must match the in-app product ID configured in Play Console.
        const val PRO_PRODUCT_ID = "sentinelle_pro"
    }
}
