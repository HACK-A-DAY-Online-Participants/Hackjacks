package com.example.disaster_management

import android.Manifest
import android.os.Bundle
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import android.widget.ImageButton
import android.widget.Button
import android.widget.TextView
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import androidx.activity.result.contract.ActivityResultContracts
import android.location.Location
import android.annotation.SuppressLint
import android.content.Intent
import android.database.Cursor
import android.util.Log
import android.provider.ContactsContract
import android.net.Uri
import android.widget.TableLayout
import android.widget.TableRow


class MainActivity : AppCompatActivity() {

    private lateinit var fusedLocationClient: FusedLocationProviderClient

    // ================ LOCATION ===============================
    var LATITUDE = ""
    var LONGITUDE = ""

    private val locationPermissionRequest = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        when {
            permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true -> {
                getLastLocation()
            }
            permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true -> {
                getLastLocation()
            }
            else -> {
                Toast.makeText(this, "Location permission denied", Toast.LENGTH_SHORT).show()
            }
        }
    }

    // 2. The function to launch the permission request
    private fun requestLocationPermission() {
        locationPermissionRequest.launch(arrayOf(
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION
        ))
    }
    @SuppressLint("MissingPermission") // We handle the permission check before this is called
    private fun getLastLocation() {
        fusedLocationClient.lastLocation
            .addOnSuccessListener { location: Location? ->
                if (location != null) {
                    // Success! Use the coordinates here.
                    val latitude = location.latitude
                    val longitude = location.longitude

                    // Example: Log the coordinates and show a Toast
                    Log.d("LocationTracker", "Lat: $latitude, Lon: $longitude")

                    LATITUDE = latitude.toString()
                    LONGITUDE = longitude.toString()

                    //coordinates_text = "Lat: $latitude, Lon: $longitude"
                    Toast.makeText(this, "Location: $latitude, $longitude", Toast.LENGTH_LONG).show()

                } else {

                    //coordinates_text = "Location not found"
                    LATITUDE = ""
                    LONGITUDE = ""
                    Toast.makeText(this, "Location not found. Try again or check settings.", Toast.LENGTH_SHORT).show()
                }
            }
            .addOnFailureListener { e ->
                Log.e("LocationTracker", "Error getting location", e)
                Toast.makeText(this, "Failed to get location.", Toast.LENGTH_SHORT).show()
            }
    }
    // ================ LOCATION ===============================


    // ================ PHONE NUMBER LIST =========================
    // 1. Create a list to store the numbers
    private val myPhoneNumbers = mutableListOf<List<String>>()

    // 2. Define the Launcher to handle the result (What happens after you pick a contact)
    private val contactPickerLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == RESULT_OK) {
            // The data returned is a URI pointing to the specific contact clicked
            val contactUri: Uri? = result.data?.data

            if (contactUri != null) {
                getPhoneNumberFromUri(contactUri)
            }
        }
    }

    // 3. Helper function to read the actual number from the database
    private fun getPhoneNumberFromUri(contactUri: Uri) {
        // We only want the Number column
        val projection = arrayOf(
            ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME,
            ContactsContract.CommonDataKinds.Phone.NUMBER
        )

        // Query the database
        val cursor: Cursor? = contentResolver.query(contactUri, projection, null, null, null)

        cursor?.use {
            // Move to the first result
            if (it.moveToFirst()) {
                val nameIndex = it.getColumnIndex(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME)
                val numberIndex = it.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER)

                val number = it.getString(numberIndex)
                val name = it.getString(nameIndex)

                // Optional: Clean the number (remove spaces, dashes, etc.)
                val cleanNumber = number.replace("[^0-9+]".toRegex(), "")

                // Add to your list
                myPhoneNumbers.add((listOf(name, cleanNumber)))

                val contacts_table = findViewById<TableLayout>(R.id.contacts_table)
                val row = TableRow(this)
                val txt_view = TextView(this)
                txt_view.text = name + '\t' + cleanNumber
                row.addView(txt_view)
                contacts_table.addView(row)



                findViewById<TextView>(R.id.textView).text = myPhoneNumbers.toString()


                Log.d("ContactApp", "Number: $cleanNumber")
                Log.d("ContactApp", "Name: $name")
                Log.d("ContactApp", "Full List: $myPhoneNumbers")

                Toast.makeText(this, "Saved: $cleanNumber", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun openContactPicker() {
        // Create an intent to pick a specific phone number
        val intent = Intent(Intent.ACTION_PICK).apply {
            type = ContactsContract.CommonDataKinds.Phone.CONTENT_TYPE
        }
        contactPickerLauncher.launch(intent)
    }
    // ================ PHONE NUMBER LIST =========================


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        var count = 0
        val emergency_button = findViewById<ImageButton>(R.id.emergency_button)
        val open_contacts_button = findViewById<Button>(R.id.open_contacts)
        val clear_contacts_button = findViewById<Button>(R.id.clear_contacts)
        val contacts_table = findViewById<TableLayout>(R.id.contacts_table)

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        requestLocationPermission()

        val text_field = findViewById<TextView>(R.id.textView)

        emergency_button.setOnClickListener {
            //Toast.makeText(this, "Emergency button clicked", Toast.LENGTH_SHORT).show()
            count += 1

            requestLocationPermission()
            //text_field.text =  LATITUDE + " " + LONGITUDE + " " + count.toString()
        }


        open_contacts_button.setOnClickListener {
            openContactPicker()
            Log.d("hello world", "hello world")
            //text_field.text = myPhoneNumbers.toString()
        }

        clear_contacts_button.setOnClickListener {
            myPhoneNumbers.clear()
            text_field.text = myPhoneNumbers.toString()
            contacts_table.removeAllViews()
        }
    }
}