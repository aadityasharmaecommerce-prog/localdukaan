package `in`.localdukaan.core.model
import java.math.BigDecimal
import java.math.RoundingMode

data class SelectedImage(val bytes:ByteArray,val mimeType:String,val previewLabel:String)
data class ProductDraft(val name:String,val brand:String="",val barcode:String="",val costPriceText:String="",val unit:String,val priceText:String,val openingStockText:String,val minimumStockText:String,val description:String="",val available:Boolean=true)
data class ValidationResult(val valid:Boolean,val message:String?=null)
object ProductRules {
 fun normalizeBarcode(value:String):String?=value.replace(" ","").replace("-","").uppercase().ifBlank{null}
 fun priceToPaise(value:String):Long?=try{val n=BigDecimal(value.trim());if(n.signum()<0||n.scale()>2)null else n.multiply(BigDecimal(100)).setScale(0,RoundingMode.UNNECESSARY).longValueExact()}catch(_:Exception){null}
 fun quantityToMilli(value:String):Long?=try{val n=BigDecimal(value.trim());if(n.signum()<0||n.scale()>3)null else n.multiply(BigDecimal(1000)).setScale(0,RoundingMode.UNNECESSARY).longValueExact()}catch(_:Exception){null}
 fun validate(d:ProductDraft):ValidationResult {if(d.name.isBlank())return ValidationResult(false,"Product name is required");if(d.unit.isBlank())return ValidationResult(false,"Unit is required");if(priceToPaise(d.priceText)==null)return ValidationResult(false,"Enter a valid non-negative price with up to 2 decimals");val c=d.costPriceText.trim();if(c.isNotEmpty()&&priceToPaise(c)==null)return ValidationResult(false,"Cost price is invalid");if(quantityToMilli(d.openingStockText)==null)return ValidationResult(false,"Opening stock is invalid");if(quantityToMilli(d.minimumStockText)==null)return ValidationResult(false,"Minimum stock is invalid");val b=normalizeBarcode(d.barcode);if(b!=null&&!b.matches(Regex("[0-9A-Z]{6,32}")))return ValidationResult(false,"Barcode is invalid");return ValidationResult(true)}
 fun isLowStock(stockMilli:Long,minimumMilli:Long)=stockMilli<=minimumMilli
 fun rupees(paise:Long)="₹%.2f".format(paise/100.0)
 fun quantity(milli:Long)="${"%.3f".format(milli/1000.0).trimEnd('0').trimEnd('.')}"
}
/** GS1 helpers for retail scanning of grocery, household and cosmetic packs.
 *  Three things make scanning "every product on the shelf" work:
 *  1. a check-digit test that throws away half-read frames (junk reads),
 *  2. the equivalent forms of the same number (UPC-A <-> EAN-13 <-> GTIN-14) because
 *     the printed symbol, the shop catalogue and the global databases disagree on padding,
 *  3. pulling the GTIN out of the QR / DataMatrix printed next to the barcode. */
object BarcodeRules {
 private val PRODUCT_LENGTHS=intArrayOf(8,12,13,14)
 fun digits(raw:String?)=raw?.filter(Char::isDigit).orEmpty()
 fun clean(raw:String?)=raw?.filterNot{it==' '||it=='-'||it=='\t'||it=='\n'}?.uppercase()?.ifBlank{null}
 /** GS1 mod-10 check digit — true for a genuine EAN-8/EAN-13/UPC-A/UPC-E/GTIN-14 symbol. */
 fun checkDigitOk(code:String?):Boolean{
  if(code==null||code.length !in PRODUCT_LENGTHS)return false
  if(code.any{it<'0'||it>'9'})return false
  var sum=0
  for(i in 0 until code.length-1){val d=code[code.length-2-i]-'0';sum+=if(i%2==0)d*3 else d}
  return (10-sum%10)%10==code[code.length-1]-'0'
 }
 /** A number a shopkeeper would actually scan: right length and a valid check digit. */
 fun isProductCode(code:String?)=code!=null&&code.length in PRODUCT_LENGTHS&&checkDigitOk(code)
 /** Anything worth handing to the catalogue: a valid number, or an alphanumeric Code-39/128 label. */
 fun isAcceptable(code:String?):Boolean{
  val c=clean(code)?:return false
  if(isProductCode(c))return true
  // A UPC-E symbol carries the UPC-A check digit, so it never validates as an EAN-8.
  if(c.length==8&&c.all{it.isDigit()}&&c!="00000000")return true
  return c.length in 6..32&&c.all{it.isDigit()||it in 'A'..'Z'}&&c.any{it in 'A'..'Z'}
 }
 /** Zero-padding never changes a GS1 check digit, so all of these are the same product. */
 fun candidates(raw:String?):List<String>{
  val d=digits(clean(raw))
  if(d.length !in PRODUCT_LENGTHS)return emptyList()
  val out=LinkedHashSet<String>();out.add(d)
  when(d.length){
   14->out.add(d.substring(1))
   13->if(d[0]=='0')out.add(d.substring(1))
   12->out.add("0$d")
  }
  if(d.length==12)out.add("00$d")
  if(d.length==13)out.add("0$d")
  return out.toList()
 }
 private fun preferEan(code:String):String{val t=code.trimStart('0');return if(t.length==12||t.length==13)t else code}
 /** 2D codes on modern Indian packs usually carry the GTIN inside a URL or a GS1 AI string. */
 fun extractProductCode(raw:String?):String?{
  val t=clean(raw)?.replace(" ","")?:return null
  if(isProductCode(t))return t
  val ai=t.indexOf("01")
  if(ai>=0&&ai+16<=t.length){val w=t.substring(ai+2,ai+16);if(w.all{it.isDigit()}&&checkDigitOk(w))return preferEan(w)}
  for(len in intArrayOf(13,12,8)){var i=0;while(i+len<=t.length){val w=t.substring(i,i+len);if(w.all{it.isDigit()}&&checkDigitOk(w))return w;i++}}
  return if(isAcceptable(t))t else null
 }
}
object ReportMath {
 data class Daily(val epochDay:Long,val salesPaise:Long,val creditPaise:Long,val profitPaise:Long,val billCount:Int,val itemsSoldMilli:Long)
 fun daily(sales:List<SaleAgg>,items:List<ItemAgg>):List<Daily>{
  val byDay=linkedMapOf<Long,LongAgg>()
  fun bucket(day:Long)=byDay.getOrPut(day){LongAgg()}
  for(s in sales){val b=bucket(s.epochDay);b.sales+=s.totalPaise;b.credit+=s.creditPaise;if(s.status=="COMPLETED")b.bills++}
  for(i in items){val b=bucket(i.epochDay);b.profit+=i.lineTotalPaise-i.costTotalPaise;b.items+=i.quantityMilli}
  return byDay.map{(d,a)->Daily(d,a.sales,a.credit,a.profit,a.bills,a.items)}.sortedByDescending{it.epochDay}
 }
 data class LongAgg(var sales:Long=0,var credit:Long=0,var profit:Long=0,var bills:Int=0,var items:Long=0)
 data class SaleAgg(val epochDay:Long,val totalPaise:Long,val creditPaise:Long,val status:String)
 data class ItemAgg(val epochDay:Long,val quantityMilli:Long,val lineTotalPaise:Long,val costTotalPaise:Long)
 fun todayEpochDay(now:Long=System.currentTimeMillis()):Long=now/86_400_000L
 fun dayRangeStart(epochDay:Long):Long=epochDay*86_400_000L
}
