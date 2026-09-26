@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class, androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
package `in`.localdukaan
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.graphics.asImageBitmap
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewModelScope
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ArrowForwardIos
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Analytics
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.Logout
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.ShoppingBag
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material.icons.filled.TrendingDown
import androidx.compose.material.icons.filled.TrendingUp
import `in`.localdukaan.core.data.AppRepository
import `in`.localdukaan.core.network.ApiException
import `in`.localdukaan.ui.DukaanColors
import `in`.localdukaan.ui.NewDukaanTheme
import `in`.localdukaan.core.database.*
import `in`.localdukaan.core.model.ProductDraft
import `in`.localdukaan.core.model.SelectedImage
import `in`.localdukaan.core.model.BarcodeRules
import `in`.localdukaan.core.model.ProductRules
import `in`.localdukaan.core.model.QuickSaleState
import `in`.localdukaan.core.model.QuickSaleLine
import `in`.localdukaan.core.voice.VoiceFail
import `in`.localdukaan.core.voice.VoiceListenResult
import `in`.localdukaan.core.voice.VoiceListener
import android.Manifest
import android.content.pm.PackageManager
import android.speech.SpeechRecognizer
import androidx.core.content.ContextCompat
import `in`.localdukaan.core.model.SalePaymentMode
import `in`.localdukaan.core.model.PaymentMethod
import `in`.localdukaan.core.model.SaleSuccess
import `in`.localdukaan.core.model.ReceiptData
import `in`.localdukaan.core.model.FinanceSummary
import `in`.localdukaan.core.model.CreditRules
import `in`.localdukaan.core.model.MoneyParser
import `in`.localdukaan.core.model.ReportMath
import `in`.localdukaan.core.model.BarcodeResolution
import `in`.localdukaan.core.model.MarketShop
import `in`.localdukaan.core.model.MarketProduct
import `in`.localdukaan.core.model.Cart
import `in`.localdukaan.core.model.OrderAddress
import `in`.localdukaan.core.model.OrderSummary
import `in`.localdukaan.core.model.OrderDetail
import `in`.localdukaan.core.model.OrderStatusFlow
import `in`.localdukaan.core.model.MyCreditShop
import `in`.localdukaan.core.model.MyCreditEntry
import `in`.localdukaan.core.model.SaleOption
import `in`.localdukaan.core.model.VoiceCmd
import `in`.localdukaan.core.model.VoiceParser
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.io.IOException

// ---- Brand palette (Modern & Colorful) ----
val BrandGreen = Color(0xFF16A34A)
val BrandGreenDark = Color(0xFF15803D)
val BrandAmber = Color(0xFFF59E0B)
val BrandBlue = Color(0xFF2563EB)
val BrandRed = Color(0xFFDC2626)
val BrandBg = Color(0xFFF6FAF7)
val BrandCard = Color(0xFFEFF7F1)

data class UiState(val screen:String="login",val busy:Boolean=false,val drawerOpen:Boolean=false,val profile:LocalProfile?=null,val shops:List<LocalShop> = emptyList(),val products:List<ProductRow> = emptyList(),val selectedShopId:String?=null,val editing:ProductRow?=null,val editingShop:LocalShop?=null,val currentImage:ByteArray?=null,val quickSale:QuickSaleState=QuickSaleState(),val scanMessage:String?=null,val scanError:Boolean=false,val scannedGlobal:BarcodeResolution.Global?=null,val pendingBarcode:String?=null,val inventoryProduct:ProductRow?=null,val inventoryHistory:List<InventoryTransactionEntity> = emptyList(),val customers:List<ShopCustomerEntity> = emptyList(),val customer:ShopCustomerEntity?=null,val creditStatement:List<CreditTransactionEntity> = emptyList(),val marketShops:List<MarketShop> = emptyList(),val marketShop:MarketShop?=null,val marketProducts:List<MarketProduct> = emptyList(),val cart:Cart=Cart(),val addresses:List<OrderAddress> = emptyList(),val orders:List<OrderSummary> = emptyList(),val order:OrderDetail?=null,val shopOrders:List<OrderSummary> = emptyList(),val myCreditShops:List<MyCreditShop> = emptyList(),val myCreditEntries:List<MyCreditEntry> = emptyList(),val customerSales:List<SaleEntity> = emptyList(),val saleSuccess:SaleSuccess?=null,val receipt:ReceiptData?=null,val finance:FinanceSummary?=null,val marketQuery:String="",val dailySales:List<DailySaleRow> = emptyList(),val dailyProfit:List<DailyProfitRow> = emptyList(),val stockValue:StockValueRow?=null,val purchaseItems:List<PurchaseListEntity> = emptyList(),val dues:Pair<Long,Int>?=null,val toast:String?=null,val error:String?=null,val offline:Boolean=false,val scanning:Boolean=false,val voiceListening:Boolean=false,val voiceHeard:String?=null,val voiceConfirm:SaleOption?=null,val voiceChoices:List<SaleOption> = emptyList(),val voiceAnswer:String?=null,val voiceError:String?=null,val returnTo:String?=null,val pendingRole:String?=null,val booting:Boolean=false)

