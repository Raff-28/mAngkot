package com.rafif.m_angkot.ui.routes

import android.content.Context
import androidx.lifecycle.LifecycleOwner
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.rafif.m_angkot.db.MainDb
import com.rafif.m_angkot.network.*
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

interface RouteContract {
    fun isLoading(value: Boolean)

    fun onError(error: String)

    fun onGetRoute(routes: List<String>, trayeks: List<String>, localRoutes: List<RouteFromDb>)

    fun onGetRoutePoly(polies: RouteModels)
}

class RoutePresenter(
    private val context: Context,
    private val viewLifecycleOwner: LifecycleOwner,
    private val contract: RouteContract
) {
    private val apiInterface = RetrofitInstance.instance.apiInterface
    private val repo = MainRepository(apiInterface)

    private val db = MainDb.getDb(context)
    private val dao = db?.mainDao

    private val localRoutes = arrayListOf<RouteFromDb>()

    fun getRoutes() {
        contract.isLoading(true)

        dao?.getRoutes()?.observe(viewLifecycleOwner) {
            localRoutes.clear()
            localRoutes.addAll(it)

            if (!it.isNullOrEmpty()) {
                val firestore = Firebase.firestore
                firestore
                    .collection("route-list")
                    .get()
                    .addOnSuccessListener { snapshot ->
                        if (snapshot.isEmpty) {
                            contract.onError("No data found!")
                        } else {
                            val routes = arrayListOf<String>()
                            val trayeks = arrayListOf<String>()

                            snapshot.documents.forEach { snapshot1 ->
                                val data = snapshot1.data
                                routes.add(data!!["name"] as String)
                                trayeks.add(data["trayek"] as String)
                            }
                            contract.onGetRoute(routes, trayeks, localRoutes)
                        }
                    }
                    .addOnFailureListener { exception ->
                        contract.onError(exception.message ?: "Something happened!")
                    }
            }
        }
    }

    fun getCoordinates(localRoutes: List<RouteFromDb>) {
        contract.isLoading(true)

        val coordinates = arrayListOf<List<Double>>()
        localRoutes.forEach { document ->
            coordinates.add(listOf(document.lng, document.lat))
        }

        val coordinateObject = Coordinates(coordinates)
        getRoute(coordinateObject)
    }

    private fun getRoute(coordinates: Coordinates) {
        contract.isLoading(true)

        repo.getRoute(coordinates).enqueue(object : Callback<RouteModels> {
            override fun onResponse(call: Call<RouteModels>, response: Response<RouteModels>) {
                if (response.body() != null) {
                    contract.onGetRoutePoly(response.body()!!)
                } else {
                    contract.onError("Something happened!")
                }
            }

            override fun onFailure(call: Call<RouteModels>, t: Throwable) {
                contract.onError(t.message.toString())
            }
        })
    }
}