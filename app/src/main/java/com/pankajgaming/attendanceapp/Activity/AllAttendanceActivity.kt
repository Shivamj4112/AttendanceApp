package com.pankajgaming.attendanceapp.Activity

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.os.Handler
import android.text.Html
import android.util.Base64
import android.util.Log
import android.view.View
import android.widget.NumberPicker
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.FileProvider
import androidx.recyclerview.widget.LinearLayoutManager
import com.bumptech.glide.Glide
import com.bumptech.glide.request.target.SimpleTarget
import com.bumptech.glide.request.transition.Transition
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.google.firebase.storage.FirebaseStorage
import com.pankajgaming.attendanceapp.LocaleHelper
import com.pankajgaming.attendanceapp.Manager.NetworkManager
import com.pankajgaming.attendanceapp.ModelClass.ModelClass
import com.pankajgaming.attendanceapp.NotesAdapter
import com.pankajgaming.attendanceapp.R
import com.pankajgaming.attendanceapp.databinding.ActivityAllAttendanceBinding
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale


class AllAttendanceActivity : AppCompatActivity() {

    private lateinit var binding: ActivityAllAttendanceBinding
    private lateinit var dbRef: DatabaseReference
    private lateinit var adapter: NotesAdapter
    private lateinit var model: ModelClass
    private lateinit var uid: String
    private lateinit var userName: String
    private lateinit var userPhone: String
    private lateinit var selectedYear: String
    private var list = ArrayList<ModelClass>()


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAllAttendanceBinding.inflate(layoutInflater)
        setContentView(binding.root)

        dbRef = FirebaseDatabase.getInstance().reference
        uid = intent.getStringExtra("uid")!!
        userName = intent.getStringExtra("userName")!!
        userPhone = intent.getStringExtra("userPhone")!!

        adapter = NotesAdapter()
        binding.attendanceRecyler.layoutManager =
            LinearLayoutManager(this@AllAttendanceActivity, LinearLayoutManager.VERTICAL, false)
        binding.attendanceRecyler.adapter = adapter


        val snack = Snackbar.make(binding.root, "No Internet", Snackbar.LENGTH_INDEFINITE)

        val networkManager = NetworkManager(this)

        networkManager.observe(this) {

            if (it) {
                binding.translucentOverlay.visibility = View.VISIBLE
                binding.animationView.visibility = View.VISIBLE

                snack.dismiss()

                Handler().postDelayed({
                    binding.translucentOverlay.visibility = View.GONE
                    binding.animationView.visibility = View.GONE
                    binding.selectYear.isEnabled = true
                    binding.btnDownload.isEnabled = true
                    binding.attendanceRecyler.isEnabled = true


                }, 2000)


            } else {
                binding.translucentOverlay.visibility = View.VISIBLE
                binding.animationView.visibility = View.VISIBLE
                binding.selectYear.isEnabled = false
                binding.btnDownload.isEnabled = false
                binding.attendanceRecyler.isEnabled = false

                Handler().postDelayed({
                    binding.translucentOverlay.visibility = View.GONE
                    binding.animationView.visibility = View.GONE
                    snack.show()

                }, 10000)
            }

        }

        val currentYear = Calendar.getInstance().get(Calendar.YEAR)
        binding.selectYear.text = currentYear.toString()

        binding.selectYear.setOnClickListener {

            val dialogView = layoutInflater.inflate(R.layout.item_layout_year, null)
            val numberPickerYear = dialogView.findViewById<NumberPicker>(R.id.filterYear)


            // Customize the number picker as needed
            val currentYear = Calendar.getInstance().get(Calendar.YEAR)
            numberPickerYear.minValue = 1990 // Adjust as needed
            numberPickerYear.maxValue = currentYear // Adjust as needed
            numberPickerYear.value = currentYear

            val dialog = AlertDialog.Builder(this)
                .setTitle(getString(R.string.selectYear))
                .setView(dialogView)
                .setPositiveButton(getString(R.string.ok)) { _, _ ->
                    selectedYear = numberPickerYear.value.toString()
                    binding.selectYear.text = selectedYear
                    gettingDataFirebase(selectedYear)
                }
                .setNegativeButton(getString(R.string.cancel)) { _, _ ->
                    // Do nothing on cancel
                }
                .create()

            dialog.show()
        }

