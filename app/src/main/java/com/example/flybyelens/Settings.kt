package com.example.flybyelens

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.MenuItem
import android.widget.*
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.drawerlayout.widget.DrawerLayout
import com.google.android.material.navigation.NavigationView
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*

data class SettingsData(
    val zoomLevel: Int = 0,
    val metricSystem: Boolean = false
)

class Settings : AppCompatActivity() {

    private lateinit var settingsRef: DatabaseReference
    lateinit var toggle: ActionBarDrawerToggle
    private lateinit var auth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Move the setPersistenceEnabled(true) call here

        setContentView(R.layout.activity_settings)
        setupDrawer()

        settingsRef = FirebaseDatabase.getInstance().getReference("settings")

        // Other setup code...

        val saveButton: Button = findViewById(R.id.buttonSave)
        saveButton.setOnClickListener {
            saveSettings()
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
    fun logout() {
        auth = FirebaseAuth.getInstance()
        auth.signOut()

        // Redirect to your login or home screen after logout
        val intent = Intent(this, Sign_in::class.java)
        startActivity(intent)
        finish() // Close the current activity
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

    private fun saveSettings() {
        // Retrieve values from UI elements
        val seekBarZoom: SeekBar = findViewById(R.id.seekBarZoom)
        val switchUnitSystem: Switch = findViewById(R.id.switchUnitSystem)

        val zoomLevel: Int = seekBarZoom.progress
        val isMetricSystem: Boolean = switchUnitSystem.isChecked

        // Create a SettingsData object
        val settings = SettingsData(zoomLevel, isMetricSystem)

        // Save to Firebase
        settingsRef.setValue(settings) { error, _ ->
            if (error == null) {
                Toast.makeText(this, "Settings saved successfully", Toast.LENGTH_SHORT).show()
                val intent = Intent(this, MainActivity::class.java)
                startActivity(intent)
                finish()
            } else {
                Toast.makeText(this, "Failed to save settings", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onStart() {
        super.onStart()

        // Read from Firebase and update UI
        settingsRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (snapshot.exists()) {
                    val settings = snapshot.getValue(SettingsData::class.java)
                    updateUI(settings)
                }
            }

            override fun onCancelled(error: DatabaseError) {
                // Handle errors
                Toast.makeText(this@Settings, "Failed to read settings", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun updateUI(settings: SettingsData?) {
        // Update UI based on settings (e.g., set SeekBar and Switch values)
        settings?.let {
            val seekBarZoom: SeekBar = findViewById(R.id.seekBarZoom)
            val switchUnitSystem: Switch = findViewById(R.id.switchUnitSystem)

            seekBarZoom.progress = it.zoomLevel
            switchUnitSystem.isChecked = it.metricSystem
        }
    }
}
