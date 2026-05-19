package com.technosaurus.MagicGamepad.components

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.google.android.gms.ads.AdListener
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.AdView
import com.google.android.gms.ads.LoadAdError

@Composable
fun AdaptiveBannerAd(adId: String) {

    val context = LocalContext.current
    val configuration = LocalConfiguration.current
    val screenWidth = configuration.screenWidthDp

    var isAdLoaded by remember {
        mutableStateOf(false)
    }

    val adView = remember {

        AdView(context).apply {

            adUnitId = adId

            setAdSize(
                AdSize.getInlineAdaptiveBannerAdSize(
                    screenWidth,
                    100
                )
            )

            adListener = object : AdListener() {

                override fun onAdLoaded() {
                    isAdLoaded = true
                }

                override fun onAdFailedToLoad(error: LoadAdError) {
                    isAdLoaded = false
                }
            }

            loadAd(AdRequest.Builder().build())
        }
    }

    DisposableEffect(Unit) {

        onDispose {
            adView.destroy()
        }
    }

    if (isAdLoaded) {
        AndroidView(
            factory = { adView }
        )
    } else {
        Spacer(
            modifier = Modifier.Companion.height(0.dp)
        )
    }
}