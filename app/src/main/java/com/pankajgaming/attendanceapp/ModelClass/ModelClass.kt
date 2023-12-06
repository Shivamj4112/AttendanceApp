package com.pankajgaming.attendanceapp.ModelClass

class ModelClass {

    val date : String
    val message : String
    val category : String
    val year : String
    val month : String
    val sign : String
    val signName : String
    val amount : String

    constructor(
        date: String,
        message: String,
        category: String,
        year: String,
        month: String,
        sign: String?,
        signName: String?,
        amount: String
    ) {
        this.date = date
        this.message = message
        this.category = category
        this.year = year
        this.month = month
        this.sign = sign!!
        this.signName = signName!!
        this.amount = amount
    }

    data class ModelClass(
        var date: String = "",
        var message: String = "",
        var category:String = "",
        var year:String = "",
        var month:String = "",
        var sign:String = "",
        var signName:String = "",
        var amount:String = ""
    )
}