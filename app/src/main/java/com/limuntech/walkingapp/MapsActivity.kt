package com.limuntech.walkingapp

import android.Manifest
import android.annotation.SuppressLint
import android.content.pm.PackageManager
import android.graphics.Color
import android.location.Location
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Looper
import android.os.PersistableBundle
import android.view.WindowManager
import androidx.activity.viewModels
import androidx.core.app.ActivityCompat
import androidx.lifecycle.lifecycleScope
import com.google.android.gms.location.*
import com.google.android.gms.maps.CameraUpdateFactory

import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.gms.maps.model.Polyline
import com.google.android.gms.maps.model.PolylineOptions
import com.google.maps.android.ktx.awaitMap
import com.google.maps.android.ktx.awaitMapLoad
import com.google.maps.android.ktx.utils.sphericalDistance
import com.limuntech.walkingapp.databinding.ActivityMapsBinding

class MapsActivity : AppCompatActivity() {

    companion object {
        const val UPDATE_INTERVAL = 15L
        const val REQUESTING_LOCATION_UPDATES_KEY = "REQ_LOC_UPDATE"
    }
    private lateinit var mMap: GoogleMap
    private lateinit var binding: ActivityMapsBinding
    private lateinit var locationCallback: LocationCallback
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var locationRequest : LocationRequest
    private var requestingLocationUpdates : Boolean = false
    private var firstLocation : Location? = null
    private var listPointModel : MutableList<LatLng> = mutableListOf()
    private var polyline: Polyline? = null



    @SuppressLint("MissingPermission")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val isRestore = savedInstanceState != null

        binding = ActivityMapsBinding.inflate(layoutInflater)
        setContentView(binding.root)
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        updateValuesFromBundle(savedInstanceState)

        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        locationRequest = LocationRequest.create()
        locationRequest.priority = Priority.PRIORITY_HIGH_ACCURACY
        locationRequest.interval = UPDATE_INTERVAL
        locationRequest.fastestInterval = UPDATE_INTERVAL
        uiFunction()
        val mapFragment = supportFragmentManager.findFragmentById(R.id.map) as SupportMapFragment
        lifecycleScope.launchWhenCreated {
            val googleMap = mapFragment.awaitMap()
            if (!isRestore) {

                googleMap.awaitMapLoad()
                googleMap.mapType = GoogleMap.MAP_TYPE_NORMAL
                googleMap.isMyLocationEnabled = true
                mMap = googleMap
                polyline = googleMap.addPolyline(
                    PolylineOptions().zIndex(3f).color(Color.parseColor("#CCed0000"))
                    .width(3f).add(googleMap.cameraPosition.target))


            }
        }
        locationCallback = object : LocationCallback() {
            override fun onLocationResult(locationResult: LocationResult) {
                locationResult ?: return
                for (location in locationResult.locations){

                    mapFunction(location)

                }
            }
        }
    }

    private fun uiFunction() {
        binding.fab.setOnClickListener{
            if(binding.fab.text == "Start") {
                startLocationUpdates()
                binding.fab.text= "Stop"
            }else{
                stopLocationUpdates()
                binding.fab.text= "Start"
            }
        }
    }
    private fun stopLocationUpdates() {
        fusedLocationClient.removeLocationUpdates(locationCallback)
        val lastLocation = listPointModel.last()

        mMap.addMarker(
            MarkerOptions()
                .position(LatLng(lastLocation.latitude, lastLocation.longitude))
                .title("Last Location")
        )
    }
    private fun mapFunction(location: Location) {
        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(LatLng(location.latitude,location.longitude),19f))
        if(firstLocation == null){
            firstLocation = location
            mMap.addMarker(
                MarkerOptions()
                    .position(LatLng(firstLocation!!.latitude,firstLocation!!.longitude))
                    .title("First Location")
            )
            listPointModel.add(LatLng(firstLocation!!.latitude,firstLocation!!.longitude))
        }else{
            val lastPoint = listPointModel.last()
            val distance =lastPoint.sphericalDistance(LatLng(location.latitude,location.longitude))
            if(distance > 2){
                listPointModel.add(LatLng(location.latitude,location.longitude))
                polyline!!.points = listPointModel
            }

        }

    }


    private fun startLocationUpdates() {
        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            return
        }
        fusedLocationClient.requestLocationUpdates(locationRequest,
            locationCallback,
            Looper.getMainLooper())
    }
    override fun onResume() {
        super.onResume()
        if (requestingLocationUpdates) startLocationUpdates()
    }

    override fun onSaveInstanceState(outState: Bundle, outPersistentState: PersistableBundle) {
        outState.putBoolean(REQUESTING_LOCATION_UPDATES_KEY, requestingLocationUpdates)
        super.onSaveInstanceState(outState)
    }
    private fun updateValuesFromBundle(savedInstanceState: Bundle?) {
        savedInstanceState ?: return
        if (savedInstanceState.keySet().contains(REQUESTING_LOCATION_UPDATES_KEY)) {
            requestingLocationUpdates = savedInstanceState.getBoolean(
                REQUESTING_LOCATION_UPDATES_KEY)
        }

    }

}