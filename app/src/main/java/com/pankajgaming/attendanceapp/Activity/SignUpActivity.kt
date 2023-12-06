package com.pankajgaming.attendanceapp.Activity

import android.annotation.SuppressLint
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Handler
import android.text.TextUtils
import android.view.View
import android.widget.Toast
import com.google.android.material.datepicker.CalendarConstraints
import com.google.android.material.datepicker.DateValidatorPointBackward
import com.google.android.material.datepicker.MaterialDatePicker
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.FirebaseException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.PhoneAuthCredential
import com.google.firebase.auth.PhoneAuthOptions
import com.google.firebase.auth.PhoneAuthProvider
import com.google.firebase.database.Query
import com.google.firebase.database.ktx.database
import com.google.firebase.ktx.Firebase
import com.pankajgaming.attendanceapp.Manager.NetworkManager
import com.pankajgaming.attendanceapp.R
import com.pankajgaming.attendanceapp.databinding.ActivitySignUpBinding
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit

class SignUpActivity : AppCompatActivity() {

    private lateinit var binding : ActivitySignUpBinding
    private lateinit var selectedYear: String
    private lateinit var selectedMonth: String
    private lateinit var auth: FirebaseAuth
    private var verificationId: String? = null

    @SuppressLint("SimpleDateFormat")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySignUpBinding.inflate(layoutInflater)
        setContentView(binding.root)

        auth = FirebaseAuth.getInstance()

        val snack = Snackbar.make(binding.root, "No Internet", Snackbar.LENGTH_INDEFINITE)


        binding.apply {

            val networkManager = NetworkManager(this@SignUpActivity)

            networkManager.observe(this@SignUpActivity) {

                if (!it){

                    binding.translucentOverlay!!.visibility = View.VISIBLE
                    binding.animationView!!.visibility = View.VISIBLE
                    binding.edtDob.isEnabled = false
                    binding.btnSignup.isEnabled = false
                    binding.edtName.isEnabled = false
                    binding.edtPhone.isEnabled = false
                    binding.btnLogin.isEnabled = false
                    progressBar.visibility = View.VISIBLE

                    Handler().postDelayed({
                        binding.translucentOverlay!!.visibility = View.GONE
                        binding.animationView!!.visibility = View.GONE
                        progressBar.visibility = View.GONE
                        snack.show()

                    }, 10000)

                }
                else{
                    binding.translucentOverlay!!.visibility = View.VISIBLE
                    binding.animationView!!.visibility = View.VISIBLE
                    progressBar.visibility = View.VISIBLE

                    Handler().postDelayed({
                        binding.translucentOverlay!!.visibility = View.GONE
                        binding.animationView!!.visibility = View.GONE
                        progressBar.visibility = View.GONE
                        binding.edtDob.isEnabled = true
                        binding.btnSignup.isEnabled = true
                        binding.btnLogin.isEnabled = true
                        binding.edtName.isEnabled = true
                        binding.edtPhone.isEnabled = true
                        snack.dismiss()

                        edtDob.setOnClickListener {

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
                                edtDob.setText(date)
                            }

                            // Setting up the event for when cancelled is clicked
                            datePicker.addOnNegativeButtonClickListener {
                            }

                            // Setting up the event for when back button is pressed
                            datePicker.addOnCancelListener {
                            }
                        }
                        btnSignup.setOnClickListener {

//                binding.progressBar.visibility = View.VISIBLE

                            if(TextUtils.isEmpty(binding.edtName.text.toString()) && TextUtils.isEmpty(binding.edtDob.text.toString()) && TextUtils.isEmpty(binding.edtDob.text.toString())){
                                progressBar.visibility = View.INVISIBLE
                                binding.txtErrorPassword.text = getString(R.string.please_add_some_data)
                                binding.txtErrorPassword.setTextColor(resources.getColor(R.color.light_red))
                                binding.txtErrorPassword.visibility = View.VISIBLE
                            }
                            else{
                                progressBar.visibility = View.VISIBLE
                                val phone = "+91" + binding.edtPhone.text.toString()

                                checkIfPhoneNumberExists(phone)

                            }
                        }
                        btnLogin.setOnClickListener {
                            val intent = Intent(this@SignUpActivity, LoginActivity::class.java)
                            startActivity(intent)
                        }

                    }, 2000)

                }

            }
        }

    }

    private fun checkIfPhoneNumberExists(phone: String) {

        val usersRef = Firebase.database.reference.child("Users")

        val query: Query = usersRef.orderByChild("Phone").equalTo(phone)

        query.get().addOnSuccessListener { dataSnapshot ->
            if (dataSnapshot.exists()) {
                Toast.makeText(this@SignUpActivity, getString(R.string.this_phone_number_is_used), Toast.LENGTH_SHORT).show()
                binding.txtErrorPassword.text = getString(R.string.this_phone_number_is_used)
                binding.txtErrorPassword.setTextColor(resources.getColor(R.color.light_red))
                binding.txtErrorPassword.visibility = View.VISIBLE
                binding.progressBar.visibility = View.GONE
            } else {
                sendVerificationCode(phone)
                binding.txtErrorPassword.text = getString(R.string.otp_send)
                binding.txtErrorPassword.setTextColor(resources.getColor(R.color.green))
                binding.txtErrorPassword.visibility = View.VISIBLE
            }

        }.addOnFailureListener { e ->
            Toast.makeText(this@SignUpActivity, "Error executing query: ${e.message}", Toast.LENGTH_LONG).show()
        }



    }

    private fun sendVerificationCode(number: String) {
        // this method is used for getting OTP on user phone number.
        val options = PhoneAuthOptions.newBuilder(auth)
            .setPhoneNumber(number) // Phone number to verify
            .setTimeout(60L, TimeUnit.SECONDS) // Timeout and unit
            .setActivity(this) // Activity (for callback binding)
            .setCallbacks(mCallBack) // OnVerificationStateChangedCallbacks
            .build()
        PhoneAuthProvider.verifyPhoneNumber(options)
    }
    private val mCallBack: PhoneAuthProvider.OnVerificationStateChangedCallbacks = object : PhoneAuthProvider.OnVerificationStateChangedCallbacks() {
        // below method is used when OTP is sent from Firebase
        override fun onCodeSent(receiveOtp: String, forceResendingToken: PhoneAuthProvider.ForceResendingToken) {
            super.onCodeSent(receiveOtp, forceResendingToken)
            // when we receive the OTP it contains a unique id which we are storing in our string  which we have already created.
            verificationId = receiveOtp

            binding.progressBar.visibility = View.INVISIBLE

            val intent = Intent(this@SignUpActivity, OtpVerificationActivity::class.java)
            intent.putExtra("verificationId",verificationId)
            intent.putExtra("phone","+91" + binding.edtPhone.text.toString())
            intent.putExtra("name",binding.edtName.text.toString())
            intent.putExtra("dob",binding.edtDob.text.toString())
            intent.putExtra("mode","AccountCreate")
            startActivity(intent)
        }

        // this method is called when user receive OTP from Firebase.
        override fun onVerificationCompleted(phoneAuthCredential: PhoneAuthCredential) {
            // below line is used for getting OTP code  which is sent in phone auth credentials.
            val code = phoneAuthCredential.smsCode
        }

        // this method is called when firebase doesn't sends our OTP code due to any error or issue.
        override fun onVerificationFailed(e: FirebaseException) {
            // displaying error message with firebase exception.
            binding.txtErrorPassword.text = e.message
            Toast.makeText(this@SignUpActivity, e.message, Toast.LENGTH_LONG).show()
        }
    }

}