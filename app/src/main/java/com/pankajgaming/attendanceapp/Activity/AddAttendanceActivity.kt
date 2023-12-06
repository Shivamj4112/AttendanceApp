package com.pankajgaming.attendanceapp.Activity


import android.annotation.SuppressLint
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Bundle
import android.os.Handler
import android.text.TextUtils
import android.util.Log
import android.view.View
import android.widget.EditText
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.github.gcacace.signaturepad.views.SignaturePad
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.datepicker.CalendarConstraints
import com.google.android.material.datepicker.DateValidatorPointBackward
import com.google.android.material.datepicker.MaterialDatePicker
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageReference
import com.pankajgaming.attendanceapp.ImageAdapter
import com.pankajgaming.attendanceapp.DataClass.ImageItem
import com.pankajgaming.attendanceapp.LocaleHelper
import com.pankajgaming.attendanceapp.Manager.NetworkManager
import com.pankajgaming.attendanceapp.ModelClass.ModelClass
import com.pankajgaming.attendanceapp.R
import com.pankajgaming.attendanceapp.databinding.ActivityAddAttendanceBinding
import java.io.ByteArrayOutputStream
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale


class AddAttendanceActivity : AppCompatActivity() {

    private lateinit var binding: ActivityAddAttendanceBinding
    private lateinit var databaseReference: DatabaseReference
    private lateinit var newKey: String
    private var selectedYear: String = ""
    private lateinit var dbStorage: StorageReference
    private lateinit var selectedMonth: String
    private lateinit var newYear: String
    private lateinit var auth: FirebaseAuth
    private lateinit var uid: String
    private lateinit var setImage: Bitmap
    private lateinit var setName: String
    private var selectedImageName: String? = null
    private var selectedImageUrl: String? = null


    @SuppressLint("SimpleDateFormat")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAddAttendanceBinding.inflate(layoutInflater)
        setContentView(binding.root)

        auth = FirebaseAuth.getInstance()
        val user = auth.currentUser
        uid = user?.uid.toString()
        dbStorage = FirebaseStorage.getInstance().reference

        val database = FirebaseDatabase.getInstance()
        databaseReference = database.reference
        newKey = databaseReference.push().key!!

