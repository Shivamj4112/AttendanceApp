package com.pankajgaming.attendanceapp.Activity

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.os.CountDownTimer
import android.os.Handler
import android.text.TextUtils
import android.view.View
import android.view.animation.AnimationUtils
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.datepicker.CalendarConstraints
import com.google.android.material.datepicker.DateValidatorPointBackward
import com.google.android.material.datepicker.MaterialDatePicker
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.FirebaseException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.PhoneAuthCredential
import com.google.firebase.auth.PhoneAuthOptions
import com.google.firebase.auth.PhoneAuthProvider
import com.google.firebase.database.ktx.database
import com.google.firebase.ktx.Firebase
import com.pankajgaming.attendanceapp.Manager.NetworkManager
import com.pankajgaming.attendanceapp.R
import com.pankajgaming.attendanceapp.databinding.ActivityOtpVerificationBinding
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit


class OtpVerificationActivity : AppCompatActivity() {

    private lateinit var binding: ActivityOtpVerificationBinding
    private var verificationId: String? = null
    private lateinit var auth: FirebaseAuth

    private var resendToken: PhoneAuthProvider.ForceResendingToken? = null
    private var canResend = true
    private var timer: CountDownTimer? = null
    private lateinit var name: String
    private lateinit var dob: String

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityOtpVerificationBinding.inflate(layoutInflater)
        setContentView(binding.root)

        auth = FirebaseAuth.getInstance()
        name = intent.getStringExtra("name").toString()
        dob = intent.getStringExtra("dob").toString()

        binding.txtErrorPassword.visibility = View.VISIBLE

        val snack = Snackbar.make(binding.root, "No Internet", Snackbar.LENGTH_INDEFINITE)

