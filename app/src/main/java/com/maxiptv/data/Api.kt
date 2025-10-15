package com.maxiptv.data
import retrofit2.http.GET
import retrofit2.http.Query

interface XtreamApi {
  @GET("player_api.php") suspend fun auth(@Query("username") u: String, @Query("password") p: String): AuthResponse
  @GET("player_api.php") suspend fun liveCategories(@Query("username") u: String, @Query("password") p: String, @Query("action") a: String = "get_live_categories"): List<LiveCategory>
  @GET("player_api.php") suspend fun liveStreams(@Query("username") u: String, @Query("password") p: String, @Query("action") a: String = "get_live_streams"): List<LiveStream>
  @GET("player_api.php") suspend fun vodCategories(@Query("username") u: String, @Query("password") p: String, @Query("action") a: String = "get_vod_categories"): List<VodCategory>
  @GET("player_api.php") suspend fun vodStreams(@Query("username") u: String, @Query("password") p: String, @Query("action") a: String = "get_vod_streams"): List<VodItem>
  @GET("player_api.php") suspend fun vodInfo(@Query("username") u: String, @Query("password") p: String, @Query("action") a: String = "get_vod_info", @Query("vod_id") vodId: Int): VodInfoResponse
  @GET("player_api.php") suspend fun seriesCategories(@Query("username") u: String, @Query("password") p: String, @Query("action") a: String = "get_series_categories"): List<SeriesCategory>
  @GET("player_api.php") suspend fun series(@Query("username") u: String, @Query("password") p: String, @Query("action") a: String = "get_series"): List<SeriesItem>
  @GET("player_api.php") suspend fun seriesInfo(@Query("username") u: String, @Query("password") p: String, @Query("action") a: String = "get_series_info", @Query("series_id") seriesId: Int): SeriesInfoResponse
}
