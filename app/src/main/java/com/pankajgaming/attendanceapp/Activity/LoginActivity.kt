package com.pankajgaming.attendanceapp.Activity

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.text.TextUtils
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.FirebaseException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.PhoneAuthCredential
import com.google.firebase.auth.PhoneAuthOptions
import com.google.firebase.auth.PhoneAuthProvider
import com.google.firebase.database.Query
import com.google.firebase.database.ktx.database
import com.google.firebase.ktx.Firebase
import com.pankajgaming.attendanceapp.R
import com.pankajgaming.attendanceapp.databinding.ActivityLoginBinding
import java.util.concurrent.TimeUnit


class LoginActivity : AppCompatActivity() {

    private lateinit var binding : ActivityLoginBinding
    private lateinit var auth: FirebaseAuth
    private var verificationId: String? = null
    private lateinit var phone: String
    private lateinit var name: String
    private lateinit var dob: String
    private var count:String = ""

    @SuppressLint("SetTextI18n")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        auth = FirebaseAuth.getInstance()
        val currentUser = auth.currentUser

        name = intent.getStringExtra("name").toString()
        dob = intent.getStringExtra("dob").toString()

        if (intent.getStringExtra("login").equals("fromSignup")){

        }
        else{

            if (currentUser != null) {
                val intent = Intent(this, MainActivity::class.java)
                intent.putExtra("currentUser",currentUser)
                startActivity(intent)
                finish()
            }

        }


        binding.btnGetOtp.setOnClickListener {

            binding.progressBar.visibility = View.VISIBLE

            if (TextUtils.isEmpty(binding.edtPhone.text.toString())) {

                binding.txtErrorPassword.text = getString(R.string.please_enter_a_valid_phone_number)
                binding.txtErrorPassword.setTextColor(resources.getColor(R.color.light_red))
                binding.txtErrorPassword.visibility = View.VISIBLE
                binding.progressBar.visibility = View.INVISIBLE
            } else {
                binding.progressBar.visibility = View.VISIBLE
                phone = "+91" + binding.edtPhone.text.toString()

                checkIfPhoneNumberExists(phone)

            }
        }

        binding.btnSignup.setOnClickListener {
            val intent =Intent(this@LoginActivity,SignUpActivity::class.java)
            startActivity(intent)
            finish()
        }

    }

    private fun checkIfPhoneNumberExists(phone: String) {

        val usersRef = Firebase.database.reference.child("Users")

        val query: Query = usersRef.orderByChild("Phone").equalTo(phone)

        query.get().addOnSuccessListener { dataSnapshot ->
            if (dataSnapshot.exists()) {
                // User with the provided phone number found
                sendVerificationCode(phone)
                binding.txtErrorPassword.text = getString(R.string.otp_send)
                binding.txtErrorPassword.setTextColor(resources.getColor(R.color.green))
                binding.txtErrorPassword.visibility = View.VISIBLE
            } else {
                // No user found with the provided phone number
                Toast.makeText(this@LoginActivity, getString(R.string.no_account_found), Toast.LENGTH_SHORT).show()
                binding.txtErrorPassword.text = getString(R.string.no_account_found)
                binding.txtErrorPassword.setTextColor(resources.getColor(R.color.light_red))
                binding.txtErrorPassword.visibility = View.VISIBLE
                binding.progressBar.visibility = View.GONE
            }

        }.addOnFailureListener { e ->
            Log.e("FirebaseQuery", "Error executing query: ${e.message}")
            Toast.makeText(this@LoginActivity, "Error executing query: ${e.message}", Toast.LENGTH_LONG).show()
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

            val intent = Intent(this@LoginActivity, OtpVerificationActivity::class.java)
            intent.putExtra("verificationId",verificationId)
            intent.putExtra("phone",phone)
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
            Log.d("AuthFirebase", e.message.toString())
            Toast.makeText(this@LoginActivity, e.message, Toast.LENGTH_LONG).show()
        }
    }


}