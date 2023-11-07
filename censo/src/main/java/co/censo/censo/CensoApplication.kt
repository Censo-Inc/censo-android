package co.censo.censo

import android.app.Application
import com.raygun.raygun4android.RaygunClient
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch

@HiltAndroidApp
class CensoApplication : Application()
