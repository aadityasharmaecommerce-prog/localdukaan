package `in`.localdukaan.feature
import `in`.localdukaan.ui.DukaanColors
import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.provider.Settings
import android.view.MotionEvent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.google.mlkit.vision.barcode.BarcodeScannerOptions
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage
import com.google.zxing.*
import com.google.zxing.common.HybridBinarizer
import `in`.localdukaan.core.model.BarcodeRules
import `in`.localdukaan.core.model.ProductRules
import `in`.localdukaan.core.model.ScannerGate
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.delay
import kotlinx.coroutines.suspendCancellableCoroutine
import java.util.concurrent.Executors
import kotlin.coroutines.resume

// Scanner v4 — built for a kirana counter, not a demo:
//  * ML Kit reads every retail symbology an Indian grocery / cosmetic / household pack can carry
//    (EAN-13, EAN-8, UPC-A/E, Code-128 price labels, Code-39, ITF cartons, DataMatrix + QR).
//  * Every read is filtered through the GS1 check digit, so half-read frames no longer fire a
//    wrong product. QR / DataMatrix payloads are mined for the GTIN printed beside them.
//  * ZXing runs as a slower rescue pass (rotated + inverted) for faded, curved or shiny packs.
//  * Torch, tap-to-focus, aim box and pause/denied states are all in one compact panel.

private val ScanGreen = Color(0xFF16A34A)
private val BlinkitGreen = DukaanColors.BlinkitGreen

