package `in`.localdukaan
import android.app.Application
import `in`.localdukaan.core.data.*
import `in`.localdukaan.core.database.LocalDatabase
class LocalDukaanApp:Application(){lateinit var repository:AppRepository;override fun onCreate(){super.onCreate();repository=AppRepository(this,LocalDatabase.create(this),SessionStore(this))}}