class MainViewModel(private val r:AppRepository):ViewModel(){
 private val _s=MutableStateFlow(UiState(screen=if(r.loggedIn())"boot" else "login",booting=r.loggedIn()));val state=_s.asStateFlow()
  private var customerJob:Job?=null;private var statementJob:Job?=null;private var productsJob:Job?=null;private var inventoryJob:Job?=null;private var salesJob:Job?=null;private var purchaseJob:Job?=null
 private val backStack=ArrayDeque<String>()
 init{
  viewModelScope.launch{r.profile.collect{_s.update{x->x.copy(profile=it)}}}
  viewModelScope.launch{r.shops.collect{_s.update{x->x.copy(shops=it)}}}
  if(r.loggedIn())viewModelScope.launch{runCatching{r.restoreSession();r.loadShops()}.fold(onSuccess={resolveStartup()},onFailure={e->_s.update{it.copy(booting=false,error=e.message?:"Session expired",screen="login",selectedShopId=null)}})}
 }
 private fun run(block:suspend()->Unit){ if(_s.value.busy){toast("Pichla kaam chal raha hai — thoda rukein");return};viewModelScope.launch{_s.update{it.copy(busy=true,error=null)};runCatching{block()}.onSuccess{_s.update{it.copy(offline=false)}}.onFailure{e->_s.update{it.copy(error=e.message?:"Something went wrong",offline=e is IOException)}};_s.update{it.copy(busy=false)}}}
 fun login(phone:String,pin:String,role:String)=run{r.login(phone,pin);val p=r.profileNow()
  val keeper=p?.shopkeeper==true;val isCustomer=p?.customer==true;val wantShopkeeper=role=="SHOPKEEPER"
  // ROLE SIRF BACKEND PROFILE SE aata hai, aur login TAB usse match hona chahiye — warna session turant revoke.
  if(p==null||(!keeper&&!isCustomer)){runCatching{r.logout()};_s.update{it.copy(screen="login",error="Account theek se set nahi hai — dobara login karein")};return@run}
  if(wantShopkeeper&&!keeper){runCatching{r.logout()};_s.update{it.copy(screen="login",error="Ye Customer account hai — neeche 'Customer' tab se login karein")};return@run}
  if(!wantShopkeeper&&keeper&&!isCustomer){runCatching{r.logout()};_s.update{it.copy(screen="login",error="Ye Shopkeeper account hai — upar 'Shopkeeper' tab se login karein")};return@run}
  if(p.name.isNullOrBlank()){_s.update{it.copy(screen="profile",selectedShopId=null,pendingRole=if(keeper)"SHOPKEEPER" else "CUSTOMER")};return@run}
  if(wantShopkeeper){r.loadShops();_s.update{it.copy(screen="boot",booting=true,selectedShopId=null,shops=emptyList(),pendingRole="SHOPKEEPER")};resolveStartup()}
  else{_s.update{it.copy(screen="home",selectedShopId=null,shops=emptyList(),pendingRole="CUSTOMER")}}}
 fun register(phone:String,pin:String,confirm:String,role:String)=run{r.register(phone,pin,confirm,role);_s.update{it.copy(screen="profile",pendingRole=role)}}
 fun profile(name:String,customer:Boolean,shopkeeper:Boolean,hi:Boolean)=run{r.saveProfile(name,hi,customer,shopkeeper)
  if(shopkeeper){r.loadShops();if(r.shopsNow().isEmpty())_s.update{it.copy(screen="shop",selectedShopId=null)}else{_s.update{it.copy(screen="boot",booting=true,selectedShopId=null)};resolveStartup()}}
  else _s.update{it.copy(screen="home",selectedShopId=null)}}
 fun shop(v:List<String>)=run{runCatching{r.createShop(v[0],v[1],v[2],v[3],v[4],v[5],v[6],v[7].toDouble(),v[8].toDouble())}.onSuccess{x->r.loadShops();_s.update{it.copy(toast="✓ Dukaan ban gayi")};resolveStartup()}.onFailure{e->val msg=(e.message?:"");if((e as? ApiException)?.code=="SHOP_ALREADY_EXISTS"){r.loadShops();_s.update{it.copy(toast="Aapki dukaan pehle se bani hai — khhol rahe hain")};resolveStartup()}else _s.update{it.copy(error=msg)}}}
 fun products(shopId:String){val same=_s.value.selectedShopId==shopId;_s.update{it.copy(screen="products",selectedShopId=shopId,products=if(same)_s.value.products else emptyList())};watchProducts(shopId);run{r.refreshProducts(shopId)}}
 private fun watchProducts(shopId:String){productsJob?.cancel();productsJob=viewModelScope.launch{r.observeProducts(shopId).collect{rows->_s.update{it.copy(products=rows)}}};salesJob?.cancel();salesJob=viewModelScope.launch{r.sales(shopId).collect{list->_s.update{it.copy(customerSales=list)}}}}
 fun openDrawer(){_s.update{it.copy(drawerOpen=true)}}
 fun closeDrawer(){_s.update{it.copy(drawerOpen=false)}}
 fun toast(msg:String){_s.update{it.copy(toast=msg)}}
 fun clearToast(){_s.update{it.copy(toast=null)}}
 fun clearError(){_s.update{it.copy(error=null)}}
 private var dashJob:Job?=null
 fun openDashboard(shopId:String){_s.update{it.copy(screen="dashboard",booting=false,selectedShopId=shopId,dailySales=emptyList(),dailyProfit=emptyList(),stockValue=null)};loadShopDashboard(shopId)}
 private fun loadDashboard(shopId:String){loadShopDashboard(shopId)}
 private fun loadShopDashboard(shopId:String){dashJob?.cancel();dashJob=viewModelScope.launch{r.dashboard(shopId).collect{(a,b,c)->_s.update{it.copy(dailySales=a,dailyProfit=b,stockValue=c)}}};run{runCatching{r.refreshProducts(shopId)}};viewModelScope.launch{runCatching{_s.update{x->x.copy(dues=r.dues(shopId))}}}}
 /** Startup/shop resolution ka ek hi source of truth — root navigation fix:
  *  0 shops → shopkeeper ko Create Shop form (warna customer home).
  *  exactly 1 shop → auto-select + DIRECT dashboard (koi beech ka shop-card screen nahi dikhta).
  *  multiple shops → selection list (home). */
 private fun resolveStartup(){viewModelScope.launch{val shops=r.shopsNow();val shopkeeper=_s.value.profile?.shopkeeper==true
  when{shops.isEmpty()->_s.update{it.copy(screen=if(shopkeeper)"shop" else "home",selectedShopId=null,booting=false)}
   shops.size==1->{val sid=shops.first().id;if(_s.value.selectedShopId==sid&&_s.value.screen=="dashboard")loadShopDashboard(sid)else openDashboard(sid)}
   else->_s.update{it.copy(screen="home",selectedShopId=null,booting=false)}}}}
 fun addProduct(){_s.update{it.copy(screen="productForm",editing=null,returnTo=null)}}
 /** Har entry point se dukaan chuni hui honi chahiye — warna "!!" crash ho jata tha. */
 private fun withSelectedShop(block:(String)->Unit){val shop=_s.value.selectedShopId;if(shop==null){_s.update{it.copy(error="Pehle apni dukaan choose karein")};return};block(shop)}
 fun quickSaleForSelected()=withSelectedShop{openQuickSale(it)}
 fun customersForSelected()=withSelectedShop{openCustomers(it)}
 fun editProduct(row:ProductRow){_s.update{it.copy(screen="productForm",editing=row,currentImage=null)};if(row.shopProduct.imageKey!=null&&!row.shopProduct.imageKey.startsWith("local-file:"))viewModelScope.launch{runCatching{r.downloadProductImage(row.shopProduct.shopId,row.shopProduct.id)}.onSuccess{bytes->_s.update{it.copy(currentImage=bytes)}}}}
 fun saveProduct(d:ProductDraft,image:SelectedImage?)=run{
  val shopId=_s.value.selectedShopId?:error("No shop selected")
  val row=_s.value.editing
  if(row==null)r.addProduct(shopId,d,image,_s.value.scannedGlobal?.id) else r.updateProduct(shopId,row,d,image)
  val back=_s.value.returnTo
  val savedBarcode=ProductRules.normalizeBarcode(d.barcode)
  _s.update{it.copy(editing=null,currentImage=null,scannedGlobal=null,pendingBarcode=null,returnTo=null,scanError=false,scanning=false)}
  // Product form ke andar se naya product bana ho to seedha billing par wapas jao — warna cart kho jata tha.
  if(back=="quickSale"){val found=savedBarcode?.let{code->r.productByBarcode(shopId,code)}
   _s.update{s->s.copy(screen="quickSale",quickSale=found?.let{s.quickSale.scan(it)}?:s.quickSale,scanMessage=if(found!=null)"✓ ${found.product.name} cart mein add ho gaya" else "✓ Product save ho gaya",scanError=false,toast="✓ Product save ho gaya")}
  } else _s.update{it.copy(screen="products",toast="✓ Product save ho gaya")}}
 fun removeImage()=run{val row=_s.value.editing?:return@run;r.removeProductImage(row.shopProduct.shopId,row);_s.update{it.copy(screen="products",editing=null,currentImage=null)}}
  fun openLowStock(){val shop=_s.value.selectedShopId?:_s.value.shops.firstOrNull()?.id;_s.update{it.copy(screen="lowStock")};if(shop!=null)watchPurchase(shop)}
  private fun watchPurchase(shop:String){purchaseJob?.cancel();purchaseJob=viewModelScope.launch{r.purchaseList(shop).collect{rows->_s.update{it.copy(purchaseItems=rows)}}}}
  fun openPurchaseList(){val shop=_s.value.selectedShopId?:_s.value.shops.firstOrNull()?.id?:return;_s.update{it.copy(screen="purchaseList",selectedShopId=shop)};watchPurchase(shop);run{r.refreshPurchaseList(shop)}}
  fun togglePurchase(item:PurchaseListEntity)=run{val shop=_s.value.selectedShopId?:error("No shop selected");r.setPurchaseItemPurchased(shop,item,!item.isPurchased)}
  fun deletePurchase(item:PurchaseListEntity)=run{val shop=_s.value.selectedShopId?:error("No shop selected");r.removePurchaseItem(shop,item);_s.update{it.copy(toast="✓ Hataya gaya")}}
 fun addPurchase(row:ProductRow)=run{val shortage=(row.shopProduct.minimumStockMilli-row.shopProduct.stockQuantityMilli).coerceAtLeast(1000);r.addToPurchaseList(row.shopProduct.shopId,row,shortage);_s.update{it.copy(scanMessage="Added to purchase list",scanError=false)}}
 fun openInventory(row:ProductRow){_s.update{it.copy(screen="inventory",inventoryProduct=row,inventoryHistory=emptyList())};inventoryJob?.cancel();inventoryJob=viewModelScope.launch{r.inventory(row.shopProduct.shopId,row.shopProduct.id).collect{history->_s.update{it.copy(inventoryHistory=history)}}}}
 fun adjustInventory(quantity:String,type:String,note:String)=run{val row=_s.value.inventoryProduct?:return@run;val q=ProductRules.quantityToMilli(quantity)?:error("Invalid quantity");r.adjustStock(row.shopProduct.shopId,row,q,type,note);_s.update{it.copy(screen="products",inventoryProduct=null)}}
 // Cart wahi rehta hai jab usi dukaan par wapas aayein (jaise udhari customer add karke) — pehle cart reset ho jata tha.
 fun openQuickSale(shopId:String){val sameShop=_s.value.selectedShopId==shopId;_s.update{it.copy(screen="quickSale",selectedShopId=shopId,quickSale=if(sameShop)it.quickSale else QuickSaleState(),scanMessage=null,scanError=false)};watchCustomers(shopId);watchProducts(shopId);run{r.refreshCustomers(shopId);runCatching{r.refreshProducts(shopId)}}}
 private fun pendingCode(code:String)=BarcodeRules.candidates(code).firstOrNull()?:ProductRules.normalizeBarcode(code)
 fun scan(code:String)=run{
  val shop=_s.value.selectedShopId?:return@run
  val entered=ProductRules.normalizeBarcode(code)
  if(entered==null){_s.update{it.copy(scanMessage="Barcode khali hai — number daalein",scanError=true)};return@run}
  _s.update{it.copy(scanning=true,error=null)}
  val found=try{r.resolveBarcode(shop,entered)}catch(t:Throwable){_s.update{it.copy(scanning=false,scanMessage="Scan problem: ${t.message?:"dobara try karo"}",scanError=true,currentImage=null)};return@run}
  when(found){
   is BarcodeResolution.Local->{val before=_s.value.quickSale.lines[found.row.shopProduct.id]?.quantityMilli?:0L
    val q=runCatching{_s.value.quickSale.scan(found.row)}.getOrDefault(_s.value.quickSale)
    val added=(q.lines[found.row.shopProduct.id]?.quantityMilli?:0L)>before
    _s.update{it.copy(quickSale=q,scanMessage=if(added)"✓ ${found.row.product.name} — cart mein ${ProductRules.quantity(q.lines[found.row.shopProduct.id]?.quantityMilli?:0L)}" else "✓ ${found.row.product.name} cart mein hai",scanError=false,scannedGlobal=null,pendingBarcode=null,currentImage=null,scanning=false)}}
   is BarcodeResolution.Global->{val bytes=try{found.imageBase64?.let{d->runCatching{android.util.Base64.decode(d,android.util.Base64.DEFAULT)}.getOrNull()}?:found.imageUrl?.let{url->runCatching{r.downloadUrl(url)}.getOrNull()}}catch(_:Throwable){null}
    _s.update{it.copy(scanMessage="✓ ${found.name}${found.brand?.let{b->" ($b)"}.orEmpty()} — \"Add to My Shop\" dabayein",scanError=false,scannedGlobal=found,pendingBarcode=found.barcode,currentImage=bytes,scanning=false)}}
   is BarcodeResolution.NotFound->_s.update{it.copy(scanMessage="Ye barcode database mein nahi mila — naya product banao",scanError=true,scannedGlobal=null,pendingBarcode=found.barcode,currentImage=null,scanning=false)}
   is BarcodeResolution.Invalid->_s.update{it.copy(scanMessage=found.message,scanError=true,pendingBarcode=pendingCode(entered),scannedGlobal=null,currentImage=null,scanning=false)}}
 }
  fun changeSaleQuantity(id:String,q:Long){_s.update{it.copy(quickSale=it.quickSale.setQuantity(id,q))}}
  /** Bina scan ke sale — search karke seedha ADD dabao. Stock se zyada cart me nahi jayega. */
  fun addLine(row:ProductRow){val cur=_s.value.quickSale.lines[row.shopProduct.id]?.quantityMilli?:0L;val avail=row.shopProduct.stockQuantityMilli-row.shopProduct.reservedQuantityMilli;if(avail-cur<=0){_s.update{it.copy(scanMessage="${row.product.name} — stock khatm hai",scanError=true)};return};_s.update{it.copy(quickSale=it.quickSale.scan(row),scanMessage="✓ ${row.product.name} cart me",scanError=false)}}
 /** Product form ke liye SCAN: sirf prefill — POS cart ko kabhi nahi chhua. */
 fun scanForForm(code:String)=run{
  val entered=ProductRules.normalizeBarcode(code)
  if(entered==null){_s.update{it.copy(scanMessage="Barcode khali hai — number daalein",scanError=true)};return@run}
  _s.update{it.copy(scanning=true,error=null)}
  val shop=_s.value.selectedShopId
  val found=try{shop?.let{r.resolveBarcode(it,entered)}?:BarcodeResolution.NotFound(entered)}catch(t:Throwable){BarcodeResolution.Invalid(t.message?:"Scan problem — dobara try karo")}
  val g=if(found is BarcodeResolution.Global)found else null
  _s.update{it.copy(scanning=false,scannedGlobal=g,pendingBarcode=if(g!=null)g.barcode else entered,scanError=found is BarcodeResolution.Invalid,scanMessage=if(g!=null)"✓ ${g.name}${g.brand?.let{b->" ($b)"}?.orEmpty()} — sirf price/stock daalo" else if(found is BarcodeResolution.Local)"Ye barcode aapki dukaan mein pehle se hai" else if(found is BarcodeResolution.Invalid)found.message else "Naya barcode set — product add ho jayega")}
 }
 fun attachScanned(){_s.update{it.copy(returnTo=if(it.screen=="quickSale") "quickSale" else null,screen="productForm",editing=null)}}
 fun archive(row:ProductRow)=run{val shop=_s.value.selectedShopId?:error("No shop selected");r.archiveProduct(shop,row.shopProduct.id)}
 fun openCustomers(shopId:String){_s.update{it.copy(screen="customers",selectedShopId=shopId)};watchCustomers(shopId);run{r.refreshCustomers(shopId)}}
 private fun watchCustomers(shopId:String){customerJob?.cancel();customerJob=viewModelScope.launch{r.customers(shopId).collect{rows->_s.update{it.copy(customers=rows)}}}}
 fun addCustomer(){_s.update{it.copy(screen="customerForm",customer=null)}}
 fun editCustomer(row:ShopCustomerEntity){_s.update{it.copy(screen="customerForm",customer=row)}}
 fun saveCustomer(name:String,mobile:String,creditEnabled:Boolean,limitText:String,notes:String)=run{val shop=_s.value.selectedShopId?:error("No shop selected");val limit=if(creditEnabled&&limitText.isNotBlank())MoneyParser.rupeesToPaise(limitText)?:error("Enter a valid credit limit") else null;val cleanMobile=mobile.trim().takeIf{it.isNotBlank()}?.let{if(it.startsWith("+"))it else "+91"+it.filter(Char::isDigit).takeLast(10)};val editing=_s.value.customer;runCatching{if(editing==null)r.createCustomer(shop,name,cleanMobile,creditEnabled,limit,notes) else r.updateCustomer(shop,editing,name,cleanMobile,creditEnabled,limit,notes)}.onFailure{e->_s.update{it.copy(error=if((e.message?:"").contains("already",true))"Ye mobile number pehle se saved hai" else e.message)}};_s.update{it.copy(screen="customers",customer=null,toast="✓ Customer saved")}}
 fun archiveCustomer(row:ShopCustomerEntity)=run{val shop=_s.value.selectedShopId?:error("No shop selected");r.archiveCustomer(shop,row.id)}
 fun openCustomer(row:ShopCustomerEntity){val shop=_s.value.selectedShopId?:return;_s.update{it.copy(screen="customerDetail",customer=row)};watchCustomers(shop);statementJob?.cancel();statementJob=viewModelScope.launch{r.creditStatement(shop,row.id).collect{list->_s.update{it.copy(creditStatement=list)}}};salesJob?.cancel();salesJob=viewModelScope.launch{_s.update{it.copy(customerSales=r.customerSales(shop,row.id))}};run{r.refreshCustomers(shop)}}
 fun clearCustomers(){customerJob?.cancel();salesJob?.cancel();_s.update{it.copy(customers=emptyList(),customer=null,customerSales=emptyList())}}
 fun openSalesHistory(){val shop=_s.value.selectedShopId?:_s.value.shops.firstOrNull()?.id?:return;_s.update{it.copy(screen="sales")};salesJob?.cancel();salesJob=viewModelScope.launch{r.sales(shop).collect{list->_s.update{it.copy(customerSales=list)}}}}
 fun credit(type:String,amountText:String,note:String)=run{val shop=_s.value.selectedShopId?:error("No shop selected");val row=_s.value.customer?:error("No customer selected");val amount=MoneyParser.rupeesToPaise(amountText)?:error("Enter a valid amount");val live=_s.value.customers.firstOrNull{it.id==row.id}?:row;when(type){"PAYMENT"->if(!CreditRules.validPayment(amount,live.currentBalancePaise))error("Payment is more than the balance due");"CREDIT_SALE"->if(!CreditRules.validCredit(amount,live.currentBalancePaise,live.creditLimitPaise,live.creditEnabled))error("Amount exceeds available credit");else->if(amount<=0)error("Enter a valid amount")};r.credit(shop,row.id,type,amount,note);_s.update{it.copy(toast=if(type=="PAYMENT")"✓ Payment received" else if(type=="CREDIT_SALE")"✓ Udhari added" else "✓ Adjusted")}}
 fun selectPayment(mode:SalePaymentMode,customerId:String?){_s.update{it.copy(quickSale=it.quickSale.selectPayment(mode,customerId))}}
 fun selectMethod(m:PaymentMethod){_s.update{it.copy(quickSale=it.quickSale.selectMethod(m))}}
 fun setDiscount(text:String){val paise=MoneyParser.rupeesToPaise(text)?:0L;_s.update{it.copy(quickSale=it.quickSale.setDiscount(paise))}}
 fun identifyCustomer(name:String,phone:String){_s.update{it.copy(quickSale=it.quickSale.identify(name,phone))}}
 fun useWalkIn(){_s.update{it.copy(quickSale=it.quickSale.walkInCustomer())}}
 /** Naya chhota customer form (naam+mobile) — save ke baad wahi cart par wapas, customer pehle se chuna hua. */
 fun saveQuickCustomer(name:String,mobile:String)=run{val shop=_s.value.selectedShopId?:error("No shop selected");val row=r.quickAddCustomer(shop,name,mobile);_s.update{it.copy(screen="posCheckout",quickSale=it.quickSale.copy(shopCustomerId=row.id,walkIn=false,customerName=row.displayName,customerPhone=row.normalizedPhone),toast="✓ Customer saved")}}
 fun completeSale()=run{val shop=_s.value.selectedShopId?:error("No shop selected");val q=_s.value.quickSale;if(q.udhaariRequiresCustomer){_s.update{it.copy(error="Please add customer details for Udhaari.")};return@run};val saleId=r.completeQuickSale(shop,q);val line=q.lines.values.firstOrNull();val due=q.paymentMode==SalePaymentMode.CREDIT;_s.update{it.copy(screen="saleSuccess",saleSuccess=SaleSuccess(saleId,q.totalPaise,q.subtotalPaise,q.discountPaise,q.method.label,if(q.walkIn)"Walk-in Customer" else (q.customerName?:"Customer"),if(due)0L else q.totalPaise,if(due)q.totalPaise else 0L,q.estimatedProfitPaise),quickSale=QuickSaleState(),toast="✓ Sale Completed")}}
 fun newSale(){_s.update{it.copy(screen="quickSale",saleSuccess=null,receipt=null)}}
 fun openReceipt(saleId:String)=run{val shop=_s.value.selectedShopId?:_s.value.shops.firstOrNull()?.id?:return@run;_s.update{it.copy(screen="receipt",receipt=r.receiptData(shop,saleId))}}
 fun shareReceipt(){val rD=_s.value.receipt?:return;val sb=StringBuilder();sb.appendLine("*${rD.shopName}*");rD.shopLine?.let{sb.appendLine(it)};sb.appendLine();sb.appendLine(java.text.SimpleDateFormat("dd MMM yyyy, hh:mm a",java.util.Locale.getDefault()).format(java.util.Date(rD.createdAt)));sb.appendLine("Invoice: ${rD.id.takeLast(8).uppercase()}");sb.appendLine();rD.items.forEach{sb.appendLine("${it.name} × ${ProductRules.quantity(it.quantityMilli)} = ${ProductRules.rupees(it.lineTotalPaise)}")};sb.appendLine();if(rD.discountPaise>0){sb.appendLine("Subtotal: ${ProductRules.rupees(rD.subtotalPaise)}");sb.appendLine("Discount: -${ProductRules.rupees(rD.discountPaise)}")};sb.appendLine("Total: ${ProductRules.rupees(rD.totalPaise)}");sb.appendLine("Payment: ${rD.methodLabel}");sb.appendLine("Customer: ${rD.customerLabel}");if(rD.duePaise>0){sb.appendLine("Paid: ₹0.00");sb.appendLine("Due: ${ProductRules.rupees(rD.duePaise)}")};if(rD.pendingSync)sb.appendLine("(offline — sync pending)");r.shareReceiptText(sb.toString())}
 fun openFinanceSafe(){if(_s.value.selectedShopId!=null||_s.value.shops.isNotEmpty())openFinance()}
 fun openFinance(){val shop=_s.value.selectedShopId?:_s.value.shops.firstOrNull()?.id?:return;_s.update{it.copy(screen="finance")};viewModelScope.launch{val since=ReportMath.todayEpochDay()*86_400_000L;val sales=runCatching{r.salesSince(shop,since)}.getOrDefault(emptyList());val cash=sales.filter{it.paymentMethod=="CASH"&&it.paymentType!="CREDIT"}.sumOf{it.totalPaise};val upi=sales.filter{it.paymentMethod=="UPI"&&it.paymentType!="CREDIT"}.sumOf{it.totalPaise};val udhaar=sales.filter{it.paymentType=="CREDIT"}.sumOf{it.totalPaise};val dues=runCatching{r.duesPair(shop)}.getOrDefault(0L to 0);val profit=runCatching{r.profitEstimate(shop,since)}.getOrNull();_s.update{it.copy(finance=FinanceSummary(sales.size,cash+upi,cash,upi,udhaar,dues.first,dues.second,profit))}}}
 fun openMarket(){_s.update{it.copy(screen="market")};loadMarket()}
 fun loadMyCredit()=run{val c=r.myCredit();_s.update{it.copy(myCreditShops=c.first,myCreditEntries=c.second)}}
 fun openReports(){val shop=_s.value.selectedShopId?:_s.value.shops.firstOrNull()?.id;_s.update{it.copy(screen="reports")};if(shop!=null)loadDashboard(shop)}
 fun openShopOrdersSafe(){val shop=_s.value.selectedShopId?:_s.value.shops.firstOrNull()?.id;if(shop!=null)openShopOrders(shop)}
 private fun loadMarket()=run{_s.update{it.copy(marketShops=r.nearbyShops(null,null,_s.value.marketQuery))}}
 fun searchMarket(q:String){_s.update{it.copy(marketQuery=q)};loadMarket()}
 fun openShopPage(shop:MarketShop)=run{val slug=shop.publicSlug?:error("This shop has no public page");val pair=r.shopPage(slug);_s.update{it.copy(screen="shopPage",marketShop=pair.first,marketProducts=pair.second,cart=Cart())}}
 fun addToCart(product:MarketProduct){_s.update{it.copy(cart=it.cart.add(product))}}
 fun setCartQuantity(shopProductId:String,quantityMilli:Long){_s.update{it.copy(cart=it.cart.setQuantity(shopProductId,quantityMilli))}}
 fun openCheckout()=run{_s.update{it.copy(screen="checkout")};_s.update{it.copy(addresses=r.addresses())}}
 fun addAddress(recipient:String,mobile:String,addressLine:String,locality:String,city:String,pincode:String)=run{val a=r.createAddress(recipient,mobile,addressLine,locality,city,pincode);_s.update{it.copy(addresses=it.addresses+a)}}
 fun placeOrder(fulfillment:String,addressId:String?,note:String)=run{val shop=_s.value.marketShop?:error("No shop selected");r.placeOrder(shop.id,_s.value.cart,fulfillment,addressId,note);_s.update{it.copy(cart=Cart(),screen="orders",toast="✓ Order placed")};runCatching{_s.update{it.copy(orders=r.myOrders())}}}
 fun openOrders()=run{_s.update{it.copy(screen="orders")};_s.update{it.copy(orders=r.myOrders())}}
 fun openOrder(orderId:String)=run{_s.update{it.copy(screen="orderDetail")};_s.update{it.copy(order=r.orderDetail(orderId))}}
 fun cancelOrder(orderId:String)=run{r.cancelOrder(orderId);runCatching{_s.update{it.copy(order=r.orderDetail(orderId))}};_s.update{it.copy(toast="Order cancelled")}}
 fun openShopOrders(shopId:String)=run{_s.update{it.copy(screen="shopOrders",selectedShopId=shopId)};_s.update{it.copy(shopOrders=r.shopOrders(shopId,""))}}
 fun advanceOrder(orderId:String,status:String)=run{val shopId=_s.value.selectedShopId?:error("No shop selected");r.advanceOrder(shopId,orderId,status);runCatching{_s.update{it.copy(shopOrders=r.shopOrders(shopId,""))}};_s.update{it.copy(toast="Order updated")}}
 fun show(s:String){pushCurrent();_s.update{it.copy(screen=s,error=null)}}
 fun showWithRole(s:String,role:String){pushCurrent();_s.update{it.copy(screen=s,error=null,pendingRole=role)}}
 fun back(){_s.update{it.copy(drawerOpen=false)};val cur=_s.value.screen;val target=backStack.removeLastOrNull()?:defaultBack(cur);val safe=if(target=="home"&&_s.value.profile?.shopkeeper==true&&_s.value.shops.size<=1)"dashboard" else target;if(safe==cur)return;_s.update{it.copy(screen=safe,error=null)}}
   private fun defaultBack(cur:String)=when(cur){"products"->shopHome();"quickSale"->"products";"lowStock"->"products";"purchaseList"->"lowStock";"inventory"->"products";"customers"->"products";"customerForm"->"customers";"customerDetail"->"customers";"productForm"->if(_s.value.returnTo=="quickSale")"quickSale" else "products";"dashboard"->if(_s.value.profile?.shopkeeper==true)"dashboard" else "home";"more"->"dashboard";"reports"->shopHome();"shopPage"->"market";"checkout"->"shopPage";"orderDetail"->"orders";"shopOrders"->shopHome();"sales"->"dashboard";"shopEdit"->shopHome();"changePin"->shopHome();else->shopHome()}
 /** Ek-shop shopkeeper ka "ghar" dashboard hai — intermediate shop screen kabhi nahi. */
 private fun shopHome()=if(_s.value.profile?.shopkeeper==true&&_s.value.shops.size<=1)"dashboard" else "home"
  fun saveShopPublish()=run{val shop=_s.value.shops.firstOrNull{it.id==_s.value.selectedShopId}?:return@run;runCatching{r.publishShop(shop,!shop.isPublished)}.onSuccess{_s.update{x->x.copy(toast=if(!shop.isPublished)"Dukaan ab marketplace par hai" else "Dukaan marketplace se hat gayi")}}.onFailure{e->_s.update{x->x.copy(error=e.message)}}}
  fun editShopSafe(){val shop=_s.value.selectedShopId?.let{id->_s.value.shops.firstOrNull{it.id==id}}?:_s.value.shops.firstOrNull()?:return;pushCurrent();_s.update{it.copy(screen="shopEdit",editingShop=shop,error=null)}}
  fun saveShopEdit(name:String,owner:String,address:String,locality:String,city:String,pincode:String,phone:String,desc:String)=run{val shop=_s.value.editingShop?:error("No shop selected");require(name.isNotBlank()&&owner.isNotBlank()&&address.isNotBlank()&&locality.isNotBlank()&&city.isNotBlank()){"Naam, owner, pata, locality, sheher zaroori hai"};require(pincode.matches(Regex("[1-9]\\d{5}"))){"Sahi 6-digit pincode daalo"};r.saveShopProfile(shop,name,owner,address,locality,city,pincode,phone,desc);_s.update{it.copy(screen="dashboard",editingShop=null,toast="✓ Dukaan update ho gayi")}}
  fun logout()=run{r.logout();backStack.clear();_s.value=UiState("login");toast("Logged out")}
  fun openChangePin(){pushCurrent();_s.update{it.copy(screen="changePin",error=null)}}
  /** Voice orders — Google STT se text, matlab apne rules se. Bina confirm ke koi action nahi. */
  fun setVoiceListening(b:Boolean){_s.update{it.copy(voiceListening=b,voiceError=null)}}
  fun clearVoice(){_s.update{it.copy(voiceHeard=null,voiceConfirm=null,voiceChoices=emptyList(),voiceAnswer=null,voiceError=null)}}
  fun voicePickChoice(opt:SaleOption){_s.update{it.copy(voiceConfirm=opt,voiceChoices=emptyList())}}
  fun voiceMicError(msg:String){_s.update{it.copy(voiceListening=false,voiceError=msg)}}
  fun onVoiceText(text:String)=run{
   val heard=text.trim();if(heard.isEmpty()){_s.update{it.copy(voiceError="Samajh nahi aaya")};return@run}
   val shop=_s.value.selectedShopId?:_s.value.shops.firstOrNull()?.id
   val cat=if(shop==null)emptyList() else r.productsSnapshot(shop)
   _s.update{it.copy(voiceHeard=heard,voiceError=null,voiceAnswer=null)}
   when(val cmd=VoiceParser.parse(heard,cat)){
    is VoiceCmd.Sale->{
     if(shop==null){_s.update{it.copy(voiceError="Pehle apni dukaan kholo")};return@run}
     val row=cat.firstOrNull{it.shopProduct.id==cmd.option.productId}
     val avail=(row?.shopProduct?.stockQuantityMilli?:0)-(row?.shopProduct?.reservedQuantityMilli?:0)
     if(row==null||avail-cmd.option.quantityMilli<0){_s.update{it.copy(voiceError="${cmd.option.name} — stock me nahi hai")};return@run}
     _s.update{it.copy(voiceConfirm=cmd.option,voiceChoices=emptyList())}
    }
    is VoiceCmd.SaleChoice->_s.update{it.copy(voiceChoices=cmd.options,voiceConfirm=null)}
    is VoiceCmd.AskStock->{
     val row=cmd.productId?.let{id->cat.firstOrNull{it.shopProduct.id==id}}
     _s.update{it.copy(voiceAnswer=if(row!=null)"${row.product.name} — stock ${ProductRules.quantity(row.shopProduct.stockQuantityMilli)} ${row.product.unit}" else if(cmd.name!=null)"${cmd.name} — dukaan me nahi mila" else "Naam sunai nahi diya — phir bolo")}
    }
    is VoiceCmd.AskProfit->{
     if(shop==null){_s.update{it.copy(voiceError="Pehle apni dukaan kholo")};return@run}
     val p=r.profitEstimate(shop,ReportMath.todayEpochDay()*86_400_000L)
     _s.update{it.copy(voiceAnswer=if(p!=null)"Aaj ka munafa ${ProductRules.rupees(p)}" else "Cost price set nahi hai — munafa nahi nikal sakta")}
    }
    is VoiceCmd.AskDues->{
     val d=if(shop==null)0L to 0 else r.duesPair(shop)
     _s.update{it.copy(voiceAnswer="Kul udhaar ${ProductRules.rupees(d.first)} (${d.second} grahak)")}
    }
    is VoiceCmd.FindCustomer->{
     val row=_s.value.customers.firstOrNull{it.displayName.contains(cmd.query,true)}
     if(row!=null){clearVoice();openCustomer(row)} else _s.update{it.copy(voiceAnswer="\"${cmd.query}\" naam ka customer nahi mila")}
    }
    is VoiceCmd.OpenReports->{clearVoice();openReports()}
    is VoiceCmd.Yes->{val opt=_s.value.voiceConfirm;if(opt!=null)voiceConfirmSale(opt) else _s.update{it.copy(voiceError="Pehle koi sale confirm karo")}}
    is VoiceCmd.No->clearVoice()
    is VoiceCmd.Unknown->_s.update{it.copy(voiceError="Samajh nahi aaya — phir bolo")}
   }
  }
  fun voiceConfirmSale(opt:SaleOption)=run{
   val shop=_s.value.selectedShopId?:_s.value.shops.firstOrNull()?.id?:error("No shop selected")
   val row=r.productsSnapshot(shop).firstOrNull{it.shopProduct.id==opt.productId}?:error("Product nahi mila")
   val line=QuickSaleLine(opt.productId,opt.name,row.product.unit,row.shopProduct.sellingPricePaise,opt.quantityMilli,row.shopProduct.costPricePaise)
   _s.update{it.copy(quickSale=QuickSaleState(lines=mapOf(opt.productId to line)),selectedShopId=shop,voiceConfirm=null,voiceChoices=emptyList(),voiceHeard=null)}
   completeSale()
  }
  fun changePin(current:String,next:String,confirm:String)=run{require(next==confirm){"Naya PIN aur confirm PIN mil nahi rahe"};require(next!=current){"Naya PIN purane se alag rakho"};require(next.matches(Regex("\\d{4}|\\d{6}"))){"PIN sirf 4 ya 6 ank ka ho"};r.changePin(current,next,confirm);_s.update{it.copy(toast="✓ PIN badal gaya")}}
 private fun pushCurrent(){val cur=_s.value.screen;if(cur=="boot")return;if(backStack.lastOrNull()!=cur)backStack.addLast(cur);if(backStack.size>12)backStack.removeFirst()}
}
class Factory(private val r:AppRepository):ViewModelProvider.Factory{override fun <T:ViewModel> create(c:Class<T>):T=MainViewModel(r) as T}

class MainActivity:ComponentActivity(){
 override fun onCreate(b:Bundle?){super.onCreate(b);setContent{LocalDukaanTheme{val vm:MainViewModel=viewModel(factory=Factory((application as LocalDukaanApp).repository));App(vm)}}}
}

@Composable fun LocalDukaanTheme(content:@Composable ()->Unit){NewDukaanTheme(content=content)}

