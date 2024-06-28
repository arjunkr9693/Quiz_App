package com.arjun.quizapp
import com.google.gson.annotations.SerializedName

data class TriviaResponse(
    @SerializedName("response_code") val responseCode: Int,
    @SerializedName("results") val results: List<TriviaQuestion>
)