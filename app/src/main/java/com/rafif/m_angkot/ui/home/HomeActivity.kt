package com.rafif.m_angkot.ui.home

import android.Manifest
import android.annotation.SuppressLint
import android.app.*
import android.content.Intent
import android.graphics.BitmapFactory
import android.graphics.Color
import android.location.Location
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.View
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.*
import com.karumi.dexter.Dexter
import com.karumi.dexter.MultiplePermissionsReport
import com.karumi.dexter.PermissionToken
import com.karumi.dexter.listener.PermissionRequest
import com.karumi.dexter.listener.multi.MultiplePermissionsListener
import com.rafif.m_angkot.R
import com.rafif.m_angkot.databinding.ActivityHomeBinding
import com.rafif.m_angkot.network.RecommendationModel
import com.rafif.m_angkot.network.RouteFromDb
import com.rafif.m_angkot.network.RouteModels
import com.rafif.m_angkot.network.SearchModel
import com.rafif.m_angkot.ui.login.MainActivity
import com.rafif.m_angkot.ui.recommendation.RecommendationActivity
import com.rafif.m_angkot.ui.routes.RoutesActivity
import com.rafif.m_angkot.utils.Const
import com.rafif.m_angkot.utils.Const.LOGGED_IN
import com.rafif.m_angkot.utils.GPSTracker
import com.rafif.m_angkot.utils.GeneralUtils.gone
import com.rafif.m_angkot.utils.GeneralUtils.isGPSOn
import com.rafif.m_angkot.utils.GeneralUtils.showToast
import com.rafif.m_angkot.utils.GeneralUtils.visible
import com.rafif.m_angkot.utils.PrefUtils
import java.text.NumberFormat
import java.util.*
import kotlin.math.ceil


class HomeActivity : AppCompatActivity(), OnMapReadyCallback, HomeContract {

    private lateinit var map: GoogleMap
    private lateinit var binding: ActivityHomeBinding
    private lateinit var presenter: HomePresenter

    private var latLng = LatLng(-6.175392, 106.827153)
    private var isTracking = false

    private var routeName = arrayListOf<String>()
    private var routeCoordinates = arrayListOf<RouteFromDb>()
    private var searchModel: SearchModel? = null
    private var pickPointCoordinate: LatLng? = null
    private var dropPointCoordinate: LatLng? = null

    private var pickShow = true
    private var dropShow = true
    private var recommendation = false

