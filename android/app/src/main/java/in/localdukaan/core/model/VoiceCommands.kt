package `in`.localdukaan.core.model

import `in`.localdukaan.core.database.ProductRow

/** Voice sale ka ek pakka option — confirm card isi par banta hai. */
data class SaleOption(val productId: String, val name: String, val unitPricePaise: Long, val quantityMilli: Long)

/** Mic se aaye text ka matlab — sirf rules, koi AI nahi. Sab kuch unit-test me pakka. */
sealed interface VoiceCmd {
 data class Sale(val option: SaleOption) : VoiceCmd
 data class SaleChoice(val query: String, val options: List<SaleOption>) : VoiceCmd
 data class AskStock(val query: String, val productId: String?, val name: String?) : VoiceCmd
 data object AskProfit : VoiceCmd
 data object AskDues : VoiceCmd
 data class FindCustomer(val query: String) : VoiceCmd
 data object OpenReports : VoiceCmd
 data object Yes : VoiceCmd
 data object No : VoiceCmd
 data class Unknown(val heard: String) : VoiceCmd
}

object VoiceParser {
 private val numbers = mapOf(
  "ek" to 1000L, "do" to 2000L, "teen" to 3000L, "chaar" to 4000L, "char" to 4000L,
  "paanch" to 5000L, "paanch" to 5000L, "chhah" to 6000L, "chah" to 6000L, "saat" to 7000L,
  "aath" to 8000L, "aathh" to 8000L, "nau" to 9000L, "das" to 10000L, "dus" to 10000L,
  "gyarah" to 11000L, "baarah" to 12000L, "terah" to 13000L, "chaudah" to 14000L,
  "pandrah" to 15000L, "solah" to 16000L, "satrah" to 17000L, "atharah" to 18000L,
  "unnis" to 19000L, "bees" to 20000L, "vees" to 20000L, "tees" to 30000L, "chaalis" to 40000L,
  "pachaas" to 50000L, "saath" to 60000L, "sattar" to 70000L, "assi" to 80000L,
  "nabbe" to 90000L, "sau" to 100000L, "aadha" to 500L, "aadhe" to 500L, "adha" to 500L
 )
 private val saleVerbs = setOf("sale", "sel", "bech", "beche", "becho", "bechde", "bechdo", "kharid", "nikal", "nikalo", "lao", "do", "de", "dedo", "dede", "dijiye", "dena", "karo", "kar", "zara", "please", "kripya")
 private val unitWords = setOf("kilo", "kg", "gram", "gm", "gramo", "litre", "liter", "litr", "piece", "peece", "packet", "pack", "dabba", "daba", "bottle", "botal", "pouch", "thaila")
 private val yesWords = setOf("haan", "han", "haji", "haji", "yes", "kar do", "kardo", "bech do", "bechdo", "theek", "ok", "okay", "sahi")
 private val noWords = setOf("naa", "na", "nahi", "nahin", "no", "ruko", "rko", "cancel", "mat", "rehne", "rehne do", "galat")
 private val profitWords = setOf("profit", "munafa", "munapha", "kamaya", "kamai", "faayda", "fayda", "bachat")
 private val stockWords = setOf("stock", "kitna", "bacha", "bache", "bachega", "pada", "hai")
 private val duesWords = setOf("udhaar", "udhar", "udhaari", "baaki", "baki", "due", "dues", "hisab", "hisaab", "khata")
 private val reportWords = setOf("report", "hisab", "hisaab", "bikri", "sales")

 private fun norm(s: String) = s.lowercase().replace(Regex("[^a-z0-9 ]"), " ").replace(Regex("\\s+"), " ").trim()

 /** "2.5" ya "do" → milli (1000 = 1 unit). Shuru ke number-token kha jata hai. */
 private fun takeQuantity(tokens: MutableList<String>): Long {
  if (tokens.isEmpty()) return 1000L
  val first = tokens.first()
  first.toDoubleOrNull()?.let { d -> if (d in 0.001..100000.0) { tokens.removeAt(0); return (d * 1000).toLong() } }
  numbers[first]?.let { v -> tokens.removeAt(0); return v }
  return 1000L
 }

