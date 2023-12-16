package com.pankajgaming.attendanceapp.Activity

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import android.widget.ImageView
import androidx.core.content.ContextCompat
import com.google.android.material.card.MaterialCardView
import com.google.firebase.auth.FirebaseAuth
import com.pankajgaming.attendanceapp.util.LocaleHelper
import com.pankajgaming.attendanceapp.R
import com.pankajgaming.attendanceapp.databinding.ActivityLanguageSelectionBinding

class LanguageSelectionActivity : AppCompatActivity() {

    private lateinit var binding : ActivityLanguageSelectionBinding
    private lateinit var auth: FirebaseAuth
    private lateinit var language: String


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLanguageSelectionBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val savedLanguage = LocaleHelper.getLanguage(this)
        language = savedLanguage
        LocaleHelper.setLocale(this, savedLanguage)


        when (language) {
            "en" -> {
                setButtonStyle(binding.btnCardOne, binding.cardOne, R.color.orange, 10, View.VISIBLE)
                setButtonStyle(binding.btnCardTwo, binding.cardTwo, R.color.grey, 2, View.INVISIBLE)
                setButtonStyle(binding.btnCardThree, binding.cardThree, R.color.grey, 2, View.INVISIBLE)
                setButtonStyle(binding.btnCardFour, binding.cardFour, R.color.grey, 2, View.INVISIBLE)
            }
            "gu" -> {
                // Set values for Gujarati language
                setButtonStyle(binding.btnCardOne, binding.cardOne, R.color.grey, 2, View.INVISIBLE)
                setButtonStyle(binding.btnCardTwo, binding.cardTwo, R.color.orange, 10, View.VISIBLE)
                setButtonStyle(binding.btnCardThree, binding.cardThree, R.color.grey, 2, View.INVISIBLE)
                setButtonStyle(binding.btnCardFour, binding.cardFour, R.color.grey, 2, View.INVISIBLE)
            }
            "hi" -> {
                // Set values for Hindi language
                setButtonStyle(binding.btnCardOne, binding.cardOne, R.color.grey, 2, View.INVISIBLE)
                setButtonStyle(binding.btnCardTwo, binding.cardTwo, R.color.grey, 2, View.INVISIBLE)
                setButtonStyle(binding.btnCardThree, binding.cardThree, R.color.orange, 10, View.VISIBLE)
                setButtonStyle(binding.btnCardFour, binding.cardFour, R.color.grey, 2, View.INVISIBLE)
            }
            "mr" -> {
                // Set values for Marathi language
                setButtonStyle(binding.btnCardOne, binding.cardOne, R.color.grey, 2, View.INVISIBLE)
                setButtonStyle(binding.btnCardTwo, binding.cardTwo, R.color.grey, 2, View.INVISIBLE)
                setButtonStyle(binding.btnCardThree, binding.cardThree, R.color.grey, 3, View.INVISIBLE)
                setButtonStyle(binding.btnCardFour, binding.cardFour, R.color.orange, 10, View.VISIBLE)
            }
            else -> {
                // Set default values or handle other languages
            }
        }

        auth = FirebaseAuth.getInstance()
        val currentUser = auth.currentUser

        if (currentUser == null){
            initView()
        }
        else{
            if (intent.getStringExtra("mode").equals("reselect")){
                initView()
            }
            else{
                val intent = Intent(this, MainActivity::class.java)
                intent.putExtra("currentUser",currentUser)
                startActivity(intent)
                finish()
            }

        }
    }
    private fun setButtonStyle(button: MaterialCardView, card: ImageView, colorRes: Int, strokeWidth: Int, visibility: Int) {
        button.strokeWidth = strokeWidth
        button.strokeColor = ContextCompat.getColor(this, colorRes)
        card.visibility = visibility
    }
    private fun initView() {
        binding.apply {

//            var count = 0

            val buttons = listOf(btnCardOne, btnCardTwo, btnCardThree, btnCardFour)
            val checked = listOf(cardOne, cardTwo, cardThree, cardFour)

            buttons.forEachIndexed { index, button ->
                button.setOnClickListener {
//                    count = 1
                    language = when (index) {
                        0 -> "en"
                        1 -> "gu"
                        2 -> "hi"
                        3 -> "mr"
                        else -> "en" // Default to English if unexpected index
                    }

                    buttons.forEachIndexed { innerIndex, innerButton ->
                        innerButton.strokeColor = resources.getColor(if (innerIndex == index) R.color.orange else R.color.grey)
                        innerButton.strokeWidth = if (innerIndex == index) 10 else 2
                    }
                    checked.forEachIndexed { innerIndex, innerChecked ->
                        innerChecked.visibility = if (innerIndex == index) View.VISIBLE else View.INVISIBLE
                    }
                }
            }



                binding.btnNext.setOnClickListener {

                    if (intent.getStringExtra("mode").equals("reselect")){

                        binding.btnNext.text = getString(R.string.save)
                        val selectedLanguage = language
                        LocaleHelper.setLocale(this@LanguageSelectionActivity, selectedLanguage)
                        LocaleHelper.saveLanguage(this@LanguageSelectionActivity, selectedLanguage)
                        val intent = Intent(this@LanguageSelectionActivity, MainActivity::class.java)
                        startActivity(intent)
                        finish()
                    }
                    else{
                        binding.btnNext.text = "Next"
                        val selectedLanguage = language
                        LocaleHelper.setLocale(this@LanguageSelectionActivity, selectedLanguage)
                        LocaleHelper.saveLanguage(this@LanguageSelectionActivity, selectedLanguage)
                        val intent = Intent(this@LanguageSelectionActivity, SignUpActivity::class.java)
                        startActivity(intent)

                    }


                }

        }
    }


}