package com.rafif.m_angkot.ui.home

import android.location.Location
import android.util.Log
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.SphericalUtil
import com.rafif.m_angkot.network.Coordinates
import com.rafif.m_angkot.network.MainRepository
import com.rafif.m_angkot.network.RetrofitInstance
import com.rafif.m_angkot.network.RouteModels
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

interface HomeContract {
    fun onError(error: String)

    fun isLoading(value: Boolean)

    fun onGetRoutePoly(polies: ArrayList<LatLng>, routeNames: List<String>, distance: Double)

    fun onGetUserRoutePoly(polies: ArrayList<LatLng>, start: LatLng, end: LatLng)
}

class HomePresenter(
    private val contract: HomeContract,
) {
    private val apiInterface = RetrofitInstance.instance.apiInterface
    private val repo = MainRepository(apiInterface)

    fun getRoute(
        start: LatLng,
        end: LatLng,
        routes: List<LatLng>,
        polies: ArrayList<LatLng>,
        routeNames: List<String>
    ) {
        contract.isLoading(true)
        val newRoutes: ArrayList<LatLng>
        val newRouteNames: List<String>

        //Find shortest coordinate from polly on map to start
        val shortest = findShortestCoordinate(start, polies)["coordinate"] as LatLng
        val shortestFromRoute = findShortestCoordinate(shortest, routes)["coordinate"] as LatLng
        var coordinateIndex = routes.indexOf(shortestFromRoute)
        val endIndex = routes.indexOf(end)

        if(coordinateIndex > endIndex) {
            if (endIndex - 1 >= 0) {
                coordinateIndex = 0
            } else {
                contract.onError("Route not found!")
            }
        }

        newRoutes = routes.slice(coordinateIndex..endIndex) as ArrayList<LatLng>
        newRoutes[0] = routes[coordinateIndex]
        newRouteNames = routeNames.slice(coordinateIndex..endIndex)

        val newRoutesDouble = arrayListOf<List<Double>>()
        newRoutes.forEach {
            newRoutesDouble.add(listOf(it.longitude, it.latitude))
        }
        val coordinates = Coordinates(newRoutesDouble)
        //Get data from ORS
        repo.getRoute(coordinates).enqueue(object : Callback<RouteModels> {
            override fun onResponse(call: Call<RouteModels>, response: Response<RouteModels>) {
                if (response.body() != null) {
                    val responseData = response.body()
                    val distance = responseData?.features?.get(0)?.properties?.summary?.distance
                    contract.onGetRoutePoly(
                        parseResponse(responseData),
                        newRouteNames,
                        distance ?: 1.0
                    )

                    val firstRoute = responseData!!.features[0].geometry.coordinates.first()
                    val firstLatLng = LatLng(firstRoute[1], firstRoute[0])

                    val endRoute = responseData.features[0].geometry.coordinates.last()
                    val endLatLng = LatLng(endRoute[1], endRoute[0])

                    getUserRoute(start, firstLatLng, endLatLng)
                } else {
                    contract.onError("Something happened!")
                }
            }

            override fun onFailure(call: Call<RouteModels>, t: Throwable) {
                contract.onError(t.message.toString())
            }
        })
    }

    private fun getUserRoute(userLocation: LatLng, pickPoint: LatLng, dropPoint: LatLng) {
        repo.getUserRoute(
            mapOf("start" to "${userLocation.longitude}, ${userLocation.latitude}"),
            mapOf("end" to "${pickPoint.longitude}, ${pickPoint.latitude}")
        )
            .enqueue(object : Callback<RouteModels> {
                override fun onResponse(call: Call<RouteModels>, response: Response<RouteModels>) {
                    if (response.body() != null) {
                        val responseData = response.body()
                        contract.onGetUserRoutePoly(
                            parseResponse(responseData),
                            pickPoint,
                            dropPoint
                        )
                    } else {
                        contract.onError("Something happened!")
                    }
                }

                override fun onFailure(call: Call<RouteModels>, t: Throwable) {
                    contract.onError(t.message.toString())
                }
            })
    }

    private fun parseResponse(routeModels: RouteModels?): ArrayList<LatLng> {
        val features = routeModels?.features?.get(0)
        val geometry = features?.geometry
        val coordinates = geometry?.coordinates
        val parsedCoordinates = arrayListOf<LatLng>()

        if (coordinates != null) {
            for (item: List<Double> in coordinates) {
                parsedCoordinates.add(LatLng(item[1], item[0]))
            }
        }

        return parsedCoordinates
    }

    private fun findShortestCoordinate(start: LatLng, polies: List<LatLng>): Map<String, Any> {
        var shortestDistance = Double.MAX_VALUE
        var shortestCoordinate = polies.last()
        polies.forEach { point ->
            val distance = SphericalUtil.computeDistanceBetween(start, point)
            if (distance < shortestDistance) {
                shortestDistance = distance
                shortestCoordinate = point
            }
        }

        return mapOf(
            "distance" to shortestDistance,
            "coordinate" to shortestCoordinate
        )
    }
}