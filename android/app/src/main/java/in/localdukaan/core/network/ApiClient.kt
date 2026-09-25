package `in`.localdukaan.core.network
import `in`.localdukaan.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
class ApiException(val status:Int,val code:String,message:String):Exception(message)
class ApiClient(private val token:()->String?){
 // Har call apna connection band karta hai. Pehle disconnect nahi hota tha, isliye lambi billing
 // session mein sockets/fd jama hote jaate the (scanner ek-ek barcode par lookup bhejta hai).
 private fun open(path:String)=(URL(BuildConfig.API_BASE_URL.trimEnd('/')+path).openConnection() as HttpURLConnection)
 private fun parse(c:HttpURLConnection,status:Int):JSONObject{
  val text=(if(status in 200..299)c.inputStream else c.errorStream).bufferedReader().use{it.readText()}
  val out=if(text.isBlank())JSONObject() else JSONObject(text)
  if(status !in 200..299){val e=out.optJSONObject("error");throw ApiException(status,e?.optString("code")?:"HTTP_ERROR",e?.optString("message")?:"Request failed")}
  return out
 }
 suspend fun call(path:String,method:String="GET",data:JSONObject?=null,authenticated:Boolean=false):JSONObject=withContext(Dispatchers.IO){
  val c=open(path).apply{requestMethod=method;connectTimeout=15_000;readTimeout=15_000;setRequestProperty("Content-Type","application/json");setRequestProperty("Accept","application/json");if(authenticated)setRequestProperty("Authorization","Bearer ${token() ?: throw ApiException(401,"NO_SESSION","Login required")}");if(data!=null){doOutput=true;outputStream.use{it.write(data.toString().toByteArray())}}}
  try{val status=c.responseCode;if(status==204)JSONObject() else parse(c,status)}finally{runCatching{c.disconnect()}}
 }
 suspend fun download(path:String):ByteArray=withContext(Dispatchers.IO){val c=open(path).apply{requestMethod="GET";connectTimeout=15_000;readTimeout=20_000;setRequestProperty("Authorization","Bearer ${token() ?: throw ApiException(401,"NO_SESSION","Login required")}")}
  try{val status=c.responseCode;if(status !in 200..299)throw ApiException(status,"IMAGE_DOWNLOAD_FAILED","Could not load image");c.inputStream.use{it.readBytes()}}finally{runCatching{c.disconnect()}}}
 suspend fun upload(path:String,bytes:ByteArray,mime:String,mutationId:String):JSONObject=withContext(Dispatchers.IO){val c=open(path).apply{requestMethod="POST";connectTimeout=20_000;readTimeout=30_000;doOutput=true;setFixedLengthStreamingMode(bytes.size);setRequestProperty("Content-Type",mime);setRequestProperty("Idempotency-Key",mutationId);setRequestProperty("Authorization","Bearer ${token() ?: throw ApiException(401,"NO_SESSION","Login required")}");outputStream.use{it.write(bytes)}}
  try{parse(c,c.responseCode)}finally{runCatching{c.disconnect()}}}
}