        initView()


    }

    @SuppressLint("MissingInflatedId", "SimpleDateFormat", "InflateParams")
    private fun initView() {
        binding.apply {


            val snack = Snackbar.make(binding.root, "No Internet", Snackbar.LENGTH_INDEFINITE)

            val networkManager = NetworkManager(this@AddAttendanceActivity)

            networkManager.observe(this@AddAttendanceActivity) {

                if (it){
                    binding.translucentOverlay.visibility = View.VISIBLE
                    binding.animationView.visibility = View.VISIBLE

                    snack.dismiss()

                    Handler().postDelayed({
                        binding.translucentOverlay.visibility = View.GONE
                        binding.animationView.visibility = View.GONE
                        binding.btnAddSign.isEnabled = true
                        binding.btnDate.isEnabled = true
                        binding.btnCategory.isEnabled = true
                        binding.btnAddSign.isEnabled = true
                        binding.btnSave.isEnabled = true
                        binding.edtMessage.isEnabled = true
                        binding.edtWithdraw.isEnabled = true
                        binding.sign.isEnabled = true

                    }, 2000)


                }
                else{
                    binding.translucentOverlay.visibility = View.VISIBLE
                    binding.animationView.visibility = View.VISIBLE
                    binding.btnAddSign.isEnabled = false
                    binding.btnDate.isEnabled = false
                    binding.btnCategory.isEnabled = false
                    binding.btnAddSign.isEnabled = false
                    binding.btnSave.isEnabled = false
                    binding.edtMessage.isEnabled = false
                    binding.edtWithdraw.isEnabled = false
                    binding.sign.isEnabled = false

                    Handler().postDelayed({
                        binding.translucentOverlay.visibility = View.GONE
                        binding.animationView.visibility = View.GONE
                        snack.show()

                    }, 10000)
                }

            }


            sign.setOnClickListener {
                val dialog = BottomSheetDialog(this@AddAttendanceActivity)
                val view = layoutInflater.inflate(R.layout.bottom_images, null)

                val imgAdapter = view.findViewById<RecyclerView>(R.id.imgAdpater)

                // Set up Firebase Storage reference
                val storageRef = FirebaseStorage.getInstance().reference.child("userSign").child(uid)

                // Retrieve image URLs from Firebase Storage
                storageRef.listAll().addOnSuccessListener { result ->
                    val imageList = mutableListOf<ImageItem>()

                    for (fileRef in result.items) {
                        fileRef.downloadUrl.addOnSuccessListener { uri ->
                            // Add each image URL to the list
                            val imageUrl = uri.toString()
                            val imageName = fileRef.name
                            imageList.add(ImageItem(imageUrl, imageName))

                            // Check if the loop is at the last item and then update the adapter
                            if (imageList.size == result.items.size) {
                                // Set the layout manager

                                imgAdapter.layoutManager = LinearLayoutManager(this@AddAttendanceActivity)

                                // Update the adapter with the new data
                                imgAdapter.adapter = ImageAdapter(imageList) { clickedImageUrl, clickedImageName ->
                                        // Handle item click here
                                        // Set the clicked image into binding.img
                                        Glide.with(this@AddAttendanceActivity).load(clickedImageUrl).into(binding.img)
                                        sign.hint = clickedImageName
                                        sign.setHintTextColor(resources.getColor(R.color.black))



                                    // Update the selected image details
                                        selectedImageName = clickedImageName
                                        selectedImageUrl = clickedImageUrl

                                        // Dismiss the dialog if needed
                                        dialog.dismiss()
                                    }
                            }
                        }
                    }
                }.addOnFailureListener {
                    // Handle failure to retrieve image URLs
                    Toast.makeText(this@AddAttendanceActivity, "Failed to retrieve sign", Toast.LENGTH_SHORT).show()
                }

                dialog.setCancelable(true)
                dialog.setContentView(view)
                dialog.show()
            }

            btnAddSign.setOnClickListener {

                val dialog = BottomSheetDialog(this@AddAttendanceActivity)
                val view = layoutInflater.inflate(R.layout.bottom_sheet_dialog, null)

                val saveSignature = view.findViewById<CardView>(R.id.btnSave)
                val clearSignature = view.findViewById<CardView>(R.id.btnClear)
                val signaturePad = view.findViewById<SignaturePad>(R.id.signature_pad)

                saveSignature.setCardBackgroundColor(resources.getColor(R.color.orange))
                saveSignature.radius = 10F
                saveSignature.elevation = 6F

                clearSignature.setCardBackgroundColor(resources.getColor(R.color.orange))
                clearSignature.radius = 10F
                clearSignature.elevation = 6F

                saveSignature.setOnClickListener {
                    val bitmap = signaturePad.transparentSignatureBitmap
                    dialog.dismiss()

                    val dialog = BottomSheetDialog(this@AddAttendanceActivity)
                    val view = layoutInflater.inflate(R.layout.bottom_sheet_name, null)
                    val saveSignature = view.findViewById<CardView>(R.id.btnSave)
                    val name = view.findViewById<EditText>(R.id.edtName)

                    saveSignature.setCardBackgroundColor(resources.getColor(R.color.orange))
                    saveSignature.radius = 10F
                    saveSignature.elevation = 6F

                    saveSignature.setOnClickListener {

                        if (TextUtils.isEmpty(name.text.toString())) {
                            Toast.makeText(this@AddAttendanceActivity, "Name is Empty", Toast.LENGTH_SHORT).show()
                        } else {
                            val dbStorage = FirebaseStorage.getInstance().reference

                            setImage = bitmap
                            setName = name.text.toString().trim().replace(" ", "_")


                            val imageRef = dbStorage.child("userSign/$uid/$setName")

                            val baos = ByteArrayOutputStream()
                            bitmap.compress(Bitmap.CompressFormat.PNG, 100, baos)
                            val data = baos.toByteArray()

                            val uploadTask = imageRef.putBytes(data)

                            uploadTask.addOnCompleteListener { task ->
                                if (task.isSuccessful) {
                                    imageRef.downloadUrl.addOnSuccessListener {

                                        val downloadUri = task.result
                                        imageRef.child(downloadUri.toString())

                                    }
                                } else {
                                    // Handle unsuccessful upload
                                    Toast.makeText(this@AddAttendanceActivity, "Failed to upload sign", Toast.LENGTH_SHORT).show()
                                }
                            }

                            dialog.dismiss()
                        }
                    }

                    dialog.setCancelable(true)
                    dialog.setContentView(view)
                    dialog.show()

                }

                clearSignature.setOnClickListener {
                    signaturePad.clear()
                }

                dialog.setCancelable(true)
                dialog.setContentView(view)
                dialog.show()
            }

            btnBack.setOnClickListener {
                finish()
            }

            if (intent.getStringExtra("MODE").equals("ADD")) {

                title = "Add"

                btnDate.setOnClickListener {

                    val constraintsBuilder = CalendarConstraints.Builder()
                        .setValidator(DateValidatorPointBackward.now())
                        .setEnd(System.currentTimeMillis())

                    val datePicker = MaterialDatePicker.Builder.datePicker()
                        .setCalendarConstraints(constraintsBuilder.build())
                        .build()
                    datePicker.show(supportFragmentManager, "DatePicker")


                    // Setting up the event for when ok is clicked
                    datePicker.addOnPositiveButtonClickListener {

                        val calendar = Calendar.getInstance()
                        calendar.timeInMillis = it
                        selectedYear = calendar.get(Calendar.YEAR).toString()
                        selectedMonth = calendar.get(Calendar.MONTH).toString()

                        val monthFormatter = SimpleDateFormat("MMMM", Locale.getDefault())
                        selectedMonth = monthFormatter.format(calendar.time)

                        // formatting date in dd-mm-yyyy format.
                        val dateFormatter = SimpleDateFormat("dd-MM-yyyy")
                        val date = dateFormatter.format(Date(it))
                        btnDate.setText(date)

                    }

                    // Setting up the event for when cancelled is clicked
                    datePicker.addOnNegativeButtonClickListener {

                    }

                    // Setting up the event for when back button is pressed
                    datePicker.addOnCancelListener {
                    }
                }

                btnSave.setOnClickListener {
                    val date = binding.btnDate.text.toString()
                    val message = binding.edtMessage.text.toString()
                    val category = binding.btnCategory.text.toString()
                    val amount = binding.edtWithdraw.text.toString()
                    var selectedYears = ""
                    selectedYears = selectedYear

                    if (selectedYears.isEmpty()){
                        binding.btnDate.error = getString(R.string.enter_date)
                    }
                    else if (TextUtils.isEmpty(date) && TextUtils.isEmpty(category) && TextUtils.isEmpty(edtWithdraw.toString())) {
                        Toast.makeText(this@AddAttendanceActivity, getString(R.string.please_add_some_data), Toast.LENGTH_LONG).show()
                    } else if (TextUtils.isEmpty(date)) {

                        binding.btnDate.error = getString(R.string.enter_date)
                    } else if (TextUtils.isEmpty(category)) {
                        binding.btnCategory.error = getString(R.string.enter_category)

                    } else if (TextUtils.isEmpty(edtWithdraw.toString())) {
                        binding.btnCategory.error = getString(R.string.enter_amount)
                    }else {

                        addDatatoFirebase(selectedYears, date, message, category, selectedMonth,amount)
                    }

                }

            } else {

                title = "Update"

                val oldYear = intent.extras?.getString("year")!!
                val oldMonth = intent.extras?.getString("month")!!
                val oldDate = intent.extras?.getString("date")!!
                val oldMessage = intent.extras?.getString("message")
                val oldCategory = intent.extras?.getString("category")
                val oldUserSign = intent.extras?.getString("userSign")!!
                val oldUserName = intent.extras?.getString("userSignName")
                val oldUserAmount = intent.extras?.getString("amount")!!

                if (oldUserName!!.isEmpty() && oldUserSign.isEmpty()){

                }
                else{
                    Glide.with(this@AddAttendanceActivity).load(oldUserSign).into(binding.img)
                    sign.hint = oldUserName
                    sign.setHintTextColor(resources.getColor(R.color.black))
                }


                btnDate.setText(oldDate)
                edtMessage.setText(oldMessage.toString())
                btnCategory.setText(oldCategory.toString(), false)
                edtWithdraw.setText(oldUserAmount)

                newYear = oldYear
                selectedMonth = oldMonth


                btnDate.setOnClickListener {

                    val constraintsBuilder = CalendarConstraints.Builder()
                        .setValidator(DateValidatorPointBackward.now())
                        .setEnd(System.currentTimeMillis())

                    val datePicker = MaterialDatePicker.Builder.datePicker()
                        .setCalendarConstraints(constraintsBuilder.build())
                        .build()

                    datePicker.show(supportFragmentManager, "DatePicker")

                    datePicker.addOnPositiveButtonClickListener {

                        val calendar = Calendar.getInstance()
                        calendar.timeInMillis = it
                        newYear = calendar.get(Calendar.YEAR).toString()
//                        selectedMonth = calendar.get(Calendar.MONTH).toString()
                        val monthFormatter = SimpleDateFormat("MMMM", Locale.getDefault())
                        selectedMonth = monthFormatter.format(calendar.time)

                        // formatting date in dd-mm-yyyy format.
                        val dateFormatter = SimpleDateFormat("dd-MM-yyyy")
                        val date = dateFormatter.format(Date(it))
                        btnDate.setText(date)
                    }

                    // Setting up the event for when cancelled is clicked
                    datePicker.addOnNegativeButtonClickListener {
                        btnDate.setText(oldDate)
                        newYear = oldYear
                    }

                    // Setting up the event for when back button is pressed
                    datePicker.addOnCancelListener {
                        btnDate.setText(oldDate)
                        newYear = oldYear
                    }
                    btnDate.setText(oldDate)
                    newYear = oldYear
                }

                btnSave.setOnClickListener {

                    val newDate = binding.btnDate.text.toString()
                    val newMessage = binding.edtMessage.text.toString()
                    val newCategory = binding.btnCategory.text.toString()
                    val amount = binding.edtWithdraw.text.toString()

                    if (TextUtils.isEmpty(newDate) && TextUtils.isEmpty(newCategory) && TextUtils.isEmpty(amount)) {
                        Toast.makeText(this@AddAttendanceActivity, getString(R.string.please_add_some_data), Toast.LENGTH_LONG).show()
                    } else if (TextUtils.isEmpty(newDate)) {

                        binding.btnDate.error = getString(R.string.enter_date)
                    } else if (TextUtils.isEmpty(newCategory)) {
                        binding.btnCategory.error = getString(R.string.enter_category)

                    } else if (TextUtils.isEmpty(amount)) {
                        binding.btnCategory.error = getString(R.string.enter_amount)
                    }else {
                        updateDataChance(oldYear, newYear, oldMonth, selectedMonth, newDate, newMessage, newCategory, oldDate, amount)
                    }
                }
            }
        }
    }


    private fun addDatatoFirebase(currentYear: String, date: String, message: String, category: String, month: String,amount:String) {

        val mappedCategory = mapEnglishToSelectedLanguage(category)

        val toPath = "Users/$uid/Attendance/$currentYear/$month/$date"

        // Reference to the location where you want to add the new data
        val newAttendanceRef = databaseReference.child(toPath)

        // Update the database with the download URL and other data
        val model = ModelClass(date, message, mappedCategory, selectedYear, month,selectedImageUrl,selectedImageName,amount)
        newAttendanceRef.setValue(model).addOnCompleteListener { taskId ->
            if (taskId.isSuccessful) {
                Toast.makeText(this@AddAttendanceActivity, getString(R.string.data_added), Toast.LENGTH_SHORT).show()
                finish()
            } else {
                // Handle any failure in adding data
                Toast.makeText(this@AddAttendanceActivity, getString(R.string.data_not_added), Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun updateDataChance(oldYear: String, newYear: String, oldMonth: String, newMonth: String, newDate: String, newMessage: String, newCategory: String, oldDate: String,amount:String) {
        val databaseReference = FirebaseDatabase.getInstance().reference
        val mappedCategory = mapEnglishToSelectedLanguage(newCategory)

        val fromPath = "Users/$uid/Attendance/$oldYear/$oldMonth/$oldDate"
        val toPath = "Users/$uid/Attendance/$newYear/$newMonth/$newDate"


        // Update the database with the download URL and other data
        val model = ModelClass(newDate, newMessage, mappedCategory, newYear, newMonth, selectedImageUrl,selectedImageName,amount)
        databaseReference.child(toPath).setValue(model).addOnCompleteListener { taskId ->
            if (taskId.isSuccessful) {
                Toast.makeText(
                    this@AddAttendanceActivity,
                    getString(R.string.data_updated),
                    Toast.LENGTH_SHORT
                ).show()

                if (oldDate != newDate) {

                }
                finish()
            } else {
                // Handle any failure in updating data
                Toast.makeText(
                    this@AddAttendanceActivity,
                    getString(R.string.data_not_updated),
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    private fun mapEnglishToSelectedLanguage(englishTerm: String): String {
        // You can replace the translations based on your language requirements
        val selectedLanguage = LocaleHelper.getLanguage(this)
        return when (selectedLanguage) {
            "gu" -> mapGujaratiToEnglish(englishTerm)
            "hi" -> mapHindiToEnglish(englishTerm)
            "mr" -> mapMarathiToEnglish(englishTerm)
            else -> {
                Log.d("LanguageDebug", "Unsupported language: $selectedLanguage")
                englishTerm // Default to English if the language is not supported
            }
        }
    }
    private fun mapGujaratiToEnglish(englishTerm: String): String {
        return when (englishTerm) {
            "હાજર" -> "P"
            "ગેરહાજર" -> "A"
            else -> englishTerm // Keep unchanged if not Present or Absent

        }
    }
    private fun mapHindiToEnglish(englishTerm: String): String {
        return when (englishTerm) {
            "उपस्थित" -> "P"
            "अनुपस्थित" -> "A"
            else -> englishTerm

        }
    }
    private fun mapMarathiToEnglish(englishTerm: String): String {
        return when (englishTerm) {
            "उपस्थित" -> "P"
            "अनुपस्थित" -> "A"
            else -> englishTerm

        }
    }

    override fun onBackPressed() {
        super.onBackPressed()
        finish()
    }
}
