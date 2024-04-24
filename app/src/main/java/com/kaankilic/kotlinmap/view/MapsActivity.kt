package com.kaankilic.kotlinmap.view

import android.Manifest
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.room.Room

import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.material.snackbar.Snackbar
import com.kaankilic.kotlinmap.R
import com.kaankilic.kotlinmap.databinding.ActivityMapsBinding
import com.kaankilic.kotlinmap.model.Place
import com.kaankilic.kotlinmap.roomdb.PlaceDao
import com.kaankilic.kotlinmap.roomdb.PlaceDatabase
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import io.reactivex.rxjava3.disposables.CompositeDisposable
import io.reactivex.rxjava3.schedulers.Schedulers

class MapsActivity : AppCompatActivity(), OnMapReadyCallback,GoogleMap.OnMapLongClickListener {

    private lateinit var mMap: GoogleMap
    private lateinit var binding: ActivityMapsBinding
    private lateinit var locationManager: LocationManager
    private lateinit var locationListener: LocationListener
    private lateinit var permissionLauncher: ActivityResultLauncher<String>
    private lateinit var sharedPreferences: SharedPreferences
    private var trackBoolean : Boolean? = null
    private var selectedLatitude:Double?=null
    private var selectedLongitude:Double?=null
    private lateinit var db: PlaceDatabase
    private lateinit var placeDao:PlaceDao
    val compositeDisposable = CompositeDisposable()
    var placeFromMain : Place? = null



    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMapsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        val mapFragment = supportFragmentManager.findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)

        registerLauncher()
        sharedPreferences=this.getSharedPreferences("com.kaankilic.kotlinmap", MODE_PRIVATE)
        trackBoolean = false
        selectedLatitude=0.0
        selectedLongitude=0.0
        binding.saveButton.isEnabled=false

        db= Room.databaseBuilder(applicationContext,PlaceDatabase::class.java,"Places").build()
            //.allowMainThreadQueries()

        placeDao = db.placeDao()

    }

    override fun onMapReady(googleMap: GoogleMap) {
        mMap = googleMap
        mMap.setOnMapLongClickListener(this)

        val intent = intent
        val info = intent.getStringExtra("info")
        if (info == "new") {
            binding.saveButton.visibility = View.VISIBLE
            binding.deleteButton.visibility = View.GONE
            locationManager = this.getSystemService(LOCATION_SERVICE) as LocationManager
            locationListener = object : LocationListener {
                override fun onLocationChanged(location: Location) {
                    trackBoolean = sharedPreferences.getBoolean("trackBoolean", false)
                    if (!trackBoolean!!) {
                        val userLocation = LatLng(location.latitude, location.longitude)
                        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(userLocation, 15f))
                        sharedPreferences.edit().putBoolean("trackBoolean", true).apply()
                    }
                }

            }
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                //request permission
                if (ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.READ_EXTERNAL_STORAGE)) {
                    Snackbar.make(binding.root, "Permission needed for location", Snackbar.LENGTH_INDEFINITE).setAction("Give Permission") {
                        permissionLauncher.launch(Manifest.permission.READ_EXTERNAL_STORAGE)
                    }.show()
                } else {
                    permissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
                }
            } else {
                locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0f, locationListener)
                val lastLocation = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER)
                if (lastLocation != null) {
                    val lastUserLocation = LatLng(lastLocation.latitude, lastLocation.longitude)
                    mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(lastUserLocation, 15f))
                }
            }
        } else{
            mMap.clear()

            placeFromMain = intent.getSerializableExtra("selectedPlace") as? Place
            //placeFromMain= intent.getSerializableExtra("selectedPlace") as? Place

            placeFromMain?.let {

                val latLng = LatLng(it.latitude,it.longitude)
                mMap.addMarker(MarkerOptions().position(latLng).title(it.name))
                mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(latLng,15f))

                binding.placeText.setText(it.name)
                binding.saveButton.visibility=View.GONE
                binding.deleteButton.visibility=View.VISIBLE

            }
        }




        /* val eiffel = LatLng(48.85391,2.2913515)//enlem boylam verdik
        mMap.addMarker(MarkerOptions().position(eiffel).title("eiffel tower"))//marker ekledik
        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(eiffel,15f))//zoom verdik */
    }

    private fun registerLauncher() {
        permissionLauncher = registerForActivityResult(ActivityResultContracts.RequestPermission()) { result ->
                if (result) {
                    //izinverildi
                   if(ContextCompat.checkSelfPermission(this,Manifest.permission.ACCESS_FINE_LOCATION)==PackageManager.PERMISSION_GRANTED){
                       locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER,0,0f,locationListener)
                       val lastLocation = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER)
                       if (lastLocation != null){
                           val lastUserLocation = LatLng(lastLocation.latitude,lastLocation.longitude)
                           mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(lastUserLocation,15f))
                       }
                       mMap.isMyLocationEnabled=true
                   }



                } else {//izin verilmedi
                    Toast.makeText(this@MapsActivity, "permission needed!", Toast.LENGTH_SHORT).show()

                }

            }

    }

    override fun onMapLongClick(p0: LatLng) {

        mMap.clear()

        mMap.addMarker(MarkerOptions().position(p0))

        selectedLongitude=p0.longitude
        selectedLatitude = p0.latitude
        binding.saveButton.isEnabled=true

    }

    fun save(view:View){

        //Main Thread UI, Default-> CPU, IO Thread Internet/Database


        if (selectedLatitude!= null && selectedLongitude !=null){
            val place = Place(binding.placeText.text.toString(),selectedLatitude!!,selectedLongitude!!)
            compositeDisposable.add(
                placeDao.insert(place)
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(this::handleResponse)


            )

        }


    }

    private fun handleResponse(){
        val intent = Intent(this,MainActivity::class.java)
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
        startActivity(intent)
    }

    fun delete(view:View){

        placeFromMain?.let {
            compositeDisposable.add(
                placeDao.delete(it)
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(this::handleResponse)

            )

        }

    }

    override fun onDestroy() {
        super.onDestroy()
        compositeDisposable.clear()
    }

}