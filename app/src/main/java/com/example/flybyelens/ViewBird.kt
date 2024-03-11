package com.example.flybyelens

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.MenuItem
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.drawerlayout.widget.DrawerLayout
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.navigation.NavigationView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*

class ViewBird : AppCompatActivity() {
    // ... (your existing imports)
    private lateinit var auth: FirebaseAuth
    private lateinit var databaseReference: DatabaseReference
    private lateinit var recyclerView: RecyclerView
    private lateinit var birdAdapter: BirdAdapter
    lateinit var toggle: ActionBarDrawerToggle

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_view_bird)
        setupDrawer()

        auth = FirebaseAuth.getInstance()
        databaseReference = FirebaseDatabase.getInstance().getReference("birds")

        recyclerView = findViewById(R.id.recyclerView)
        birdAdapter = BirdAdapter { bird -> onBirdItemClick(bird) }
        recyclerView.adapter = birdAdapter
        recyclerView.layoutManager = LinearLayoutManager(this)

        loadBirds()
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

    private fun loadBirds() {
        val currentUser = auth.currentUser
        if (currentUser != null) {
            val userId = currentUser.uid
            val query: Query = databaseReference.child(userId)
            query.addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val birds = mutableListOf<Bird>()
                    for (birdSnapshot in snapshot.children) {
                        try {
                            val name = birdSnapshot.child("name").getValue(String::class.java)
                            val discrip = birdSnapshot.child("discrip").getValue(String::class.java)
                            val location = birdSnapshot.child("location").getValue(String::class.java)
                            val pictureUrl = birdSnapshot.child("pictureUrl").getValue(String::class.java)

                            if (name != null && discrip != null && location != null && pictureUrl != null) {
                                val bird = Bird(name, discrip, location, pictureUrl)
                                birds.add(bird)
                            } else {
                                Log.e("ViewBird", "One or more fields are null for a bird: $birdSnapshot")
                            }
                        } catch (e: Exception) {
                            Log.e("ViewBird", "Error parsing bird data: ${e.message}")
                        }
                    }


                    birdAdapter.submitList(birds)
                }

                override fun onCancelled(error: DatabaseError) {
                    // Handle error
                    Log.e("ViewBird", "Database error: ${error.message}")
                }
            })
        } else {
            Log.e("ViewBird", "Current user is null.")
        }
    }

    private fun onBirdItemClick(bird: Bird) {
        // Handle click on a bird item (e.g., open details activity)
    }
}
