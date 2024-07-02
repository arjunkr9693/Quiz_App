package com.arjun.quizapp

import androidx.lifecycle.ViewModel

class QuizViewModel : ViewModel(){
    var currentQuestions: List<Question> = emptyList()
    var questionsAnswered = 0
    var correctAnswers = 0
    var remainingTime: Long = 10 * 60 * 1000L // Default to 10 minutes
}