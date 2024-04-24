package com.kaankilic.kotlinmap.view

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.room.Room
import com.kaankilic.kotlinmap.R
import com.kaankilic.kotlinmap.adapter.PlaceAdapter
import com.kaankilic.kotlinmap.databinding.ActivityMainBinding
import com.kaankilic.kotlinmap.model.Place
import com.kaankilic.kotlinmap.roomdb.PlaceDatabase
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import io.reactivex.rxjava3.disposables.CompositeDisposable
import io.reactivex.rxjava3.schedulers.Schedulers

class MainActivity : AppCompatActivity() {
    private val compositeDisposable = CompositeDisposable()

    private lateinit var binding: ActivityMainBinding
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        val view = binding.root
        setContentView(view)

        val db = Room.databaseBuilder(applicationContext,PlaceDatabase::class.java,"Places").build()
        val placeDao = db.placeDao()
        compositeDisposable.add(
            placeDao.getAll()
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(this::handlerResponse)


        )

    }

    private fun handlerResponse(placeList: List<Place>){
        binding.recyclerView.layoutManager= LinearLayoutManager(this)
        val adapter = PlaceAdapter(placeList)
        binding.recyclerView.adapter=adapter


    }



        override fun onCreateOptionsMenu(menu: Menu?): Boolean {//bağlama işlemi menüyü
        val menuInflater = menuInflater
        menuInflater.inflate(R.menu.place_menu,menu)
        return super.onCreateOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {//menüden eleman seçilirse ne olacak
        if (item.itemId== R.id.add_place){
            val intent = Intent(this, MapsActivity::class.java)
            intent.putExtra("info" ,"new")
            startActivity(intent)
        }
        return super.onOptionsItemSelected(item)
    }

}