        binding.btnBack.setOnClickListener {
            finish()
        }

        val selectedYear = this.binding.selectYear.text.toString()

        binding.btnDownload.setOnClickListener {
            val selectedYear = this.binding.selectYear.text.toString()
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE),
                PackageManager.PERMISSION_GRANTED
            )

            if (list.isEmpty()) {
                Toast.makeText(this, "No Data of this Year :- $selectedYear", Toast.LENGTH_LONG)
                    .show()
            } else {
                createPdf(selectedYear)
            }

        }

        gettingDataFirebase(selectedYear)

    }

        private fun createPdf(selectedYear: String) {
        val myPdfDocument = PdfDocument()
        val myPaint = Paint()

        // A4 paper size in points (1 inch = 72 points)
        val pageInfo1 = PdfDocument.PageInfo.Builder(595, 842, 1).create()
        var page1: PdfDocument.Page? = null
        var canvas1: Canvas? = null

        // Draw content from binding.layoutDownload
        binding.layoutDownload.post {
            page1 = myPdfDocument.startPage(pageInfo1)
            canvas1 = page1!!.canvas
            canvas1!!.drawColor(Color.WHITE) // Clear the canvas for the first page


            val centerX = (pageInfo1.pageWidth - binding.layoutDownload.width) / 2f

            // Draw the layout at the calculated X position
            canvas1!!.translate(centerX, 0f)
            binding.layoutDownload.draw(canvas1!!)
            canvas1!!.translate(-centerX, 0f)

            // Get the height of the drawn layout
            val layoutHeight = binding.layoutDownload.height

            // Calculate the position for additional text below the drawn layout
            val textPaint = Paint()
            textPaint.textSize = 16f
            var yPosition = layoutHeight + 0f // Start below the drawn layout
            //var yPosition = 80f // Start below the drawn layout

            val canvasWidth = pageInfo1.pageWidth.toFloat()

            val marginStart = 30f
            val marginEnd = 30f

            yPosition += 20f // Adjust the Y position for additional spacing

            canvas1!!.drawText("Name: $userName", marginStart, yPosition, textPaint)
            yPosition += 30f

            // Draw mobile number
            canvas1!!.drawText("Mobile: $userPhone", marginStart, yPosition, textPaint)
            yPosition += 30f

            // Draw year
            canvas1!!.drawText("Year: $selectedYear", marginStart, yPosition, textPaint)
            yPosition += 30f


            for (item in list) {
                val dateText = item.date
                val messageText = item.message
                val categoryText = item.category

                val messageWidth = textPaint.measureText(messageText)
                val categoryWidth = textPaint.measureText(categoryText)
                val maxWidth = maxOf(messageWidth, categoryWidth)

                // Check if the current page is finished
                if (yPosition + 80f > pageInfo1.pageHeight) {
                    // Start a new page
                    myPdfDocument.finishPage(page1)
                    val pageInfoNext = PdfDocument.PageInfo.Builder(595, 842, myPdfDocument.pages.size + 1).create()
                    page1 = myPdfDocument.startPage(pageInfoNext)
                    canvas1 = page1!!.canvas
                    canvas1!!.drawColor(Color.WHITE) // Clear the canvas for the new page
                    yPosition = 60f // Reset Y position for the new page
                }

                // Adjust Y position for the text inside the box
                val textYPosition = yPosition + 20f

                // Draw the box (stroke)
                myPaint.style = Paint.Style.STROKE
                canvas1!!.drawRect(marginStart, textYPosition - 20f, canvasWidth - marginEnd, textYPosition + 40f, myPaint)

                // Draw the text inside the box with adjusted positions
                canvas1!!.drawText(dateText, marginStart + 10f, textYPosition + 15f, textPaint)

                // Center the message text inside the box
                val messageStartX = (canvasWidth - maxWidth) / 2 + 10f
                canvas1!!.drawText(messageText, messageStartX, textYPosition + 15f, textPaint)

                // Align the category text to the end inside the box
                val categoryStartX = canvasWidth - marginEnd - categoryWidth - 10f
                canvas1!!.drawText(categoryText, categoryStartX, textYPosition + 15f, textPaint)

                // Increment Y position for the next item (considering box height)
                yPosition += 60f
            }

            myPdfDocument.finishPage(page1)

            val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
            val pdfFileName = "${userName}_$timeStamp.pdf"

            val directory = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), getString(
                R.string.app_name
            ))