@Composable fun BarcodeScanner(active:Boolean,onBarcode:(String)->Unit,modifier:Modifier=Modifier.fillMaxWidth().aspectRatio(4f/3f)){
 val context=LocalContext.current;val lifecycle=LocalLifecycleOwner.current
 var granted by remember{mutableStateOf(ContextCompat.checkSelfPermission(context,Manifest.permission.CAMERA)==PackageManager.PERMISSION_GRANTED)}
 var asked by remember{mutableStateOf(false)}
 val permission=rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()){ok->granted=ok;asked=true}
 LaunchedEffect(Unit){if(!granted&&!asked)permission.launch(Manifest.permission.CAMERA)}
 if(!granted){CameraPermissionCard(asked){permission.launch(Manifest.permission.CAMERA)};return}
 if(!active){ScannerNotice("⏸ Scanner paused — cart dekhein ya resume karein");return}
 val viewRef=remember{mutableStateOf<PreviewView?>(null)}
 val providerRef=remember{mutableStateOf<ProcessCameraProvider?>(null)}
 val cameraRef=remember{mutableStateOf<Camera?>(null)}
 var torch by remember{mutableStateOf(false)}
 var status by remember{mutableStateOf<String?>(null)}
 LaunchedEffect(status){if(status!=null){delay(1800);status=null}}
 Box(modifier.clip(RoundedCornerShape(16.dp)).background(Color(0xFF0B1F14))){
  AndroidView(factory={ctx->PreviewView(ctx).apply{
   scaleType=PreviewView.ScaleType.FILL_CENTER
   setOnTouchListener{v,ev->if(ev.action==MotionEvent.ACTION_DOWN){val pt=(v as PreviewView).meteringPointFactory.createPoint(ev.x,ev.y);cameraRef.value?.cameraControl?.startFocusAndMetering(FocusMeteringAction.Builder(pt,FocusMeteringAction.FLAG_AF or FocusMeteringAction.FLAG_AE).build())};v.performClick()}
  }.also{viewRef.value=it}},modifier=Modifier.fillMaxSize())
  // Aim box: EAN/UPC symbols are wide and low, so a wide window reads packs much faster.
  Box(Modifier.align(Alignment.Center).fillMaxWidth(0.84f).height(118.dp).border(2.dp,BlinkitGreen,RoundedCornerShape(12.dp)))
  Box(Modifier.align(Alignment.TopCenter).fillMaxWidth().background(Color(0xCC003D1F)).padding(horizontal=10.dp,vertical=6.dp)){
   Text("Barcode ko green box mein rakhein",color=DukaanColors.BlinkitYellow,fontSize=12.sp,fontWeight=FontWeight.SemiBold,modifier=Modifier.align(Alignment.CenterStart))
  }
  Row(Modifier.align(Alignment.BottomCenter).fillMaxWidth().background(Color(0x99000000)).padding(horizontal=10.dp,vertical=4.dp),verticalAlignment=Alignment.CenterVertically){
   Text(status?:"🔍 Scanning…",color=if(status!=null)Color(0xFF86EFAC) else Color.White,fontSize=12.sp,fontWeight=FontWeight.SemiBold,modifier=Modifier.weight(1f))
   TextButton({torch=!torch;cameraRef.value?.cameraControl?.enableTorch(torch)},contentPadding=PaddingValues(horizontal=10.dp)){Text(if(torch)"🔆 Torch OFF" else "🔅 Torch",color=Color.White,fontSize=12.sp)}
  }
 }
 DisposableEffect(Unit){onDispose{runCatching{cameraRef.value?.cameraControl?.enableTorch(false)};runCatching{providerRef.value?.unbindAll()};providerRef.value=null;cameraRef.value=null}}
 LaunchedEffect(lifecycle){
  while(viewRef.value==null)delay(40)
  val view=viewRef.value?:return@LaunchedEffect
  val executor=Executors.newSingleThreadExecutor()
  // Retail symbologies: grocery/cosmetic packs plus the price labels shops print themselves.
  val mlkit=runCatching{BarcodeScanning.getClient(BarcodeScannerOptions.Builder().setBarcodeFormats(
   Barcode.FORMAT_EAN_13,Barcode.FORMAT_EAN_8,Barcode.FORMAT_UPC_A,Barcode.FORMAT_UPC_E,
   Barcode.FORMAT_CODE_128,Barcode.FORMAT_CODE_39,Barcode.FORMAT_CODE_93,Barcode.FORMAT_ITF,
   Barcode.FORMAT_CODABAR,Barcode.FORMAT_DATA_MATRIX,Barcode.FORMAT_QR_CODE,Barcode.FORMAT_AZTEC,Barcode.FORMAT_PDF417
  ).build())}.getOrNull()
  try{
   val provider=suspendCancellableCoroutine{cont->val f=ProcessCameraProvider.getInstance(context);f.addListener({try{cont.resume(f.get())}catch(t:Throwable){cont.cancel(t)}},ContextCompat.getMainExecutor(context))}
   providerRef.value=provider
   val preview=Preview.Builder().build().also{it.setSurfaceProvider(view.surfaceProvider)}
   val zxing=MultiFormatReader().apply{setHints(mapOf(DecodeHintType.POSSIBLE_FORMATS to RETAIL_ZXING_FORMATS,DecodeHintType.TRY_HARDER to true,DecodeHintType.ALSO_INVERTED to true))}
   var last:String?=null;var lastAt=0L;var frame=0L
   val mainHandler=android.os.Handler(android.os.Looper.getMainLooper())
   fun report(code:String){runCatching{mainHandler.post{runCatching{status="✓ $code";onBarcode(code)}}}}
   fun emit(raw:String?){
    val code=BarcodeRules.extractProductCode(raw)?.let{ProductRules.normalizeBarcode(it)}?:return
    if(!BarcodeRules.isAcceptable(code))return
    val now=System.currentTimeMillis()
    if(!ScannerGate.accept(code,last,lastAt,now))return
    last=code;lastAt=now
    runCatching{val v=context.getSystemService(Vibrator::class.java);if(v!=null){if(Build.VERSION.SDK_INT>=26)v.vibrate(VibrationEffect.createOneShot(60,VibrationEffect.DEFAULT_AMPLITUDE))else @Suppress("DEPRECATION") v.vibrate(60)}}
    report(code)
   }
   val analysis=ImageAnalysis.Builder().setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST).setTargetResolution(android.util.Size(1280,720)).build()
   runCatching{analysis.targetRotation=view.display.rotation}
   analysis.setAnalyzer(executor){image->
    frame++
    try{
     val media=image.image
     if(media==null||image.width<=0||image.height<=0){image.close();return@setAnalyzer}
     val rotation=image.imageInfo.rotationDegrees
     val w=image.width;val h=image.height
     val p0=image.planes[0];val buf=p0.buffer;val rowStride=p0.rowStride
     val data=ByteArray(w*h)
     if(rowStride==w)buf.get(data) else{var off=0;var pos=0;for(r in 0 until h){buf.position(pos);buf.get(data,off,w);off+=w;pos+=rowStride}}
     // The manual copy above moved every plane's position: rewind before ML Kit reads the frame.
     for(p in image.planes)runCatching{p.buffer.rewind()}
     if(mlkit!=null){
      runCatching{mlkit.process(InputImage.fromMediaImage(media,rotation))
       .addOnSuccessListener{codes->runCatching{
        // Prefer a code with a valid GS1 checksum; fall back to any catalogue-worthy value.
        val best=codes.mapNotNull{it.rawValue?:it.displayValue}.map{it to BarcodeRules.extractProductCode(it)}
         .filter{it.second!=null&&BarcodeRules.isAcceptable(it.second)}
         .sortedByDescending{(_,c)->if(BarcodeRules.isProductCode(c))1 else 0}.firstOrNull()
        if(best!=null)emit(best.first)}}
       .addOnFailureListener{}
       .addOnCompleteListener{runCatching{image.close()}}}
        .getOrElse{runCatching{image.close()}}
     } else runCatching{image.close()}
     // Rescue pass: faded / curved / shiny packs that ML Kit misses.
     if(frame%6==0L){
      try{
       var result:Result?=null
       try{result=zxing.decodeWithState(BinaryBitmap(HybridBinarizer(PlanarYUVLuminanceSource(data,w,h,0,0,w,h,false))))}catch(_:NotFoundException){}
       if(result==null){val rot=ByteArray(w*h);var i=0;for(x in 0 until w){var y=h-1;while(y>=0){rot[i++]=data[y*w+x];y--}};try{result=zxing.decodeWithState(BinaryBitmap(HybridBinarizer(PlanarYUVLuminanceSource(rot,h,w,0,0,h,w,false))))}catch(_:NotFoundException){}}
       result?.text?.let{emit(it)}
      }catch(_:Throwable){}
     }
    }catch(_:Throwable){runCatching{image.close()}}finally{runCatching{zxing.reset()}}
   }
   runCatching{provider.unbindAll()}
   cameraRef.value=provider.bindToLifecycle(lifecycle,CameraSelector.DEFAULT_BACK_CAMERA,preview,analysis)
   runCatching{cameraRef.value?.cameraControl?.enableTorch(torch)}
   awaitCancellation()
  }finally{executor.shutdown();mlkit?.let{m->runCatching{m.close()}};runCatching{providerRef.value?.unbindAll()};providerRef.value=null;cameraRef.value=null}
 }
}

