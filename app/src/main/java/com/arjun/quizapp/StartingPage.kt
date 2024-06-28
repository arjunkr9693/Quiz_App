package com.arjun.quizapp

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity

class StartingPage : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_starting_page)

        val startBtn = findViewById<Button>(R.id.startQuizBtn)

        startBtn.setOnClickListener {
            startActivity(Intent(this, MainActivity::class.java))
        }
    }
}