/** Profile mein "हिन्दी" chuna ho to poora UI values-hi/strings.xml se aata hai (pehle sirf save hota tha, badalta kuch nahi tha). */
@Composable fun App(vm:MainViewModel){
 val s by vm.state.collectAsState()
 val base=androidx.compose.ui.platform.LocalContext.current
 val hindi=s.profile?.language=="hi"
 val localized=remember(hindi,base){if(!hindi)base else base.createConfigurationContext(android.content.res.Configuration(base.resources.configuration).apply{setLocale(java.util.Locale("hi","IN"))})}
 androidx.compose.runtime.CompositionLocalProvider(androidx.compose.ui.platform.LocalContext provides localized){AppBody(vm,s)}
}
@Composable private fun AppBody(vm:MainViewModel,s:UiState){
 val snackbar=remember{SnackbarHostState()}
 val moreOpen=s.screen=="more"
 androidx.compose.runtime.LaunchedEffect(s.toast){s.toast?.let{snackbar.showSnackbar(it);vm.clearToast()}}
 LaunchedEffect(s.screen,s.profile?.customer){if(s.screen=="home"&&s.profile?.customer==true)vm.loadMyCredit()}
 BackHandler(enabled=s.screen!="login"&&s.screen!="boot"){if(s.drawerOpen)vm.closeDrawer() else vm.back()}
 Surface(Modifier.fillMaxSize(),color=MaterialTheme.colorScheme.background){
  Box(Modifier.fillMaxSize()){
  androidx.compose.material3.Scaffold(snackbarHost={SnackbarHost(snackbar)},topBar={if(s.screen!="login"&&s.screen!="register"&&s.screen!="boot")TopBar(s,{vm.openDrawer()},{vm.back()},{vm.saveShopPublish()},null)},bottomBar={if((s.screen=="home"||s.screen=="dashboard")&&s.profile?.shopkeeper==true)DukaanNavBar(moreOpen,{tab->when(tab){DukaanTab.DASHBOARD->s.selectedShopId?.let{vm.openDashboard(it)};DukaanTab.CATALOGUE->s.selectedShopId?.let{vm.products(it)}?:vm.show("shop");DukaanTab.QUICK_SALE->vm.quickSaleForSelected();DukaanTab.KHATA->vm.customersForSelected();DukaanTab.MORE->vm.show("more")}})}){pad->
   Box(Modifier.fillMaxSize().padding(pad)){
    if(s.screen=="boot"){Column(Modifier.fillMaxSize(),horizontalAlignment=Alignment.CenterHorizontally,verticalArrangement=Arrangement.Center){Text("🛍️",fontSize=44.sp);Spacer(Modifier.height(10.dp));Text(stringResource(R.string.app_name),style=MaterialTheme.typography.titleLarge,fontWeight=FontWeight.ExtraBold,color=DukaanColors.Navy);Spacer(Modifier.height(6.dp));LinearProgressIndicator(Modifier.width(140.dp),color=DukaanColors.BlinkitGreen)}}
    else Column(Modifier.fillMaxSize()){Column(Modifier.padding(horizontal=20.dp).weight(1f)){
     s.error?.let{ErrorBanner(it,{vm.clearError()})}
     when(s.screen){
      "login"->PremiumWelcome({p,n,role->vm.login(p,n,role)},{role->vm.showWithRole("register",role)})
      "register"->Register(s.pendingRole,{p,n,c,r->vm.register(p,n,c,r)},{vm.back()})
      "profile"->Profile(s.pendingRole){n,c,k,h->vm.profile(n,c,k,h)}
       "shop"->ShopForm{vm.shop(it)}
       "shopEdit"->ShopEditForm(s.editingShop,{n,o,a,l,c,p,ph,d->vm.saveShopEdit(n,o,a,l,c,p,ph,d)})
       "changePin"->ChangePinScreen{o,n,c->vm.changePin(o,n,c)}
      "dashboard"->ShopDashboard(s,{id->vm.products(id)},{vm.quickSaleForSelected()},{vm.customersForSelected()},{vm.openLowStock()},{vm.openOrders()},{vm.openShopOrdersSafe()},{vm.openMarket()},{id->vm.openReports()},{vm.openSalesHistory()})
      "reports"->ReportsScreen(s)
      "products"->ProductList(s,{vm.addProduct()},{vm.editProduct(it)},{vm.archive(it)},{vm.openInventory(it)},{vm.quickSaleForSelected()},{vm.openLowStock()},{vm.customersForSelected()})
       "lowStock"->LowStockScreen(s,{vm.addPurchase(it)},{vm.openPurchaseList()})
       "purchaseList"->PurchaseListScreen(s,{vm.togglePurchase(it)},{vm.deletePurchase(it)},{vm.openLowStock()})
      "inventory"->InventoryScreen(s,{q,t,n->vm.adjustInventory(q,t,n)})
       "quickSale"->QuickSaleScreen(s,{vm.scan(it)},{id,q->vm.changeSaleQuantity(id,q)},{vm.addLine(it)},{vm.onVoiceText(it)},{vm.setVoiceListening(true)},{vm.setVoiceListening(false)},{vm.voiceMicError(it)},{vm.attachScanned()},{vm.show("posCart")},{vm.customersForSelected()})
      "posCart"->PosCartScreen(s,{id,q->vm.changeSaleQuantity(id,q)},{t->vm.setDiscount(t)},{vm.show("posCheckout")})
      "posCheckout"->PosCheckoutScreen(s,{m->vm.selectMethod(m)},{vm.useWalkIn()},{vm.show("quickAddCustomer")},{vm.completeSale()})
      "quickAddCustomer"->QuickAddCustomerForm(s,{n,m->vm.saveQuickCustomer(n,m)})
      "saleSuccess"->SaleSuccessScreen(s,{vm.newSale()},{s.saleSuccess?.saleId?.let{vm.openReceipt(it)}?:Unit})
      "receipt"->ReceiptScreen(s,{vm.shareReceipt()})
      "finance"->FinanceScreen(s)
      "customers"->CustomersScreen(s,{vm.addCustomer()},{vm.editCustomer(it)},{vm.openCustomer(it)},{vm.archiveCustomer(it)})
      "customerForm"->CustomerForm(s,{n,m,c,l,no->vm.saveCustomer(n,m,c,l,no)})
      "customerDetail"->CustomerDetail(s,{t,a,no->vm.credit(t,a,no)});      "sales"->SalesHistoryScreen(s,{vm.openReceipt(it)})
      "market"->MarketScreen(s,{vm.searchMarket(it)},{vm.openShopPage(it)})
      "shopPage"->ShopPageScreen(s,{vm.addToCart(it)},{id,q->vm.setCartQuantity(id,q)},{vm.openCheckout()})
      "checkout"->CheckoutScreen(s,{f,a,n->vm.placeOrder(f,a,n)},{rec,mob,l,loc,city,pin->vm.addAddress(rec,mob,l,loc,city,pin)})
      "orders"->OrdersScreen(s,{vm.openOrder(it)})
      "orderDetail"->OrderDetailScreen(s,{vm.cancelOrder(it)})
      "shopOrders"->ShopOrdersScreen(s,{id,st->vm.advanceOrder(id,st)})
      "productForm"->ProductForm(s.editing,s.currentImage,s.pendingBarcode,s.scannedGlobal?.name,s.scannedGlobal?.brand,{draft,image->vm.saveProduct(draft,image)},{vm.removeImage()},{vm.scanForForm(it)},s.products)
       "more"->MoreTabScreen(s,{vm.openShopOrdersSafe()},{vm.openMarket()},{vm.openReports()},{vm.openSalesHistory()},{vm.openOrders()},{vm.show("shop")},{vm.editShopSafe()},{vm.openChangePin()},{vm.openFinanceSafe()},{vm.logout()})
      else->Home(s,{id->vm.products(id)},{id->vm.openDashboard(id)},{id->vm.openQuickSale(id)},{vm.openMarket()},{vm.openOrders()},{vm.openShopOrdersSafe()},{vm.show("shop")},{vm.logout()})
     }
    }}
    if(s.busy)LinearProgressIndicator(Modifier.fillMaxWidth().height(4.dp).align(Alignment.TopCenter))
    }
   }
    // Voice cards har screen ke upar-neeche float karte hain (drawer ke neeche).
    Box(Modifier.fillMaxSize().align(Alignment.BottomCenter)){VoiceOverlay(s,{opt->vm.voiceConfirmSale(opt)},{opt->vm.voicePickChoice(opt)},{vm.clearVoice()})}
    // Drawer Scaffold ke sibling hai — poore viewport (top bar + bottom nav dono) ke UPAR overlay hota hai
    DrawerOverlay(s,{vm.closeDrawer()},{id->vm.closeDrawer();vm.openDashboard(id)},{vm.closeDrawer();vm.openMarket()},{vm.closeDrawer();vm.openOrders()},{vm.closeDrawer();vm.openShopOrdersSafe()},{vm.closeDrawer();vm.openReports()},{vm.closeDrawer();vm.openChangePin()},{vm.logout()})
  }
 }}

@Composable fun TopBar(s:UiState,openDrawer:()->Unit,back:()->Unit,togglePublish:()->Unit,homeTitle:String?=null){val screen=s.screen;val title=when(screen){"home"->homeTitle?:"LocalDukaan";"dashboard"->s.shops.firstOrNull{it.id==s.selectedShopId}?.name?:"Dukaan";"reports"->"📊 Reports";"finance"->"💰 Finance";"posCart"->stringResource(R.string.cart);"posCheckout"->stringResource(R.string.checkout);"quickAddCustomer"->stringResource(R.string.add_customer);"saleSuccess"->"";"receipt"->stringResource(R.string.receipt);"products"->stringResource(R.string.products);"quickSale"->stringResource(R.string.quick_sale);"lowStock"->stringResource(R.string.low_stock_title);"purchaseList"->stringResource(R.string.purchase_list);"inventory"->stringResource(R.string.inventory_title);"customers"->stringResource(R.string.customers);"customerForm"->if(s.customer!=null)stringResource(R.string.edit_customer) else stringResource(R.string.add_customer);"customerDetail"->stringResource(R.string.statement);"market"->stringResource(R.string.nearby_shops);"shopPage"->s.marketShop?.name?:stringResource(R.string.cart);"checkout"->stringResource(R.string.place_order);"orders"->stringResource(R.string.my_orders);"orderDetail"->stringResource(R.string.order_label);"shopOrders"->stringResource(R.string.shop_orders);"sales"->"🧾 Sales History";"productForm"->if(s.editing!=null)stringResource(R.string.edit_product) else stringResource(R.string.add_product);"profile"->"👤 Profile";"shop"->"🏪 Naya dukaan";"shopEdit"->stringResource(R.string.edit_shop);"changePin"->stringResource(R.string.change_pin);"more"->"🛠️ Shop Operations";else->""}
  Row(Modifier.fillMaxWidth().statusBarsPadding().padding(start=4.dp,end=16.dp,top=6.dp,bottom=4.dp),verticalAlignment=Alignment.CenterVertically){
   Surface(shape=RoundedCornerShape(14.dp),color=Color.White,shadowElevation=3.dp){
   Row(Modifier.padding(end=14.dp),verticalAlignment=Alignment.CenterVertically){
   if(screen=="home"||screen=="dashboard"){IconButton(openDrawer){Text("☰",fontSize=24.sp)}} else {IconButton(back){Text("←",fontSize=22.sp)}}
   Spacer(Modifier.width(2.dp))
   Text(title,style=MaterialTheme.typography.titleLarge,fontWeight=FontWeight.Bold,maxLines=1,overflow=androidx.compose.ui.text.style.TextOverflow.Ellipsis,modifier=Modifier.weight(1f))}}
  if(screen=="dashboard"&&s.selectedShopId!=null)PublishChip(s.shops.firstOrNull{it.id==s.selectedShopId}?.isPublished==false,togglePublish)
  Spacer(Modifier.width(6.dp))}}
enum class DukaanTab{DASHBOARD,CATALOGUE,QUICK_SALE,KHATA,MORE}
@Composable fun DukaanNavBar(moreSelected:Boolean,onSelect:(DukaanTab)->Unit){
 fun bold(sel:Boolean)=if(sel)FontWeight.Bold else FontWeight.Normal
 Column(Modifier.fillMaxWidth().background(Color.White).navigationBarsPadding()){
 NavigationBar(containerColor=Color.White,tonalElevation=8.dp,windowInsets=WindowInsets(0.dp)){
  NavigationBarItem(selected=!moreSelected,onClick={onSelect(DukaanTab.DASHBOARD)},icon={NavIcon(Icons.Default.Dashboard,"Home",!moreSelected)},label={Text("Home",fontSize=11.sp,fontWeight=bold(!moreSelected),maxLines=1)},colors=navColors(!moreSelected))
  NavigationBarItem(selected=false,onClick={onSelect(DukaanTab.CATALOGUE)},icon={NavIcon(Icons.Default.Inventory2,"Products",false)},label={Text("Products",fontSize=11.sp,maxLines=1)},colors=navColors(false))
  NavigationBarItem(selected=false,onClick={onSelect(DukaanTab.QUICK_SALE)},icon={NavIcon(Icons.Default.Bolt,"POS",false)},label={Text("POS",fontSize=11.sp,maxLines=1)},colors=navColors(false))
  NavigationBarItem(selected=false,onClick={onSelect(DukaanTab.KHATA)},icon={NavIcon(Icons.Default.AccountBalanceWallet,"Udhari",false)},label={Text("Udhari",fontSize=11.sp,maxLines=1)},colors=navColors(false))
  NavigationBarItem(selected=moreSelected,onClick={onSelect(DukaanTab.MORE)},icon={NavIcon(Icons.Default.Storefront,"Shop",moreSelected)},label={Text("Shop",fontSize=11.sp,fontWeight=bold(moreSelected),maxLines=1)},colors=navColors(moreSelected))
 }}}
@Composable private fun navColors(active:Boolean)=NavigationBarItemDefaults.colors(indicatorColor=DukaanColors.LightGreen,selectedIconColor=DukaanColors.BlinkitGreen,selectedTextColor=DukaanColors.BlinkitGreen,unselectedIconColor=DukaanColors.Slate400,unselectedTextColor=DukaanColors.Slate400)
@Composable private fun NavIcon(icon:androidx.compose.ui.graphics.vector.ImageVector,desc:String,active:Boolean){
 Box(Modifier.size(34.dp).clip(RoundedCornerShape(11.dp)).background(if(active)DukaanColors.LightGreen else Color.Transparent),contentAlignment=Alignment.Center){Icon(icon,desc,tint=if(active)DukaanColors.BlinkitGreen else DukaanColors.Slate400,modifier=Modifier.size(21.dp))}}
@Composable fun MoreTabScreen(s:UiState,shopOrders:()->Unit,market:()->Unit,reports:()->Unit,salesHistory:()->Unit,myOrders:()->Unit,shopSettings:()->Unit,editShop:()->Unit,changePin:()->Unit,finance:()->Unit,logout:()->Unit){
 LazyColumn(Modifier.fillMaxSize(),verticalArrangement=Arrangement.spacedBy(10.dp),contentPadding=PaddingValues(top=6.dp,bottom=20.dp)){
  item{ScreenTitle("🛠️ Shop Operations","Orders, reports aur settings — sab yahan se")}
  item{MoreMenuItem(Icons.Default.ReceiptLong,"Orders & Sales Register","Complete, pending aur cancelled sales ka poora record",shopOrders)}
  item{MoreMenuItem(Icons.Default.Storefront,"Customer Marketplace","Pados ki dukaanein dekho, apni dukaan publish karo",market)}
  item{MoreMenuItem(Icons.Default.Analytics,"Business Reports","Revenue trends, profit aur payment breakdown",reports)}
  item{MoreMenuItem(Icons.Default.Payments,"💰 Finance","Aaj ka collection, cash/UPI/udhaar aur dues — sab automatic",finance)}
  item{MoreMenuItem(Icons.Default.History,"Sales History","Kaun sa samaan CASH ya UDHARI mein gaya",salesHistory)}
  item{MoreMenuItem(Icons.Default.ShoppingBag,"My Orders","Aapke diye gaye online orders",myOrders)}
   item{MoreMenuItem(Icons.Default.Settings,"Naya Dukaan","Doosri dukaan banayein (30-din free trial)",shopSettings)}
   item{MoreMenuItem(Icons.Default.Storefront,"Dukaan Settings","Naam, pata, phone aur marketplace description badlein",editShop)}
   item{MoreMenuItem(Icons.Default.Lock,"PIN Badlo","Login PIN surakshit tareeke se change karein",changePin)}
  item{MoreMenuItem(Icons.Default.Logout,"Logout","Is device se session khatam karein",logout,danger=true)}
 }}
@Composable fun MoreMenuItem(icon:androidx.compose.ui.graphics.vector.ImageVector,title:String,subtitle:String,onClick:()->Unit,danger:Boolean=false){
 Card(onClick=onClick,Modifier.fillMaxWidth(),shape=RoundedCornerShape(12.dp),colors=CardDefaults.cardColors(containerColor=Color.White),elevation=CardDefaults.cardElevation(1.dp)){
  Row(Modifier.fillMaxWidth().padding(16.dp),verticalAlignment=Alignment.CenterVertically){
   Box(Modifier.size(40.dp).clip(RoundedCornerShape(12.dp)).background(if(danger)DukaanColors.LightRed else DukaanColors.LightBlue),contentAlignment=Alignment.Center){Icon(icon,null,tint=if(danger)DukaanColors.Red else DukaanColors.Navy,modifier=Modifier.size(20.dp))}
   Spacer(Modifier.width(12.dp));Column(Modifier.weight(1f)){Text(title,fontWeight=FontWeight.Bold,color=DukaanColors.Slate900);Text(subtitle,fontSize=12.sp,color=DukaanColors.Slate400)}
   Text("›",fontSize=18.sp,color=DukaanColors.Slate400)
  }}}
@Composable fun PublishChip(unpublished:Boolean,onToggle:()->Unit){Box(Modifier.clip(RoundedCornerShape(999.dp)).background(if(unpublished)Color(0xFFFEF3C7) else DukaanColors.LightBlue).clickable{onToggle()}.padding(horizontal=12.dp,vertical=6.dp)){Text(if(unpublished)"● Not live · tap to go live" else "● LIVE",fontSize=11.sp,fontWeight=FontWeight.Bold,color=if(unpublished)Color(0xFF92400E) else DukaanColors.Navy)}}
/** Drawer header ka pure logic — JVM unit test me verify hota hai (Avni-case samet). */
data class DrawerIdentity(val title:String,val subtitle:String?,val roleKey:String,val initial:String)
fun drawerIdentity(profile:LocalProfile?,shops:List<LocalShop>,selectedShopId:String?):DrawerIdentity{
 val displayName=profile?.name?.trim().takeIf{!it.isNullOrEmpty()}?:"LocalDukaan"
 val isKeeper=profile?.shopkeeper==true
 val selected=selectedShopId?.let{id->shops.firstOrNull{it.id==id}}?:shops.firstOrNull()
 val title=if(isKeeper)selected?.name?.takeIf{it.isNotBlank()}?:displayName else displayName
 // Subtitle kabhi title jaisa nahi — warna "Avni • Avni" jaisi duplicate line banti hai.
 val subtitle=if(isKeeper)(selected?.ownerName?.takeIf{it.isNotBlank()&&it!=title}?:displayName.takeIf{it!=title}) else null
 return DrawerIdentity(title,subtitle,if(isKeeper)"SHOPKEEPER" else "CUSTOMER",title.trim().firstOrNull()?.uppercase() ?: "L")
}
/** Full-viewport navigation drawer — Scaffold ke sibling overlay ke roop me render hota hai,
 *  isliye top bar + bottom nav dono ke UPAR aata hai. Width 84% screen, safe-area aware. */
@Composable fun DrawerOverlay(s:UiState,close:()->Unit,dashboard:(String)->Unit,market:()->Unit,myOrders:()->Unit,shopOrders:()->Unit,reports:()->Unit,changePin:()->Unit,logout:()->Unit){
 val configuration=androidx.compose.ui.platform.LocalConfiguration.current
 val drawerWidth=(configuration.screenWidthDp*0.84f).dp
 androidx.compose.animation.AnimatedVisibility(s.drawerOpen,enter=androidx.compose.animation.fadeIn(),exit=androidx.compose.animation.fadeOut()){Box(Modifier.fillMaxSize().background(Color(0x8A0F172A)).clickable{close()})}
 androidx.compose.animation.AnimatedVisibility(s.drawerOpen,enter=androidx.compose.animation.slideInHorizontally{it->-it},exit=androidx.compose.animation.slideOutHorizontally{it->-it}){
  Row(Modifier.fillMaxSize()){
   Card(Modifier.fillMaxHeight().width(drawerWidth),shape=RoundedCornerShape(0.dp),colors=CardDefaults.cardColors(containerColor=Color.White),elevation=CardDefaults.cardElevation(16.dp)){
    Column(Modifier.fillMaxSize().systemBarsPadding().imePadding()){
      // Premium identity header — SHOPKEEPER ke liye dukaan ka naam hero, vyakti ka naam subtitle.
      // Pehle hamesha vyakti ka naam bada dikhta tha (dukaan pehchani nahi jaati thi).
      Row(Modifier.fillMaxWidth().background(Brush.verticalGradient(listOf(DukaanColors.GreenDark,DukaanColors.BlinkitGreen))).padding(start=16.dp,end=16.dp,top=20.dp,bottom=20.dp),verticalAlignment=Alignment.CenterVertically){
       val id=drawerIdentity(s.profile,s.shops,s.selectedShopId)
       Box(Modifier.size(52.dp).clip(CircleShape).background(Color.White),contentAlignment=Alignment.Center){Text(id.initial,fontSize=22.sp,fontWeight=FontWeight.ExtraBold,color=DukaanColors.GreenDark)}
       Spacer(Modifier.width(12.dp))
       Column(Modifier.weight(1f)){
        Text(id.title,color=Color.White,fontWeight=FontWeight.ExtraBold,fontSize=17.sp,maxLines=1,overflow=androidx.compose.ui.text.style.TextOverflow.Ellipsis)
        Spacer(Modifier.height(5.dp))
        Row(verticalAlignment=Alignment.CenterVertically){
         Box(Modifier.clip(RoundedCornerShape(999.dp)).background(Color(0x40FFFFFF)).padding(horizontal=9.dp,vertical=3.dp)){Text((if(id.roleKey=="SHOPKEEPER")stringResource(R.string.shopkeeper) else stringResource(R.string.customer)).uppercase(),fontSize=10.sp,fontWeight=FontWeight.ExtraBold,color=Color.White,letterSpacing=0.8.sp)}
         if(id.subtitle!=null){Text("  •  ${id.subtitle}",fontSize=12.sp,fontWeight=FontWeight.Medium,color=Color(0xFFDBEAFE),maxLines=1,overflow=androidx.compose.ui.text.style.TextOverflow.Ellipsis,modifier=Modifier.weight(1f,false))}
        }
       }
      }
     // Scrollable menu — chhoti screens par bhi sab items accessible
     Column(Modifier.weight(1f,true).verticalScroll(rememberScrollState()).padding(vertical=6.dp)){
      if(s.profile?.shopkeeper==true){
       DrawerLabel("MY SHOP");DrawerItem(Icons.Default.Storefront,"My Shop",{s.shops.firstOrNull()?.let{dashboard(it.id)}})
       DrawerItem(Icons.Default.Analytics,"Reports",reports)
       DrawerLabel("ORDERS");DrawerItem(Icons.Default.ShoppingBag,"My Orders",myOrders)
       DrawerItem(Icons.Default.ReceiptLong,"Shop Orders",shopOrders)
       DrawerLabel("MARKET");DrawerItem(Icons.Default.Storefront,"Nearby Shops",market)
       } else {
        DrawerLabel("MARKET");DrawerItem(Icons.Default.Storefront,"Nearby Shops",market)
        DrawerItem(Icons.Default.ShoppingBag,"My Orders",myOrders)
        DrawerLabel("ACCOUNT");DrawerItem(Icons.Default.Lock,stringResource(R.string.change_pin),changePin)
       }
     }
     // Logout footer — hamesha visible, divider ke saath
     Divider(color=DukaanColors.Slate200)
     Column(Modifier.fillMaxWidth().padding(vertical=6.dp)){DrawerItem(Icons.Default.Logout,stringResource(R.string.logout),logout,danger=true)}
    }
   }
   // Right side tap-to-close area
   Box(Modifier.weight(1f).fillMaxHeight().clickable{close()})
  }}}
@Composable fun DrawerLabel(text:String){Text(text,fontSize=11.sp,fontWeight=FontWeight.ExtraBold,color=DukaanColors.Slate500,letterSpacing=1.2.sp,modifier=Modifier.padding(start=20.dp,top=14.dp,bottom=4.dp))}
@Composable fun DrawerItem(icon:androidx.compose.ui.graphics.vector.ImageVector,label:String,action:()->Unit,danger:Boolean=false){
 Row(Modifier.fillMaxWidth().padding(horizontal=12.dp).clip(RoundedCornerShape(12.dp)).clickable{action()}.padding(horizontal=8.dp,vertical=5.dp).heightIn(min=48.dp),verticalAlignment=Alignment.CenterVertically){
  Box(Modifier.size(38.dp).clip(RoundedCornerShape(11.dp)).background(if(danger)DukaanColors.LightRed else DukaanColors.LightGreen),contentAlignment=Alignment.Center){Icon(icon,null,tint=if(danger)DukaanColors.Red else DukaanColors.BlinkitGreen,modifier=Modifier.size(20.dp))}
  Spacer(Modifier.width(12.dp))
  Text(label,fontSize=15.sp,fontWeight=FontWeight.SemiBold,color=if(danger)DukaanColors.Red else DukaanColors.Dark,modifier=Modifier.weight(1f))
 }}
