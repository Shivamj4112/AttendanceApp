package com.pankajgaming.attendanceapp.Activity

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import android.net.Uri
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Environment
import android.text.Html
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.FileProvider
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.pankajgaming.attendanceapp.Adapter.NotesAdapter
import com.pankajgaming.attendanceapp.ModelClass.ModelClass
import com.pankajgaming.attendanceapp.R
import com.pankajgaming.attendanceapp.databinding.ActivityMonthAttendanceBinding
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class MonthAttendanceActivity : AppCompatActivity() {

    private lateinit var binding : ActivityMonthAttendanceBinding
    private lateinit var dbRef: DatabaseReference
    private lateinit var adapter: NotesAdapter
    private lateinit var model: ModelClass
    lateinit var uid:String
    private lateinit var userName:String
    private lateinit var userPhone:String
    private var list = ArrayList<ModelClass>()


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMonthAttendanceBinding.inflate(layoutInflater)
        setContentView(binding.root)

        dbRef = FirebaseDatabase.getInstance().reference
        uid = intent.getStringExtra("uid")!!
        userName = intent.getStringExtra("userName")!!
        userPhone = intent.getStringExtra("userPhone")!!

        adapter = NotesAdapter()
        binding.attendanceRecyler.layoutManager = LinearLayoutManager(this@MonthAttendanceActivity, LinearLayoutManager.VERTICAL,false)
        binding.attendanceRecyler.adapter = adapter

        val currentYear = Calendar.getInstance().get(Calendar.YEAR)
        val currentMonth = getCurrentMonth()
        gettingDataFirebase(currentYear.toString(),currentMonth)

        binding.btnBack.setOnClickListener {
            finish()
        }

        binding.btnDownload.setOnClickListener {

            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE), PackageManager.PERMISSION_GRANTED)

            createPdf(currentYear.toString(),currentMonth)
        }

    }


    private fun createPdf(currentYear: String,currentMonth: String) {
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

            canvas1!!.drawText("Mobile: $userPhone", marginStart, yPosition, textPaint)
            yPosition += 30f

            canvas1!!.drawText("Month: $currentMonth", marginStart, yPosition, textPaint)
            yPosition += 30f

            canvas1!!.drawText("Year: $currentYear", marginStart, yPosition, textPaint)
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
            val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
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
                    this@MonthAttendanceActivity,
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


    private fun gettingDataFirebase(selectedYear: String, currentMonth: String) {

        dbRef.root.child("Users").child(uid).child("Attendance").child(selectedYear).child(currentMonth).addValueEventListener(object :
            ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {

                val tempList = ArrayList<ModelClass>()
                    for (daySnap in snapshot.children) {
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

                val sortedList = tempList.sortedByDescending { it.date }
                val sortedArrayList = ArrayList<ModelClass>(sortedList)

                list.clear()
                list.addAll(sortedArrayList)


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
                    val alertDialog = AlertDialog.Builder(this@MonthAttendanceActivity)
                        .setTitle(getString(R.string.delete))
                        .setMessage(
                            Html.fromHtml(
                                getString(R.string.are_you_sure_you_want_to_delete) + "<b>" + it.date + "</b>",
                                Html.FROM_HTML_MODE_LEGACY
                            )
                        )
                        .setCancelable(true)
                        .setPositiveButton(getString(R.string.yes)) { dialog, which ->

                            Toast.makeText(this@MonthAttendanceActivity,
                                getString(R.string.data_deleted), Toast.LENGTH_SHORT).show()

//                            val media = MediaPlayer.create(this@MonthAttendanceActivity, R.raw.data_delete)
//                            media.start()


                            val itemRef = dbRef.root.child("Users").child(uid).child("Attendance").child(selectedYear).child(it.month).child(it.date)
                            itemRef.removeValue()
                            itemRef.removeEventListener(this)
                        }
                        .setNegativeButton(getString(R.string.no)) { dialog, which ->
                            dialog.dismiss()
                        }
                    alertDialog.show()
                }

                adapter.onItemClick = { it ->

                    var intent = Intent(this@MonthAttendanceActivity, AddAttendanceActivity::class.java)
                    intent.putExtra("date", it.date)
                    intent.putExtra("message", it.message)
                    intent.putExtra("category",it.category )
                    intent.putExtra("year",it.year)
                    intent.putExtra("month",it.month)
                    intent.putExtra("uid", uid)
                    intent.putExtra("userSign",it.sign)
                    intent.putExtra("userSignName",it.signName)
                    intent.putExtra("amount",it.amount)

                    startActivity(intent)

                }
            }
            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(this@MonthAttendanceActivity, error.message, Toast.LENGTH_SHORT).show()
            }

        })
    }
    private fun getCurrentMonth(): String {
        var selectedMonth:String
        val calendar = Calendar.getInstance()
        val monthFormatter = SimpleDateFormat("MMMM", Locale.getDefault())
        selectedMonth = monthFormatter.format(calendar.time)
        return selectedMonth
    }
    override fun onBackPressed() {
        super.onBackPressed()
        finish()
    }
}