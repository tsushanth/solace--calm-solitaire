package com.factory.solacecalmsolitaire.billing

/** Outcome of a purchase attempt, surfaced to the UI so it can react to every edge case. */
sealed class PurchaseResult {
    data class Success(val productId: String) : PurchaseResult()
    data class TipReceived(val productId: String) : PurchaseResult()
    data object Pending : PurchaseResult()
    data object UserCancelled : PurchaseResult()
    data object AlreadyOwned : PurchaseResult()
    data object NetworkError : PurchaseResult()
    data class Error(val message: String) : PurchaseResult()
}

enum class BillingConnectionState {
    CONNECTING,
    CONNECTED,
    DISCONNECTED,
    UNAVAILABLE
}
