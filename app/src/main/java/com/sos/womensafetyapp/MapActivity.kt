package com.sos.womensafetyapp

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.Looper
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import com.google.android.gms.location.LocationCallback

import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.Priority
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.MarkerOptions


import okhttp3.ResponseBody
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import org.json.JSONObject


class MapActivity : AppCompatActivity(), OnMapReadyCallback {

    private lateinit var mMap: GoogleMap
    private val LOCATION_REQUEST_CODE = 1001
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var locationCallback: LocationCallback
    private val stations = mutableListOf<LatLng>()
    private var hasFetched = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_map)
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        val mapFragment = supportFragmentManager
            .findFragmentById(R.id.map) as SupportMapFragment

        mapFragment.getMapAsync(this)
        requestLocationPermission()


    }

    private fun requestLocationPermission() {
        ActivityCompat.requestPermissions(
            this,
            arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
            LOCATION_REQUEST_CODE
        )
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        if (requestCode == LOCATION_REQUEST_CODE &&
            grantResults.isNotEmpty() &&
            grantResults[0] == PackageManager.PERMISSION_GRANTED
        ) {
            enableLocationUpdates()
        }
    }

    private fun enableLocationUpdates() {

        val locationRequest = LocationRequest.Builder(
            Priority.PRIORITY_HIGH_ACCURACY,
            3000
        ).build()

        locationCallback = object : LocationCallback() {

            override fun onLocationResult(result: LocationResult) {


                val location = result.lastLocation ?: return
                val userLat = location.latitude
                val userLng = location.longitude

                val latLng = LatLng(location.latitude, location.longitude)

                mMap.animateCamera(
                    CameraUpdateFactory.newLatLngZoom(latLng, 17f)
                )
               /* if (!hasFetched) {
                    fetchPoliceStations(location.latitude, location.longitude)
                    hasFetched = true
                }*/

                val nearest = findNearest(userLat, userLng)

                nearest?.let {

                    mMap.addMarker(
                        MarkerOptions()
                            .position(it)
                            .title("Nearest Police Station")
                            .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_GREEN))
                    )

                    mMap.animateCamera(
                        CameraUpdateFactory.newLatLngZoom(it, 15f)
                    )
                }
            }


        }

        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) return

        fusedLocationClient.requestLocationUpdates(
            locationRequest,
            locationCallback,
            Looper.getMainLooper()
        )
    }


    override fun onMapReady(googleMap: GoogleMap) {
        mMap = googleMap
        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            googleMap.isMyLocationEnabled = true
        }

        mMap.addMarker(
            MarkerOptions()
                .position(LatLng(26.9124, 75.7873))
                .title("Police Station")
                .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_BLUE))
        )

       /* mMap.addMarker(
            MarkerOptions()
                .position(LatLng(26.9150, 75.7800))
                .title("Hospital")
                .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED))
        )*/
        val jsonString = assets.open("police_stations.geojson")
            .bufferedReader()
            .use { it.readText() }

        parseAndShowMarkers(jsonString)

        //Log.d("DATA_TEST", jsonString)


    }

    private fun distance(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Float {
        val result = FloatArray(1)
        android.location.Location.distanceBetween(lat1, lon1, lat2, lon2, result)
        return result[0]
    }

    private fun findNearest(userLat: Double, userLng: Double): LatLng? {

        var nearest: LatLng? = null
        var minDistance = Float.MAX_VALUE

        for (station in stations) {

            val d = distance(
                userLat, userLng,
                station.latitude, station.longitude
            )

            if (d < minDistance) {
                minDistance = d
                nearest = station
            }
        }

        return nearest
    }

   /* private fun fetchPoliceStations(lat: Double, lng: Double) {

        val query = """
        [out:json];
        node["amenity"="police"](around:5000,$lat,$lng);
        out;
    """.trimIndent()

        RetrofitClient.api.getPoliceStations(query)
            .enqueue(object : Callback<ResponseBody> {

                override fun onResponse(
                    call: Call<ResponseBody>,
                    response: Response<ResponseBody>
                ) {
                    val json = response.body()?.string() ?: return
                    android.util.Log.d("OSM_RESPONSE", json)
                    parseAndShowMarkers(json)

                }

                override fun onFailure(call: Call<ResponseBody>, t: Throwable) {
                    t.printStackTrace()
                }
            })
    }

    private fun parseAndShowMarkers(jsonString: String) {

        val jsonObject = JSONObject(jsonString)
        val elements = jsonObject.getJSONArray("elements")

        for (i in 0 until elements.length()) {

            val item = elements.getJSONObject(i)

            val lat = item.getDouble("lat")
            val lon = item.getDouble("lon")

            val tags = item.optJSONObject("tags")
            val name = tags?.optString("name") ?: "Police Station"

            val location = LatLng(lat, lon)

            mMap.addMarker(
                MarkerOptions()
                    .position(location)
                    .title(name)
                    .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_BLUE))
            )



        }

        if (elements.length() > 0) {
            val first = elements.getJSONObject(0)
            val lat = first.getDouble("lat")
            val lon = first.getDouble("lon")

            mMap.animateCamera(
                CameraUpdateFactory.newLatLngZoom(
                    LatLng(lat, lon),
                    14f
                )
            )
        }
    }*/
   private fun parseAndShowMarkers(jsonString: String) {

       val jsonObject = JSONObject(jsonString)
       val features = jsonObject.getJSONArray("features")

      // val stations = mutableListOf<LatLng>()

       for (i in 0 until features.length()) {

           val feature = features.getJSONObject(i)
           val geometry = feature.getJSONObject("geometry")
           val coords = geometry.getJSONArray("coordinates")

           val lon = coords.getDouble(0)
           val lat = coords.getDouble(1)

           val position = LatLng(lat, lon)
           stations.add(position)

           mMap.addMarker(
               MarkerOptions()
                   .position(position)
                   .title("Police Station")
                   .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_BLUE))
           )
       }

       if (stations.isNotEmpty()) {
           mMap.moveCamera(
               CameraUpdateFactory.newLatLngZoom(stations[0], 12f)
           )
       }
   }

}