/** Compact shop summary: [icon] naam / sheher + LIVE badge. Logout yahan NAHI hota — drawer mein hai. */
@Composable fun DashHeader(s:UiState){
 val shop=s.shops.firstOrNull{it.id==s.selectedShopId}
 val trialDays=shop?.let{if(it.trialEndsAt>0)((it.trialEndsAt-System.currentTimeMillis()/1000)/86400).toInt() else null}
 Card(Modifier.fillMaxWidth(),shape=RoundedCornerShape(16.dp),colors=CardDefaults.cardColors(containerColor=Color.White),elevation=CardDefaults.cardElevation(1.dp)){
  Column(Modifier.fillMaxWidth().padding(12.dp)){
   Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically){
    Box(Modifier.size(42.dp).clip(RoundedCornerShape(12.dp)).background(DukaanColors.LightYellow),contentAlignment=Alignment.Center){Text("🏪",fontSize=20.sp)}
    Spacer(Modifier.width(10.dp))
    Column(Modifier.weight(1f)){
     Text(shop?.name?:"Dukaan",fontWeight=FontWeight.Bold,fontSize=15.sp,color=DukaanColors.Dark,maxLines=1,overflow=androidx.compose.ui.text.style.TextOverflow.Ellipsis)
     Text("${shop?.locality.orEmpty()}, ${shop?.city.orEmpty()}",fontSize=12.sp,color=DukaanColors.Slate600,maxLines=1,overflow=androidx.compose.ui.text.style.TextOverflow.Ellipsis)}
    StatusBadge(if(shop?.isPublished==true)"LIVE" else "DRAFT")
   }
   if(trialDays!=null){Spacer(Modifier.height(8.dp));Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically){Box(Modifier.clip(RoundedCornerShape(999.dp)).background(if(trialDays>0)Color(0xFFFEF3C7) else DukaanColors.LightRed).padding(horizontal=10.dp,vertical=4.dp)){Text(if(trialDays>0)"🎟 Trial: $trialDays din baaki" else "🎟 Trial khatm",fontSize=11.sp,fontWeight=FontWeight.Bold,color=if(trialDays>0)Color(0xFF92400E) else Color(0xFF991B1B))}}}
  }}}
@Composable fun ShopDashboard(s:UiState,products:(String)->Unit,quickSale:()->Unit,customers:()->Unit,lowStock:()->Unit,myOrders:()->Unit,shopOrders:()->Unit,market:()->Unit,reports:(String)->Unit,salesHistory:()->Unit){
 val today=ReportMath.todayEpochDay();val todaySales=s.dailySales.firstOrNull{it.epochDay==today};val todayProfit=s.dailyProfit.firstOrNull{it.epochDay==today}
 LazyColumn(Modifier.fillMaxSize(),verticalArrangement=Arrangement.spacedBy(10.dp),contentPadding=PaddingValues(top=6.dp,bottom=24.dp)){
  item{Text("👋 ${stringResource(R.string.namaste)} ${s.profile?.name.orEmpty()}",style=MaterialTheme.typography.headlineSmall,fontWeight=FontWeight.ExtraBold,color=DukaanColors.Dark)}
  item{Text("Your shop, your business",style=MaterialTheme.typography.bodyMedium,color=DukaanColors.Slate600)}
  item{DashHeader(s)}
  item{Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(10.dp)){MetricCard(Icons.Default.Payments,"Today Sales",ProductRules.rupees(todaySales?.salesPaise?:0),DukaanColors.BlinkitGreen,Modifier.weight(1f));MetricCard(Icons.Default.TrendingUp,"Today Profit",ProductRules.rupees(todayProfit?.profitPaise?:0),DukaanColors.Gold,Modifier.weight(1f))}}
  item{Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(10.dp)){MetricCard(Icons.Default.Inventory2,"Stock Value",ProductRules.rupees(s.stockValue?.totalStockValuePaise?:0),DukaanColors.Navy,Modifier.weight(1f));MetricCard(Icons.Default.ReceiptLong,"Bills Today","${todaySales?.bills?:0}",DukaanColors.Slate600,Modifier.weight(1f))}}
  item{if((s.dues?.second?:0)>0)Card(Modifier.fillMaxWidth(),shape=RoundedCornerShape(16.dp),colors=CardDefaults.cardColors(containerColor=DukaanColors.LightRed)){Row(Modifier.padding(14.dp),verticalAlignment=Alignment.CenterVertically){Text("⚠️",fontSize=20.sp);Spacer(Modifier.width(8.dp));Text("${s.dues!!.second} customers ka udhari baaki: ${ProductRules.rupees(s.dues!!.first)}",color=Color(0xFF991B1B),fontWeight=FontWeight.SemiBold)}}}
  item{Text("Quick Actions",style=MaterialTheme.typography.titleMedium,fontWeight=FontWeight.Bold,color=DukaanColors.Dark)}
  item{Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(10.dp)){DashAction(Icons.Default.Bolt,stringResource(R.string.quick_sale),quickSale,Modifier.weight(1f));DashAction(Icons.Default.Inventory2,stringResource(R.string.products),{s.selectedShopId?.let(products)},Modifier.weight(1f))}}
  item{Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(10.dp)){DashAction(Icons.Default.People,stringResource(R.string.customers),customers,Modifier.weight(1f));DashAction(Icons.Default.TrendingDown,stringResource(R.string.low_stock_title),lowStock,Modifier.weight(1f))}}
  item{Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(10.dp)){DashAction(Icons.Default.ReceiptLong,stringResource(R.string.shop_orders),shopOrders,Modifier.weight(1f));DashAction(Icons.Default.Analytics,"Reports",{s.selectedShopId?.let(reports)},Modifier.weight(1f))}}
  item{DashAction(Icons.Default.History,"Sales History — kaun sa samaan CASH ya UDHARI mein gaya",salesHistory,Modifier.fillMaxWidth())}
  item{Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(10.dp)){DashAction(Icons.Default.Storefront,stringResource(R.string.nearby_shops),market,Modifier.weight(1f));DashAction(Icons.Default.ShoppingBag,stringResource(R.string.my_orders),myOrders,Modifier.weight(1f))}}
  item{Text("Last 7 days",style=MaterialTheme.typography.titleMedium,fontWeight=FontWeight.Bold,color=BrandGreenDark)}
  if(s.dailySales.isEmpty())item{Text("Abhi koi sale nahi hui. Quick Sale se pehli bikri karo!",color=Color(0xFF5B6B62))}
  val days=s.dailySales.take(7);val profitByDay=s.dailyProfit.associateBy{it.epochDay}
  items(days,key={it.epochDay}){row->val p=profitByDay[row.epochDay];Card(shape=RoundedCornerShape(14.dp),colors=CardDefaults.cardColors(containerColor=Color.White)){Row(Modifier.padding(12.dp),verticalAlignment=Alignment.CenterVertically){Column(Modifier.weight(1f)){Text(dayLabel(row.epochDay,today),fontWeight=FontWeight.Bold);Text("${row.bills} bills • ${ProductRules.rupees(row.creditPaise)} udhari",color=Color(0xFF5B6B62),fontSize=12.sp)};Column(horizontalAlignment=Alignment.End){Text(ProductRules.rupees(row.salesPaise),fontWeight=FontWeight.ExtraBold,color=BrandGreenDark);Text("+${ProductRules.rupees(p?.profitPaise?:0)}",color=BrandBlue,fontSize=12.sp,fontWeight=FontWeight.SemiBold)}}}}
 }}
/** POS — Scan/Search → grid → tap-to-add. Bahut kam text, bade touch targets. */
/** POS — Scan/Search → grid → tap-to-add. Bahut kam text, bade touch targets. */
@Composable fun QuickSaleScreen(s:UiState,onScan:(String)->Unit,onQuantity:(String,Long)->Unit,addLine:(ProductRow)->Unit,onMicHeard:(String)->Unit,onMicStart:()->Unit,onMicStop:()->Unit,onMicError:(String)->Unit,createProduct:()->Unit,openCart:()->Unit,openCustomers:()->Unit){
 var scanning by remember{mutableStateOf(false)};var query by remember{mutableStateOf("")}
 val shown=s.products.filter{query.isBlank()||it.product.name.contains(query,true)||it.product.brand.orEmpty().contains(query,true)||it.product.barcode.orEmpty().contains(query,true)}
 Column(Modifier.fillMaxSize()){
  Row(Modifier.fillMaxWidth().padding(horizontal=16.dp),horizontalArrangement=Arrangement.spacedBy(8.dp),verticalAlignment=Alignment.CenterVertically){
   OutlinedTextField(query,{query=it},placeholder={Text(stringResource(R.string.search_products))},leadingIcon={Text("🔍",fontSize=16.sp)},modifier=Modifier.weight(1f),shape=RoundedCornerShape(14.dp),singleLine=true)
   VoiceMicButton(s.voiceListening,onMicHeard,onMicStart,onMicStop,onMicError)
   Button({scanning=!scanning},Modifier.height(56.dp),colors=ButtonDefaults.buttonColors(containerColor=DukaanColors.Navy),shape=RoundedCornerShape(14.dp)){Text(if(scanning)"✕" else "📷",fontSize=20.sp)}
  }
  if(scanning){`in`.localdukaan.feature.BarcodeScanner(true,onScan,Modifier.fillMaxWidth().padding(horizontal=16.dp,vertical=8.dp).aspectRatio(16f/9f).clip(RoundedCornerShape(14.dp)))}
  s.scanMessage?.let{Text(it,Modifier.padding(horizontal=16.dp),color=if(s.scanError==true)Color(0xFFB91C1C) else BrandGreenDark,fontWeight=FontWeight.SemiBold,fontSize=13.sp)}
  if(s.scannedGlobal!=null||(s.scanError==true&&s.pendingBarcode!=null))Row(Modifier.fillMaxWidth().padding(horizontal=16.dp),verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(10.dp)){s.currentImage?.let{bytes->android.graphics.BitmapFactory.decodeByteArray(bytes,0,bytes.size)?.let{bmp->androidx.compose.foundation.Image(bmp.asImageBitmap(),null,modifier=Modifier.size(48.dp).clip(RoundedCornerShape(10.dp)),contentScale=androidx.compose.ui.layout.ContentScale.Crop)}};Column(Modifier.weight(1f)){Text(s.scannedGlobal?.name?:"Naya product",fontWeight=FontWeight.Bold,fontSize=13.sp,maxLines=1,overflow=androidx.compose.ui.text.style.TextOverflow.Ellipsis)};Button(createProduct,colors=ButtonDefaults.buttonColors(containerColor=BrandGreen)){Text(stringResource(if(s.scannedGlobal!=null)R.string.add_to_my_shop else R.string.create_product),fontSize=12.sp)}}
  if(shown.isEmpty())Column(Modifier.fillMaxWidth().padding(top=40.dp),horizontalAlignment=Alignment.CenterHorizontally){Text("📦",fontSize=44.sp);Text(stringResource(R.string.empty_products),color=Color(0xFF5B6B62),textAlign=androidx.compose.ui.text.style.TextAlign.Center)}
  androidx.compose.foundation.lazy.grid.LazyVerticalGrid(columns=androidx.compose.foundation.lazy.grid.GridCells.Fixed(2),modifier=Modifier.weight(1f).padding(horizontal=16.dp),horizontalArrangement=Arrangement.spacedBy(10.dp),verticalArrangement=Arrangement.spacedBy(10.dp),contentPadding=PaddingValues(vertical=10.dp)){
    items(shown.size){i->val row=shown[i];Card(onClick={addLine(row)},shape=RoundedCornerShape(18.dp),colors=CardDefaults.cardColors(containerColor=Color.White),elevation=CardDefaults.cardElevation(1.dp)){Column(Modifier.fillMaxWidth().padding(12.dp)){
     Box(Modifier.fillMaxWidth().height(64.dp).clip(RoundedCornerShape(12.dp)).background(DukaanColors.LightGreen),contentAlignment=Alignment.Center){ProductThumb(row)}
     Spacer(Modifier.height(8.dp));Text(row.product.name,fontWeight=FontWeight.Bold,fontSize=14.sp,color=DukaanColors.Dark,maxLines=1,overflow=androidx.compose.ui.text.style.TextOverflow.Ellipsis)
     Text(ProductRules.rupees(row.shopProduct.sellingPricePaise),style=MaterialTheme.typography.titleLarge,fontWeight=FontWeight.ExtraBold,color=BrandGreenDark)
     val out=row.shopProduct.stockQuantityMilli-row.shopProduct.reservedQuantityMilli<=0
     Text(when{out->stringResource(R.string.out_of_stock);row.lowStock->stringResource(R.string.low_stock);else->stringResource(R.string.in_stock)},fontSize=11.sp,fontWeight=FontWeight.SemiBold,color=if(out)BrandRed else Color(0xFF5B6B62))
     Spacer(Modifier.height(6.dp));Button({addLine(row)},Modifier.fillMaxWidth().height(38.dp),enabled=!out,colors=ButtonDefaults.buttonColors(containerColor=BrandGreen),shape=RoundedCornerShape(10.dp)){Text("+ "+stringResource(R.string.add),fontWeight=FontWeight.Bold,fontSize=13.sp)}
    }}}}
  if(s.quickSale.lines.isNotEmpty())Surface(color=Color.White,shadowElevation=10.dp){Row(Modifier.fillMaxWidth().padding(horizontal=16.dp,vertical=12.dp),verticalAlignment=Alignment.CenterVertically){
   Column(Modifier.weight(1f)){Text("${s.quickSale.lines.size} "+stringResource(R.string.items),fontWeight=FontWeight.Bold,fontSize=13.sp,color=DukaanColors.Slate600);Text(ProductRules.rupees(s.quickSale.subtotalPaise),style=MaterialTheme.typography.titleLarge,fontWeight=FontWeight.ExtraBold,color=DukaanColors.Navy)}
   Button(openCart,Modifier.height(52.dp),colors=ButtonDefaults.buttonColors(containerColor=BrandGreen),shape=RoundedCornerShape(14.dp)){Text("🛒 "+stringResource(R.string.view_cart),fontWeight=FontWeight.Bold,fontSize=15.sp)}}}
  else Spacer(Modifier.height(12.dp))
}}
/** 🎙 Voice orders — mic dabao, bolo, screen par confirm karo. Awaz record nahi hoti, sirf text samjha jata hai. */
@Composable fun VoiceMicButton(listening:Boolean,onHeard:(String)->Unit,onStart:()->Unit,onStop:()->Unit,onMicError:(String)->Unit){
 val context=androidx.compose.ui.platform.LocalContext.current
 val listener=remember(context){VoiceListener(context)}
 var hasMic by remember{mutableStateOf(ContextCompat.checkSelfPermission(context,Manifest.permission.RECORD_AUDIO)==PackageManager.PERMISSION_GRANTED)}
 val failMsg={f:VoiceFail->when(f){VoiceFail.NOT_SUPPORTED->context.getString(R.string.voice_no_support);VoiceFail.NO_MIC->context.getString(R.string.voice_need_mic);VoiceFail.NEEDS_NET->context.getString(R.string.voice_need_net);VoiceFail.NO_SPEECH->context.getString(R.string.voice_not_understood);VoiceFail.BUSY->context.getString(R.string.voice_listening)}}
 fun begin(){
  if(!SpeechRecognizer.isRecognitionAvailable(context)){onMicError(context.getString(R.string.voice_no_support));return}
  onStart()
  listener.start{res->when(res){
   is VoiceListenResult.Heard->{onHeard(res.text);if(res.final){listener.stop();onStop()}}
   is VoiceListenResult.Failed->{listener.stop();onStop();onMicError(failMsg(res.reason))}
  }}
 }
 val perm=androidx.activity.compose.rememberLauncherForActivityResult(androidx.activity.result.contract.ActivityResultContracts.RequestPermission()){granted->hasMic=granted;if(granted)begin() else onMicError(context.getString(R.string.voice_need_mic))}
 androidx.compose.runtime.DisposableEffect(Unit){onDispose{listener.stop();onStop()}}
 Button(onClick={if(listening){listener.stop();onStop()}else{if(!hasMic)perm.launch(Manifest.permission.RECORD_AUDIO) else begin()}},Modifier.height(56.dp),colors=ButtonDefaults.buttonColors(containerColor=if(listening)BrandRed else BrandGreen),shape=RoundedCornerShape(14.dp)){Text(if(listening)"⏺" else "🎙",fontSize=20.sp)}
}
/** Voice overlay — har screen ke upar: sunna, confirm, choice, jawab. Koi auto-sale nahi. */
@Composable fun VoiceOverlay(s:UiState,confirm:(SaleOption)->Unit,pick:(SaleOption)->Unit,dismiss:()->Unit){
 val showListen=s.voiceListening||s.voiceHeard!=null;val showConfirm=s.voiceConfirm!=null;val showChoices=s.voiceChoices.isNotEmpty();val showAnswer=s.voiceAnswer!=null;val showError=s.voiceError!=null
 if(!showListen&&!showConfirm&&!showChoices&&!showAnswer&&!showError)return
 Column(Modifier.fillMaxWidth().padding(horizontal=16.dp,vertical=8.dp),verticalArrangement=Arrangement.spacedBy(8.dp)){
  if(s.voiceListening)Card(Modifier.fillMaxWidth(),shape=RoundedCornerShape(16.dp),colors=CardDefaults.cardColors(containerColor=DukaanColors.Navy)){Row(Modifier.padding(14.dp),verticalAlignment=Alignment.CenterVertically){Text("🎙",fontSize=22.sp);Spacer(Modifier.width(10.dp));Column(Modifier.weight(1f)){Text(stringResource(R.string.voice_listening),color=Color.White,fontWeight=FontWeight.Bold);s.voiceHeard?.let{Text("“$it”",color=Color(0xFFC7F0D2),fontSize=13.sp,maxLines=2,overflow=androidx.compose.ui.text.style.TextOverflow.Ellipsis)}}}}
  s.voiceConfirm?.let{opt->
   val row=s.products.firstOrNull{it.shopProduct.id==opt.productId}
   val total=opt.unitPricePaise*opt.quantityMilli/1000;val profit=if((row?.shopProduct?.costPricePaise?:0)>0)(opt.unitPricePaise-row!!.shopProduct.costPricePaise)*opt.quantityMilli/1000 else 0L
   Card(Modifier.fillMaxWidth(),shape=RoundedCornerShape(18.dp),colors=CardDefaults.cardColors(containerColor=Color.White),elevation=CardDefaults.cardElevation(6.dp)){Column(Modifier.padding(16.dp)){
    Text("🎙 "+stringResource(R.string.voice_confirm_sale),fontWeight=FontWeight.ExtraBold,fontSize=16.sp,color=DukaanColors.Dark)
    Spacer(Modifier.height(6.dp));Text("${opt.name} × ${ProductRules.quantity(opt.quantityMilli)} = ${ProductRules.rupees(total)}",fontWeight=FontWeight.Bold,color=DukaanColors.Navy)
    if(profit>0)Text("📈 "+stringResource(R.string.voice_sale_profit)+": ${ProductRules.rupees(profit)}",fontSize=13.sp,color=BrandGreenDark,fontWeight=FontWeight.SemiBold)
    Spacer(Modifier.height(10.dp));Row(horizontalArrangement=Arrangement.spacedBy(8.dp)){Button({confirm(opt)},Modifier.weight(1f),colors=ButtonDefaults.buttonColors(containerColor=BrandGreen),shape=RoundedCornerShape(12.dp)){Text("✓ "+stringResource(R.string.voice_sell),fontWeight=FontWeight.Bold)};OutlinedButton(dismiss,Modifier.weight(1f),shape=RoundedCornerShape(12.dp)){Text("✕ "+stringResource(R.string.voice_no))}}
    Text("\"${stringResource(R.string.voice_yes)}\" bolo ya button dabao",fontSize=11.sp,color=DukaanColors.Slate500)
   }}
  }
  if(showChoices)Card(Modifier.fillMaxWidth(),shape=RoundedCornerShape(18.dp),colors=CardDefaults.cardColors(containerColor=Color.White),elevation=CardDefaults.cardElevation(6.dp)){Column(Modifier.padding(12.dp)){Text("Kaun sa wala?",fontWeight=FontWeight.Bold);s.voiceChoices.forEach{opt->Row(Modifier.fillMaxWidth().clickable{pick(opt)}.padding(vertical=8.dp,horizontal=4.dp),verticalAlignment=Alignment.CenterVertically){Column(Modifier.weight(1f)){Text(opt.name,fontWeight=FontWeight.SemiBold,maxLines=1,overflow=androidx.compose.ui.text.style.TextOverflow.Ellipsis);Text(ProductRules.rupees(opt.unitPricePaise),fontSize=12.sp,color=BrandGreenDark)};Text("›",fontSize=20.sp,color=DukaanColors.Slate400)}}}}
  s.voiceAnswer?.let{ans->Card(Modifier.fillMaxWidth(),shape=RoundedCornerShape(16.dp),colors=CardDefaults.cardColors(containerColor=DukaanColors.LightGreen)){Row(Modifier.padding(14.dp),verticalAlignment=Alignment.CenterVertically){Text("🔊",fontSize=20.sp);Spacer(Modifier.width(10.dp));Text(ans,Modifier.weight(1f),fontWeight=FontWeight.SemiBold,color=DukaanColors.Dark);TextButton(dismiss){Text("OK",color=DukaanColors.Navy,fontWeight=FontWeight.Bold)}}}}
  s.voiceError?.let{err->Card(Modifier.fillMaxWidth(),shape=RoundedCornerShape(16.dp),colors=CardDefaults.cardColors(containerColor=DukaanColors.LightRed)){Row(Modifier.padding(14.dp),verticalAlignment=Alignment.CenterVertically){Text("⚠️",fontSize=20.sp);Spacer(Modifier.width(10.dp));Text(err,Modifier.weight(1f),color=Color(0xFF991B1B));TextButton(dismiss){Text("OK",color=Color(0xFF991B1B),fontWeight=FontWeight.Bold)}}}}
 }
}
@Composable fun ProductThumb(row:ProductRow){val key=row.shopProduct.imageKey
 if(key!=null&&key.startsWith("local-file:")){val bmp=remember(key){runCatching{android.graphics.BitmapFactory.decodeFile(key.removePrefix("local-file:"))}.getOrNull()};if(bmp!=null){androidx.compose.foundation.Image(bmp.asImageBitmap(),null,modifier=Modifier.fillMaxSize(),contentScale=androidx.compose.ui.layout.ContentScale.Crop);return}}
 Text("📦",fontSize=30.sp)}
