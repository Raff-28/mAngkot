package com.rafif.m_angkot.ui.recommendation

import android.content.Context
import android.util.Log
import androidx.lifecycle.LifecycleOwner
import com.google.android.gms.maps.model.LatLng
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.google.maps.android.SphericalUtil
import com.rafif.m_angkot.db.MainDb
import com.rafif.m_angkot.network.*
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

interface RecommendationContract {
    fun isLoading(value: Boolean)

    fun onError(error: String)

    fun onGetRouteFromDb(value: List<RouteFromDb>)

    fun onGetRoute(distance: Double, route: Map<String, Any>)
}

class RecommendationPresenter(
    private val context: Context,
    private val lifecycleOwner: LifecycleOwner,
    private val contract: RecommendationContract
) {

    private val apiInterface = RetrofitInstance.instance.apiInterface
    private val repo = MainRepository(apiInterface)

    private val db = MainDb.getDb(context)
    private val dao = db?.mainDao
    private val routeFromDb = arrayListOf<RouteFromDb>()

    private var tempDistance = Double.MAX_VALUE
    private var tempRoute = listOf<List<Double>>()
    private var tempUserRoute = listOf<List<Double>>()
    private var tempRouteName = listOf<String>()
    private var tempTrayek = ""

    private var currentMinDistance = Double.MAX_VALUE
    private var currentSelectedRoute = mutableMapOf<String, Any>()

    private var currentItemIndex = 0
    private var filteredData = listOf<MutableMap<String, String>>()

    fun getAllRoute() {
        contract.isLoading(true)

        dao?.getRoutes()?.observe(lifecycleOwner) {
            if(!it.isNullOrEmpty()) {
                routeFromDb.clear()
                routeFromDb.addAll(it)
                contract.onGetRouteFromDb(it)
            }
        }
    }

    fun calculateRecommendation(
        isUserLocation: Boolean,
        pickPoint: LatLng,
        dropPoint: LatLng
    ) {
        contract.isLoading(true)

        val firestore = Firebase.firestore
        firestore
            .collection("route-list")
            .get()
            .addOnSuccessListener { snapshot ->
                val response = arrayListOf<MutableMap<String, String>>()
                //Looping response
                snapshot.documents.forEach {
                    val data = it.data as MutableMap<String, String>?

                    if (data != null) {
                        response.add(data)
                    }
                }

                doCalculation(isUserLocation, pickPoint, dropPoint, response)
            }
            .addOnFailureListener {
                contract.onError(it.message ?: "Something happened!")
            }
    }

    var isUserLocation = false
    var pickPoint: LatLng? = null
    var dropPoint: LatLng? = null
    var data = arrayListOf<MutableMap<String, String>>()

    private fun doCalculation(
        isUserLocation: Boolean,
        pickPoint: LatLng,
        dropPoint: LatLng,
        data: ArrayList<MutableMap<String, String>>
    ) {

        this.isUserLocation = isUserLocation
        this.pickPoint = pickPoint
        this.dropPoint = dropPoint
        this.data = data

        val mapData = arrayListOf<MutableMap<String, String>>()

        data.forEach {
            val splittedRoute = it["name"]!!.split(" - ")
            var routeCoordinate = ""

            splittedRoute.forEach {
                val copiedRouteFromDb = routeFromDb.toMutableList()
                copiedRouteFromDb.removeIf { routeFromDb ->
                    routeFromDb.name != it
                }

                routeCoordinate = if(routeCoordinate.isEmpty()) {
                    "${copiedRouteFromDb[0].lng}, ${copiedRouteFromDb[0].lat}"
                } else {
                    "$routeCoordinate - ${copiedRouteFromDb[0].lng}, ${copiedRouteFromDb[0].lat}"
                }
            }

            mapData.add(mutableMapOf(
                "name" to it["name"]!!,
                "trayek" to it["trayek"]!!,
                "lngLats" to routeCoordinate
            ))
        }

        val tempFiltered = arrayListOf<MutableMap<String, String>>()
        //Looping again response
        mapData.forEach {
            //Check if the response has drop point in its route and add it to list
            if (it["lngLats"]!!.contains("${dropPoint.longitude}, ${dropPoint.latitude}")) {
                tempFiltered.add(it)
            }
        }

        if (!isUserLocation) {
            //If user location is off, remove data that didn't have pick point in its route
            tempFiltered.removeIf {
                !it["lngLats"]!!.contains("${pickPoint.longitude}, ${pickPoint.latitude}")
            }
        }

        filteredData = tempFiltered

        if (filteredData.isEmpty()) {
            contract.onError("Route not found")
            return
        }

        //Split lng lats and name by " - "
        val splittedCoordinate =
            filteredData[currentItemIndex]["lngLats"]!!.split(" - ") as ArrayList
        val splittedRouteName = filteredData[currentItemIndex]["name"]!!.split(" - ") as ArrayList
        tempTrayek = filteredData[currentItemIndex]["trayek"] ?: ""

        //Find index of drop point from splitted lng lats
        val dropIndex =
            splittedCoordinate.indexOf("${dropPoint.longitude}, ${dropPoint.latitude}")

        val newCoordinates = splittedCoordinate.slice(0..dropIndex)
        tempRouteName = splittedRouteName.slice(0..dropIndex)

        if (newCoordinates.size < 2) {
            //Skip this looping if new coordinate only have 1 item
            contract.onGetRoute(currentMinDistance, currentSelectedRoute)
            return
        }

        //Convert coordinate to double
        val coordinatesDouble = arrayListOf<List<Double>>()
        newCoordinates.forEach {
            val splitted = it.split(", ")
            coordinatesDouble.add(
                listOf(splitted[0].toDouble(), splitted[1].toDouble())
            )
        }

        val coordinate = Coordinates(coordinatesDouble)
        getRouteDistance(pickPoint, coordinate)
    }

    private fun getRouteDistance(userPoint: LatLng, coordinates: Coordinates) {
        //Get route detail from ORS
        repo.getRoute(coordinates).enqueue(object : Callback<RouteModels> {
            override fun onResponse(call: Call<RouteModels>, response: Response<RouteModels>) {
                val responseData = response.body()
                if (responseData != null) {
                    //Store temporary minimum distance from response
                    tempDistance = responseData.features[0].properties.summary.distance

                    val route = responseData.features[0].geometry.coordinates
                    val routeLatLng = arrayListOf<LatLng>()

                    route.forEach {
                        routeLatLng.add(LatLng(it[1], it[0]))
                    }

                    //Find shortest coordinate from available coordinates to pick point
                    val shortestCoordinateBetweenUserAndResponse = findShortestCoordinate(
                        userPoint, routeLatLng
                    )["coordinate"] as LatLng
                    //Get shortest coordinate index from route lat lng
                    val shortestCoordinateIndex =
                        routeLatLng.indexOf(shortestCoordinateBetweenUserAndResponse)
                    val polies = arrayListOf<List<Double>>()

                    for (i: Int in shortestCoordinateIndex until routeLatLng.size) {
                        polies.add(listOf(routeLatLng[i].latitude, routeLatLng[i].longitude))
                    }

                    tempRoute = polies
                    val coordinate = tempRoute[0]

                    val coordinateLatLng = arrayListOf<LatLng>()
                    coordinates.coordinates.forEach {
                        coordinateLatLng.add(LatLng(it[1], it[0]))
                    }

                    //Get shortest route from current shortest to coordinate from db
                    val shortestRouteFromCurrentShortestToCoordinate = findShortestCoordinate(
                        shortestCoordinateBetweenUserAndResponse,
                        coordinateLatLng
                    )["coordinate"] as LatLng
                    val shortestRouteIndex = coordinateLatLng.indexOf(
                        shortestRouteFromCurrentShortestToCoordinate
                    )

                    tempRouteName = tempRouteName.slice(shortestRouteIndex until tempRouteName.size)
                    getUserRoute(
                        userPoint,
                        LatLng(coordinate[0], coordinate[1])
                    )
                } else {
                    contract.onError("Invalid pick/drop point")
                }
            }

            override fun onFailure(call: Call<RouteModels>, t: Throwable) {
                contract.onError(t.message.toString())
            }
        })
    }

    private fun getUserRoute(userLocation: LatLng, pickPoint: LatLng) {
        repo.getUserRoute(
            mapOf("start" to "${userLocation.longitude}, ${userLocation.latitude}"),
            mapOf("end" to "${pickPoint.longitude}, ${pickPoint.latitude}")
        )
            .enqueue(object : Callback<RouteModels> {
                override fun onResponse(call: Call<RouteModels>, response: Response<RouteModels>) {
                    val responseData = response.body()

                    if (responseData != null) {
                        tempDistance += responseData.features[0].properties.summary.distance
                        tempUserRoute = responseData.features[0].geometry.coordinates

                        if (tempDistance < currentMinDistance) {
                            currentMinDistance = tempDistance
                            currentSelectedRoute["routeName"] = tempRouteName
                            currentSelectedRoute["route"] = tempRoute
                            currentSelectedRoute["userRoute"] = tempUserRoute
                            currentSelectedRoute["trayek"] = tempTrayek
                        }

                        if (currentItemIndex == filteredData.lastIndex) {
                            contract.onGetRoute(currentMinDistance, currentSelectedRoute)
                        } else {
                            currentItemIndex += 1
                            doCalculation(
                                this@RecommendationPresenter.isUserLocation,
                                this@RecommendationPresenter.pickPoint!!,
                                this@RecommendationPresenter.dropPoint!!,
                                this@RecommendationPresenter.data
                            )
                        }
                    } else {
                        contract.onError("Something happened!")
                    }
                }

                override fun onFailure(call: Call<RouteModels>, t: Throwable) {
                    contract.onError(t.message.toString())
                }
            })
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