package co.censo.vault

import android.app.Application
import co.censo.vault.storage.SharedPrefsHelper
import dagger.hilt.android.HiltAndroidApp


@HiltAndroidApp
class VaultApplication : Application() {

    override fun onCreate() {
        super.onCreate()
        SharedPrefsHelper.setup(this)
    }
}