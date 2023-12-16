package com.pankajgaming.attendanceapp.Adapter

import android.annotation.SuppressLint
import android.content.Context
import android.util.Log
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.pankajgaming.attendanceapp.util.LocaleHelper
import com.pankajgaming.attendanceapp.ModelClass.ModelClass
import com.pankajgaming.attendanceapp.R
import com.pankajgaming.attendanceapp.databinding.ItemLayoutBinding

class NotesAdapter() : RecyclerView.Adapter<NotesAdapter.DataHolder>() {

    lateinit var onLongClick: (ModelClass) -> Unit
    lateinit var onItemClick: (ModelClass) -> Unit

    var list = ArrayList<ModelClass>()
    lateinit var context: Context

    class DataHolder(itemView: ItemLayoutBinding) : RecyclerView.ViewHolder(itemView.root) {

        var binding = itemView
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DataHolder {
        context = parent.context
        var binding = ItemLayoutBinding.inflate(LayoutInflater.from(context), parent, false)
        return DataHolder(binding)
    }

    override fun getItemCount(): Int {
        return list.size
    }

    @SuppressLint("SetTextI18n")
    override fun onBindViewHolder(holder: DataHolder, position: Int) {

        holder.binding.apply {

            val dataModel = list[position]

            dataModel.apply {

                val mappedCategory = mapEnglishToSelectedLanguage(category)

                txtDate.text = date
                txtMessage.text = message
                txtStatus.text = mappedCategory
                txtAmount.text = amount

                Glide.with(context).load(sign).into(imgSign)

//                val animation = AnimationUtils.loadAnimation(holder.binding.root.context, android.R.anim.slide_in_left)
//                root.startAnimation(animation)

                // Display serial number in ascending order
                val serialNumber = (position + 1).toString()
                txtSerialNumber.text = "$serialNumber."


                if (mappedCategory == context.getString(R.string.status_currentPresent)) {

                    layout1.setBackgroundColor(context.resources.getColor(R.color.dark_green))
                } else {
                    layout1.setBackgroundColor(context.resources.getColor(R.color.light_red))
                }

                root.setOnLongClickListener() {
                    onLongClick.invoke(dataModel)

                    true
                }

                root.setOnClickListener {

                    onItemClick.invoke(dataModel)
                }


            }


        }

    }

    @SuppressLint("NotifyDataSetChanged")
    fun UpdateData(list: ArrayList<ModelClass>) {
        this.list = list // Use 'this.list' to refer to the class variable
        notifyDataSetChanged()
    }

    private fun mapEnglishToSelectedLanguage(englishTerm: String): String {
        // You can replace the translations based on your language requirements
        val selectedLanguage = LocaleHelper.getLanguage(context)
        return when (selectedLanguage) {
            "gu" -> mapEnglishToGujarati(englishTerm)
            "hi" -> mapEnglishToHindi(englishTerm)
            "mr" -> mapEnglishToMarathi(englishTerm)
            else -> {
                Log.d("LanguageDebug", "Unsupported language: $selectedLanguage")
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


}

