package com.rafif.m_angkot.ui.routes

import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.rafif.m_angkot.databinding.ActivityRoutesBinding
import com.rafif.m_angkot.network.RouteFromDb
import com.rafif.m_angkot.network.RouteModels
import com.rafif.m_angkot.network.SearchModel
import com.rafif.m_angkot.utils.GeneralUtils
import com.rafif.m_angkot.utils.GeneralUtils.gone
import com.rafif.m_angkot.utils.GeneralUtils.visible


class RoutesActivity : AppCompatActivity(), RouteContract {
    private lateinit var binding: ActivityRoutesBinding
    private lateinit var presenter: RoutePresenter
    private lateinit var routeAdapter: RouteAdapter

    private var routeFromDb = arrayListOf<RouteFromDb>()

    private var trayeks = listOf<String>()
    private var splittedRouteNames = listOf<String>()

    private var route = ""
    private var selectedIndex = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityRoutesBinding.inflate(layoutInflater)
        setContentView(binding.root)

        presenter = RoutePresenter(this, this, this)
        presenter.getRoutes()

        routeAdapter = RouteAdapter { route, position ->
            this.route = route

            splittedRouteNames = route.split(" - ")
            routeFromDb.removeIf {
                it.name !in splittedRouteNames
            }

            routeFromDb = sortData(routeFromDb)
            presenter.getCoordinates(routeFromDb)
            selectedIndex = position
        }

        binding.rvRoutes.apply {
            layoutManager = LinearLayoutManager(this@RoutesActivity)
            this.adapter = routeAdapter
        }
    }

    override fun isLoading(value: Boolean) {
        if (value) {
            binding.pbRoutes.visible()
        } else {
            binding.pbRoutes.gone()
        }
    }

    override fun onError(error: String) {
        isLoading(false)
        GeneralUtils.showToast(this, error)
    }

    override fun onGetRoute(routes: List<String>, trayeks: List<String>, localRoutes: List<RouteFromDb>) {
        isLoading(false)

        routeAdapter.setData(routes)
        this.trayeks = trayeks

        this.routeFromDb.clear()
        this.routeFromDb.addAll(localRoutes)
    }

    override fun onGetRoutePoly(polies: RouteModels) {
        val returnIntent = Intent()
        val routeFromDbs = arrayListOf<RouteFromDb>()

        routeFromDb.forEachIndexed { index, value ->
            val routeFromDb = RouteFromDb(splittedRouteNames[index], value.lat, value.lng)

            routeFromDbs.add(routeFromDb)
        }

        val parsedData = SearchModel(
            polies,
            trayeks[selectedIndex],
            routeFromDbs
        )

        returnIntent.putExtra("data", parsedData)
        setResult(RESULT_OK, returnIntent)
        finish()
    }

    private fun sortData(datas: ArrayList<RouteFromDb>): ArrayList<RouteFromDb> {
        val result = arrayListOf<RouteFromDb>()
        var copiedData: MutableList<RouteFromDb>
        splittedRouteNames.forEach { name ->
            copiedData = datas.toMutableList()
            copiedData.removeIf {
                it.name != name
            }

            result.add(copiedData.first())
        }

        return result
    }
}