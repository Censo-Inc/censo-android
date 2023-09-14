package co.censo.vault.presentation.facetec_auth

import android.content.Context
import co.censo.vault.BuildConfig
import com.facetec.sdk.FaceTecSDK
import com.facetec.sdk.FaceTecSDK.InitializeCallback

object FaceTecConfig {
    fun initializeFaceTecSDKInDevelopmentMode(
        context: Context,
        deviceKeyId: String,
        publicKey: String,
        callback: InitializeCallback
    ) {
        FaceTecSDK.initializeInDevelopmentMode(context, deviceKeyId, publicKey, callback)
    }
}