//            Toast.makeText(this, "Downloading......", Toast.LENGTH_LONG).show()

            if (!directory.exists()) {
                directory.mkdirs()
            }
            val pdfFile = File(directory, pdfFileName)

            try {
                myPdfDocument.writeTo(FileOutputStream(pdfFile))

                sendBroadcast(Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE, Uri.fromFile(pdfFile)))

                Toast.makeText(this, ""+directory, Toast.LENGTH_SHORT).show()
                showDownloadNotification(pdfFile)

            } catch (e: IOException) {
                e.printStackTrace()
            }

            myPdfDocument.close()
        }
    }



//    private fun createPdf(selectedYear: String) {
//        val myPdfDocument = PdfDocument()
//        val myPaint = Paint()
//
//        // A4 paper size in points (1 inch = 72 points)
//        val pageInfo1 = PdfDocument.PageInfo.Builder(595, 842, 1).create()
//        var page1: PdfDocument.Page? = null
//        var canvas1: Canvas? = null
//
//        // Draw content from binding.layoutDownload
//        binding.layoutDownload.post {
//            page1 = myPdfDocument.startPage(pageInfo1)
//            canvas1 = page1!!.canvas
//            canvas1!!.drawColor(Color.WHITE) // Clear the canvas for the first page
//
//
//            val centerX = (pageInfo1.pageWidth - binding.layoutDownload.width) / 2f
//
//            // Draw the layout at the calculated X position
//            canvas1!!.translate(centerX, 0f)
//            binding.layoutDownload.draw(canvas1!!)
//            canvas1!!.translate(-centerX, 0f)
//
//            // Get the height of the drawn layout
//            val layoutHeight = binding.layoutDownload.height
//
//            // Calculate the position for additional text below the drawn layout
//            val textPaint = Paint()
//            textPaint.textSize = 16f
//            var yPosition = layoutHeight + 0f // Start below the drawn layout
//            //var yPosition = 80f // Start below the drawn layout
//
//            val canvasWidth = pageInfo1.pageWidth.toFloat()
//
//            val marginStart = 30f
//            val marginEnd = 30f
//
//            yPosition += 20f // Adjust the Y position for additional spacing
//
//            canvas1!!.drawText("Name: $userName", marginStart, yPosition, textPaint)
//            yPosition += 30f
//
//            // Draw mobile number
//            canvas1!!.drawText("Mobile: $userPhone", marginStart, yPosition, textPaint)
//            yPosition += 30f
//
//            // Draw year
//            canvas1!!.drawText("Year: $selectedYear", marginStart, yPosition, textPaint)
//            yPosition += 30f
//
//            val storage = FirebaseStorage.getInstance()
//            val storageRef = storage.reference.child("userSign").child(uid).child("shivam_sign")
//
//            Glide.with(this@AllAttendanceActivity)
//                .asBitmap()
//                .load(storageRef)
//                .into(object : SimpleTarget<Bitmap>() {
//                    override fun onResourceReady(resource: Bitmap, transition: Transition<in Bitmap>?) {
//                        // Resize the bitmap if needed
//                        val resizedBitmap = Bitmap.createScaledBitmap(resource, 60, 60, false)
//
//                        // Draw the bitmap on the canvas
//                        canvas1!!.drawBitmap(resizedBitmap, marginStart, yPosition, myPaint)
//                        yPosition += 30f
//
//                        Log.d("Bitmap", "Bitmap loaded successfully")
//                    }
//                })
//
//
//            for (item in list) {
//                val dateText = item.date
//                val messageText = item.message
//                val categoryText = item.category
//                val amountText = item.amount
//
//                val messageWidth = textPaint.measureText(messageText)
//                val categoryWidth = textPaint.measureText(categoryText)
//                val amountWidth = textPaint.measureText(amountText)
//                val maxWidth = maxOf(messageWidth, categoryWidth, amountWidth)
//
//                // Calculate equal spacing between elements
//                val spacing = (canvasWidth - 2 * marginStart - maxWidth - amountWidth) / 4f
//
//                // Check if the current page is finished
//                if (yPosition + 80f > pageInfo1.pageHeight) {
//                    // Start a new page
//                    myPdfDocument.finishPage(page1)
//                    val pageInfoNext = PdfDocument.PageInfo.Builder(595, 842, myPdfDocument.pages.size + 1).create()
//                    page1 = myPdfDocument.startPage(pageInfoNext)
//                    canvas1 = page1!!.canvas
//                    canvas1!!.drawColor(Color.WHITE) // Clear the canvas for the new page
//                    yPosition = 60f // Reset Y position for the new page
//                }
//
//                // Adjust Y position for the text inside the box
//                val textYPosition = yPosition + 20f
//
//                // Draw the box (stroke)
//                myPaint.style = Paint.Style.STROKE
//                canvas1!!.drawRect(marginStart, textYPosition - 20f, canvasWidth - marginEnd, textYPosition + 60f, myPaint)
//
//                // Draw the text inside the box with adjusted positions
//                canvas1!!.drawText(dateText, marginStart + 10f, textYPosition + 15f, textPaint)
//
//                // Draw the message text under the date
//                canvas1!!.drawText(messageText, marginStart + 10f, textYPosition + 40f, textPaint)
//
//                // Center the category text inside the box
//                val categoryStartX = marginStart + 2 * spacing + maxWidth
//                canvas1!!.drawText(categoryText, categoryStartX, textYPosition + 30f, textPaint)
//
//                // Draw the amount text to the right of the category text
//                val amountStartX = categoryStartX + spacing
//                canvas1!!.drawText(amountText, amountStartX, textYPosition + 30f, textPaint)
//
//
//                // Increment Y position for the next item (considering box height)
//                yPosition += 80f
//            }
//
//            myPdfDocument.finishPage(page1)
//
//            val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
//            val pdfFileName = "${userName}_$timeStamp.pdf"
//
//            val directory = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), getString(
//                R.string.app_name
//            ))
////            Toast.makeText(this, "Downloading......", Toast.LENGTH_LONG).show()
//
//            if (!directory.exists()) {
//                directory.mkdirs()
//            }
//            val pdfFile = File(directory, pdfFileName)
//
//            try {
//                myPdfDocument.writeTo(FileOutputStream(pdfFile))
//
//                sendBroadcast(Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE, Uri.fromFile(pdfFile)))
//
//                Toast.makeText(this, ""+directory, Toast.LENGTH_SHORT).show()
//                showDownloadNotification(pdfFile)
//
//            } catch (e: IOException) {
//                e.printStackTrace()
//            }
//
//            myPdfDocument.close()
//        }
//    }

    private fun showDownloadNotification(pdfFile: File) {
        val channelId = "download_channel"
        val notificationId = 1

        // Create a NotificationChannel for Android Oreo and higher
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "Download Channel",
                NotificationManager.IMPORTANCE_DEFAULT
            )
            val notificationManager =
                getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }

        val notificationBuilder = NotificationCompat.Builder(this, channelId)
            .setSmallIcon(R.drawable.icon_logo)
            .setContentTitle(getString(R.string.app_name))
            .setContentText(getString(R.string.pdf_downloaded_successfully))
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .setContentIntent(getOpenPdfIntent(pdfFile))

        // Show the notification
        with(NotificationManagerCompat.from(this)) {
            if (ActivityCompat.checkSelfPermission(
                    this@AllAttendanceActivity,
                    Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                // TODO: Consider calling
                //    ActivityCompat#requestPermissions
                // here to request the missing permissions, and then overriding
                //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
                //                                          int[] grantResults)
                // to handle the case where the user grants the permission. See the documentation
                // for ActivityCompat#requestPermissions for more details.
                return
            }
            notify(notificationId, notificationBuilder.build())
        }
    }

    private fun getOpenPdfIntent(pdfFile: File): PendingIntent {
        val intent = Intent(Intent.ACTION_VIEW)
        intent.setDataAndType(
            FileProvider.getUriForFile(
                this,
                "com.pankajgaming.attendanceapp.fileprovider",
                pdfFile
            ),
            "application/pdf"
        )
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)

        // Use PendingIntent.FLAG_IMMUTABLE to comply with changes in Android 12
        return PendingIntent.getActivity(
            this,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    private fun gettingDataFirebase(selectedYear: String) {

        dbRef.root.child("Users").child(uid).child("Attendance").child(selectedYear)
            .addValueEventListener(object :
                ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {

                    val tempList = ArrayList<ModelClass>()
//                list.clear()

                    for (monthSnap in snapshot.children) {

                        for (daySnap in monthSnap.children) {
                            model = ModelClass(
                                daySnap.child("date").value.toString(),
                                daySnap.child("message").value.toString(),
                                daySnap.child("category").value.toString(),
                                daySnap.child("year").value.toString(),
                                daySnap.child("month").value.toString(),
                                daySnap.child("sign").value.toString(),
                                daySnap.child("signName").value.toString(),
                                daySnap.child("amount").value.toString()
                            )
                            tempList.add(model)

                        }
                    }


                    val sortedList = tempList.sortedWith(compareByDescending<ModelClass> {
                        it.year.toInt()
                    }.thenByDescending {
                        it.monthAsNumber()
                    }.thenByDescending {
                        it.dateAsNumber()
                    })
                    list.clear()
                    list.addAll(sortedList)


                    binding.progressBar.visibility = View.INVISIBLE
                    binding.attendanceRecyler.visibility = View.VISIBLE
                    adapter.UpdateData(list)

                    if (list.isEmpty()) {
                        // Data is empty, show the image
                        binding.emptyDataImage.visibility = View.VISIBLE
                        binding.attendanceRecyler.visibility = View.GONE
                    } else {
                        // Data is present, hide the image
                        binding.emptyDataImage.visibility = View.GONE
                        binding.attendanceRecyler.visibility = View.VISIBLE
                    }

                    adapter.onLongClick = { it ->
                        val alertDialog = AlertDialog.Builder(this@AllAttendanceActivity)
                            .setTitle(getString(R.string.delete))
                            .setMessage(
                                Html.fromHtml(
                                    getString(R.string.are_you_sure_you_want_to_delete) + "<b>" + it.date + "</b>",
                                    Html.FROM_HTML_MODE_LEGACY
                                )
                            )
                            .setCancelable(true)
                            .setPositiveButton(getString(R.string.yes)) { dialog, which ->

                                Toast.makeText(
                                    this@AllAttendanceActivity,
                                    getString(R.string.data_deleted), Toast.LENGTH_SHORT
                                ).show()

//                            val media = MediaPlayer.create(this@AllAttendanceActivity, R.raw.data_delete)
//                            media.start()


                                val itemRef =
                                    dbRef.root.child("Users").child(uid).child("Attendance")
                                        .child(selectedYear).child(it.month).child(it.date)
                                itemRef.removeValue()
                                itemRef.removeEventListener(this)

                                val position = list.indexOf(it)
                                list.remove(it)
                                adapter.notifyItemRemoved(position)

                            }
                            .setNegativeButton(getString(R.string.no)) { dialog, which ->
                                dialog.dismiss()
                            }
                        alertDialog.show()
                    }

                    adapter.onItemClick = { it ->

                        var intent =
                            Intent(this@AllAttendanceActivity, AddAttendanceActivity::class.java)
                        intent.putExtra("date", it.date)
                        intent.putExtra("message", it.message)
                        intent.putExtra("category", it.category)
                        intent.putExtra("year", it.year)
                        intent.putExtra("month", it.month)
                        intent.putExtra("uid", uid)
                        intent.putExtra("userSign", it.sign)
                        intent.putExtra("userSignName", it.signName)
                        intent.putExtra("amount", it.amount)



                        startActivity(intent)

                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    Toast.makeText(this@AllAttendanceActivity, error.message, Toast.LENGTH_SHORT)
                        .show()
                }

            })
    }

    // Add these extension functions to your ModelClass
    fun ModelClass.monthAsNumber(): Int {
        return when (month) {
            "January" -> 1
            "February" -> 2
            "March" -> 3
            "April" -> 4
            "May" -> 5
            "June" -> 6
            "July" -> 7
            "August" -> 8
            "September" -> 9
            "October" -> 10
            "November" -> 11
            "December" -> 12
            else -> 0
        }
    }

    fun ModelClass.dateAsNumber(): Int {
        return date.split("-")[0].toInt()
    }

    override fun onBackPressed() {
        super.onBackPressed()
        finish()
    }

    override fun onResume() {
        super.onResume()

        val savedLanguage = LocaleHelper.getLanguage(this)
        LocaleHelper.setLocale(this, savedLanguage)


    }
}