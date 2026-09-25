package `in`.localdukaan.core.model
import java.math.BigDecimal
import java.math.RoundingMode
object MoneyParser{
 fun rupeesToPaise(input:String):Long?=try{val value=BigDecimal(input.trim());if(value.scale()>2||value<=BigDecimal.ZERO)null else value.movePointRight(2).setScale(0,RoundingMode.UNNECESSARY).longValueExact()}catch(_:Exception){null}
}
data class CreditDraft(val mutationId:String,val shopCustomerId:String,val type:String,val amountPaise:Long,val state:String="PENDING",val rejectionReason:String?=null)
object CreditRules{
 fun available(limit:Long?,balance:Long,enabled:Boolean)=if(enabled&&limit!=null)(limit-balance).coerceAtLeast(0) else null
 fun validCredit(amount:Long,balance:Long,limit:Long?,enabled:Boolean)=amount>0&&enabled&&(limit==null||amount<=limit-balance)
 fun validPayment(amount:Long,balance:Long)=amount>0&&amount<=balance
}