/** Cart — quantity -/+, remove, optional discount, bada CONTINUE. */
@Composable fun PosCartScreen(s:UiState,onQuantity:(String,Long)->Unit,onDiscount:(String)->Unit,checkout:()->Unit){
 var discount by remember{mutableStateOf(if(s.quickSale.discountPaise>0)ProductRules.rupees(s.quickSale.discountPaise) else "")}
 Column(Modifier.fillMaxSize()){
  LazyColumn(Modifier.weight(1f),verticalArrangement=Arrangement.spacedBy(8.dp),contentPadding=PaddingValues(horizontal=16.dp,vertical=8.dp)){
   items(s.quickSale.lines.values.toList(),key={it.shopProductId}){line->Card(shape=RoundedCornerShape(16.dp),colors=CardDefaults.cardColors(containerColor=Color.White),elevation=CardDefaults.cardElevation(1.dp)){Row(Modifier.fillMaxWidth().padding(12.dp),verticalAlignment=Alignment.CenterVertically){
    Column(Modifier.weight(1f)){Text(line.name,fontWeight=FontWeight.Bold,color=DukaanColors.Slate900,maxLines=1,overflow=androidx.compose.ui.text.style.TextOverflow.Ellipsis);Text("${ProductRules.rupees(line.unitPricePaise)} × ${ProductRules.quantity(line.quantityMilli)} = ${ProductRules.rupees(line.lineTotalPaise)}",fontSize=12.sp,color=DukaanColors.Slate600)}
    Row(verticalAlignment=Alignment.CenterVertically,modifier=Modifier.clip(RoundedCornerShape(10.dp)).background(DukaanColors.LightBlue)){IconButton({onQuantity(line.shopProductId,line.quantityMilli-1000)},Modifier.size(36.dp)){Icon(Icons.Default.Remove,null,tint=DukaanColors.Navy,modifier=Modifier.size(18.dp))};Text(ProductRules.quantity(line.quantityMilli),fontWeight=FontWeight.Bold,color=DukaanColors.Navy);IconButton({onQuantity(line.shopProductId,line.quantityMilli+1000)},Modifier.size(36.dp)){Icon(Icons.Default.Add,null,tint=DukaanColors.Navy,modifier=Modifier.size(18.dp))}}
    IconButton({onQuantity(line.shopProductId,0)}){Icon(Icons.Default.Close,null,tint=BrandRed)}
   }}}
   item{SectionCard{Field(stringResource(R.string.discount_optional_short),discount,{discount=it;onDiscount(it)},KeyboardType.Decimal)}}
  }
  Surface(color=Color.White,shadowElevation=10.dp){Column(Modifier.fillMaxWidth().padding(horizontal=16.dp,vertical=12.dp)){
   Row(Modifier.fillMaxWidth()){Text(stringResource(R.string.subtotal),color=DukaanColors.Slate600,modifier=Modifier.weight(1f));Text(ProductRules.rupees(s.quickSale.subtotalPaise),fontWeight=FontWeight.Bold)}
   if(s.quickSale.discountPaise>0)Row(Modifier.fillMaxWidth()){Text(stringResource(R.string.discount),color=BrandGreenDark,modifier=Modifier.weight(1f));Text("-"+ProductRules.rupees(s.quickSale.discountPaise),fontWeight=FontWeight.Bold,color=BrandGreenDark)}
   Row(Modifier.fillMaxWidth()){Text(stringResource(R.string.total),style=MaterialTheme.typography.titleLarge,fontWeight=FontWeight.ExtraBold,modifier=Modifier.weight(1f));Text(ProductRules.rupees(s.quickSale.totalPaise),style=MaterialTheme.typography.titleLarge,fontWeight=FontWeight.ExtraBold,color=DukaanColors.Navy)}
   Spacer(Modifier.height(10.dp));GradientButton(stringResource(R.string.continue_label)+"  →",true,checkout);Spacer(Modifier.height(6.dp))
  }}}}
/** Ek hi checkout screen — customer OPTIONAL (Walk-in default) + CASH/UPI/UDHAARI. */
@Composable fun PosCheckoutScreen(s:UiState,onMethod:(PaymentMethod)->Unit,onWalkIn:()->Unit,openCustomerForm:()->Unit,complete:()->Unit){
 val q=s.quickSale
 Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal=16.dp)){
  Spacer(Modifier.height(6.dp));SectionCard{Column{Text(stringResource(R.string.customer),style=MaterialTheme.typography.titleMedium,fontWeight=FontWeight.Bold)
   Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically){RadioButton(selected=q.walkIn,onClick={onWalkIn()});Text("✓ "+stringResource(R.string.walk_in_customer),fontWeight=if(q.walkIn)FontWeight.Bold else FontWeight.Normal,color=DukaanColors.Dark)}
   Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically){RadioButton(selected=!q.walkIn,onClick={openCustomerForm()});Column{Text(if(!q.walkIn)q.customerName?:stringResource(R.string.add_customer_details) else stringResource(R.string.add_customer_details),fontWeight=if(!q.walkIn)FontWeight.Bold else FontWeight.Normal,color=DukaanColors.Dark);if(!q.walkIn)q.customerPhone?.let{Text(it,fontSize=12.sp,color=DukaanColors.Slate500)}}}
  }}
  Spacer(Modifier.height(10.dp));Text(stringResource(R.string.payment_method),style=MaterialTheme.typography.titleMedium,fontWeight=FontWeight.Bold);Spacer(Modifier.height(8.dp))
  Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(10.dp)){listOf(PaymentMethod.CASH,PaymentMethod.UPI,PaymentMethod.CREDIT).forEach{m->val sel=q.method==m;Card(onClick={onMethod(m)},Modifier.weight(1f),shape=RoundedCornerShape(16.dp),colors=CardDefaults.cardColors(containerColor=if(sel)DukaanColors.Navy else Color.White),elevation=CardDefaults.cardElevation(if(sel)4.dp else 1.dp)){Column(Modifier.fillMaxWidth().padding(vertical=16.dp),horizontalAlignment=Alignment.CenterHorizontally){Text(when(m){PaymentMethod.CASH->"💵";PaymentMethod.UPI->"📱";PaymentMethod.CREDIT->"📖"},fontSize=24.sp);Spacer(Modifier.height(4.dp));Text(m.label,fontWeight=FontWeight.Bold,color=if(sel)Color.White else DukaanColors.Dark)}}}}
  if(q.udhaariRequiresCustomer){Spacer(Modifier.height(10.dp));Card(Modifier.fillMaxWidth(),colors=CardDefaults.cardColors(containerColor=DukaanColors.LightRed),shape=RoundedCornerShape(12.dp)){Column(Modifier.padding(14.dp)){Text(stringResource(R.string.udhaari_needs_customer),color=Color(0xFF991B1B),fontWeight=FontWeight.SemiBold);TextButton(openCustomerForm){Text("＋ "+stringResource(R.string.add_customer),color=BrandRed,fontWeight=FontWeight.Bold)}}}}
  Spacer(Modifier.height(10.dp));SectionCard{Row(Modifier.fillMaxWidth()){Text(stringResource(R.string.total),style=MaterialTheme.typography.titleLarge,fontWeight=FontWeight.ExtraBold,modifier=Modifier.weight(1f));Text(ProductRules.rupees(q.totalPaise),style=MaterialTheme.typography.titleLarge,fontWeight=FontWeight.ExtraBold,color=DukaanColors.Navy)};if(q.discountPaise>0)Text(stringResource(R.string.discount)+": -"+ProductRules.rupees(q.discountPaise),color=BrandGreenDark,fontSize=13.sp)}
  Spacer(Modifier.height(12.dp));GradientButton("✓  "+stringResource(R.string.complete_sale),q.canComplete,complete);Spacer(Modifier.height(20.dp))
}}
/** POS ka chhota customer form — SIRF naam + mobile. Baaki kuch nahi. */
@Composable fun QuickAddCustomerForm(s:UiState,save:(String,String)->Unit){var n by remember{mutableStateOf("")};var m by remember{mutableStateOf("+91")}
 Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).imePadding()){Spacer(Modifier.height(10.dp));ScreenTitle("＋ "+stringResource(R.string.add_customer),stringResource(R.string.only_name_mobile));Spacer(Modifier.height(12.dp));SectionCard{Field(stringResource(R.string.name),n,{n=it});Field(stringResource(R.string.mobile_number),m,{m=it},KeyboardType.Phone)};Spacer(Modifier.height(14.dp));GradientButton(stringResource(R.string.save_customer),n.isNotBlank()){save(n,m)}}}
@Composable fun ErrorBanner(msg:String,onDismiss:()->Unit){Card(Modifier.fillMaxWidth().padding(vertical=4.dp),colors=CardDefaults.cardColors(containerColor=DukaanColors.LightRed),shape=RoundedCornerShape(12.dp)){Row(Modifier.padding(start=12.dp),verticalAlignment=Alignment.CenterVertically){Text("⚠️ ",fontSize=18.sp);Text(msg,Modifier.weight(1f).padding(vertical=12.dp),color=Color(0xFF991B1B),style=MaterialTheme.typography.bodyMedium);TextButton(onDismiss){Text("✕",color=Color(0xFF991B1B))}}}}

@Composable fun GradientButton(label:String,enabled:Boolean=true,action:()->Unit){Button(action,Modifier.fillMaxWidth().height(54.dp),enabled=enabled,colors=ButtonDefaults.buttonColors(containerColor=Color.Transparent,disabledContainerColor=Color.Transparent),shape=RoundedCornerShape(16.dp),elevation=ButtonDefaults.buttonElevation(if(enabled)6.dp else 0.dp),contentPadding=PaddingValues()){Box(Modifier.fillMaxSize().background(if(enabled)DukaanColors.ButtonGradient else Brush.verticalGradient(listOf(DukaanColors.Slate200,DukaanColors.Slate200)),RoundedCornerShape(16.dp)),contentAlignment=Alignment.Center){Text(label,color=Color.White,fontSize=17.sp,fontWeight=FontWeight.Bold)}}}
@Composable fun ActionButton(label:String,action:()->Unit){OutlinedButton(action,Modifier.fillMaxWidth().height(50.dp),shape=RoundedCornerShape(14.dp),colors=ButtonDefaults.outlinedButtonColors(contentColor=BrandGreenDark)){Text(label,fontWeight=FontWeight.SemiBold)}}
@Composable fun SectionCard(content:@Composable ColumnScope.()->Unit){Card(Modifier.fillMaxWidth(),colors=CardDefaults.cardColors(containerColor=DukaanColors.Slate100),shape=RoundedCornerShape(16.dp),elevation=CardDefaults.cardElevation(1.dp)){Column(Modifier.padding(16.dp),verticalArrangement=Arrangement.spacedBy(10.dp),content=content)}}
@Composable fun ScreenTitle(text:String,sub:String?=null){Column{Text(text,style=MaterialTheme.typography.headlineMedium,fontWeight=FontWeight.ExtraBold,color=DukaanColors.Navy);if(sub!=null)Text(sub,style=MaterialTheme.typography.bodyMedium,color=DukaanColors.Slate500)}}

@Composable fun StatusBadge(status:String){val (bg,fg)=when(status){"SYNCED","ACCEPTED","ACTIVE","DELIVERED","PAID","LIVE"->DukaanColors.LightGreen to DukaanColors.GreenDark;"PENDING","PENDING_APPROVAL"->DukaanColors.LightAmber to Color(0xFF8A6D00);"REJECTED","REJECTED_PERMANENT","FAILED"->DukaanColors.LightRed to Color(0xFF991B1B);"LOW STOCK"->Color(0xFFFFEDD5) to Color(0xFF9A3412);else->DukaanColors.Slate200 to DukaanColors.Gray};Box(Modifier.background(bg,RoundedCornerShape(999.dp)).padding(horizontal=10.dp,vertical=3.dp)){Text(status,fontSize=11.sp,fontWeight=FontWeight.Bold,color=fg)}}

@Composable fun PinField(label:String,value:String,on:(String)->Unit){var visible by remember{mutableStateOf(false)};OutlinedTextField(value,on,label={Text(label)},modifier=Modifier.fillMaxWidth(),shape=RoundedCornerShape(14.dp),colors=OutlinedTextFieldDefaults.colors(focusedContainerColor=Color.White,unfocusedContainerColor=Color.White,focusedTextColor=DukaanColors.Slate900,unfocusedTextColor=DukaanColors.Slate900,focusedBorderColor=DukaanColors.Navy,unfocusedBorderColor=DukaanColors.Slate400,cursorColor=DukaanColors.Navy,focusedLabelColor=DukaanColors.Navy,unfocusedLabelColor=DukaanColors.Slate600),keyboardOptions=KeyboardOptions(keyboardType=KeyboardType.NumberPassword),singleLine=true,visualTransformation=if(visible) VisualTransformation.None else PasswordVisualTransformation(),trailingIcon={TextButton({visible=!visible}){Text(if(visible)"Hide" else "Show",color=DukaanColors.Navy)}})}
@Composable fun Field(label:String,value:String,on:(String)->Unit,type:KeyboardType=KeyboardType.Text,secret:Boolean=false){OutlinedTextField(value,on,label={Text(label)},modifier=Modifier.fillMaxWidth(),shape=RoundedCornerShape(14.dp),colors=OutlinedTextFieldDefaults.colors(focusedContainerColor=Color.White,unfocusedContainerColor=Color.White,focusedTextColor=DukaanColors.Slate900,unfocusedTextColor=DukaanColors.Slate900,focusedBorderColor=DukaanColors.Navy,unfocusedBorderColor=DukaanColors.Slate400,cursorColor=DukaanColors.Navy,focusedLabelColor=DukaanColors.Navy,unfocusedLabelColor=DukaanColors.Slate600),keyboardOptions=KeyboardOptions(keyboardType=type),visualTransformation=if(secret)PasswordVisualTransformation() else VisualTransformation.None,singleLine=true)}
/** Premium auth: upar deep-indigo hero (brand + role tabs), neeche white sheet (fields). Ek hi card, zero confusion. */
@Composable fun PremiumWelcome(onLogin:(String,String,String)->Unit,showRegister:(String)->Unit){
 var tab by remember{mutableStateOf("CUSTOMER")}
 Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).navigationBarsPadding()){
  Column(Modifier.fillMaxWidth().background(DukaanColors.HeroGradient).statusBarsPadding().padding(horizontal=20.dp,vertical=32.dp),horizontalAlignment=Alignment.CenterHorizontally){
   Text("🛍️",fontSize=40.sp);Spacer(Modifier.height(6.dp));Text(stringResource(R.string.app_name),style=MaterialTheme.typography.headlineMedium,fontWeight=FontWeight.ExtraBold,color=Color.White);Spacer(Modifier.height(4.dp));Text(stringResource(R.string.tagline),fontSize=13.sp,color=Color(0xFFC7D2FE))
   Spacer(Modifier.height(20.dp));RoleTabs(tab){tab=it}
  }
  Column(Modifier.fillMaxWidth().padding(20.dp)){
   PremiumLogin(tab,onLogin,{showRegister(tab)})
  }
 }}
@Composable fun RoleTabs(selected:String,on:(String)->Unit){
 Surface(shape=RoundedCornerShape(14.dp),color=Color(0x33FFFFFF)){Row(Modifier.fillMaxWidth().padding(4.dp),horizontalArrangement=Arrangement.spacedBy(4.dp)){
  Box(Modifier.weight(1f).clip(RoundedCornerShape(11.dp)).background(if(selected=="CUSTOMER")Color.White else Color.Transparent).clickable{on("CUSTOMER")}.padding(vertical=10.dp),contentAlignment=Alignment.Center){Text("🛒 Customer",fontWeight=FontWeight.Bold,fontSize=13.sp,color=if(selected=="CUSTOMER")DukaanColors.Dark else Color(0xFFC7D2FE))}
  Box(Modifier.weight(1f).clip(RoundedCornerShape(11.dp)).background(if(selected=="SHOPKEEPER")Color.White else Color.Transparent).clickable{on("SHOPKEEPER")}.padding(vertical=10.dp),contentAlignment=Alignment.Center){Text("🏪 Shopkeeper",fontWeight=FontWeight.Bold,fontSize=13.sp,color=if(selected=="SHOPKEEPER")DukaanColors.Dark else Color(0xFFC7D2FE))}
 }}}
@Composable fun PremiumLogin(role:String,go:(String,String,String)->Unit,showRegister:()->Unit){var p by remember{mutableStateOf("+91")};var pin by remember{mutableStateOf("")};val valid=p.filter{it.isDigit()}.length>=10&&(pin.length==4||pin.length==6)
 Card(Modifier.fillMaxWidth(),shape=RoundedCornerShape(20.dp),colors=CardDefaults.cardColors(containerColor=Color.White),elevation=CardDefaults.cardElevation(4.dp)){
  Column(Modifier.fillMaxWidth().padding(18.dp)){
   Text(if(role=="SHOPKEEPER")"🏪 Shopkeeper Login" else "🛒 Customer Login",style=MaterialTheme.typography.titleMedium,fontWeight=FontWeight.Bold,color=DukaanColors.Slate900);Spacer(Modifier.height(4.dp));Text(if(role=="SHOPKEEPER")"Apni dukaan ka poora hisaab ek jagah" else "Pados ki dukaanein, orders aur udhaar",fontSize=12.sp,color=DukaanColors.Slate500);Spacer(Modifier.height(12.dp))
   Field(stringResource(R.string.mobile_number),p,{p=it},KeyboardType.Phone);Spacer(Modifier.height(8.dp));PinField(stringResource(R.string.pin),pin,{pin=it});Spacer(Modifier.height(14.dp))
   GradientButton("Login karein",valid){go(p,pin,role)};Spacer(Modifier.height(4.dp))
   TextButton(showRegister,Modifier.fillMaxWidth()){Text(if(role=="SHOPKEEPER")"Nayi dukaan register karein →" else "Naya customer account banayein →",color=DukaanColors.Blue,fontSize=13.sp,fontWeight=FontWeight.SemiBold)}}}}

@Composable fun Register(initialRole:String?,go:(String,String,String,String)->Unit,showLogin:()->Unit){var p by remember{mutableStateOf("+91")};var pin by remember{mutableStateOf("")};var confirm by remember{mutableStateOf("")};var role by remember{mutableStateOf(initialRole?:"CUSTOMER")};val valid=p.filter{it.isDigit()}.length>=10&&(pin.length==4||pin.length==6)&&pin==confirm
 Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).imePadding().navigationBarsPadding(),verticalArrangement=Arrangement.Center){
  Text(if(role=="SHOPKEEPER")"🏪" else "🛒",fontSize=48.sp);ScreenTitle(stringResource(R.string.create_account),if(role=="SHOPKEEPER")"Aapki dukaan ke liye naya account" else "Samaan khareedne ke liye naya account");Spacer(Modifier.height(16.dp))
  SectionCard{Field(stringResource(R.string.mobile_number),p,{p=it},KeyboardType.Phone);PinField(stringResource(R.string.create_pin),pin,{pin=it});PinField(stringResource(R.string.confirm_pin),confirm,{confirm=it})}
  Spacer(Modifier.height(12.dp));RolePicker(role){role=it};Spacer(Modifier.height(12.dp))
  GradientButton(stringResource(R.string.register),valid){go(p,pin,confirm,role)};TextButton(showLogin,Modifier.fillMaxWidth()){Text(stringResource(R.string.already_registered),color=DukaanColors.Blue)}}}
@Composable fun RolePicker(role:String,on:(String)->Unit){Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(10.dp)){RoleCard("🛒",stringResource(R.string.customer),role=="CUSTOMER",{on("CUSTOMER")},Modifier.weight(1f));RoleCard("🏪",stringResource(R.string.shopkeeper),role=="SHOPKEEPER",{on("SHOPKEEPER")},Modifier.weight(1f))}}
@Composable fun RoleCard(icon:String,label:String,selected:Boolean,on:()->Unit,modifier:Modifier=Modifier){Card(onClick=on,modifier=modifier,shape=RoundedCornerShape(16.dp),colors=CardDefaults.cardColors(containerColor=if(selected)DukaanColors.Navy else Color.White),elevation=CardDefaults.cardElevation(if(selected)4.dp else 1.dp)){Column(Modifier.fillMaxWidth().padding(vertical=14.dp),horizontalAlignment=Alignment.CenterHorizontally){Text(icon,fontSize=26.sp);Text(label,color=if(selected)Color.White else DukaanColors.Slate700,fontWeight=FontWeight.Bold)}}}
/** Bada, saaf role card — icon + title + description. Welcome screen par use hota hai. */
@Composable fun BigRoleCard(icon:androidx.compose.ui.graphics.vector.ImageVector,title:String,desc:String,selected:Boolean,on:()->Unit,modifier:Modifier=Modifier){
 Card(onClick=on,modifier=modifier,shape=RoundedCornerShape(16.dp),colors=CardDefaults.cardColors(containerColor=if(selected)DukaanColors.LightBlue else Color.White),elevation=CardDefaults.cardElevation(if(selected)3.dp else 1.dp)){
  Row(Modifier.fillMaxWidth().padding(14.dp),verticalAlignment=Alignment.CenterVertically){
   Box(Modifier.size(46.dp).clip(RoundedCornerShape(12.dp)).background(if(selected)DukaanColors.Navy else DukaanColors.Slate100),contentAlignment=Alignment.Center){Icon(icon,null,tint=if(selected)Color.White else DukaanColors.Slate600,modifier=Modifier.size(24.dp))}
   Spacer(Modifier.width(12.dp));Column(Modifier.weight(1f)){Text(title,fontWeight=FontWeight.Bold,fontSize=15.sp,color=if(selected)DukaanColors.Navy else DukaanColors.Slate900);Text(desc,fontSize=11.sp,color=DukaanColors.Slate500,lineHeight=14.sp)}
   Icon(Icons.Default.ArrowForwardIos,null,tint=if(selected)DukaanColors.Navy else DukaanColors.Slate400,modifier=Modifier.size(14.dp))
  }}}

@Composable fun Profile(role:String?,go:(String,Boolean,Boolean,Boolean)->Unit){var n by remember{mutableStateOf("")};var hi by remember{mutableStateOf(false)}
 // Signup ka role FINAL hai — checkboxes se badal ke account ka role nahi bana ja sakta (customer≠shopkeeper).
 val c=role!="SHOPKEEPER";val k=role=="SHOPKEEPER"
 Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).imePadding().statusBarsPadding()){Spacer(Modifier.height(24.dp));ScreenTitle("👋 ${stringResource(R.string.namaste)}!",stringResource(R.string.name));Spacer(Modifier.height(16.dp))
  SectionCard{Field(stringResource(R.string.name),n,{n=it});Row(verticalAlignment=Alignment.CenterVertically){Text(if(k)"🏪 "+stringResource(R.string.shopkeeper) else "🛒 "+stringResource(R.string.customer),fontWeight=FontWeight.Bold,color=DukaanColors.Navy)};Row(verticalAlignment=Alignment.CenterVertically){Checkbox(hi,{hi=it});Text("हिन्दी")}
  };Spacer(Modifier.height(16.dp));GradientButton(stringResource(R.string.continue_label),n.isNotBlank()){go(n,c,k,hi)}}}

