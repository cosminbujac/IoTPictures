package com.ubb.iotpics

import android.R.attr
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Base64
import java.io.ByteArrayOutputStream


object BitmapToMessageAdapter {

    fun adaptBitmapToMessage(bitmap: Bitmap): String {
        val baos = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, 100, baos)
        val b = baos.toByteArray()
        return Base64.encodeToString(b, Base64.DEFAULT)
    }

    fun adaptMessageToBitmap(message:String):Bitmap{
        val imageBytes = Base64.decode(message, 0)
        return  BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size)
    }
}