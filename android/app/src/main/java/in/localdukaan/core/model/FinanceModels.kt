package `in`.localdukaan.core.model

/** Finance screen ka summary — sab kuch sales/inventory/credit se AUTOMATIC banta hai, koi alag form nahi. */
data class FinanceSummary(
    val todayBills:Int,
    val totalCollectionPaise:Long,
    val cashPaise:Long,
    val upiPaise:Long,
    val udhaarPaise:Long,
    val outstandingDuesPaise:Long,
    val dueCustomers:Int,
    val profitEstimatePaise:Long?
)