@Composable fun ShopForm(go:(List<String>)->Unit){val labels=listOf(stringResource(R.string.shop_name),stringResource(R.string.owner_name),stringResource(R.string.address),stringResource(R.string.locality),stringResource(R.string.city),stringResource(R.string.pincode),stringResource(R.string.category),stringResource(R.string.latitude),stringResource(R.string.longitude));val v=remember{labels.mapIndexed{i,_->mutableStateOf(when(i){6->"Kirana";7->"28.6139";8->"77.2090";else->""})}}
 val valid=v[0].value.isNotBlank()&&v[1].value.isNotBlank()&&v[2].value.isNotBlank()&&v[4].value.isNotBlank()&&v[5].value.matches(Regex("[1-9]\\d{5}"))&&v[6].value.isNotBlank()&&(v[7].value.toDoubleOrNull()!=null&&v[7].value.toDoubleOrNull()!! in -90.0..90.0)&&(v[8].value.toDoubleOrNull()!=null&&v[8].value.toDoubleOrNull()!! in -180.0..180.0)
  Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).imePadding().statusBarsPadding()){Spacer(Modifier.height(24.dp));ScreenTitle("🏪 "+stringResource(R.string.shop_name),stringResource(R.string.create_shop_trial));Spacer(Modifier.height(12.dp));SectionCard{labels.forEachIndexed{i,l->Field(l,v[i].value,{v[i].value=it},when(i){5->KeyboardType.Number;7,8->KeyboardType.Decimal;else->KeyboardType.Text})}};Spacer(Modifier.height(16.dp));GradientButton(stringResource(R.string.create_shop_trial),valid){go(v.map{it.value})}}}

/** Maujooda dukaan edit — naam, pata, phone, description. Version-conflict ho to server batata hai (pehle refresh karo). */
@Composable fun ShopEditForm(shop:LocalShop?,save:(String,String,String,String,String,String,String,String)->Unit){val current=shop?:return
 var name by remember(current){mutableStateOf(current.name)};var owner by remember(current){mutableStateOf(current.ownerName)};var address by remember(current){mutableStateOf(current.addressLine)};var locality by remember(current){mutableStateOf(current.locality)};var city by remember(current){mutableStateOf(current.city)};var pincode by remember(current){mutableStateOf(current.pincode)};var phone by remember(current){mutableStateOf(current.phone.orEmpty())};var desc by remember(current){mutableStateOf(current.description.orEmpty())}
 val valid=name.isNotBlank()&&owner.isNotBlank()&&address.isNotBlank()&&locality.isNotBlank()&&city.isNotBlank()&&pincode.matches(Regex("[1-9]\\d{5}"))
 Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).imePadding().statusBarsPadding()){Spacer(Modifier.height(24.dp));ScreenTitle("🏪 "+stringResource(R.string.edit_shop),current.name);Spacer(Modifier.height(12.dp))
  SectionCard{Field(stringResource(R.string.shop_name),name,{name=it});Field(stringResource(R.string.owner_name),owner,{owner=it});Field(stringResource(R.string.address),address,{address=it});Field(stringResource(R.string.locality),locality,{locality=it});Field(stringResource(R.string.city),city,{city=it});Field(stringResource(R.string.pincode),pincode,{pincode=it},KeyboardType.Number);Field(stringResource(R.string.mobile_number),phone,{phone=it},KeyboardType.Phone);Field(stringResource(R.string.description_optional),desc,{desc=it})}
  Spacer(Modifier.height(16.dp));GradientButton("✓ "+stringResource(R.string.save),valid){save(name,owner,address,locality,city,pincode,phone,desc)}}}

/** PIN change — purana PIN verify, naya 4/6-digit PIN. Kamyabi par doosre devices ke sessions revoke (server). */
@Composable fun ChangePinScreen(go:(String,String,String)->Unit){var current by remember{mutableStateOf("")};var next by remember{mutableStateOf("")};var confirm by remember{mutableStateOf("")}
 val valid=current.length>=4&&next.length>=4&&confirm.length>=4
 Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).imePadding().statusBarsPadding()){Spacer(Modifier.height(24.dp));ScreenTitle("🔑 "+stringResource(R.string.change_pin),stringResource(R.string.change_pin_hint));Spacer(Modifier.height(12.dp))
  SectionCard{PinField(stringResource(R.string.current_pin),current,{current=it});PinField(stringResource(R.string.new_pin),next,{next=it});PinField(stringResource(R.string.confirm_pin),confirm,{confirm=it})}
  Spacer(Modifier.height(16.dp));GradientButton("✓ "+stringResource(R.string.change_pin),valid){go(current,next,confirm)}}}

@OptIn(ExperimentalMaterial3Api::class)
@Composable fun Home(s:UiState,products:(String)->Unit,dashboard:(String)->Unit,quickSale:(String)->Unit,market:()->Unit,orders:()->Unit,shopOrders:()->Unit,createShop:()->Unit,logout:()->Unit){
 val isShopkeeper=s.profile?.shopkeeper==true
 LazyColumn(Modifier.fillMaxSize(),verticalArrangement=Arrangement.spacedBy(10.dp)){item{Spacer(Modifier.height(8.dp));Text("👋 ${stringResource(R.string.namaste)} ${s.profile?.name.orEmpty()}",style=MaterialTheme.typography.headlineMedium,fontWeight=FontWeight.ExtraBold,color=DukaanColors.Navy);Text(if(isShopkeeper)"Aapki dukaan, aapka business" else "Pados ki dukaanein, ab online",color=DukaanColors.Slate500)}
  if(!isShopkeeper){
   item{Spacer(Modifier.height(4.dp));GradientButton("🛍️ "+stringResource(R.string.nearby_shops),true,market)}
   item{GradientButton("📦 "+stringResource(R.string.my_orders),true,orders)}
    item{SectionCard{Row(verticalAlignment=Alignment.CenterVertically){Text("📖",fontSize=20.sp);Spacer(Modifier.width(8.dp));Text("Aapka Udhaar",fontWeight=FontWeight.Bold);Spacer(Modifier.weight(1f));if(s.myCreditShops.isNotEmpty())StatusBadge(if(s.myCreditShops.sumOf{it.balancePaise}>0)"BAKI HAI" else "CLEAR")}
     if(s.myCreditShops.isEmpty())Text("Koi udhaar nahi — sab clear ✓",color=Color(0xFF5B6B62))
     s.myCreditShops.forEach{c->Row(Modifier.fillMaxWidth().padding(vertical=4.dp),verticalAlignment=Alignment.CenterVertically){Column(Modifier.weight(1f)){Text(c.shopName,fontWeight=FontWeight.SemiBold);Text(if(c.balancePaise>0)"Dukaan ko dena hai" else "Sab clear",fontSize=11.sp,color=Color(0xFF5B6B62))};Text(ProductRules.rupees(c.balancePaise),fontWeight=FontWeight.ExtraBold,color=if(c.balancePaise>0)BrandRed else BrandGreenDark)}}
     if(s.myCreditEntries.isNotEmpty()){Divider(color=Color(0xFFE4EFE8));Text("Aapne kya liya (recent):",fontSize=11.sp,color=Color(0xFF5B6B62));s.myCreditEntries.take(5).forEach{e->Row(Modifier.fillMaxWidth().padding(vertical=2.dp),verticalAlignment=Alignment.CenterVertically){Text(if(e.type=="CREDIT_SALE")"🛒 Samaan liya" else if(e.type=="PAYMENT")"✅ Payment kiya" else "⚖️ Adjust",fontSize=12.sp,modifier=Modifier.weight(1f));Text((if(e.type=="PAYMENT"||e.type=="ADJUSTMENT_DEBIT")"− " else "+ ")+ProductRules.rupees(e.amountPaise),fontSize=12.sp,fontWeight=FontWeight.SemiBold,color=if(e.type=="PAYMENT"||e.type=="ADJUSTMENT_DEBIT")BrandGreenDark else BrandRed)}}}}}
  } else {
    if(s.shops.isEmpty()){item{Card(Modifier.fillMaxWidth(),shape=RoundedCornerShape(16.dp),colors=CardDefaults.cardColors(containerColor=DukaanColors.LightYellow)){Column(Modifier.padding(18.dp)){Text("🏪 Aapki dukaan abhi nahi hai",fontWeight=FontWeight.Bold,color=DukaanColors.Dark);Text("Ek account se ek hi dukaan ban sakti hai — neeche se apni pehli dukaan banayein.",color=DukaanColors.Slate600,fontSize=13.sp)}}};item{GradientButton("🏪 "+stringResource(R.string.create_shop_trial),true,createShop)}}
    else item{Text("Apni dukaanein",style=MaterialTheme.typography.titleMedium,fontWeight=FontWeight.Bold,color=DukaanColors.Dark)}
    s.shops.forEach{shopRow->item{Card(onClick={dashboard(shopRow.id)},modifier=Modifier.fillMaxWidth(),shape=RoundedCornerShape(16.dp),colors=CardDefaults.cardColors(containerColor=Color.White),elevation=CardDefaults.cardElevation(1.dp)){Row(Modifier.fillMaxWidth().padding(12.dp),verticalAlignment=Alignment.CenterVertically){Box(Modifier.size(42.dp).clip(RoundedCornerShape(12.dp)).background(DukaanColors.LightYellow),contentAlignment=Alignment.Center){Text("🏪",fontSize=20.sp)};Spacer(Modifier.width(10.dp));Column(Modifier.weight(1f)){Text(shopRow.name,fontWeight=FontWeight.Bold,fontSize=15.sp,color=DukaanColors.Dark,maxLines=1,overflow=androidx.compose.ui.text.style.TextOverflow.Ellipsis);Text("${shopRow.locality}, ${shopRow.city}",fontSize=12.sp,color=DukaanColors.Slate600,maxLines=1,overflow=androidx.compose.ui.text.style.TextOverflow.Ellipsis)};StatusBadge(if(shopRow.isPublished)"LIVE" else "DRAFT")}}}}
  }}}

@Composable fun ProductList(s:UiState,add:()->Unit,edit:(ProductRow)->Unit,archive:(ProductRow)->Unit,inventory:(ProductRow)->Unit,quickSale:()->Unit,lowStock:()->Unit,customers:()->Unit){var query by remember{mutableStateOf("")};var confirmArchive by remember{mutableStateOf<ProductRow?>(null)}
 Column(Modifier.fillMaxSize()){Spacer(Modifier.height(4.dp));Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(8.dp)){Button(quickSale,Modifier.weight(1.4f).height(48.dp),colors=ButtonDefaults.buttonColors(containerColor=BrandGreen),shape=RoundedCornerShape(14.dp)){Text("⚡ "+stringResource(R.string.quick_sale),fontWeight=FontWeight.Bold)};OutlinedButton(lowStock,Modifier.weight(1f).height(48.dp),shape=RoundedCornerShape(14.dp)){Text("📉 "+stringResource(R.string.low_stock_title))}}
  Spacer(Modifier.height(8.dp));Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(8.dp)){OutlinedButton(customers,Modifier.weight(1f).height(48.dp),shape=RoundedCornerShape(14.dp)){Text("👥 "+stringResource(R.string.customers))};Button(add,Modifier.weight(1f).height(48.dp),colors=ButtonDefaults.buttonColors(containerColor=DukaanColors.Navy),shape=RoundedCornerShape(14.dp)){Text("+ "+stringResource(R.string.add_product))}}
  Spacer(Modifier.height(8.dp));Field(stringResource(R.string.search_products),query,{query=it})
  val shown=s.products.filter{query.isBlank()||it.product.name.contains(query,true)||it.product.brand.orEmpty().contains(query,true)||it.product.barcode.orEmpty().contains(query,true)}
  if(shown.isEmpty())Column(Modifier.fillMaxWidth().padding(top=40.dp),horizontalAlignment=Alignment.CenterHorizontally){Text("📦",fontSize=44.sp);Text(stringResource(R.string.empty_products),color=Color(0xFF5B6B62),textAlign=androidx.compose.ui.text.style.TextAlign.Center)}
  confirmArchive?.let{row->AlertDialog(onDismissRequest={confirmArchive=null},title={Text(stringResource(R.string.archive))},text={Text(stringResource(R.string.archive_confirm)+" (${row.product.name})")},confirmButton={TextButton({archive(row);confirmArchive=null}){Text(stringResource(R.string.archive),color=BrandRed)}},dismissButton={TextButton({confirmArchive=null}){Text(stringResource(R.string.cancel))}})}
  LazyColumn(verticalArrangement=Arrangement.spacedBy(8.dp),contentPadding=PaddingValues(bottom=16.dp)){items(shown,key={it.shopProduct.id}){row->Card(Modifier.fillMaxWidth(),shape=RoundedCornerShape(16.dp),colors=CardDefaults.cardColors(containerColor=if(row.lowStock)Color(0xFFFFF7ED) else Color.White)){Column(Modifier.padding(12.dp)){Row(verticalAlignment=Alignment.CenterVertically){Text(row.product.name,style=MaterialTheme.typography.titleMedium,fontWeight=FontWeight.Bold,modifier=Modifier.weight(1f));if(row.lowStock)StatusBadge("LOW STOCK") else StatusBadge(row.shopProduct.syncState)};Text("${ProductRules.rupees(row.shopProduct.sellingPricePaise)} • ${ProductRules.quantity(row.shopProduct.stockQuantityMilli)} ${row.product.unit}",color=BrandGreenDark,fontWeight=FontWeight.SemiBold);if(!row.shopProduct.isAvailable)Text(stringResource(R.string.unavailable),color=BrandRed);if(row.shopProduct.syncState!="SYNCED")Text(stringResource(R.string.waiting_sync)+" — net aane par edit aur stock unlock ho jayega",color=Color(0xFF92400E),fontSize=12.sp);if(row.shopProduct.syncState=="SYNCED")Row{TextButton({inventory(row)}){Text(stringResource(R.string.stock))};TextButton({edit(row)}){Text(stringResource(R.string.edit))};TextButton({confirmArchive=row}){Text(stringResource(R.string.archive),color=BrandRed)}}}}}}}}
@Composable fun LowStockScreen(s:UiState,add:(ProductRow)->Unit,openPurchase:()->Unit){val low=s.products.filter{it.lowStock}.sortedBy{it.shopProduct.stockQuantityMilli-it.shopProduct.minimumStockMilli}
 val pendingCount=s.purchaseItems.count{!it.isPurchased}
 OutlinedButton(openPurchase,Modifier.fillMaxWidth().height(48.dp),shape=RoundedCornerShape(14.dp)){Text("📋 "+stringResource(R.string.purchase_list)+(if(pendingCount>0)" ($pendingCount)" else ""),fontWeight=FontWeight.Bold)}
 Spacer(Modifier.height(8.dp))
 s.scanMessage?.let{Text(it,color=BrandGreenDark,fontWeight=FontWeight.SemiBold,fontSize=13.sp,modifier=Modifier.padding(bottom=6.dp))}
 if(low.isEmpty())Column(Modifier.fillMaxWidth().padding(top=60.dp),horizontalAlignment=Alignment.CenterHorizontally){Text("✅",fontSize=48.sp);Text(stringResource(R.string.no_low_stock),color=Color(0xFF5B6B62))};LazyColumn(verticalArrangement=Arrangement.spacedBy(8.dp),contentPadding=PaddingValues(bottom=16.dp)){items(low,key={it.shopProduct.id}){row->Card(Modifier.fillMaxWidth(),shape=RoundedCornerShape(16.dp),colors=CardDefaults.cardColors(containerColor=Color(0xFFFFF7ED))){Column(Modifier.padding(12.dp)){Text(row.product.name,fontWeight=FontWeight.Bold);Text("${stringResource(R.string.current)} ${ProductRules.quantity(row.shopProduct.stockQuantityMilli)} • ${stringResource(R.string.minimum)} ${ProductRules.quantity(row.shopProduct.minimumStockMilli)}",color=Color(0xFF92400E));Button({add(row)},colors=ButtonDefaults.buttonColors(containerColor=BrandAmber)){Text(stringResource(R.string.add_to_purchase_list))}}}}}}
/** Purchase List — kharid-suchi: tick karke kharida mark karo, swipe nahi seedha 🗑 se hatayo. Offline tick PENDING rehta hai, net par sync. */
@Composable fun PurchaseListScreen(s:UiState,toggle:(PurchaseListEntity)->Unit,remove:(PurchaseListEntity)->Unit,backToLowStock:()->Unit){
 var confirmRemove by remember{mutableStateOf<PurchaseListEntity?>(null)}
 val pending=s.purchaseItems.filter{!it.isPurchased};val done=s.purchaseItems.filter{it.isPurchased}
 Column(Modifier.fillMaxSize()){
  if(s.purchaseItems.isEmpty())Column(Modifier.fillMaxWidth().padding(top=60.dp),horizontalAlignment=Alignment.CenterHorizontally){Text("📋",fontSize=48.sp);Spacer(Modifier.height(8.dp));Text(stringResource(R.string.purchase_empty),color=Color(0xFF5B6B62),textAlign=androidx.compose.ui.text.style.TextAlign.Center);Spacer(Modifier.height(12.dp));OutlinedButton(backToLowStock,shape=RoundedCornerShape(14.dp)){Text("📉 "+stringResource(R.string.low_stock_title))}}
  else LazyColumn(verticalArrangement=Arrangement.spacedBy(8.dp),contentPadding=PaddingValues(bottom=16.dp)){
   if(pending.isNotEmpty()){item{Text("🛒 "+stringResource(R.string.to_buy)+" (${pending.size})",fontWeight=FontWeight.Bold,color=DukaanColors.Dark)};items(pending,key={it.id}){item->PurchaseRow(item,false,{toggle(item)},{confirmRemove=item})}}
   if(done.isNotEmpty()){item{Spacer(Modifier.height(4.dp));Text("✅ "+stringResource(R.string.bought)+" (${done.size})",fontWeight=FontWeight.Bold,color=DukaanColors.Slate600)};items(done,key={it.id}){item->PurchaseRow(item,true,{toggle(item)},{confirmRemove=item})}}
  }
 }
 confirmRemove?.let{item->AlertDialog(onDismissRequest={confirmRemove=null},title={Text(stringResource(R.string.remove_item))},text={Text(item.productName)},confirmButton={TextButton({remove(item);confirmRemove=null}){Text(stringResource(R.string.remove_label),color=BrandRed)}},dismissButton={TextButton({confirmRemove=null}){Text(stringResource(R.string.cancel))}})}
}
@Composable private fun PurchaseRow(item:PurchaseListEntity,purchased:Boolean,onToggle:()->Unit,onRemove:()->Unit){
 Card(Modifier.fillMaxWidth(),shape=RoundedCornerShape(16.dp),colors=CardDefaults.cardColors(containerColor=if(purchased)DukaanColors.Slate100 else Color.White)){
  Row(Modifier.fillMaxWidth().padding(12.dp),verticalAlignment=Alignment.CenterVertically){
   Checkbox(checked=purchased,onCheckedChange={onToggle()})
   Column(Modifier.weight(1f)){Text(item.productName,fontWeight=if(purchased)FontWeight.Normal else FontWeight.Bold,color=if(purchased)DukaanColors.Slate500 else DukaanColors.Dark,textDecoration=if(purchased)androidx.compose.ui.text.style.TextDecoration.LineThrough else null,maxLines=2,overflow=androidx.compose.ui.text.style.TextOverflow.Ellipsis);Text(ProductRules.quantity(item.quantityMilli)+(if(item.syncState!="SYNCED")" • ⟳" else ""),fontSize=12.sp,color=DukaanColors.Slate600)}
   IconButton(onRemove){Text("🗑",fontSize=18.sp)}
  }}}
@Composable fun InventoryScreen(s:UiState,confirm:(String,String,String)->Unit){val row=s.inventoryProduct?:return;var quantity by remember{mutableStateOf("")};var type by remember{mutableStateOf("PURCHASE")};var note by remember{mutableStateOf("")}
 Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).imePadding()){Text("${row.product.name}",style=MaterialTheme.typography.titleLarge,fontWeight=FontWeight.Bold);Text("${stringResource(R.string.current)}: ${ProductRules.quantity(row.shopProduct.stockQuantityMilli)} ${row.product.unit}",color=BrandGreenDark,fontWeight=FontWeight.SemiBold);Spacer(Modifier.height(10.dp));SectionCard{Field(stringResource(R.string.quantity),quantity,{quantity=it},KeyboardType.Decimal);val reasons=listOf("PURCHASE","SALE","DAMAGE","EXPIRED","LOSS","ADJUSTMENT","RETURN_OUT");FlowRow(horizontalArrangement=Arrangement.spacedBy(6.dp)){reasons.forEach{r->FilterChip(selected=type==r,onClick={type=r},label={Text(r,fontSize=12.sp)})}};Field(stringResource(R.string.notes_optional),note,{note=it})}
  Spacer(Modifier.height(12.dp));GradientButton(if(type=="PURCHASE")"⬆️ "+stringResource(R.string.stock_in) else "⬇️ "+stringResource(R.string.stock_out),(ProductRules.quantityToMilli(quantity)?:0)>0){confirm(quantity,type,note)};Spacer(Modifier.height(16.dp));Text(stringResource(R.string.history),style=MaterialTheme.typography.titleMedium,fontWeight=FontWeight.Bold);if(s.inventoryHistory.isEmpty())Text("—",color=Color(0xFF5B6B62));SectionCard{s.inventoryHistory.forEach{Text("${it.type}: ${if(it.quantityDeltaMilli>0)"+" else ""}${ProductRules.quantity(it.quantityDeltaMilli)} → ${ProductRules.quantity(it.stockAfterMilli)}")}}}}

/** Success — ✓ amount, payment, customer; [New Sale] seedha POS par wapas. */
@Composable fun SaleSuccessScreen(s:UiState,newSale:()->Unit,viewReceipt:()->Unit){val x=s.saleSuccess?:return
 Column(Modifier.fillMaxSize().padding(horizontal=24.dp),horizontalAlignment=Alignment.CenterHorizontally,verticalArrangement=Arrangement.Center){
  Box(Modifier.size(84.dp).clip(RoundedCornerShape(999.dp)).background(BrandGreen),contentAlignment=Alignment.Center){Text("✓",fontSize=44.sp,color=Color.White,fontWeight=FontWeight.ExtraBold)}
  Spacer(Modifier.height(16.dp));Text(stringResource(R.string.sale_completed),style=MaterialTheme.typography.headlineSmall,fontWeight=FontWeight.ExtraBold,color=DukaanColors.Dark)
  Spacer(Modifier.height(4.dp));Text(ProductRules.rupees(x.totalPaise),style=MaterialTheme.typography.displaySmall,fontWeight=FontWeight.ExtraBold,color=DukaanColors.Navy)
  if(x.discountPaise>0)Text(stringResource(R.string.discount)+": -"+ProductRules.rupees(x.discountPaise),color=BrandGreenDark,fontWeight=FontWeight.SemiBold)
  Spacer(Modifier.height(10.dp));Text(stringResource(R.string.payment_method)+": "+x.methodLabel,color=DukaanColors.Slate600);Text(stringResource(R.string.customer)+": "+x.customerLabel,color=DukaanColors.Slate600)
   if(x.duePaise>0){Spacer(Modifier.height(6.dp));Text("💰 "+stringResource(R.string.due)+": "+ProductRules.rupees(x.duePaise),color=BrandRed,fontWeight=FontWeight.Bold)}
   if(x.profitPaise>0){Spacer(Modifier.height(6.dp));Text("📈 "+stringResource(R.string.voice_sale_profit)+": "+ProductRules.rupees(x.profitPaise),color=BrandGreenDark,fontWeight=FontWeight.Bold)}
  Spacer(Modifier.height(28.dp));GradientButton("🛒  "+stringResource(R.string.new_sale),true,newSale);Spacer(Modifier.height(8.dp));ActionButton("🧾 "+stringResource(R.string.view_receipt),viewReceipt)}}
