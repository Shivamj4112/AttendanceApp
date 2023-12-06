package com.pankajgaming.attendanceapp.Activity


import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.ConnectivityManager
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Handler
import android.util.Log
import android.view.Gravity
import android.view.View
import android.view.animation.AnimationUtils
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.core.app.ActivityCompat
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.BuildConfig
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.Logger
import com.google.firebase.database.ValueEventListener
import com.google.firebase.database.ktx.database
import com.google.firebase.ktx.Firebase
import com.pankajgaming.attendanceapp.databinding.ActivityMainBinding
import com.pankajgaming.attendanceapp.DataClass.User
import com.pankajgaming.attendanceapp.LocaleHelper
import com.pankajgaming.attendanceapp.Manager.NetworkManager
import com.pankajgaming.attendanceapp.R
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class MainActivity : AppCompatActivity() {

    private lateinit var binding : ActivityMainBinding
    private lateinit var auth: FirebaseAuth
    private lateinit var dbRef: DatabaseReference
    private lateinit var userUid: String
    private lateinit var headerView :View
    private lateinit var navUsername: TextView
    private lateinit var navUserphone: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.btnMenu.isEnabled = false
        binding.btnYear.isEnabled = false
        binding.btnMonth.isEnabled = false


        val snack = Snackbar.make(binding.root, "No Internet", Snackbar.LENGTH_INDEFINITE)

        binding.apply {

            val networkManager = NetworkManager(this@MainActivity)
            networkManager.observe(this@MainActivity) {

                if (it) {
                    auth = FirebaseAuth.getInstance()
                    val user = auth.currentUser
                    userUid = user?.uid.toString()
                    dbRef = FirebaseDatabase.getInstance().reference
                    getUserData()
                    setupAttendanceListener(userUid)

                    binding.translucentOverlay.visibility = View.VISIBLE
                    binding.animationView.visibility = View.VISIBLE

                    snack.dismiss()

                    Handler().postDelayed({
                        binding.translucentOverlay.visibility = View.GONE
                        binding.animationView.visibility = View.GONE
                        binding.btnMenu.isEnabled = true
                        binding.btnYear.isEnabled = true
                        binding.btnMonth.isEnabled = true
                        binding.floatingActionButton.isEnabled = true

                    }, 2000)
                } else {

                    binding.translucentOverlay.visibility = View.VISIBLE
                    binding.animationView.visibility = View.VISIBLE
                    binding.btnMenu.isEnabled = false
                    binding.btnYear.isEnabled = false
                    binding.btnMonth.isEnabled = false
                    binding.floatingActionButton.isEnabled = false

                    Handler().postDelayed({
                        binding.translucentOverlay.visibility = View.GONE
                        binding.animationView.visibility = View.GONE
                        snack.show()

                    }, 10000)
                }
            }



            if (BuildConfig.DEBUG) {
                Firebase.database.setLogLevel(Logger.Level.DEBUG)
            }


            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                ActivityCompat.requestPermissions(this@MainActivity, arrayOf(Manifest.permission.POST_NOTIFICATIONS), PackageManager.PERMISSION_GRANTED)
            }
            ActivityCompat.requestPermissions(this@MainActivity, arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE), PackageManager.PERMISSION_GRANTED)
            ActivityCompat.requestPermissions(this@MainActivity, arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE), PackageManager.PERMISSION_GRANTED)


            val currentDate = Date()
            val dateFormat = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
            val formattedDate = dateFormat.format(currentDate)
            binding.txtDate.text = formattedDate

            binding.btnMenu.setOnClickListener {

                binding.drawerLayout.openDrawer(Gravity.LEFT)



                binding.navigationView.setNavigationItemSelectedListener { item ->

                    when (item.itemId) {

                        R.id.language -> {

                            val intent = Intent(this@MainActivity, LanguageSelectionActivity::class.java)
                            intent.putExtra("mode","reselect")
                            startActivity(intent)
                            finish()
                        }

                        R.id.logout -> {

                            val alertDialog = AlertDialog.Builder(this@MainActivity)
                                .setTitle("Logout")
                                .setMessage("Are you sure you want to Logout ?")
                                .setCancelable(true)
                                .setPositiveButton("Yes") { dialog, which ->

                                    Toast.makeText(
                                        this@MainActivity,
                                        "Your are Logout",
                                        Toast.LENGTH_SHORT
                                    ).show()

//                                    val media = MediaPlayer.create(this@MainActivity, R.raw.logout)
//                                    media.start()

                                    val intent = Intent(this@MainActivity, LoginActivity::class.java)
                                    startActivity(intent)
                                    finish()
                                    auth.signOut()

                                }
                                .setNegativeButton("No") { dialog, which ->
                                    dialog.dismiss()
                                }
                            alertDialog.show()
                        }


                    }

                    false
                }

            }


            btnYear.setOnClickListener {
                headerView = binding.navigationView.getHeaderView(0)
                navUsername = headerView.findViewById(R.id.txtName)
                navUserphone = headerView.findViewById(R.id.txtPhone)

                val intent = Intent(this@MainActivity, AllAttendanceActivity::class.java)
                intent.putExtra("uid", userUid)
                intent.putExtra("userName", navUsername.text.toString())
                intent.putExtra("userPhone", navUserphone.text.toString())
                startActivity(intent)
            }

            btnMonth.setOnClickListener {
                headerView = binding.navigationView.getHeaderView(0)
                navUsername = headerView.findViewById(R.id.txtName)
                navUserphone = headerView.findViewById(R.id.txtPhone)

                val intent = Intent(this@MainActivity, MonthAttendanceActivity::class.java)
                intent.putExtra("uid", userUid)
                intent.putExtra("userName", navUsername.text.toString())
                intent.putExtra("userPhone", navUserphone.text.toString())
                startActivity(intent)
            }

            floatingActionButton.setOnClickListener {

                val intent = Intent(this@MainActivity, AddAttendanceActivity::class.java)
                intent.putExtra("MODE", "ADD")
                startActivity(intent)
            }


        }

    }

    private fun getUserData() {
        val databaseReference = FirebaseDatabase.getInstance().reference.child("Users").child(userUid)

        databaseReference.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (snapshot.exists()) {
                    val user = snapshot.getValue(User::class.java)

                    if (user != null) {
                        val userName = user.Name
                        val userPhone = user.Phone

                        var headerView: View = binding.navigationView.getHeaderView(0)
                        val navUsername: TextView = headerView.findViewById(R.id.txtName)
                        val navUserphone: TextView = headerView.findViewById(R.id.txtPhone)

                        navUsername.text = userName
                        navUserphone.text = userPhone

                    }

                }
            }

            override fun onCancelled(error: DatabaseError) {
                // Handle any database read errors here
            }
        })
    }

    private fun setupAttendanceListener(uid: String) {
        val currentMonth = getCurrentMonth()
        val currentYear = getCurrentYear()

        // Reference to the "Attendance" node for the current user, year, and month
        val attendanceRef = dbRef.child("Users").child(uid).child("Attendance").child(currentYear).child(currentMonth)

        // Add a ValueEventListener to listen for changes in the "Attendance" node
        attendanceRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                // Handle data changes here
                // Update your UI based on the changes in the snapshot
                // For example, you can call the methods to update year, month, and day counts
                getAttendanceCountForYear(uid)
                getAttendanceCountForMonth(uid)
                getAttendanceCountForDay(uid)

//                binding.translucentOverlay.visibility = View.GONE
//                binding.animationView.visibility = View.GONE
//                binding.floatingActionButton.isEnabled = true
//                binding.btnMenu.isEnabled = true
//                binding.btnYear.isEnabled = true
//                binding.btnMonth.isEnabled = true
            }

            override fun onCancelled(error: DatabaseError) {
                // Handle errors here
                Log.e("FirebaseListener", "Error listening for changes: ${error.message}")
            }
        })
    }

    // Status of Day
    private fun getAttendanceCountForDay(uid: String) {

        val currentMonth = getCurrentMonth()
        val currentYear = getCurrentYear()

        val currentDate = Date()
        val dateFormatter = SimpleDateFormat("dd-MM-yyyy")
        val formattedDate = dateFormatter.format(currentDate)
        binding.txtDate.text = formattedDate

        var isTodayPresent = false

        dbRef.child("Users").child(uid).child("Attendance").child(currentYear).child(currentMonth)
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    for (daySnap in snapshot.children) {
                        val date = daySnap.child("date").value.toString()
                        val category = daySnap.child("category").value.toString()
                        val mappedCategory = mapEnglishToSelectedLanguage(category)
                        if (date == formattedDate && mappedCategory == getString(R.string.status_currentDay)) {
                            binding.txtDay.text = getString(R.string.status_currentDay)
                            isTodayPresent = true
                            binding.btnDay.isEnabled = false
                            break
                        }
                        else if(date == formattedDate && mappedCategory == getString(R.string.status_currentPresent)){
                            binding.txtDay.text = getString(R.string.status_currentPresent)
                            isTodayPresent = true
                            binding.btnDay.isEnabled = false
                            break
                        }
                    }

                    if (!isTodayPresent){
                        binding.txtDay.text = getString(R.string.pending)

                        binding.apply {

                            btnDay.isEnabled = true
                            btnDay.setOnClickListener {
                                val intent = Intent(this@MainActivity, AddAttendanceActivity::class.java)
                                intent.putExtra("MODE", "ADD")
                                startActivity(intent)
                            }
                        }
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    Toast.makeText(this@MainActivity, error.message, Toast.LENGTH_SHORT).show()
                }
            })
    }
    // Status of Month
    private fun getAttendanceCountForMonth(uid: String) {
        var currentMonth = getCurrentMonth()
        val currentYear = getCurrentYear()

        binding.txtMonth.text = currentMonth

        dbRef.child("Users").child(uid).child("Attendance").child(currentYear).child(currentMonth)
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    var present = 0
                    var absent = 0

                    for (daySnap in snapshot.children) {
                        val category = daySnap.child("category").value.toString()
                        val mappedCategory = mapEnglishToSelectedLanguage(category)
                        if (mappedCategory == getString(R.string.status_currentPresent)) {
                            present++
                        } else if (mappedCategory == getString(R.string.status_currentDay)) {
                            absent++
                        }
                    }

                    binding.presentMonth.text = "$present"
                    binding.absentMonth.text = "$absent"
                }

                override fun onCancelled(error: DatabaseError) {
                    Toast.makeText(this@MainActivity, error.message, Toast.LENGTH_SHORT).show()
                }
            })
    }
    private fun getCurrentMonth(): String {
        val calendar = Calendar.getInstance()
        val monthFormatter = SimpleDateFormat("MMMM", Locale("en", "IN"))
        return monthFormatter.format(calendar.time)
    }
    // Status of Year
    private fun getAttendanceCountForYear(uid: String) {
        val currentYear = getCurrentYear()
        binding.yearTxt.text = getString(R.string.total_present_in, currentYear)

        dbRef.child("Users").child(uid).child("Attendance").child(currentYear).addListenerForSingleValueEvent(object : ValueEventListener {
                @SuppressLint("StringFormatMatches")
                override fun onDataChange(snapshot: DataSnapshot) {

                    var count = 0
                    for (monthSnap in snapshot.children) {
                        for (daySnap in monthSnap.children) {
                            val category = daySnap.child("category").value.toString()
                            val mappedCategory = mapEnglishToSelectedLanguage(category)
                            if (mappedCategory == getString(R.string.status_currentPresent)) {
                                count++
                            }
                        }
                    }
                    binding.dayPresentYear.text = getString(R.string.days, count)
                }

                override fun onCancelled(error: DatabaseError) {
                    Toast.makeText(this@MainActivity, error.message, Toast.LENGTH_SHORT).show()
                }
            })
    }


    private fun getCurrentYear(): String {
        val calendar = Calendar.getInstance()
        return calendar.get(Calendar.YEAR).toString()
    }

    private fun mapEnglishToSelectedLanguage(englishTerm: String): String {
        // You can replace the translations based on your language requirements
        val selectedLanguage = LocaleHelper.getLanguage(this)
        return when (selectedLanguage) {
            "gu" -> mapEnglishToGujarati(englishTerm)
            "hi" -> mapEnglishToHindi(englishTerm)
            "mr" -> mapEnglishToMarathi(englishTerm)
            else -> {
                englishTerm // Default to English if the language is not supported
            }
        }
    }
    private fun mapEnglishToGujarati(englishTerm: String): String {
        return when (englishTerm) {
            "P" -> "P"
            "A" -> "A"
            else -> englishTerm // Keep unchanged if not Present or Absent

        }
    }
    private fun mapEnglishToHindi(englishTerm: String): String {
        return when (englishTerm) {
            "P" -> "P"
            "A" -> "A"
            else -> englishTerm

        }
    }
    private fun mapEnglishToMarathi(englishTerm: String): String {
        return when (englishTerm) {
            "P" -> "P"
            "A" -> "A"
            else -> englishTerm

        }
    }

//    override fun onResume() {
//        super.onResume()
//
//        getUserData()
//    }
}
