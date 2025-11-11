package com.maxiptv.ui.theme

import android.content.Context
import android.view.View
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.ui.unit.LayoutDirection

data class SafeAreaSnapshot(
  val version: Int,
  val profile: String,
  val topDp: Float,
  val bottomDp: Float,
  val startDp: Float,
  val endDp: Float,
  val topPx: Float,
  val bottomPx: Float,
  val startPx: Float,
  val endPx: Float,
  val scaleFactor: Float,
  val diagonalInches: Float,
  val density: Float,
  val screenWidthDp: Int,
  val screenHeightDp: Int,
  val updatedAt: Long
)

object SafeAreaMetrics {
  private const val PREF_NAME = "safe_area_metrics"
  private const val KEY_VERSION = "version"
  private const val KEY_PROFILE = "profile"
  private const val KEY_TOP_DP = "top_dp"
  private const val KEY_BOTTOM_DP = "bottom_dp"
  private const val KEY_START_DP = "start_dp"
  private const val KEY_END_DP = "end_dp"
  private const val KEY_TOP_PX = "top_px"
  private const val KEY_BOTTOM_PX = "bottom_px"
  private const val KEY_START_PX = "start_px"
  private const val KEY_END_PX = "end_px"
  private const val KEY_SCALE = "scale_factor"
  private const val KEY_DIAGONAL = "diagonal_inches"
  private const val KEY_UPDATED_AT = "updated_at"
  private const val KEY_DENSITY = "density"
  private const val KEY_WIDTH_DP = "width_dp"
  private const val KEY_HEIGHT_DP = "height_dp"
  private const val CURRENT_VERSION = 1

  fun save(
    context: Context,
    padding: PaddingValues,
    scaleFactor: Float,
    diagonalInches: Double,
    profile: String
  ) {
    val resources = context.resources
    val metrics = resources.displayMetrics
    val density = metrics.density
    val configuration = resources.configuration

    val layoutDirection = if (configuration.layoutDirection == View.LAYOUT_DIRECTION_RTL) {
      LayoutDirection.Rtl
    } else {
      LayoutDirection.Ltr
    }

    val topDp = padding.calculateTopPadding().value
    val bottomDp = padding.calculateBottomPadding().value
    val startDp = padding.calculateLeftPadding(layoutDirection).value
    val endDp = padding.calculateRightPadding(layoutDirection).value

    val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
    prefs.edit()
      .putInt(KEY_VERSION, CURRENT_VERSION)
      .putString(KEY_PROFILE, profile)
      .putFloat(KEY_TOP_DP, topDp)
      .putFloat(KEY_BOTTOM_DP, bottomDp)
      .putFloat(KEY_START_DP, startDp)
      .putFloat(KEY_END_DP, endDp)
      .putFloat(KEY_TOP_PX, topDp * density)
      .putFloat(KEY_BOTTOM_PX, bottomDp * density)
      .putFloat(KEY_START_PX, startDp * density)
      .putFloat(KEY_END_PX, endDp * density)
      .putFloat(KEY_SCALE, scaleFactor)
      .putFloat(KEY_DIAGONAL, diagonalInches.toFloat())
      .putFloat(KEY_DENSITY, density)
      .putInt(KEY_WIDTH_DP, configuration.screenWidthDp)
      .putInt(KEY_HEIGHT_DP, configuration.screenHeightDp)
      .putLong(KEY_UPDATED_AT, System.currentTimeMillis())
      .apply()
  }

  fun snapshot(context: Context): SafeAreaSnapshot? {
    val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
    val version = prefs.getInt(KEY_VERSION, 0)
    if (version == 0) return null

    val density = prefs.getFloat(KEY_DENSITY, 0f).takeIf { it > 0f } ?: context.resources.displayMetrics.density

    return SafeAreaSnapshot(
      version = version,
      profile = prefs.getString(KEY_PROFILE, "unknown") ?: "unknown",
      topDp = prefs.getFloat(KEY_TOP_DP, 0f),
      bottomDp = prefs.getFloat(KEY_BOTTOM_DP, 0f),
      startDp = prefs.getFloat(KEY_START_DP, 0f),
      endDp = prefs.getFloat(KEY_END_DP, 0f),
      topPx = prefs.getFloat(KEY_TOP_PX, 0f),
      bottomPx = prefs.getFloat(KEY_BOTTOM_PX, 0f),
      startPx = prefs.getFloat(KEY_START_PX, 0f).takeIf { it != 0f } ?: (prefs.getFloat(KEY_START_DP, 0f) * density),
      endPx = prefs.getFloat(KEY_END_PX, 0f).takeIf { it != 0f } ?: (prefs.getFloat(KEY_END_DP, 0f) * density),
      scaleFactor = prefs.getFloat(KEY_SCALE, 1f),
      diagonalInches = prefs.getFloat(KEY_DIAGONAL, 0f),
      density = density,
      screenWidthDp = prefs.getInt(KEY_WIDTH_DP, context.resources.configuration.screenWidthDp),
      screenHeightDp = prefs.getInt(KEY_HEIGHT_DP, context.resources.configuration.screenHeightDp),
      updatedAt = prefs.getLong(KEY_UPDATED_AT, 0L)
    )
  }
}