/** Saaf digital receipt — shop, items, total, payment, customer; Share support. */
@Composable fun ReceiptScreen(s:UiState,share:()->Unit){val rD=s.receipt?:return
 Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal=16.dp)){
  Spacer(Modifier.height(6.dp));Card(Modifier.fillMaxWidth(),shape=RoundedCornerShape(16.dp),colors=CardDefaults.cardColors(containerColor=Color.White)){Column(Modifier.fillMaxWidth().padding(18.dp),horizontalAlignment=Alignment.CenterHorizontally){
   Text(rD.shopName,style=MaterialTheme.typography.titleLarge,fontWeight=FontWeight.ExtraBold,color=DukaanColors.Dark);rD.shopLine?.let{Text(it,fontSize=11.sp,color=DukaanColors.Slate500,textAlign=androidx.compose.ui.text.style.TextAlign.Center)}
   Text(java.text.SimpleDateFormat("dd MMM yyyy, hh:mm a",java.util.Locale.getDefault()).format(java.util.Date(rD.createdAt)),fontSize=12.sp,color=DukaanColors.Slate500)
   Text("Invoice #"+rD.id.takeLast(8).uppercase(),fontSize=12.sp,fontWeight=FontWeight.SemiBold,color=DukaanColors.Slate600)
   Divider(Modifier.padding(vertical=10.dp))
   Column(Modifier.fillMaxWidth()){rD.items.forEach{Row(Modifier.fillMaxWidth().padding(vertical=3.dp)){Column(Modifier.weight(1f)){Text(it.name,fontWeight=FontWeight.SemiBold,fontSize=13.sp);Text("× "+ProductRules.quantity(it.quantityMilli)+" @ "+ProductRules.rupees(it.unitPricePaise),fontSize=11.sp,color=DukaanColors.Slate500)};Text(ProductRules.rupees(it.lineTotalPaise),fontWeight=FontWeight.Bold,fontSize=13.sp)}}}
   Divider(Modifier.padding(vertical=10.dp))
   Row(Modifier.fillMaxWidth()){Text(stringResource(R.string.subtotal),color=DukaanColors.Slate600,modifier=Modifier.weight(1f));Text(ProductRules.rupees(rD.subtotalPaise))}
   if(rD.discountPaise>0)Row(Modifier.fillMaxWidth()){Text(stringResource(R.string.discount),color=BrandGreenDark,modifier=Modifier.weight(1f));Text("-"+ProductRules.rupees(rD.discountPaise),color=BrandGreenDark)}
   Row(Modifier.fillMaxWidth()){Text(stringResource(R.string.total),style=MaterialTheme.typography.titleMedium,fontWeight=FontWeight.ExtraBold,modifier=Modifier.weight(1f));Text(ProductRules.rupees(rD.totalPaise),style=MaterialTheme.typography.titleMedium,fontWeight=FontWeight.ExtraBold)}
   Spacer(Modifier.height(6.dp));Row(Modifier.fillMaxWidth()){Text(stringResource(R.string.payment_method),color=DukaanColors.Slate600,modifier=Modifier.weight(1f));Text(rD.methodLabel,fontWeight=FontWeight.Bold)}
   Row(Modifier.fillMaxWidth()){Text(stringResource(R.string.customer),color=DukaanColors.Slate600,modifier=Modifier.weight(1f));Text(rD.customerLabel,fontWeight=FontWeight.Bold)}
   if(rD.duePaise>0){Row(Modifier.fillMaxWidth()){Text(stringResource(R.string.paid),color=DukaanColors.Slate600,modifier=Modifier.weight(1f));Text("₹0.00")};Row(Modifier.fillMaxWidth()){Text(stringResource(R.string.due),color=BrandRed,fontWeight=FontWeight.Bold,modifier=Modifier.weight(1f));Text(ProductRules.rupees(rD.duePaise),color=BrandRed,fontWeight=FontWeight.Bold)}}
   if(rD.pendingSync){Spacer(Modifier.height(6.dp));Text("⟳ "+stringResource(R.string.pending_server_verification),fontSize=11.sp,color=Color(0xFF92400E))}
   Spacer(Modifier.height(8.dp));Text("✓ "+stringResource(R.string.thank_you),fontSize=12.sp,color=BrandGreenDark,fontWeight=FontWeight.SemiBold)
  }}
  Spacer(Modifier.height(12.dp));GradientButton(stringResource(R.string.share_receipt),true,share);Spacer(Modifier.height(20.dp))
}}
/** Finance — sirf AUTO data: collections, cash/UPI/udhaar split, dues, profit estimate. */
@Composable fun FinanceScreen(s:UiState){val f=s.finance?:return
 Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal=16.dp)){Spacer(Modifier.height(6.dp))
  ScreenTitle("💰 "+stringResource(R.string.finance),stringResource(R.string.finance_auto));Spacer(Modifier.height(10.dp))
  Card(Modifier.fillMaxWidth(),shape=RoundedCornerShape(18.dp),colors=CardDefaults.cardColors(containerColor=DukaanColors.Navy)){Column(Modifier.fillMaxWidth().padding(18.dp)){Text(stringResource(R.string.total_collection),color=Color(0xFFC7F0D2),fontSize=13.sp);Text(ProductRules.rupees(f.totalCollectionPaise),style=MaterialTheme.typography.displaySmall,fontWeight=FontWeight.ExtraBold,color=Color.White);Text("🧾 "+f.todayBills.toString()+" "+stringResource(R.string.bills_today),color=Color(0xFFC7F0D2),fontSize=13.sp)}}
  Spacer(Modifier.height(10.dp));Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(10.dp)){MetricCard(Icons.Default.Payments,stringResource(R.string.cash_sales),ProductRules.rupees(f.cashPaise),DukaanColors.BlinkitGreen,Modifier.weight(1f));MetricCard(Icons.Default.ShoppingCart,stringResource(R.string.upi_sales),ProductRules.rupees(f.upiPaise),DukaanColors.Navy,Modifier.weight(1f))}
  Spacer(Modifier.height(10.dp));Card(Modifier.fillMaxWidth(),shape=RoundedCornerShape(18.dp),colors=CardDefaults.cardColors(containerColor=if(f.udhaarPaise>0)Color(0xFFFFF7ED) else Color.White)){Row(Modifier.padding(16.dp),verticalAlignment=Alignment.CenterVertically){Column(Modifier.weight(1f)){Text(stringResource(R.string.udhaar_sales),fontWeight=FontWeight.Bold);Text(ProductRules.rupees(f.udhaarPaise),fontWeight=FontWeight.ExtraBold,color=Color(0xFF92400E))}}}
  Spacer(Modifier.height(10.dp));Card(Modifier.fillMaxWidth(),shape=RoundedCornerShape(18.dp),colors=CardDefaults.cardColors(containerColor=if(f.outstandingDuesPaise>0)DukaanColors.LightRed else Color.White)){Row(Modifier.padding(16.dp),verticalAlignment=Alignment.CenterVertically){Column(Modifier.weight(1f)){Text(stringResource(R.string.outstanding_udhaari),fontWeight=FontWeight.Bold);Text(ProductRules.rupees(f.outstandingDuesPaise),fontWeight=FontWeight.ExtraBold,color=Color(0xFF991B1B))};if(f.dueCustomers>0)StatusBadge(f.dueCustomers.toString()+" due")}}
  f.profitEstimatePaise?.let{Spacer(Modifier.height(10.dp));Card(Modifier.fillMaxWidth(),shape=RoundedCornerShape(18.dp),colors=CardDefaults.cardColors(containerColor=DukaanColors.LightGreen)){Row(Modifier.padding(16.dp),verticalAlignment=Alignment.CenterVertically){Column(Modifier.weight(1f)){Text(stringResource(R.string.profit_estimate),fontWeight=FontWeight.Bold);Text(stringResource(R.string.profit_estimate_hint),fontSize=11.sp,color=DukaanColors.Slate600)};Text("+"+ProductRules.rupees(it),fontWeight=FontWeight.ExtraBold,color=BrandGreenDark)}}}
  Spacer(Modifier.height(12.dp));Text(stringResource(R.string.finance_footer),fontSize=12.sp,color=DukaanColors.Slate500);Spacer(Modifier.height(20.dp))
 }}
fun dayLabel(epochDay:Long,today:Long)=when(epochDay){today->"Today";today-1->"Yesterday";else->java.text.SimpleDateFormat("dd MMM",java.util.Locale.US).format(java.util.Date(epochDay*86_400_000L))}
@Composable fun MetricCard(icon:androidx.compose.ui.graphics.vector.ImageVector,label:String,value:String,color:Color,modifier:Modifier=Modifier){Card(modifier,shape=RoundedCornerShape(16.dp),colors=CardDefaults.cardColors(containerColor=Color.White),elevation=CardDefaults.cardElevation(2.dp)){Column(Modifier.fillMaxWidth().padding(14.dp)){Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween,verticalAlignment=Alignment.CenterVertically){Text(label,color=DukaanColors.Slate600,fontSize=12.sp);Box(Modifier.size(30.dp).clip(RoundedCornerShape(10.dp)).background(DukaanColors.Slate100),contentAlignment=Alignment.Center){Icon(icon,null,tint=color,modifier=Modifier.size(17.dp))}};Spacer(Modifier.height(8.dp));Text(value,style=MaterialTheme.typography.titleLarge,fontWeight=FontWeight.ExtraBold,color=DukaanColors.Dark)}}}
@Composable fun DashAction(icon:androidx.compose.ui.graphics.vector.ImageVector,label:String,action:()->Unit,modifier:Modifier=Modifier){Card(onClick=action,modifier,shape=RoundedCornerShape(16.dp),colors=CardDefaults.cardColors(containerColor=Color.White),elevation=CardDefaults.cardElevation(1.dp)){Row(Modifier.fillMaxWidth().padding(horizontal=12.dp,vertical=14.dp),verticalAlignment=Alignment.CenterVertically){Box(Modifier.size(38.dp).clip(RoundedCornerShape(12.dp)).background(DukaanColors.LightGreen),contentAlignment=Alignment.Center){Icon(icon,null,tint=DukaanColors.BlinkitGreen,modifier=Modifier.size(20.dp))};Spacer(Modifier.width(10.dp));Text(label,fontWeight=FontWeight.Bold,fontSize=13.sp,color=DukaanColors.Dark,lineHeight=16.sp)}}}
@Composable fun ReportsScreen(s:UiState){val today=ReportMath.todayEpochDay()
 Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())){Spacer(Modifier.height(6.dp))
  ScreenTitle("📊 Business Reports","Sales, profit & stock — last 30 days");Spacer(Modifier.height(12.dp))
  SectionCard{Text("Daily breakdown",fontWeight=FontWeight.Bold);if(s.dailySales.isEmpty())Text("No sales yet",color=Color(0xFF5B6B62))
   val profitByDay=s.dailyProfit.associateBy{it.epochDay}
   s.dailySales.take(14).forEach{row->val p=profitByDay[row.epochDay];Column{Row(Modifier.fillMaxWidth().padding(vertical=6.dp),verticalAlignment=Alignment.CenterVertically){Column(Modifier.weight(1f)){Text(dayLabel(row.epochDay,today),fontWeight=FontWeight.SemiBold);Text("${row.bills} bills • ${ProductRules.rupees(row.creditPaise)} udhari",fontSize=11.sp,color=Color(0xFF5B6B62))};Column(horizontalAlignment=Alignment.End){Text(ProductRules.rupees(row.salesPaise),fontWeight=FontWeight.Bold,color=BrandGreenDark);Text("profit +${ProductRules.rupees(p?.profitPaise?:0)}",fontSize=11.sp,color=BrandBlue)}};Divider(color=Color(0xFFE4EFE8))}}
  Spacer(Modifier.height(12.dp));SectionCard{Text("Inventory",fontWeight=FontWeight.Bold);Row(Modifier.fillMaxWidth()){Column(Modifier.weight(1f)){Text("Stock value",color=Color(0xFF5B6B62),fontSize=12.sp);Text(ProductRules.rupees(s.stockValue?.totalStockValuePaise?:0),fontWeight=FontWeight.Bold)};Column(Modifier.weight(1f)){Text("Products",color=Color(0xFF5B6B62),fontSize=12.sp);Text("${s.stockValue?.totalItems?:0}",fontWeight=FontWeight.Bold)};Column(Modifier.weight(1f)){Text("Low stock",color=Color(0xFF5B6B62),fontSize=12.sp);Text("${s.stockValue?.lowCount?:0}",fontWeight=FontWeight.Bold,color=if((s.stockValue?.lowCount?:0)>0)BrandRed else BrandGreenDark)}}}}}}

private fun readImageLimited(input:java.io.InputStream):ByteArray{val out=java.io.ByteArrayOutputStream();val buffer=ByteArray(8192);var total=0;while(true){val n=input.read(buffer);if(n<0)break;total+=n;require(total<=5*1024*1024){"Image must be 5 MiB or smaller"};out.write(buffer,0,n)};return out.toByteArray()}
@Composable fun ProductForm(existing:ProductRow?,currentImage:ByteArray?,initialBarcode:String?,initialName:String?,initialBrand:String?,save:(ProductDraft,SelectedImage?)->Unit,removeImage:()->Unit,onScan:(String)->Unit,catalogue:List<ProductRow> = emptyList()){
 val context=androidx.compose.ui.platform.LocalContext.current
 var selected by remember{mutableStateOf<SelectedImage?>(null)}
  var scannedCode by remember{mutableStateOf<String?>(null)}
  var pick by remember{mutableStateOf<ProductRow?>(null)};var pickQuery by remember{mutableStateOf("")}
 var imageError by remember{mutableStateOf<String?>(null)}
 val picker=androidx.activity.compose.rememberLauncherForActivityResult(androidx.activity.result.contract.ActivityResultContracts.OpenDocument()){uri->if(uri!=null){runCatching{context.contentResolver.takePersistableUriPermission(uri,android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION);val mime=context.contentResolver.getType(uri)?:error("Unknown image type");require(mime in setOf("image/jpeg","image/png","image/webp")){"Choose a JPEG, PNG or WebP image"};val bytes=(context.contentResolver.openInputStream(uri)?:error("Image khul nahi payi — dobara try karein")).use{input->readImageLimited(input)};require(bytes.isNotEmpty()){"Image is empty"};SelectedImage(bytes,mime,uri.lastPathSegment?:"Selected image")}.onSuccess{selected=it;imageError=null}.onFailure{imageError=it.message}}}
 var name by remember(existing,pick,initialName){mutableStateOf(if(!initialName.isNullOrBlank())initialName else pick?.product?.name?:existing?.product?.name.orEmpty())};var brand by remember(existing,pick,initialBrand){mutableStateOf(if(!initialBrand.isNullOrBlank())initialBrand else pick?.product?.brand?:existing?.product?.brand.orEmpty())};var barcode by remember(existing,pick,initialBarcode,initialName){mutableStateOf(existing?.product?.barcode?:pick?.product?.barcode?:initialBarcode.orEmpty())};var unit by remember(existing,pick){mutableStateOf(pick?.product?.unit?:existing?.product?.unit?:"piece")};var price by remember(existing,pick){mutableStateOf((pick?:existing)?.let{java.math.BigDecimal(it.shopProduct.sellingPricePaise).movePointLeft(2).toPlainString()}.orEmpty())};var opening by remember(existing,pick){mutableStateOf(existing?.let{ProductRules.quantity(it.shopProduct.stockQuantityMilli)}?:"0")};var minimum by remember(existing,pick){mutableStateOf((pick?:existing)?.let{ProductRules.quantity(it.shopProduct.minimumStockMilli)}?:"0")};var description by remember(existing,pick){mutableStateOf(pick?.product?.description?:existing?.product?.description.orEmpty())};var cost by remember(existing,pick){mutableStateOf((pick?:existing)?.let{if(it.shopProduct.costPricePaise>0)java.math.BigDecimal(it.shopProduct.costPricePaise).movePointLeft(2).toPlainString() else ""}.orEmpty())};var available by remember(existing,pick){mutableStateOf(existing?.shopProduct?.isAvailable?:true)}
   Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).imePadding()){Spacer(Modifier.height(8.dp));if(existing==null){
   Field(stringResource(R.string.search_products),pickQuery,{pickQuery=it;if(it.isBlank())pick=null})
   val matches=if(pickQuery.isBlank())emptyList() else catalogue.filter{it.product.name.contains(pickQuery,true)||it.product.brand.orEmpty().contains(pickQuery,true)||it.product.barcode.orEmpty().contains(pickQuery,true)}.take(5)
   matches.forEach{row->Card(onClick={pick=row;pickQuery=row.product.name},Modifier.fillMaxWidth().padding(vertical=2.dp),shape=RoundedCornerShape(12.dp),colors=CardDefaults.cardColors(containerColor=DukaanColors.LightGreen)){Row(Modifier.fillMaxWidth().padding(horizontal=12.dp,vertical=10.dp),verticalAlignment=Alignment.CenterVertically){Column(Modifier.weight(1f)){Text(row.product.name,fontWeight=FontWeight.Bold,maxLines=1,overflow=androidx.compose.ui.text.style.TextOverflow.Ellipsis);Text(ProductRules.rupees(row.shopProduct.sellingPricePaise),fontSize=12.sp,color=BrandGreenDark)};Text("✓ "+stringResource(R.string.use_label),fontWeight=FontWeight.Bold,color=BrandGreenDark,fontSize=13.sp)}}}
   pick?.let{Text("✓ \"${it.product.name}\" se copy ho raha hai — badlo aur save karo",fontWeight=FontWeight.SemiBold,color=BrandGreenDark,fontSize=13.sp,modifier=Modifier.padding(horizontal=16.dp))};`in`.localdukaan.feature.BarcodeScanner(true,{code->scannedCode=code;barcode=code;onScan(code)},Modifier.fillMaxWidth().padding(horizontal=16.dp).aspectRatio(16f/9f));scannedCode?.let{Text("✓ Scan: $it",fontWeight=FontWeight.SemiBold,color=BrandGreenDark,modifier=Modifier.padding(horizontal=16.dp))}};SectionCard{Field(stringResource(R.string.product_name),name,{name=it});Field(stringResource(R.string.brand_optional),brand,{brand=it});Field(stringResource(R.string.barcode_optional),barcode,{barcode=it},KeyboardType.Number);Field(stringResource(R.string.unit),unit,{unit=it});Field(stringResource(R.string.selling_price),price,{price=it},KeyboardType.Decimal);Field(stringResource(R.string.cost_price),cost,{cost=it},KeyboardType.Decimal);if(existing==null)Field(stringResource(R.string.opening_stock),opening,{opening=it},KeyboardType.Decimal) else Text("${stringResource(R.string.stock)}: ${ProductRules.quantity(existing.shopProduct.stockQuantityMilli)}");Field(stringResource(R.string.minimum_stock),minimum,{minimum=it},KeyboardType.Decimal);Field(stringResource(R.string.description_optional),description,{description=it})}
  val localCurrent=existing?.shopProduct?.imageKey?.takeIf{it.startsWith("local-file:")}?.removePrefix("local-file:")?.let{runCatching{java.io.File(it).readBytes()}.getOrNull()}
  val preview=selected?.bytes?:currentImage?:localCurrent
  preview?.let{bytes->android.graphics.BitmapFactory.decodeByteArray(bytes,0,bytes.size)?.let{bitmap->androidx.compose.foundation.Image(bitmap.asImageBitmap(),contentDescription=stringResource(R.string.product_image),modifier=Modifier.fillMaxWidth().height(180.dp),contentScale=androidx.compose.ui.layout.ContentScale.Fit)}}
  Row(horizontalArrangement=Arrangement.spacedBy(8.dp)){ActionButton(if(existing?.shopProduct?.imageKey==null)"📷 "+stringResource(R.string.choose_image) else "📷 "+stringResource(R.string.replace_image)){picker.launch(arrayOf("image/jpeg","image/png","image/webp"))};if(existing?.shopProduct?.imageKey!=null)ActionButton(stringResource(R.string.remove_image),removeImage)}
  imageError?.let{Text(it,color=BrandRed)}
  Row{Checkbox(available,{available=it});Text(stringResource(R.string.available),Modifier.padding(top=12.dp))}
  val draft=ProductDraft(name,brand,barcode,cost,unit,price,if(existing==null)opening else ProductRules.quantity(existing.shopProduct.stockQuantityMilli),minimum,description,available);val validation=ProductRules.validate(draft);validation.message?.let{Text(it,color=BrandRed)}
  Spacer(Modifier.height(8.dp));GradientButton(stringResource(R.string.save_product),validation.valid){save(draft,selected)};Spacer(Modifier.height(10.dp))}}
