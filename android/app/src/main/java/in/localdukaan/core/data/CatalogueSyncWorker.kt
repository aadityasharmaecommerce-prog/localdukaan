package `in`.localdukaan.core.data
import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import `in`.localdukaan.LocalDukaanApp
class CatalogueSyncWorker(context:Context,params:WorkerParameters):CoroutineWorker(context,params){override suspend fun doWork():Result=try{(applicationContext as LocalDukaanApp).repository.syncPending();Result.success()}catch(_:Exception){Result.retry()}}