        if (intent.getStringExtra("mode").equals("AccountCreate")) {
            verificationId = intent.getStringExtra("verificationId")
            val phone = intent.getStringExtra("phone").toString()
            setResendTimer(60000)

            val networkManager = NetworkManager(this@OtpVerificationActivity)

            networkManager.observe(this@OtpVerificationActivity) {

                if (it){


                    binding.translucentOverlay.visibility = View.VISIBLE
                    binding.animationView.visibility = View.VISIBLE

                    snack.dismiss()

                    Handler().postDelayed({
                        binding.translucentOverlay.visibility = View.GONE
                        binding.animationView.visibility = View.GONE
                        binding.pinview.isEnabled = true
                        binding.btnLogin.isEnabled = true

                        requestVerificationCodewithTimer(phone)
                        buttonLoginNewUser(phone)

                    }, 2000)


                }
                else{
                    binding.translucentOverlay.visibility = View.VISIBLE
                    binding.animationView.visibility = View.VISIBLE
                    binding.btnLogin.isEnabled = false
                    binding.pinview.isEnabled = false

                    Handler().postDelayed({
                        binding.translucentOverlay.visibility = View.GONE
                        binding.animationView.visibility = View.GONE
                        snack.show()

                    }, 10000)
                }

            }

        } else {

            verificationId = intent.getStringExtra("verificationId")
            val phone = intent.getStringExtra("phone").toString()
            setResendTimer(60000)

            val networkManager = NetworkManager(this@OtpVerificationActivity)

            networkManager.observe(this@OtpVerificationActivity) {

                if (!it){

                    binding.btnLogin.isEnabled = false
                    binding.pinview.isEnabled = false

                    Handler().postDelayed({
                        binding.translucentOverlay.visibility = View.GONE
                        binding.animationView.visibility = View.GONE
                        snack.show()

                    }, 10000)

                }
                else{
                    binding.translucentOverlay.visibility = View.VISIBLE
                    binding.animationView.visibility = View.VISIBLE

                    snack.dismiss()

                    Handler().postDelayed({
                        binding.translucentOverlay.visibility = View.GONE
                        binding.animationView.visibility = View.GONE
                        binding.pinview.isEnabled = true
                        binding.btnLogin.isEnabled = true

                        requestVerificationCodewithTimer(phone)
                        buttonLoginExistingUser(phone)

                    }, 2000)

                }

            }
        }

    }

    private fun storeData(name: String, dob: String, phone: String) {

        auth = FirebaseAuth.getInstance()
        val user = auth.currentUser
        val uid = user?.uid.toString()

        // Create a user profile map
        val userProfile = mapOf(
            "Name" to name,
            "dob" to dob,
            "Phone" to phone
        )

        val userRef = Firebase.database.reference.child("Users").child(uid)
        userRef.setValue(userProfile)

        // Move to the next activity or provide feedback to the user
        val animation = AnimationUtils.loadAnimation(
            this@OtpVerificationActivity,
            R.anim.slide_out
        )
        val intent = Intent(this@OtpVerificationActivity, MainActivity::class.java)
        binding.root.startAnimation(animation)
        startActivity(intent)
        finish()
    }

    private fun requestVerificationCodewithTimer(phone: String?) {
        if (canResend) {
            binding.txtErrorPassword.setOnClickListener {
                resendVerificationCode("+91$phone", resendToken)
                setResendTimer(60000)
                canResend = false
            }
        }

    }

    private fun setResendTimer(duration: Long) {
        timer?.cancel() // Cancel the previous timer if running
        timer = object : CountDownTimer(duration, 1000) {
            @SuppressLint("StringFormatMatches")
            override fun onTick(millisUntilFinished: Long) {
                val timeLeft = millisUntilFinished / 1000
                binding.txtErrorPassword.text = getString(R.string.resend_otp_in_seconds, timeLeft)
            }

            override fun onFinish() {
                binding.txtErrorPassword.text = getString(R.string.resend_otp)
                canResend = true
            }
        }
        timer?.start()
    }

    private fun resendVerificationCode(
        number: String,
        token: PhoneAuthProvider.ForceResendingToken?
    ) {
        val options = PhoneAuthOptions.newBuilder(auth)
            .setPhoneNumber(number) // Phone number to verify
            .setTimeout(60L, TimeUnit.SECONDS) // Timeout and unit
            .setActivity(this) // Activity (for callback binding)
            .setCallbacks(mCallBack) // OnVerificationStateChangedCallbacks
            .setForceResendingToken(token!!)
            .build()
        PhoneAuthProvider.verifyPhoneNumber(options)
    }

    private val mCallBack: PhoneAuthProvider.OnVerificationStateChangedCallbacks =
        object : PhoneAuthProvider.OnVerificationStateChangedCallbacks() {
            // below method is used when OTP is sent from Firebase
            override fun onCodeSent(
                receiveOtp: String,
                forceResendingToken: PhoneAuthProvider.ForceResendingToken
            ) {
                super.onCodeSent(receiveOtp, forceResendingToken)
                // when we receive the OTP it contains a unique id which we are storing in our string  which we have already created.
                verificationId = receiveOtp

            }

            // this method is called when user receive OTP from Firebase.
            override fun onVerificationCompleted(phoneAuthCredential: PhoneAuthCredential) {
                // below line is used for getting OTP code  which is sent in phone auth credentials.
                val code = phoneAuthCredential.smsCode
                if (code != null) {
                    binding.pinview.setText(code)
                }

            }

            // this method is called when firebase doesn't sends our OTP code due to any error or issue.
            override fun onVerificationFailed(e: FirebaseException) {
                // displaying error message with firebase exception.
                Toast.makeText(this@OtpVerificationActivity, e.message, Toast.LENGTH_LONG).show()
            }
        }


    // below methods is use to verify code from Firebase for New User.
    private fun buttonLoginNewUser(phone: String) {
        binding.btnLogin.setBackgroundColor(resources.getColor(R.color.orange))
        binding.btnLogin.setOnClickListener {

            binding.progressBar.visibility = View.VISIBLE
            if (TextUtils.isEmpty(binding.pinview.text.toString())) {

                binding.progressBar.visibility = View.INVISIBLE
                Toast.makeText(this@OtpVerificationActivity, getString(R.string.please_enter_otp), Toast.LENGTH_SHORT).show()
            } else {
                val code = binding.pinview.text.toString()
                verifyCodeNewUser(code, phone)
            }

        }

    }

    private fun verifyCodeNewUser(code: String, phone: String) {
        // below line is used for getting credentials from our verification id and code.
        val credential = PhoneAuthProvider.getCredential(verificationId!!, code)

        // after getting credential we are calling sign in method.
        signInWithCredentialNewUser(credential, phone)
    }

    private fun signInWithCredentialNewUser(credential: PhoneAuthCredential, phone: String) {
        // inside this method we are checking if the code entered is correct or not.
        auth.signInWithCredential(credential).addOnCompleteListener { task ->
            if (task.isSuccessful) {
                // if the code is correct and the task is successful we are sending our user to new activity.
                storeData(name, dob, phone)
                binding.progressBar.visibility = View.INVISIBLE

            } else {
                // if the code is not correct then we are displaying an error message to the user.
                Toast.makeText(this@OtpVerificationActivity, task.exception!!.message, Toast.LENGTH_LONG).show()
            }
        }
    }

    // below methods is use to verify code from Firebase for Existing User.
    private fun buttonLoginExistingUser(phone: String) {
        binding.btnLogin.setBackgroundColor(resources.getColor(R.color.orange))
        binding.btnLogin.setOnClickListener {

            binding.progressBar.visibility = View.VISIBLE
            if (TextUtils.isEmpty(binding.pinview.text.toString())) {

                binding.progressBar.visibility = View.INVISIBLE
                Toast.makeText(
                    this@OtpVerificationActivity,
                    getString(R.string.please_enter_otp),
                    Toast.LENGTH_SHORT
                ).show()
            } else {
                val code = binding.pinview.text.toString()
                verifyCodeExistingUser(code, phone)
            }

        }

    }

    private fun verifyCodeExistingUser(code: String, phone: String) {
        // below line is used for getting credentials from our verification id and code.
        val credential = PhoneAuthProvider.getCredential(verificationId!!, code)

        // after getting credential we are calling sign in method.
        signInWithCredentialExistingUser(credential, phone)
    }

    private fun signInWithCredentialExistingUser(credential: PhoneAuthCredential, phone: String) {
        // inside this method we are checking if the code entered is correct or not.
        auth.signInWithCredential(credential).addOnCompleteListener { task ->
            if (task.isSuccessful) {
                // if the code is correct and the task is successful we are sending our user to new activity.
                binding.progressBar.visibility = View.INVISIBLE
                val intent = Intent(this@OtpVerificationActivity, MainActivity::class.java)
                startActivity(intent)
            } else {
                // if the code is not correct then we are displaying an error message to the user.
                Toast.makeText(
                    this@OtpVerificationActivity,
                    task.exception!!.message,
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }
}