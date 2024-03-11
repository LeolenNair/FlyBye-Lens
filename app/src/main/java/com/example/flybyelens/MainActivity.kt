package com.example.flybyelens

import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.drawable.BitmapDrawable
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.MenuItem
import android.view.View
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.appcompat.content.res.AppCompatResources
import androidx.drawerlayout.widget.DrawerLayout
import com.google.android.material.navigation.NavigationView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.mapbox.android.gestures.MoveGestureDetector
import com.mapbox.maps.CameraOptions
import com.mapbox.maps.MapView
import com.mapbox.maps.Style
import com.mapbox.maps.extension.style.expressions.dsl.generated.interpolate
import com.mapbox.maps.extension.style.expressions.generated.Expression
import com.mapbox.maps.extension.style.layers.generated.CircleLayer
import com.mapbox.maps.extension.style.layers.getLayer
import com.mapbox.maps.plugin.LocationPuck2D
import com.mapbox.maps.plugin.gestures.OnMoveListener
import com.mapbox.maps.plugin.gestures.gestures
import com.mapbox.maps.plugin.locationcomponent.OnIndicatorBearingChangedListener
import com.mapbox.maps.plugin.locationcomponent.OnIndicatorPositionChangedListener
import com.mapbox.maps.plugin.locationcomponent.location
import java.lang.ref.WeakReference

class MainActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var locationPermissionHelper: LocationPermissionHelper
    private lateinit var mapView: MapView

    private val onIndicatorBearingChangedListener = OnIndicatorBearingChangedListener {
        mapView.getMapboxMap().setCamera(CameraOptions.Builder().bearing(it).build())
    }

    private val onIndicatorPositionChangedListener = OnIndicatorPositionChangedListener {
        mapView.getMapboxMap().setCamera(CameraOptions.Builder().center(it).build())
        mapView.gestures.focalPoint = mapView.getMapboxMap().pixelForCoordinate(it)
    }

    private val onMoveListener = object : OnMoveListener {
        override fun onMoveBegin(detector: MoveGestureDetector) {
            onCameraTrackingDismissed()
        }

        override fun onMove(detector: MoveGestureDetector): Boolean {
            return false
        }

        override fun onMoveEnd(detector: MoveGestureDetector) {}
    }

    lateinit var toggle: ActionBarDrawerToggle


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Move setPersistenceEnabled(true) to the beginning of your MainActivity's onCreate


        setContentView(R.layout.activity_main)
        setupDrawer()
        mapView = findViewById(R.id.mapView)
        locationPermissionHelper = LocationPermissionHelper(WeakReference(this))
        locationPermissionHelper.checkPermissions {
            fetchAndApplySettings()
            onMapReady()
        }
    }


    private fun setupDrawer() {
        val drawerLayout: DrawerLayout = findViewById(R.id.drawerLayout)
        val navView: NavigationView = findViewById(R.id.nav_view)

        toggle = ActionBarDrawerToggle(this, drawerLayout, R.string.open, R.string.close)
        drawerLayout.addDrawerListener(toggle)
        toggle.syncState()

        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        navView.setNavigationItemSelectedListener {
            when (it.itemId) {
                R.id.nav_home -> Toast.makeText(applicationContext, "Clicked home", Toast.LENGTH_SHORT).show()
                R.id.nav_save -> {
                    val intent = Intent(this, SaveBird::class.java)
                    startActivity(intent)
                    finish()
                }
                R.id.nav_birds -> {
                    val intent = Intent(this, ViewBird::class.java)
                    startActivity(intent)
                    finish()
                }
                R.id.nav_settings -> {
                    // Replace "SettingsActivity" with the actual activity you want to navigate to
                    val intent = Intent(this, Settings::class.java)
                    startActivity(intent)
                    finish()
                }
                R.id.nav_logout -> {
                    logout()
                }
            }
            true
        }
        updateUserEmailInNavHeader()
    }


    override fun onOptionsItemSelected(item: MenuItem): Boolean {

        if(toggle.onOptionsItemSelected(item)){
            return true
        }
        return super.onOptionsItemSelected(item)
    }

    private fun updateUserEmailInNavHeader(){
        auth = FirebaseAuth.getInstance()
        val currentUser = auth.currentUser

        // Update the email in the NavigationView header
        val navView: NavigationView = findViewById(R.id.nav_view)
        val headerView = navView.getHeaderView(0)
        val userEmailTextView: TextView = headerView.findViewById(R.id.user_email)

        // Check if the user is logged in before updating the email
        currentUser?.let {
            userEmailTextView.text = it.email
        }
    }

    fun logout() {
        auth = FirebaseAuth.getInstance()
        auth.signOut()

        // Redirect to your login or home screen after logout
        val intent = Intent(this, Sign_in::class.java)
        startActivity(intent)
        finish() // Close the current activity
    }

    private fun fetchAndApplySettings() {
        val settingsRef = FirebaseDatabase.getInstance().getReference("settings")

        settingsRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (snapshot.exists()) {
                    val settings = snapshot.getValue(SettingsData::class.java)
                    applySettingsToMap(settings)
                }
            }

            override fun onCancelled(error: DatabaseError) {
                // Handle errors
                Toast.makeText(this@MainActivity, "Failed to fetch settings", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun applySettingsToMap(settings: SettingsData?) {
        settings?.let {
            mapView.getMapboxMap().setCamera(
                CameraOptions.Builder()
                    .zoom(it.zoomLevel.toDouble())
                    .build()
            )

            mapView.getMapboxMap().loadStyleUri(
                "mapbox://styles/leolen/clp0cb521003501pc3o3bhu8d"
            ) { style ->
                // Customize map style settings here
                initLocationComponent()
                setupGesturesListener()
            }
        }
    }

    private fun onMapReady() {
        mapView.getMapboxMap().setCamera(
            CameraOptions.Builder()
                .zoom(14.0)
                .build()
        )

        mapView.getMapboxMap().loadStyleUri(
            "mapbox://styles/leolen/clp0cb521003501pc3o3bhu8d"
        ) { loadedStyle ->
            // 'loadedStyle' is the reference to the loaded map style
            loadedStyle?.let { style ->
                // Customize map style settings here
                initLocationComponent()
                setupGesturesListener()

                // Adjust properties of a specific layer, assuming you have a layer with ID "my-points-layer"
                val myPointsLayer = style.getLayer("bird-hotspots") as? CircleLayer
                myPointsLayer?.circleRadius(Expression.literal(8.0))
                myPointsLayer?.circleColor(Expression.color(Color.parseColor("#ff0000")))
            }
        }
    }





    private fun setupGesturesListener() {
        mapView.gestures.addOnMoveListener(onMoveListener)
    }
    private fun initLocationComponent() {
        val locationComponentPlugin = mapView.location
        locationComponentPlugin.updateSettings {
            this.enabled = true

            val originalDrawable = AppCompatResources.getDrawable(
                this@MainActivity,
                R.drawable.mapbox_user_puck_icon
            )

            // Scale the drawable to 1/5 of its original size
            val scaledDrawable = BitmapDrawable(
                resources,
                Bitmap.createScaledBitmap(
                    (originalDrawable as BitmapDrawable).bitmap,
                    (originalDrawable.intrinsicWidth / 15),
                    (originalDrawable.intrinsicHeight / 15),
                    false
                )
            )

            this.locationPuck = LocationPuck2D(
                bearingImage = scaledDrawable,
                shadowImage = scaledDrawable // You can adjust shadow size similarly if needed
            )
        }

        locationComponentPlugin.addOnIndicatorPositionChangedListener(onIndicatorPositionChangedListener)
        locationComponentPlugin.addOnIndicatorBearingChangedListener(onIndicatorBearingChangedListener)
    }


    private fun onCameraTrackingDismissed() {
        Toast.makeText(this, "onCameraTrackingDismissed", Toast.LENGTH_SHORT).show()
        mapView.location
            .removeOnIndicatorPositionChangedListener(onIndicatorPositionChangedListener)
        mapView.location
            .removeOnIndicatorBearingChangedListener(onIndicatorBearingChangedListener)
        mapView.gestures.removeOnMoveListener(onMoveListener)
    }

    override fun onDestroy() {
        super.onDestroy()
        mapView.location
            .removeOnIndicatorBearingChangedListener(onIndicatorBearingChangedListener)
        mapView.location
            .removeOnIndicatorPositionChangedListener(onIndicatorPositionChangedListener)
        mapView.gestures.removeOnMoveListener(onMoveListener)
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        locationPermissionHelper.onRequestPermissionsResult(requestCode, permissions, grantResults)
    }
}