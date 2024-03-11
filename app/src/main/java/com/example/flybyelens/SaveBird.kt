package com.example.flybyelens

import android.content.Intent
import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.view.MenuItem
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.drawerlayout.widget.DrawerLayout
import com.google.android.material.navigation.NavigationView
import com.google.android.material.textfield.TextInputEditText
import com.google.firebase.FirebaseException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageReference

class SaveBird : AppCompatActivity() {


        private val PICK_IMAGE_REQUEST = 1
        private lateinit var imageView: ImageView
        private lateinit var textView: TextView
        private lateinit var nameEditText: TextInputEditText
        private lateinit var discripEditText: TextInputEditText
        private lateinit var locationEditText: TextInputEditText
        private var imageUri: Uri? = null
        private lateinit var auth: FirebaseAuth
        private lateinit var currentUser: FirebaseUser
        private lateinit var databaseReference: DatabaseReference
        private lateinit var storageReference: StorageReference
        lateinit var toggle: ActionBarDrawerToggle

        override fun onCreate(savedInstanceState: Bundle?) {
            super.onCreate(savedInstanceState)
            setContentView(R.layout.activity_save_bird)
            setupDrawer()

            // Initialize Firebase components
            auth = FirebaseAuth.getInstance()
            currentUser = auth.currentUser!!

            databaseReference = FirebaseDatabase.getInstance().getReference("birds")
            storageReference = FirebaseStorage.getInstance().getReference("birdImage")

            // Initialize UI elements
            imageView = findViewById(R.id.imageView)
            textView = findViewById(R.id.textView)
            nameEditText = findViewById(R.id.birdName)
            discripEditText = findViewById(R.id.discrip)
            locationEditText = findViewById(R.id.location)

            imageView.setOnClickListener {
                openImagePicker()
            }

            // Handle saving user profile data
            findViewById<View>(R.id.saveButton).setOnClickListener {
                saveBird()
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
        private fun openImagePicker() {
            val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
            startActivityForResult(intent, PICK_IMAGE_REQUEST)
        }

        override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
            super.onActivityResult(requestCode, resultCode, data)

            if (requestCode == PICK_IMAGE_REQUEST && resultCode == RESULT_OK && data != null && data.data != null) {
                imageUri = data.data
                imageView.setImageURI(imageUri)
            }
        }

    private fun saveBird() {
        val name = nameEditText.text.toString().trim()
        val discrip = discripEditText.text.toString().trim()
        val location = locationEditText.text.toString().trim()

        try {
            if (currentUser == null) {
                Toast.makeText(this@SaveBird, "User is not authenticated", Toast.LENGTH_SHORT).show()
                return
            }

            if (name.isEmpty() || discrip.isEmpty() || location.isEmpty() || imageUri == null) {
                Toast.makeText(this@SaveBird, "Please fill in all fields and select an image", Toast.LENGTH_SHORT).show()
                return
            }

            val imageReference = storageReference.child("${currentUser.uid}_${System.currentTimeMillis()}.jpg")
            imageReference.putFile(imageUri!!).addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    // Get the image download URL
                    imageReference.downloadUrl.addOnCompleteListener { urlTask ->
                        if (urlTask.isSuccessful) {
                            val imageUrl = urlTask.result.toString()

                            // Create a map for bird data
                            val birdData = mapOf(
                                "name" to name,
                                "discrip" to discrip,
                                "location" to location,
                                "pictureUrl" to imageUrl
                            )

                            // Generate a unique key for the bird
                            val birdId = databaseReference.child(currentUser.uid).push().key

                            if (birdId != null) {
                                // Save the bird data under the unique key
                                databaseReference.child(currentUser.uid).child(birdId).setValue(birdData)

                                Toast.makeText(this@SaveBird, "Bird saved successfully", Toast.LENGTH_SHORT).show()
                            } else {
                                Log.e("SaveBird", "Failed to generate a unique key for the bird.")
                            }
                        } else {
                            handleFirebaseException(urlTask.exception)
                        }
                    }
                } else {
                    handleFirebaseException(task.exception)
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
            handleFirebaseException(e)
        }

        val intent = Intent(applicationContext, MainActivity::class.java)
        startActivity(intent)
        finish()
    }


    private fun handleFirebaseException(exception: Exception?) {
            if (exception is FirebaseException) {
                // Handle Firebase-related exceptions
                val errorMessage = exception.message
                Toast.makeText(this@SaveBird, "Firebase Error: $errorMessage", Toast.LENGTH_SHORT).show()
            } else {
                // Handle other exceptions
                Toast.makeText(this@SaveBird, "Error: ${exception?.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }