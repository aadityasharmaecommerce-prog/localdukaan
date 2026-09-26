package `in`.localdukaan.core.model
import `in`.localdukaan.core.database.ProductRow

/** Ek POS line — immutable snapshot (naam/price server-authoritative hote hain sync par). */
data class QuickSaleLine(val shopProductId:String,val name:String,val unit:String,val unitPricePaise:Long,val quantityMilli:Long,val costPricePaise:Long=0){
 val lineTotalPaise:Long get()=Math.multiplyExact(unitPricePaise,quantityMilli)/1000
 val lineCostPaise:Long get()=Math.multiplyExact(costPricePaise,quantityMilli)/1000
 val localStockWarning:Boolean=false}

enum class SalePaymentMode{PAID,CREDIT}
/** Pay karne ka tareeka — CASH default, UPI, aur udhaar (CREDIT). */
enum class PaymentMethod(val label:String){CASH("Cash"),UPI("UPI"),CREDIT("Udhaari")}

data class QuickSaleState(
 val lines:Map<String,QuickSaleLine> = emptyMap(),
 val paymentMode:SalePaymentMode=SalePaymentMode.PAID,
 val shopCustomerId:String?=null,
 val discountPaise:Long=0,
 val method:PaymentMethod=PaymentMethod.CASH,
 val walkIn:Boolean=true,
 val customerName:String?=null,
 val customerPhone:String?=null
){
 fun selectPayment(mode:SalePaymentMode,customerId:String?=shopCustomerId)=copy(paymentMode=mode,shopCustomerId=if(mode==SalePaymentMode.CREDIT) customerId else null)
 fun selectMethod(m:PaymentMethod)=copy(method=m)
 fun setDiscount(paise:Long)=copy(discountPaise=paise.coerceIn(0,subtotalPaise))
 /** Walk-in = koi pehchaan nahi; sirf udhaari ke liye customer zaroori. */
 fun identify(name:String?,phone:String?):QuickSaleState=if(name.isNullOrBlank()&&phone.isNullOrBlank())copy(walkIn=true,customerName=null,customerPhone=null) else copy(walkIn=false,customerName=name?.trim()?.takeIf{it.isNotBlank()},customerPhone=phone?.trim()?.takeIf{it.isNotBlank()})
 fun walkInCustomer():QuickSaleState=copy(walkIn=true,customerName=null,customerPhone=null)
  val subtotalPaise:Long get()=lines.values.fold(0L){sum,line->Math.addExact(sum,line.lineTotalPaise)}
  /** Sirf wahi lines jinka cost set hai — warna munafa overestimate hota. */
  val estimatedProfitPaise:Long get()=lines.values.filter{it.costPricePaise>0}.fold(0L){sum,line->Math.addExact(sum,(line.unitPricePaise-line.costPricePaise)*line.quantityMilli/1000)}
 val totalPaise:Long get()=(subtotalPaise-discountPaise).coerceAtLeast(0)
 val estimatedCostPaise:Long get()=lines.values.fold(0L){sum,line->Math.addExact(sum,line.lineCostPaise)}
 val canSubmit:Boolean get()=lines.isNotEmpty()&&(paymentMode==SalePaymentMode.PAID||!shopCustomerId.isNullOrBlank())
 /** Udhaari bina customer ke mana hai — rule #18. */
 val udhaariRequiresCustomer:Boolean get()=method==PaymentMethod.CREDIT&&walkIn
 val canComplete:Boolean get()=lines.isNotEmpty()&&!udhaariRequiresCustomer&&(paymentMode==SalePaymentMode.PAID||!shopCustomerId.isNullOrBlank())
 fun scan(row:ProductRow):QuickSaleState{val id=row.shopProduct.id;val current=lines[id];val next=(current?.quantityMilli?:0)+1000;return copy(lines=lines+(id to QuickSaleLine(id,row.product.name,row.product.unit,row.shopProduct.sellingPricePaise,next,row.shopProduct.costPricePaise)))}
 fun setQuantity(id:String,quantityMilli:Long):QuickSaleState=if(quantityMilli<=0)copy(lines=lines-id)else lines[id]?.let{copy(lines=lines+(id to it.copy(quantityMilli=quantityMilli)))}?:this
 fun removeLine(id:String)=copy(lines=lines-id)
 fun hasLocalStockWarning(products:List<ProductRow>)=lines.values.any{line->products.firstOrNull{it.shopProduct.id==line.shopProductId}?.let{it.shopProduct.stockQuantityMilli-it.shopProduct.reservedQuantityMilli<line.quantityMilli}?:true}
}

/** Success screen ka summary — sale complete hone ke turant baad. */
data class SaleSuccess(val saleId:String,val totalPaise:Long,val subtotalPaise:Long,val discountPaise:Long,val methodLabel:String,val customerLabel:String,val paidPaise:Long,val duePaise:Long,val profitPaise:Long=0)

/** Receipt screen ke liye poora sale snapshot (local PENDING + synced COMPLETED dono chalte hain). */
data class ReceiptData(val id:String,val shopName:String,val shopLine:String?,val createdAt:Long,val items:List<QuickSaleLine>,val subtotalPaise:Long,val discountPaise:Long,val totalPaise:Long,val methodLabel:String,val customerLabel:String,val paidPaise:Long,val duePaise:Long,val pendingSync:Boolean)

sealed interface BarcodeResolution {data class Local(val row:ProductRow):BarcodeResolution;data class Global(val id:String,val name:String,val brand:String?,val unit:String,val barcode:String,val imageBase64:String?=null,val imageUrl:String?=null):BarcodeResolution;data class NotFound(val barcode:String):BarcodeResolution;data class Invalid(val message:String):BarcodeResolution}
object ScannerGate {fun accept(code:String,lastCode:String?,lastAtMs:Long,nowMs:Long,windowMs:Long=800)=code!=lastCode||nowMs-lastAtMs>=windowMs}