@OptIn(ExperimentalMaterial3Api::class)
@Composable fun CustomersScreen(s:UiState,add:()->Unit,edit:(ShopCustomerEntity)->Unit,open:(ShopCustomerEntity)->Unit,archive:(ShopCustomerEntity)->Unit){var confirmArchive by remember{mutableStateOf<ShopCustomerEntity?>(null)};var query by remember{mutableStateOf("")};var dueOnly by remember{mutableStateOf(false)}
 val dueCount=s.customers.count{it.currentBalancePaise>0}
 Button(add,Modifier.fillMaxWidth().height(48.dp),colors=ButtonDefaults.buttonColors(containerColor=BrandGreen),shape=RoundedCornerShape(14.dp)){Text("+ "+stringResource(R.string.add_customer),fontWeight=FontWeight.Bold)}
 Spacer(Modifier.height(8.dp));Field(stringResource(R.string.search_customers),query,{query=it})
 Spacer(Modifier.height(8.dp));Row(verticalAlignment=Alignment.CenterVertically){FilterChip(selected=dueOnly,onClick={dueOnly=!dueOnly},label={Text("💰 "+stringResource(R.string.due_only)+(if(dueCount>0)" ($dueCount)" else ""),fontSize=13.sp)});Spacer(Modifier.width(8.dp));Text(ProductRules.rupees(s.customers.sumOf{it.currentBalancePaise})+" "+stringResource(R.string.total_due),fontSize=12.sp,color=BrandRed,fontWeight=FontWeight.SemiBold)}
 val shown=s.customers.filter{(!dueOnly||it.currentBalancePaise>0)&&(query.isBlank()||it.displayName.contains(query,true)||it.normalizedPhone.orEmpty().contains(query,true))}
 if(shown.isEmpty())Column(Modifier.fillMaxWidth().padding(top=40.dp),horizontalAlignment=Alignment.CenterHorizontally){Text("👥",fontSize=44.sp);Text(stringResource(if(s.customers.isEmpty())R.string.no_customers else R.string.no_match),color=Color(0xFF5B6B62))}
 confirmArchive?.let{row->AlertDialog(onDismissRequest={confirmArchive=null},title={Text(stringResource(R.string.archive))},text={Text(stringResource(R.string.archive_confirm)+" (${row.displayName})")},confirmButton={TextButton({archive(row);confirmArchive=null}){Text(stringResource(R.string.archive),color=BrandRed)}},dismissButton={TextButton({confirmArchive=null}){Text(stringResource(R.string.cancel))}})}
 LazyColumn(verticalArrangement=Arrangement.spacedBy(8.dp),contentPadding=PaddingValues(bottom=16.dp)){items(shown,key={it.id}){row->Card(onClick={open(row)},modifier=Modifier.fillMaxWidth(),shape=RoundedCornerShape(16.dp)){Column(Modifier.padding(12.dp)){Row(verticalAlignment=Alignment.CenterVertically){Text(row.displayName,style=MaterialTheme.typography.titleMedium,fontWeight=FontWeight.Bold,modifier=Modifier.weight(1f));StatusBadge(row.syncState)};row.normalizedPhone?.let{Text(it,color=Color(0xFF5B6B62))};if(row.creditEnabled){val dueColor=if(row.currentBalancePaise>0)BrandRed else BrandGreenDark;Text("${stringResource(R.string.due)}: ${ProductRules.rupees(row.currentBalancePaise)}",color=dueColor,fontWeight=FontWeight.SemiBold);Text("${stringResource(R.string.credit_limit_short)} ${row.creditLimitPaise?.let{ProductRules.rupees(it)}?:"-" } • ${stringResource(R.string.available_short)}: ${row.availableCreditPaise?.let{ProductRules.rupees(it)}?:"-" }",color=Color(0xFF5B6B62))} else Text(stringResource(R.string.credit_disabled),color=Color(0xFF5B6B62));Row{TextButton({edit(row)}){Text(stringResource(R.string.edit))};TextButton({confirmArchive=row}){Text(stringResource(R.string.archive),color=BrandRed)}}}}}}}
@Composable fun CustomerForm(s:UiState,save:(String,String,Boolean,String,String)->Unit){val e=s.customer;var name by remember{mutableStateOf(e?.displayName.orEmpty())};var mobile by remember{mutableStateOf(e?.normalizedPhone.orEmpty())};var creditEnabled by remember{mutableStateOf(e?.creditEnabled?:false)};var limit by remember{mutableStateOf(e?.creditLimitPaise?.let{java.math.BigDecimal(it).movePointLeft(2).toPlainString()}.orEmpty())};var notes by remember{mutableStateOf(e?.notes.orEmpty())}
 Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).imePadding()){Spacer(Modifier.height(8.dp));SectionCard{Field(stringResource(R.string.name),name,{name=it});Field(stringResource(R.string.mobile_optional),mobile,{mobile=it},KeyboardType.Phone);Row(verticalAlignment=Alignment.CenterVertically){Checkbox(creditEnabled,{creditEnabled=it});Text(stringResource(R.string.credit_enabled))};if(creditEnabled)Field(stringResource(R.string.credit_limit),limit,{limit=it},KeyboardType.Decimal);Field(stringResource(R.string.notes_optional),notes,{notes=it})};Spacer(Modifier.height(14.dp));GradientButton(stringResource(R.string.save),name.isNotBlank()){save(name,mobile,creditEnabled,limit,notes)}}}
@Composable fun CustomerDetail(s:UiState,onCredit:(String,String,String)->Unit){val row=s.customer?:return;val live=s.customers.firstOrNull{it.id==row.id}?:row;var amount by remember{mutableStateOf("")};var note by remember{mutableStateOf("")}
 Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).imePadding()){Text(live.displayName,style=MaterialTheme.typography.headlineSmall,fontWeight=FontWeight.ExtraBold);live.normalizedPhone?.let{Text(it,color=Color(0xFF5B6B62))};Text("${stringResource(R.string.balance_due)}: ${ProductRules.rupees(live.currentBalancePaise)}",style=MaterialTheme.typography.headlineSmall,color=if(live.currentBalancePaise>0)BrandRed else BrandGreenDark,fontWeight=FontWeight.Bold);if(live.creditEnabled)Text("${stringResource(R.string.credit_limit_short)}: ${live.creditLimitPaise?.let{ProductRules.rupees(it)}?:"-" } • ${stringResource(R.string.available_short)}: ${live.availableCreditPaise?.let{ProductRules.rupees(it)}?:"-" }",color=Color(0xFF5B6B62)) else Text(stringResource(R.string.credit_disabled),color=Color(0xFF5B6B62))
  Spacer(Modifier.height(10.dp));SectionCard{Field(stringResource(R.string.amount_rupees),amount,{amount=it},KeyboardType.Decimal);Field(stringResource(R.string.notes_optional),note,{note=it})}
  Spacer(Modifier.height(10.dp));GradientButton("＋ "+stringResource(R.string.add_udhari),amount.isNotBlank()){onCredit("CREDIT_SALE",amount,note)};Spacer(Modifier.height(8.dp));GradientButton("₹ "+stringResource(R.string.receive_payment),amount.isNotBlank()){onCredit("PAYMENT",amount,note)};Spacer(Modifier.height(8.dp));Row(horizontalArrangement=Arrangement.spacedBy(8.dp)){OutlinedButton({onCredit("ADJUSTMENT_CREDIT",amount,note)},Modifier.weight(1f)){Text(stringResource(R.string.adjust_plus))};OutlinedButton({onCredit("ADJUSTMENT_DEBIT",amount,note)},Modifier.weight(1f)){Text(stringResource(R.string.adjust_minus))}}
  Spacer(Modifier.height(16.dp));Text("Is customer ne kya kharida (CASH / UDHARI):",style=MaterialTheme.typography.titleMedium,fontWeight=FontWeight.Bold);if(s.customerSales.isEmpty())Text("Abhi koi purchase nahi hui",color=Color(0xFF5B6B62));s.customerSales.take(30).forEach{sale->Card(Modifier.fillMaxWidth().padding(vertical=3.dp),shape=RoundedCornerShape(12.dp),colors=CardDefaults.cardColors(containerColor=if(sale.paymentType=="CREDIT")Color(0xFFFFF7ED) else Color(0xFFEFF7F1))){Row(Modifier.padding(10.dp),verticalAlignment=Alignment.CenterVertically){Column(Modifier.weight(1f)){Text(java.text.SimpleDateFormat("dd MMM, hh:mm a",java.util.Locale.getDefault()).format(java.util.Date(sale.createdAt)),fontSize=12.sp,color=Color(0xFF5B6B62));Text(if(sale.paymentType=="CREDIT")"📖 UDHARI" else "💵 CASH",fontSize=12.sp,fontWeight=FontWeight.Bold,color=if(sale.paymentType=="CREDIT")Color(0xFF92400E) else Color(0xFF15803D));if(sale.status=="PENDING")Text("(sync baaki)",fontSize=10.sp,color=Color(0xFF92400E))};Text(ProductRules.rupees(sale.totalPaise),fontWeight=FontWeight.ExtraBold,color=if(sale.paymentType=="CREDIT")Color(0xFF92400E) else BrandGreenDark)}}}
  Spacer(Modifier.height(16.dp));Text(stringResource(R.string.statement),style=MaterialTheme.typography.titleMedium,fontWeight=FontWeight.Bold);if(s.creditStatement.isEmpty())Text(stringResource(R.string.no_transactions),color=Color(0xFF5B6B62));s.creditStatement.forEach{t->Text("• ${t.type}: ${ProductRules.rupees(t.amountPaise)} → ${ProductRules.rupees(t.balanceAfterPaise)}"+(if(t.status!="SYNCED") " (${t.status})" else ""))}}}
@Composable fun SalesHistoryScreen(s:UiState,openReceipt:(String)->Unit){Column(Modifier.fillMaxSize()){LazyColumn(Modifier.fillMaxSize(),verticalArrangement=Arrangement.spacedBy(8.dp),contentPadding=PaddingValues(start=16.dp,end=16.dp,bottom=16.dp)){if(s.customerSales.isEmpty())item{Column(Modifier.fillMaxWidth().padding(top=60.dp),horizontalAlignment=Alignment.CenterHorizontally){Text("🧾",fontSize=44.sp);Text("Abhi koi sale nahi hui",color=Color(0xFF5B6B62))}};items(s.customerSales,key={it.id}){sale->Card(onClick={openReceipt(sale.id)},shape=RoundedCornerShape(14.dp),colors=CardDefaults.cardColors(containerColor=if(sale.paymentType=="CREDIT")Color(0xFFFFF7ED) else Color.White)){Row(Modifier.padding(12.dp),verticalAlignment=Alignment.CenterVertically){Column(Modifier.weight(1f)){Row(verticalAlignment=Alignment.CenterVertically){Text(if(sale.paymentType=="CREDIT")"📖 UDHARI" else if(sale.paymentMethod=="UPI")"📱 UPI" else "💵 CASH",fontWeight=FontWeight.Bold,color=if(sale.paymentType=="CREDIT")Color(0xFF92400E) else Color(0xFF15803D),fontSize=13.sp);if(sale.shopCustomerId!=null)Text("  • customer se",fontSize=11.sp,color=Color(0xFF5B6B62))};Text(java.text.SimpleDateFormat("dd MMM yyyy, hh:mm a",java.util.Locale.getDefault()).format(java.util.Date(sale.createdAt)),fontSize=12.sp,color=Color(0xFF5B6B62))};Column(horizontalAlignment=Alignment.End){Text(ProductRules.rupees(sale.totalPaise),fontWeight=FontWeight.ExtraBold,color=if(sale.paymentType=="CREDIT")Color(0xFF92400E) else BrandGreenDark);StatusBadge(sale.status)}}}}}}}
@Composable fun MarketScreen(s:UiState,search:(String)->Unit,open:(MarketShop)->Unit){var q by remember{mutableStateOf(s.marketQuery)}
 Field(stringResource(R.string.search_shops),q,{q=it})
 LaunchedEffect(q){if(q!=s.marketQuery){delay(450);search(q)}}
 if(s.marketShops.isEmpty())Column(Modifier.fillMaxWidth().padding(top=40.dp),horizontalAlignment=Alignment.CenterHorizontally){Text("🏪",fontSize=44.sp);Text(stringResource(R.string.no_shops),color=Color(0xFF5B6B62))}
 LazyColumn(verticalArrangement=Arrangement.spacedBy(8.dp),contentPadding=PaddingValues(bottom=16.dp)){items(s.marketShops,key={it.id}){shop->Card(modifier=Modifier.fillMaxWidth(),shape=RoundedCornerShape(16.dp),colors=CardDefaults.cardColors(containerColor=BrandCard)){Column(Modifier.padding(12.dp)){Row(verticalAlignment=Alignment.CenterVertically){Text(shop.name,style=MaterialTheme.typography.titleMedium,fontWeight=FontWeight.Bold,modifier=Modifier.weight(1f));if(shop.deliveryEnabled)StatusBadge("🛵 DELIVERY")};Text("${shop.category} • ${shop.locality}, ${shop.city}",color=Color(0xFF5B6B62));shop.distanceM?.let{Text("📍 ${"%.1f".format(it/1000.0)} ${stringResource(R.string.km)}")};Text("🛒 ${stringResource(R.string.minimum_order)}: ${ProductRules.rupees(shop.minimumOrderPaise)} • ${stringResource(R.string.delivery)}: ${ProductRules.rupees(shop.deliveryFeePaise)}",color=Color(0xFF5B6B62));Button({open(shop)},colors=ButtonDefaults.buttonColors(containerColor=BrandGreen)){Text("🛍️ "+stringResource(R.string.view_shop))}}}}}}
@Composable fun ShopPageScreen(s:UiState,add:(MarketProduct)->Unit,setQty:(String,Long)->Unit,checkout:()->Unit){Column(Modifier.fillMaxSize()){Text(s.marketShop?.name.orEmpty(),style=MaterialTheme.typography.titleLarge,fontWeight=FontWeight.Bold)
 LazyColumn(Modifier.weight(1f),verticalArrangement=Arrangement.spacedBy(8.dp)){items(s.marketProducts,key={it.shopProductId}){p->val line=s.cart.lines.firstOrNull{it.shopProductId==p.shopProductId};Card(shape=RoundedCornerShape(16.dp)){Column(Modifier.padding(12.dp)){Text(p.name,style=MaterialTheme.typography.titleMedium,fontWeight=FontWeight.Bold);Text("${ProductRules.rupees(p.sellingPricePaise)} • ${ProductRules.quantity(p.stockQuantityMilli)} ${p.unit}",color=BrandGreenDark,fontWeight=FontWeight.SemiBold);if(line==null)Button({add(p)},colors=ButtonDefaults.buttonColors(containerColor=BrandGreen)){Text(stringResource(R.string.add_to_cart))} else Row(verticalAlignment=Alignment.CenterVertically){TextButton({setQty(p.shopProductId,line.quantityMilli-1000)}){Text("−",fontSize=20.sp)};Text(ProductRules.quantity(line.quantityMilli),fontWeight=FontWeight.Bold);TextButton({setQty(p.shopProductId,line.quantityMilli+1000)}){Text("+",fontSize=20.sp)}}}}}}
 Column(Modifier.navigationBarsPadding()){Text("${stringResource(R.string.cart)}: ${ProductRules.rupees(s.cart.subtotalPaise)}",style=MaterialTheme.typography.titleLarge,fontWeight=FontWeight.ExtraBold,color=BrandGreenDark);GradientButton(stringResource(R.string.place_order),!s.cart.isEmpty,checkout);Spacer(Modifier.height(10.dp))}}}
@Composable fun CheckoutScreen(s:UiState,place:(String,String?,String)->Unit,addAddress:(String,String,String,String,String,String)->Unit){var fulfillment by remember{mutableStateOf("PICKUP")};var addressId by remember{mutableStateOf<String?>(null)};var note by remember{mutableStateOf("")};var recipient by remember{mutableStateOf("")};var mobile by remember{mutableStateOf("")};var line by remember{mutableStateOf("")};var locality by remember{mutableStateOf("")};var city by remember{mutableStateOf("")};var pincode by remember{mutableStateOf("")};var addingAddress by remember{mutableStateOf(false)}
 LaunchedEffect(s.addresses){if(s.addresses.isNotEmpty()&&s.addresses.none{it.id==addressId})addressId=s.addresses.last().id}
 Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).imePadding().navigationBarsPadding()){Row(horizontalArrangement=Arrangement.spacedBy(8.dp)){FilterChip(selected=fulfillment=="PICKUP",onClick={fulfillment="PICKUP"},label={Text("🏪 "+stringResource(R.string.pickup))});FilterChip(selected=fulfillment=="DELIVERY",onClick={fulfillment="DELIVERY"},label={Text("🛵 "+stringResource(R.string.delivery))})}
  if(fulfillment=="DELIVERY"){Text(stringResource(R.string.choose_address),style=MaterialTheme.typography.titleMedium,fontWeight=FontWeight.Bold);if(s.addresses.isEmpty())Text(stringResource(R.string.no_addresses),color=Color(0xFF5B6B62));s.addresses.forEach{a->Row(verticalAlignment=Alignment.CenterVertically){RadioButton(addressId==a.id,{addressId=a.id});Text("${a.recipientName}, ${a.addressLine}, ${a.city}",modifier=Modifier.weight(1f))}};if(addingAddress){Text(stringResource(R.string.add_address),style=MaterialTheme.typography.titleMedium,fontWeight=FontWeight.Bold);SectionCard{Field(stringResource(R.string.recipient),recipient,{recipient=it});Field(stringResource(R.string.mobile_number),mobile,{mobile=it},KeyboardType.Phone);Field(stringResource(R.string.address),line,{line=it});Field(stringResource(R.string.locality),locality,{locality=it});Field(stringResource(R.string.city),city,{city=it});Field(stringResource(R.string.pincode),pincode,{pincode=it},KeyboardType.Number)};Button({addAddress(recipient,mobile,line,locality,city,pincode)},enabled=recipient.isNotBlank()&&line.isNotBlank()&&pincode.length==6,colors=ButtonDefaults.buttonColors(containerColor=DukaanColors.Navy)){Text(stringResource(R.string.save))}}}else TextButton({addingAddress=true}){Text("＋ "+stringResource(R.string.add_address),color=BrandBlue,fontWeight=FontWeight.SemiBold)}
  Spacer(Modifier.height(8.dp));Field(stringResource(R.string.note_optional),note,{note=it});Text("${stringResource(R.string.cart)}: ${ProductRules.rupees(s.cart.subtotalPaise)}",style=MaterialTheme.typography.titleLarge,fontWeight=FontWeight.ExtraBold,color=BrandGreenDark);Spacer(Modifier.height(8.dp));GradientButton(stringResource(R.string.place_order),!s.cart.isEmpty&&(fulfillment=="PICKUP"||addressId!=null)){place(fulfillment,if(fulfillment=="DELIVERY")addressId else null,note)};Spacer(Modifier.height(16.dp))}}
@Composable fun OrdersScreen(s:UiState,open:(String)->Unit){if(s.orders.isEmpty())Column(Modifier.fillMaxWidth().padding(top=60.dp),horizontalAlignment=Alignment.CenterHorizontally){Text("📦",fontSize=44.sp);Text(stringResource(R.string.no_orders),color=Color(0xFF5B6B62))}
 LazyColumn(verticalArrangement=Arrangement.spacedBy(8.dp),contentPadding=PaddingValues(bottom=16.dp)){items(s.orders,key={it.id}){o->Card(modifier=Modifier.fillMaxWidth(),shape=RoundedCornerShape(16.dp),onClick={open(o.id)}){Column(Modifier.padding(12.dp)){Row(verticalAlignment=Alignment.CenterVertically){Text(if(o.shopName.isBlank())o.shopId else o.shopName,style=MaterialTheme.typography.titleMedium,fontWeight=FontWeight.Bold,modifier=Modifier.weight(1f));StatusBadge(o.status)};Text("${o.fulfillmentType} • ${ProductRules.rupees(o.totalPaise)}",color=BrandGreenDark,fontWeight=FontWeight.SemiBold);Text(java.text.SimpleDateFormat("dd MMM, hh:mm a",java.util.Locale.getDefault()).format(java.util.Date(o.createdAt*1000)),fontSize=11.sp,color=Color(0xFF5B6B62))}}}}}
@Composable fun OrderDetailScreen(s:UiState,cancel:(String)->Unit){val o=s.order?:return
 Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())){Text("${stringResource(R.string.order_label)} ${o.id.take(8)}",style=MaterialTheme.typography.headlineSmall,fontWeight=FontWeight.Bold);StatusBadge(o.status);Text("${o.fulfillmentType} • ${ProductRules.rupees(o.totalPaise)}",fontWeight=FontWeight.SemiBold);if(o.deliveryFeePaise>0)Text("${stringResource(R.string.delivery_fee)}: ${ProductRules.rupees(o.deliveryFeePaise)}",color=Color(0xFF5B6B62));Spacer(Modifier.height(8.dp));SectionCard{o.lines.forEach{l->Text("• ${l.name} × ${ProductRules.quantity(l.quantityMilli)} = ${ProductRules.rupees(l.lineTotalPaise)}")}}
  Spacer(Modifier.height(12.dp));Text(stringResource(R.string.history),style=MaterialTheme.typography.titleMedium,fontWeight=FontWeight.Bold);SectionCard{o.history.forEach{Text("• $it")}}
  if(o.status=="PENDING"||o.status=="ACCEPTED"){Spacer(Modifier.height(10.dp));Button({cancel(o.id)},Modifier.fillMaxWidth(),colors=ButtonDefaults.buttonColors(containerColor=BrandRed)){Text(stringResource(R.string.cancel_order))}};Spacer(Modifier.height(16.dp))}}
@Composable fun ShopOrdersScreen(s:UiState,advance:(String,String)->Unit){if(s.shopOrders.isEmpty())Column(Modifier.fillMaxWidth().padding(top=60.dp),horizontalAlignment=Alignment.CenterHorizontally){Text("🧾",fontSize=44.sp);Text(stringResource(R.string.no_orders),color=Color(0xFF5B6B62))}
 LazyColumn(verticalArrangement=Arrangement.spacedBy(8.dp),contentPadding=PaddingValues(bottom=16.dp)){items(s.shopOrders,key={it.id}){o->Card(Modifier.fillMaxWidth(),shape=RoundedCornerShape(16.dp)){Column(Modifier.padding(12.dp)){Row(verticalAlignment=Alignment.CenterVertically){Text(o.id.take(8),style=MaterialTheme.typography.titleMedium,fontWeight=FontWeight.Bold,modifier=Modifier.weight(1f));StatusBadge(o.status)};Text(ProductRules.rupees(o.totalPaise),fontWeight=FontWeight.SemiBold,color=BrandGreenDark);Text(java.text.SimpleDateFormat("dd MMM, hh:mm a",java.util.Locale.getDefault()).format(java.util.Date(o.createdAt*1000)),fontSize=11.sp,color=Color(0xFF5B6B62));Row{if(o.status=="PENDING"){Button({advance(o.id,"ACCEPTED")},colors=ButtonDefaults.buttonColors(containerColor=BrandGreen)){Text(stringResource(R.string.accept))};Spacer(Modifier.width(8.dp));Button({advance(o.id,"REJECTED")},colors=ButtonDefaults.buttonColors(containerColor=BrandRed)){Text(stringResource(R.string.reject))}}else{OrderStatusFlow.next(o.status).forEach{next->TextButton({advance(o.id,next)}){Text(if(next=="CANCELLED")stringResource(R.string.cancel_order) else when(next){"PACKING"->stringResource(R.string.start_packing);"READY"->stringResource(R.string.mark_ready);"OUT_FOR_DELIVERY"->stringResource(R.string.out_for_delivery);"DELIVERED"->stringResource(R.string.mark_delivered);else->next},color=if(next=="CANCELLED")BrandRed else BrandGreenDark,fontWeight=FontWeight.SemiBold)}}}}}}}}}
