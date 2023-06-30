package com.rafif.m_angkot.ui.recommendation

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.ArrayAdapter
import androidx.appcompat.app.AppCompatActivity
import com.google.android.gms.maps.model.LatLng
import com.rafif.m_angkot.databinding.ActivityRecommendationBinding
import com.rafif.m_angkot.network.RecommendationModel
import com.rafif.m_angkot.network.RouteFromDb
import com.rafif.m_angkot.utils.GPSTracker
import com.rafif.m_angkot.utils.GeneralUtils
import com.rafif.m_angkot.utils.GeneralUtils.showToast

class RecommendationActivity : AppCompatActivity(), RecommendationContract {
    private lateinit var binding: ActivityRecommendationBinding
    private lateinit var presenter: RecommendationPresenter

    private var routes = arrayListOf<RouteFromDb>()

    private var latLng: LatLng? = null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityRecommendationBinding.inflate(layoutInflater)
        setContentView(binding.root)

        presenter = RecommendationPresenter(this, this, this)
        presenter.getAllRoute()

        val gps = GPSTracker(this)

        if (GeneralUtils.isGPSOn(this)) {
            binding.ctUseCurrent.visibility = View.VISIBLE
            gps.getLocation().observe(this) {
                latLng = LatLng(it.latitude, it.longitude)
            }
        }
        binding.btnGetRecommendation.setOnClickListener {
            onFindClick()
        }
    }

    override fun isLoading(value: Boolean) {
        if (value) {
            binding.pbRecommend.visibility = View.VISIBLE
        } else {
            binding.pbRecommend.visibility = View.GONE
        }
    }

    override fun onError(error: String) {
        isLoading(false)
        showToast(this, error)
    }

    override fun onGetRouteFromDb(value: List<RouteFromDb>) {
        isLoading(false)

        routes.clear()

        val labels = arrayListOf<String>()
        value.forEach {
            if (!labels.contains(it.name)) {
                labels.add(it.name)
                routes.add(it)
            }
        }

        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, labels)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)

        binding.spPick.adapter = adapter
        binding.spDrop.adapter = adapter
    }

    override fun onGetRoute(distance: Double, route: Map<String, Any>) {
        val returnIntent = Intent()
        val routeName = route["routeName"] as List<String>
        val model = RecommendationModel(
            routeName.joinToString { "$it - " },
            route["route"] as List<List<Double>>,
            route["userRoute"] as List<List<Double>>,
            distance,
            route["trayek"] as String
        )

        returnIntent.putExtra("data", model)
        setResult(RESULT_OK, returnIntent)
        finish()
    }

    private fun onFindClick() {
        val pickRoute = routes[binding.spPick.selectedItemPosition]
        val dropRoute = routes[binding.spDrop.selectedItemPosition]
        val isUserLocation = binding.ctUseCurrent.isChecked

        if (pickRoute == dropRoute) {
            onError("Invalid pick & drop point!")
            return
        }

        val pickLatLng = if (latLng != null && isUserLocation) {
            latLng!!
        } else {
            LatLng(pickRoute.lat, pickRoute.lng)
        }

        val dropLatLng = LatLng(dropRoute.lat, dropRoute.lng)
        presenter.calculateRecommendation(isUserLocation, pickLatLng, dropLatLng)
    }
}