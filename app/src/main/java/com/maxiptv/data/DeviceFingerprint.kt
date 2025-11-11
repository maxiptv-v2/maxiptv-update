package com.maxiptv.data

import android.content.Context
import android.os.Build
import android.util.DisplayMetrics
import java.util.Locale

data class DeviceFingerprintInfo(
  val key: String,
  val manufacturer: String,
  val brand: String,
  val model: String,
  val product: String,
  val sdkInt: Int,
  val widthPx: Int,
  val heightPx: Int,
  val densityDpi: Int,
  val density: Float
)

object DeviceFingerprint {
  fun collect(context: Context): DeviceFingerprintInfo {
    val metrics: DisplayMetrics = context.resources.displayMetrics
    val manufacturer = Build.MANUFACTURER ?: "unknown"
    val brand = Build.BRAND ?: manufacturer
    val model = Build.MODEL ?: "device"
    val product = Build.PRODUCT ?: model
    val sdk = Build.VERSION.SDK_INT

    val width = metrics.widthPixels
    val height = metrics.heightPixels
    val densityDpi = metrics.densityDpi
    val density = metrics.density

    val key = listOf(
      manufacturer.lowercase(Locale.getDefault()),
      brand.lowercase(Locale.getDefault()),
      model.lowercase(Locale.getDefault()),
      product.lowercase(Locale.getDefault()),
      "sdk$sdk",
      "${width}x$height",
      "dpi$densityDpi"
    ).joinToString("|")

    return DeviceFingerprintInfo(
      key = key,
      manufacturer = manufacturer,
      brand = brand,
      model = model,
      product = product,
      sdkInt = sdk,
      widthPx = width,
      heightPx = height,
      densityDpi = densityDpi,
      density = density
    )
  }
}