 private fun matchScore(query: String, row: ProductRow): Int {
  if (query.isBlank()) return 0
  val hay = norm(listOfNotNull(row.product.name, row.product.brand, row.product.barcode).joinToString(" "))
  if (hay.isEmpty()) return 0
  if (hay == query) return 100
  if (hay.startsWith(query)) return 80
  if (hay.contains(query)) return 60
  val qt = query.split(" ").filter { it.length > 1 }
  if (qt.isEmpty()) return 0
  val hits = qt.count { hay.contains(it) }
  if (hits == qt.size && qt.size > 1) return 50
  if (hits == qt.size) return 40
  return if (hits > 0) 10 + hits * 10 else 0
 }

 fun parse(heard: String, catalogue: List<ProductRow>): VoiceCmd {
  val clean = norm(heard)
  if (clean.isEmpty()) return VoiceCmd.Unknown(heard)
  // Haan/naa — sirf ViewModel tab manega jab confirm card khula ho.
  if (clean in yesWords || yesWords.any { clean.startsWith("$it ") }) return VoiceCmd.Yes
  if (noWords.any { it == clean || clean == it || clean.startsWith("$it ") || clean.endsWith(" $it") }) return VoiceCmd.No
  val tokens = clean.split(" ").toMutableList()
  // Sawaal pehle (sale-verb se takrahat na ho)
  if (profitWords.any { it in tokens } && ("aaj" in tokens || "today" in tokens || tokens.size <= 4)) return VoiceCmd.AskProfit
  if (reportWords.any { it in tokens } && ("khol" in tokens || "dikha" in tokens || "dikhao" in tokens || "bata" in tokens || "batao" in tokens || tokens.size <= 2)) return VoiceCmd.OpenReports
  val hasDues = duesWords.any { it in tokens }
  val hasStockQ = ("stock" in tokens) || ("kitna" in tokens && ("bacha" in tokens || "bache" in tokens || "pada" in tokens || "hai" in tokens))
  if (hasDues || hasStockQ) {
   val drop = duesWords + stockWords + setOf("ka", "ki", "ke", "kya", "mera", "meri", "mere", "kul", "total", "saara", "sara", "sab", "bata", "batao", "dikha", "dikhao", "hai", "kitna")
   val name = tokens.filter { it !in drop }.joinToString(" ").trim()
   if (hasDues && (name.isBlank() || name in setOf("kul", "total"))) return VoiceCmd.AskDues
   if (hasDues) return VoiceCmd.FindCustomer(name)
   if (name.isNotBlank()) {
    val ranked = catalogue.map { it to matchScore(name, it) }.filter { it.second >= 40 }.sortedByDescending { it.second }
    val best = ranked.firstOrNull()
    return VoiceCmd.AskStock(name, best?.first?.shopProduct?.id, best?.first?.product?.name)
   }
   return VoiceCmd.Unknown(heard)
  }
  // Sale: shuru ki ginti + aakhir ke verb hatao
  val qty = takeQuantity(tokens)
  while (tokens.isNotEmpty() && tokens.last() in saleVerbs) tokens.removeAt(tokens.size - 1)
  // Beech ke unit-shabd hatao ("2 kilo parle g" → "2 parle g" jaisa)
  val nameTokens = tokens.filter { it !in unitWords && it !in saleVerbs }
  val query = nameTokens.joinToString(" ").trim()
  if (query.isBlank()) return VoiceCmd.Unknown(heard)
  val ranked = catalogue.filter { !it.shopProduct.isArchived }
   .map { it to matchScore(query, it) }.filter { it.second > 0 }.sortedByDescending { it.second }
  if (ranked.isEmpty()) return VoiceCmd.Unknown(heard)
  val best = ranked.first()
  // Ek shabd ("parle") kayi products me mile to seedha pehla mat becho — choice do.
  val strong = ranked.filter { it.second >= 50 }
  if (best.second >= 50 && !(query.split(" ").size == 1 && strong.size > 1)) {
   val row = best.first
   return VoiceCmd.Sale(SaleOption(row.shopProduct.id, row.product.name, row.shopProduct.sellingPricePaise, qty))
  }
  return VoiceCmd.SaleChoice(query, ranked.take(3).map {
   SaleOption(it.first.shopProduct.id, it.first.product.name, it.first.shopProduct.sellingPricePaise, qty)
  })
 }
}
