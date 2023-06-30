package com.rafif.m_angkot.network

import com.rafif.m_angkot.utils.Const.API_KEY

class MainRepository(
    private val apiInterface: ApiInterface
) {
    fun getRoute(coordinates: Coordinates) =
        apiInterface.findUsers(coordinates)

    fun getUserRoute(start: Map<String, String>, end: Map<String, String>) =
        apiInterface.findUsers(API_KEY, start, end)

    fun getDistance(coordinates: Coordinates) =
        apiInterface.getDistance(coordinates)
}