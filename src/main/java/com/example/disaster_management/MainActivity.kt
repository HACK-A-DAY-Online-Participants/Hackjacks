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
import android.content.Context
import android.content.Intent
import android.database.Cursor
import android.util.Log
import android.provider.ContactsContract
import android.net.Uri
import android.widget.TableLayout
import android.widget.TableRow
import android.content.pm.PackageManager
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import android.telephony.SmsManager
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch


class MainActivity : AppCompatActivity() {

    private lateinit var fusedLocationClient: FusedLocationProviderClient

    // ================ LOCATION (START) ===============================
    var LATITUDE = ""
    var LONGITUDE = ""

    var recepient_phone_number = ""

    private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "contacts")

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

                    for (i in 0 until myPhoneNumbers.size) {
                        recepient_phone_number = myPhoneNumbers[i][0]
                        sendSms()
                    }

                    //coordinates_text = "Lat: $latitude, Lon: $longitude"
                    Toast.makeText(this, "Location: $latitude, $longitude", Toast.LENGTH_SHORT).show()

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
    // ================ LOCATION (END) ===============================


    // ================ PHONE NUMBER LIST (START) =========================
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
                myPhoneNumbers.add((listOf(cleanNumber, name)))

                val obj = object {
                    val number = stringPreferencesKey(cleanNumber)
                }
                suspend fun saveUserName(context: Context, name: String) {
                    context.dataStore.edit { preferences ->
                        preferences[obj.number] = name
                    }
                }

                lifecycleScope.launch{
                    saveUserName(this@MainActivity, name)
                }

                val contacts_table = findViewById<TableLayout>(R.id.contacts_table)
                val row = TableRow(this)
                val txt_view = TextView(this)
                txt_view.text = cleanNumber + " = " + name
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
    // ================ PHONE NUMBER LIST (END) =========================


    ///=============== SMS MANAGER (START) =======================
    private val SMS_PERMISSION_CODE = 101

    private fun checkAndRequestSmsPermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.SEND_SMS) != PackageManager.PERMISSION_GRANTED) {
            // Permission is not granted, request it
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.SEND_SMS),
                SMS_PERMISSION_CODE
            )
        } else {
            // Permission is already granted
            //sendSms()
        }
    }

    // Handle the result of the permission request
    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == SMS_PERMISSION_CODE) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // Permission granted
                //sendSms()
            } else {
                // Permission denied
                // Handle the denial, e.g., show a Toast or disable the SMS feature
                Log.d("failed", "PERMISSION NOT GIVEN")
            }
        }
    }

    fun sendSms() {
        val phoneNumber = recepient_phone_number // The recipient's phone number
        var message = "PLEASE HELP ME , i am at this location \n latitude: " + LATITUDE + " longitude: " + LONGITUDE + "\n"
        message += "https://maps.google.com/?q=" + LATITUDE + "," + LONGITUDE

        try {
            // Get the default SmsManager instance
            // For API levels 23 and higher (Marshmallow+), use getSystemService
            val smsManager: SmsManager = if (android.os.Build.VERSION.SDK_INT >= 23) {
                this.getSystemService(SmsManager::class.java)
            } else {
                @Suppress("DEPRECATION")
                SmsManager.getDefault()
            }

            // The last two arguments (PendingIntents) are optional but can be used
            // to receive status updates (sent and delivered)
            smsManager.sendTextMessage(phoneNumber, null, message, null, null)

            Toast.makeText(applicationContext, "SMS sent successfully to " + phoneNumber + "!", Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            Toast.makeText(applicationContext, "SMS sending failed: ${e.message}", Toast.LENGTH_SHORT).show()
            e.printStackTrace()
        }
    }
    ///=============== SMS MANAGER (END) =======================



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

        suspend fun getAllKeys(context: Context) {
            context.dataStore.data.first().asMap().forEach { (key, value) ->
                myPhoneNumbers.add(listOf(key.name, value.toString()))
                val row = TableRow(context)
                val txtView = TextView(context)
                txtView.text = "${key.name} + " = " + "{value}"
                row.addView(txtView)
                contacts_table.addView(row)
            }
        }
        lifecycleScope.launch {
            getAllKeys(this@MainActivity)
        }

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        //requestLocationPermission()

        val text_field = findViewById<TextView>(R.id.textView)

        checkAndRequestSmsPermission()

        emergency_button.setOnClickListener {
            //Toast.makeText(this, "Emergency button clicked", Toast.LENGTH_SHORT).show()

            requestLocationPermission()

            /*
            for (i in 0 until myPhoneNumbers.size) {
                recepient_phone_number = myPhoneNumbers[i][0]
                sendSms()
            }
            */
        }


        open_contacts_button.setOnClickListener {
            openContactPicker()
            Log.d("hello world", "hello world")
            //text_field.text = myPhoneNumbers.toString()
        }

        suspend fun clearDataStore(context: Context) {
            context.dataStore.edit { preferences ->
                preferences.clear()
            }
        }
        clear_contacts_button.setOnClickListener {
            myPhoneNumbers.clear()
            //text_field.text = myPhoneNumbers.toString()
            contacts_table.removeAllViews()
            lifecycleScope.launch {
                clearDataStore(this@MainActivity)
                Toast.makeText(this@MainActivity, "DataStore cleared!", Toast.LENGTH_SHORT).show()
            }
        }
    }
}