    val onActivityResult =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
            askLocationPermission()
        }

    //Do something when receive result from route activity
    @SuppressLint("SetTextI18n")
    private val onActivityResultRoute =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
            recommendation = false

            //Get data receiver from route activity
            val i = it.data
            searchModel = i?.getParcelableExtra("data")

            if (searchModel != null) {
                binding.tvHomeMove.visibility = View.GONE
                binding.btnHomeRecomment.visibility = View.GONE
                isTracking = false

                val features = searchModel!!.polies.features[0]
                val distance = features.properties.summary.distance

                val coordinate = features.geometry.coordinates.first()
                moveCamera(LatLng(coordinate[1], coordinate[0]))

                binding.tvHomeDistance.text = "${ceil(distance / 1000)}KM"
                binding.tvHomePrice.text = calculatePrice(distance)

                processRoute(parseResponse(searchModel!!.polies), null, true)

                routeName.clear()
                routeCoordinates.clear()
                searchModel!!.routeCoordinates.forEach { route ->
                    routeCoordinates.add(route)
                    routeName.add(route.name)
                }

                binding.tvHomeRoute.text = routeName.joinToString { name -> "$name - " }
                binding.tvHomeRouteName.text = searchModel!!.trayek
                binding.botSheetHome.visibility = View.VISIBLE

                //Check if gps on
                if (isGPSOn(this)) {
                    binding.btnHomeDirectoin.visibility = View.VISIBLE
                }
            }
        }

    //Do something when receive result from recommendation activity
    @SuppressLint("SetTextI18n")
    private val onActivityResultRecommendation =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
            recommendation = true
            isTracking = false

            val i = it.data
            val recommendationModel = i?.getParcelableExtra<RecommendationModel>("data")

            if (recommendationModel != null) {
                binding.tvHomeMove.visibility = View.GONE
                binding.btnHomeRecomment.visibility = View.VISIBLE
                isTracking = false

                binding.tvHomeDistance.text = "${ceil(recommendationModel.distance / 1000)}KM"
                binding.tvHomePrice.text = calculatePrice(recommendationModel.distance)

                val firstLatLng = recommendationModel.route.first()
                //Move map camera
                moveCamera(LatLng(firstLatLng[0], firstLatLng[1]))

                val lngLats = arrayListOf<LatLng>()
                recommendationModel.route.forEach { coordinate ->
                    lngLats.add(LatLng(coordinate[0], coordinate[1]))
                }

                val userLngLats = arrayListOf<LatLng>()
                recommendationModel.userRoute.forEach { coordinate ->
                    userLngLats.add(LatLng(coordinate[1], coordinate[0]))
                }

                //Process route
                processRoute(lngLats, null, true)
                //Process user route to pick point
                processRoute(userLngLats, listOf(Dot(), Gap(20f)), false)

                binding.tvHomeRoute.text = recommendationModel.routeName
                binding.tvHomeRouteName.text = recommendationModel.trayek
                binding.botSheetHome.visibility = View.VISIBLE

                if (isGPSOn(this)) {
                    binding.btnHomeDirectoin.visibility = View.VISIBLE
                }
            }
        }

    @SuppressLint("MissingPermission")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityHomeBinding.inflate(layoutInflater)
        setContentView(binding.root)

        presenter = HomePresenter(this)

        val mapFragment = supportFragmentManager
            .findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)

        askLocationPermission()

        binding.btnHomeDirectoin.setOnClickListener {
            if (recommendation) {
                isTracking = true
                binding.tvHomeMove.visibility = View.VISIBLE
                binding.btnHomeDirectoin.visibility = View.GONE
                binding.btnHomeRecomment.visibility = View.GONE
            } else {
                showDialog()
            }
        }
        binding.btnHomeRecomment.setOnClickListener {
            moveToRecommend()
        }
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.menu, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.menu_search -> {
                //Move to route activity and send latlng data
                val i = Intent(this, RoutesActivity::class.java)
                i.putExtra("latlng", latLng)
                onActivityResultRoute.launch(i)
            }
            R.id.menu_recommend -> {
                moveToRecommend()
            }
            else -> {
                //Delete data from shared preferences
                val prefUtils = PrefUtils(this, LOGGED_IN, MODE_PRIVATE)
                prefUtils.deleteFromPref(LOGGED_IN)

                //Move to login activity
                val i = Intent(this, MainActivity::class.java)
                i.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK or Intent.FLAG_ACTIVITY_NEW_TASK)
                startActivity(i)
            }
        }

        return super.onOptionsItemSelected(item)
    }

    /**
     * Manipulates the map once available.
     * This callback is triggered when the map is ready to be used.
     * This is where we can add markers or lines, add listeners or move the camera. In this case,
     * we just add a marker near Sydney, Australia.
     * If Google Play services is not installed on the device, the user will be prompted to install
     * it inside the SupportMapFragment. This method will only be triggered once the user has
     * installed Google Play services and returned to the app.
     */
    override fun onMapReady(googleMap: GoogleMap) {
        map = googleMap

        moveCamera(LatLng(-6.595038, 106.816635))
        //Ask for location permissions
        askLocationPermission()
    }

    @SuppressLint("MissingPermission")
    private fun fetchGPS() {
        var init = true

        val gps = GPSTracker(this)
        //Observe if user location changes
        gps.getLocation().observe(this) {
            latLng = LatLng(it.latitude, it.longitude)

            //If its first fetch then move camera to user location
            if (init) {
                moveCamera(latLng)
                init = false
            }

            //Enable user indicator on map
            map.isMyLocationEnabled = true

            //Check is current state in tracking / direction
            if (isTracking) {
                if (dropPointCoordinate != null && pickPointCoordinate != null) {
                    //Convert latLng to location
                    val origin = Location("originLocation")
                    origin.longitude = latLng.longitude
                    origin.latitude = latLng.latitude

                    val pickPoint = Location("pickLocation")
                    pickPoint.longitude = pickPointCoordinate!!.longitude
                    pickPoint.latitude = pickPointCoordinate!!.latitude

                    val destination = Location("destLocation")
                    destination.longitude = dropPointCoordinate!!.longitude
                    destination.latitude = dropPointCoordinate!!.latitude
                    //Calculate distance between two location and show notification if distance < 20 meter
                    val pickDistance = origin.distanceTo(pickPoint)
                    if (pickDistance < 20 && pickShow) {
                        showNotification("Kamu sudah mendekati titik naik!")
                        pickShow = false
                    }

                    val distance = origin.distanceTo(destination)
                    if (distance < 20 && dropShow) {
                        showNotification("Kamu sudah mendekati titik turun!")
                        dropShow = false
                    }
                }
            }
        }
    }

    private fun askLocationPermission() {
        Dexter.withContext(this)
            .withPermissions(
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ).withListener(object : MultiplePermissionsListener {

                @SuppressLint("MissingPermission")
                override fun onPermissionsChecked(p0: MultiplePermissionsReport) {
                    if (p0.areAllPermissionsGranted()) {
                        fetchGPS()
                    }
                }

                override fun onPermissionRationaleShouldBeShown(
                    p0: MutableList<PermissionRequest>?,
                    p1: PermissionToken?
                ) {
                    p1?.continuePermissionRequest()
                }
            }).check()
    }

    private fun processRoute(result: List<LatLng>, pattern: List<PatternItem>?, isClear: Boolean) {
        //Add polyline to map
        val lineOption = PolylineOptions()
        lineOption.addAll(result)
        lineOption.width(12f)
        lineOption.color(Color.BLUE)
        lineOption.geodesic(true)

        if (pattern != null) {
            lineOption.pattern(pattern)
            lineOption.color(Color.CYAN)
        }

        if (isClear) {
            map.clear()
        }

        map.addPolyline(lineOption)
    }

    private fun calculatePrice(distance: Double): String {
        var inKm = ceil(distance / 1000).toInt()
        var totalPrice = 2000

        if (inKm > 1) {
            inKm -= 1
            totalPrice += (inKm * 1000)
        }

        val format = NumberFormat.getCurrencyInstance(Locale("ID", "id"))
        return format.format(totalPrice).dropLast(3)
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

    @SuppressLint("InlinedApi")
    private fun showNotification(msg: String) {
        val notificationChannel: NotificationChannel
        val builder: Notification.Builder
        val channelId = "i.apps.notifications"
        val description = "Test notification"
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val notificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            notificationChannel =
                NotificationChannel(channelId, description, NotificationManager.IMPORTANCE_HIGH)
            notificationChannel.enableLights(true)
            notificationChannel.lightColor = Color.GREEN
            notificationChannel.enableVibration(false)
            notificationManager.createNotificationChannel(notificationChannel)

            builder = Notification.Builder(this, channelId)
                .setSmallIcon(R.drawable.ic_launcher_background)
                .setLargeIcon(
                    BitmapFactory.decodeResource(
                        this.resources,
                        R.drawable.ic_launcher_background
                    )
                )
                .setContentTitle(msg)
                .setContentIntent(pendingIntent)
        } else {

            builder = Notification.Builder(this)
                .setSmallIcon(R.drawable.ic_launcher_background)
                .setLargeIcon(
                    BitmapFactory.decodeResource(
                        this.resources,
                        R.drawable.ic_launcher_background
                    )
                )
                .setContentIntent(pendingIntent)
        }
        notificationManager.notify(1234, builder.build())
    }

    override fun onError(error: String) {
        isLoading(false)
        showToast(this, error)
    }

    override fun isLoading(value: Boolean) {
        if (value) {
            binding.pbHome.visible()
        } else {
            binding.pbHome.gone()
        }
    }

    @SuppressLint("SetTextI18n")
    override fun onGetRoutePoly(
        polies: ArrayList<LatLng>,
        routeNames: List<String>,
        distance: Double
    ) {
        processRoute(polies, null, true)
        binding.tvHomeMove.visibility = View.VISIBLE
        binding.btnHomeDirectoin.visibility = View.GONE
        binding.btnHomeRecomment.visibility = View.GONE

        binding.tvHomeDistance.text = "${ceil(distance / 1000)}KM"
        binding.tvHomePrice.text = calculatePrice(distance)

        var joinRoute = routeNames.joinToString { "$it - " }
        joinRoute = joinRoute.replace(",", "")
        binding.tvHomeRoute.text = joinRoute
    }

    override fun onGetUserRoutePoly(polies: ArrayList<LatLng>, start: LatLng, end: LatLng) {
        isLoading(false)
        dropPointCoordinate = end
        pickPointCoordinate = start
        processRoute(polies, listOf(Dot(), Gap(20f)), false)
    }

    private fun showDialog() {
        val b = AlertDialog.Builder(this)
        b.setTitle("Silahkan pilih lokasi turun")
        b.setItems(routeName.toTypedArray()) { dialog, which ->
            dialog.dismiss()

            val polies = searchModel!!.polies.features[0].geometry.coordinates
            val poliesLatLng = arrayListOf<LatLng>()
            polies.forEach {
                val latlng = LatLng(it[1], it[0])
                poliesLatLng.add(latlng)
            }

            val dropLocationLatLng = LatLng(
                routeCoordinates[which].lat,
                routeCoordinates[which].lng
            )

            isTracking = true

            val route = arrayListOf<LatLng>()
            routeCoordinates.forEach {
                route.add(LatLng(it.lat, it.lng))
            }

            presenter.getRoute(latLng, dropLocationLatLng, route, poliesLatLng, routeName)
        }
        b.show()
    }

    private fun moveToRecommend() {
        val i = Intent(this, RecommendationActivity::class.java)
        i.putExtra("latlng", latLng)
        onActivityResultRecommendation.launch(i)
    }

    private fun moveCamera(latLng: LatLng) {
        val cameraUpdate = CameraUpdateFactory.newLatLngZoom(latLng, 15f)
        map.animateCamera(cameraUpdate)
    }
}