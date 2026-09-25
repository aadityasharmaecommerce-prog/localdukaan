package `in`.localdukaan.core.model

data class MarketShop(val id:String,val name:String,val category:String,val locality:String,val city:String,val publicSlug:String?,val distanceM:Long?,val deliveryEnabled:Boolean,val minimumOrderPaise:Long,val deliveryFeePaise:Long)
data class MarketProduct(val shopProductId:String,val name:String,val brand:String?,val unit:String,val sellingPricePaise:Long,val stockQuantityMilli:Long)
data class CartLine(val shopProductId:String,val name:String,val unit:String,val unitPricePaise:Long,val quantityMilli:Long){val lineTotalPaise:Long get()=Math.multiplyExact(unitPricePaise,quantityMilli)/1000}
data class Cart(val lines:List<CartLine> = emptyList()){
 val subtotalPaise:Long get()=lines.fold(0L){s,l->Math.addExact(s,l.lineTotalPaise)}
 val isEmpty:Boolean get()=lines.isEmpty()
 fun add(product:MarketProduct):Cart{val existing=lines.firstOrNull{it.shopProductId==product.shopProductId};val next=if(existing==null)lines+CartLine(product.shopProductId,product.name,product.unit,product.sellingPricePaise,1000) else lines.map{if(it.shopProductId==product.shopProductId)it.copy(quantityMilli=it.quantityMilli+1000) else it};return copy(lines=next)}
 fun setQuantity(shopProductId:String,quantityMilli:Long):Cart=if(quantityMilli<=0)copy(lines=lines.filter{it.shopProductId!=shopProductId}) else copy(lines=lines.map{if(it.shopProductId==shopProductId)it.copy(quantityMilli=quantityMilli) else it})
}
data class OrderAddress(val id:String,val recipientName:String,val addressLine:String,val locality:String,val city:String,val pincode:String,val mobile:String?)
data class OrderSummary(val id:String,val shopId:String,val shopName:String,val status:String,val fulfillmentType:String,val totalPaise:Long,val createdAt:Long)
data class OrderLine(val shopProductId:String,val name:String,val unit:String,val unitPricePaise:Long,val quantityMilli:Long,val lineTotalPaise:Long)
data class OrderDetail(val id:String,val shopId:String,val status:String,val fulfillmentType:String,val totalPaise:Long,val deliveryFeePaise:Long,val lines:List<OrderLine>,val history:List<String>)

data class MyCreditShop(val shopId:String,val shopName:String,val balancePaise:Long,val creditLimitPaise:Long?,val updatedAt:Long)
data class MyCreditEntry(val shopName:String,val type:String,val amountPaise:Long,val balanceAfterPaise:Long,val createdAt:Long)

object OrderStatusFlow{
 private val transitions=mapOf("PENDING" to setOf("ACCEPTED","REJECTED","CANCELLED"),"ACCEPTED" to setOf("PACKING","CANCELLED"),"PACKING" to setOf("READY","CANCELLED"),"READY" to setOf("OUT_FOR_DELIVERY","DELIVERED"),"OUT_FOR_DELIVERY" to setOf("DELIVERED"),"DELIVERED" to emptySet(),"REJECTED" to emptySet(),"CANCELLED" to emptySet())
 fun next(from:String):Set<String> =transitions[from]?:emptySet()
 fun canTransition(from:String,to:String)=next(from).contains(to)
 fun isTerminal(status:String)=next(status).isEmpty()
}