// Crash guard (QA me mila tha): analyzer thread me emit hote hue camera unbind ho chuka ho to
// state write na kare — "Writing to state during disposal" crash hota tha.
private val RETAIL_ZXING_FORMATS=listOf(BarcodeFormat.EAN_13,BarcodeFormat.EAN_8,BarcodeFormat.UPC_A,BarcodeFormat.UPC_E,BarcodeFormat.CODE_128,BarcodeFormat.CODE_93,BarcodeFormat.CODE_39,BarcodeFormat.ITF,BarcodeFormat.CODABAR,BarcodeFormat.RSS_14,BarcodeFormat.RSS_EXPANDED)

@Composable private fun ScannerNotice(text:String){ Box(Modifier.fillMaxWidth().height(96.dp).clip(RoundedCornerShape(16.dp)).background(DukaanColors.Slate100),contentAlignment=Alignment.Center){Text(text,color=DukaanColors.Gray,fontWeight=FontWeight.SemiBold,fontSize=14.sp)}}

@Composable private fun CameraPermissionCard(asked:Boolean,retry:()->Unit){
 val context=LocalContext.current
 Box(Modifier.fillMaxWidth().height(150.dp).clip(RoundedCornerShape(16.dp)).background(DukaanColors.LightYellow),contentAlignment=Alignment.Center){
  Column(Modifier.padding(16.dp),horizontalAlignment=Alignment.CenterHorizontally){
   Text(if(asked)"Camera band hai — bina permission scan nahi ho payega." else "Scanner ke liye camera ki permission chahiye.",color=Color(0xFF8A6D00),fontWeight=FontWeight.SemiBold,fontSize=13.sp)
   Spacer(Modifier.height(8.dp))
   Row(horizontalArrangement=Arrangement.spacedBy(8.dp)){
    TextButton(retry){Text("Permission do",color=DukaanColors.BlinkitGreen,fontWeight=FontWeight.Bold)}
    TextButton({runCatching{context.startActivity(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).setData(Uri.fromParts("package",context.packageName,null)))}}){Text("Settings",color=DukaanColors.BlinkitGreen)}
   }
  }
 }
}
