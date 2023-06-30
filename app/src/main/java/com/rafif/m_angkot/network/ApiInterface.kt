package com.rafif.m_angkot.network

import com.rafif.m_angkot.utils.Const
import retrofit2.Call
import retrofit2.http.*


interface ApiInterface {

    @Headers("Authorization:${Const.API_KEY}")
    @POST("v2/directions/driving-car/geojson")
    @JvmSuppressWildcards
    fun findUsers(
        @Body coordinates: Coordinates
    ): Call<RouteModels>

    @GET("v2/directions/driving-car")
    fun findUsers(
        @Query("api_key") apiKey: String,
        @QueryMap start: Map<String, String>,
        @QueryMap end: Map<String, String>
    ): Call<RouteModels>

    @Headers("Authorization:${Const.API_KEY}")
    @POST("v2/directions/driving-car")
    @JvmSuppressWildcards
    fun getDistance(
        @Body coordinates: Coordinates
    ): Call<DistanceModels>
}