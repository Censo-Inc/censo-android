package co.censo.censo

import android.app.Application
import co.censo.shared.util.BIP39
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class CensoApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        BIP39.setup(applicationContext)
    }
}
