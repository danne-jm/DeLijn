package com.danieljm.delijn.data.local

import androidx.room.TypeConverter
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types

object Converters {
    private val moshi = Moshi.Builder().build()
    private val listType = Types.newParameterizedType(List::class.java, String::class.java)
    private val adapter = moshi.adapter<List<String>>(listType)

    @TypeConverter
    @JvmStatic
    fun fromStringList(list: List<String>?): String? = list?.let { adapter.toJson(it) }

    @TypeConverter
    @JvmStatic
    fun toStringList(json: String?): List<String> = json?.let { adapter.fromJson(it) } ?: emptyList()
}

