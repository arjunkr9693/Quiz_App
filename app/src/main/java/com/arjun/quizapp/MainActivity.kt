package com.arjun.quizapp

import android.animation.Animator
import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.content.Context
import android.content.SharedPreferences
import android.os.Bundle
import android.os.CountDownTimer
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.arjun.quizapp.databinding.ActivityMainBinding
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    private lateinit var quizViewModel: QuizViewModel
    private lateinit var sharedPref: SharedPreferences
    private var timer: CountDownTimer? = null
    private val totalQuizTime = 10 * 60 * 1000L // 10 minutes in milliseconds

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        supportActionBar?.hide()
        setContentView(binding.root)

        sharedPref = getPreferences(Context.MODE_PRIVATE)
        quizViewModel = ViewModelProvider(this)[QuizViewModel::class.java]

        binding.nextButton.setOnClickListener {
            if (binding.answersRadioGroup.checkedRadioButtonId != -1) {
                onAnswerSelected()
                animateQuestionChange()
            } else {
                Toast.makeText(this, "Please select an answer", Toast.LENGTH_SHORT).show()
            }
        }
        loadSavedState()
    }

    private fun onAnswerSelected() {
        val selectedOptionId = binding.answersRadioGroup.checkedRadioButtonId
        if (selectedOptionId != -1) {
            val selectedAnswerIndex = when (selectedOptionId) {
                binding.answerButton1.id -> 0
                binding.answerButton2.id -> 1
                binding.answerButton3.id -> 2
                binding.answerButton4.id -> 3
                else -> -1
            }

            if (selectedAnswerIndex == quizViewModel.currentQuestions[quizViewModel.questionsAnswered].correctAnswerIndex) {
                quizViewModel.correctAnswers++
            }
            quizViewModel.questionsAnswered++
            updateProgressBar()
        }
    }

    private fun updateProgressBar() {
        val progress = (quizViewModel.questionsAnswered.toFloat() / quizViewModel.currentQuestions.size) * 100
        Log.d("triviaTag", quizViewModel.questionsAnswered.toString())
        binding.progressBar.progress = progress.toInt()
    }

    private fun loadNextQuestion() {
        if (quizViewModel.questionsAnswered < quizViewModel.currentQuestions.size) {
            val question = quizViewModel.currentQuestions[quizViewModel.questionsAnswered]
            binding.questionTextView.text = question.questionText
            binding.answerButton1.text = question.options[0]
            binding.answerButton2.text = question.options[1]
            binding.answerButton3.text = question.options[2]
            binding.answerButton4.text = question.options[3]
            binding.answersRadioGroup.clearCheck()
            binding.questionCounterTextView.text = "Question ${quizViewModel.questionsAnswered+1}/${quizViewModel.currentQuestions.size}"
        } else {
            // Quiz finished
            binding.questionTextView.text = "Quiz Completed! You got ${quizViewModel.correctAnswers} out of ${quizViewModel.currentQuestions.size} correct."
            timer?.cancel()
            resetValues()
            disableRadioButtons()

        }
    }

    private fun disableRadioButtons() {
        binding.answerButton1.isEnabled = false
        binding.answerButton2.isEnabled = false
        binding.answerButton3.isEnabled = false
        binding.answerButton4.isEnabled = false
        binding.nextButton.isEnabled = false
    }

    private fun fetchQuizData() {
        binding.progressLayout.visibility = View.VISIBLE
        binding.questionLayout.visibility = View.INVISIBLE
        val call = RetrofitInstance.api.getTriviaQuestions(10, 18, "easy", "multiple")

        call.enqueue(object : Callback<TriviaResponse> {
            override fun onResponse(call: Call<TriviaResponse>, response: Response<TriviaResponse>) {
                if (response.isSuccessful) {
                    val triviaResponse = response.body()
                    triviaResponse?.results?.let { results ->
                        quizViewModel.currentQuestions = results.map { result ->
                            val allAnswers = result.incorrectAnswers.toMutableList().apply { add(result.correctAnswer) }.shuffled()
                            Question(result.question, allAnswers, allAnswers.indexOf(result.correctAnswer))
                        }
                        binding.progressLayout.visibility = View.INVISIBLE
                        binding.questionLayout.visibility = View.VISIBLE
                        loadNextQuestion()
                        startTimer(quizViewModel.remainingTime)
                        saveQuestions()
                    }
                }
            }

            override fun onFailure(call: Call<TriviaResponse>, t: Throwable) {
                Log.d("triviaTag", t.message.toString())
            }
        })
    }

    private fun saveQuestions() {
        with(sharedPref.edit()) {
            putString("currentQuestions", Gson().toJson(quizViewModel.currentQuestions))
            apply()
        }
    }

    private fun animateQuestionChange() {
        val screenWidth = resources.displayMetrics.widthPixels.toFloat()

        // Animate the current question out
        val outAnimatorQuestion = ObjectAnimator.ofFloat(binding.questionTextView, "translationX", 0f, -screenWidth)
        outAnimatorQuestion.duration = 300

        // Animate the current answers out
        val outAnimatorAnswers = ObjectAnimator.ofFloat(binding.answersRadioGroup, "translationX", 0f, -screenWidth)
        outAnimatorAnswers.duration = 300

        // Animate the next question in
        val inAnimatorQuestion = ObjectAnimator.ofFloat(binding.questionTextView, "translationX", screenWidth, 0f)
        inAnimatorQuestion.duration = 300

        // Animate the next answers in
        val inAnimatorAnswers = ObjectAnimator.ofFloat(binding.answersRadioGroup, "translationX", screenWidth, 0f)
        inAnimatorAnswers.duration = 300

        outAnimatorQuestion.addListener(object : Animator.AnimatorListener {
            override fun onAnimationStart(animation: Animator) {}
            override fun onAnimationEnd(animation: Animator) {
                // Load the next question after the current one moves out
                loadNextQuestion()
            }

            override fun onAnimationCancel(animation: Animator) {}
            override fun onAnimationRepeat(animation: Animator) {}
        })

        // Run both animations in sequence
        val animatorSetOut = AnimatorSet()
        animatorSetOut.playTogether(outAnimatorQuestion, outAnimatorAnswers)

        val animatorSetIn = AnimatorSet()
        animatorSetIn.playTogether(inAnimatorQuestion, inAnimatorAnswers)

        val finalAnimatorSet = AnimatorSet()
        finalAnimatorSet.playSequentially(animatorSetOut, animatorSetIn)
        finalAnimatorSet.start()
    }


    private fun startTimer(timeMillis: Long) {
        timer = object : CountDownTimer(timeMillis, 1000) {
            override fun onTick(millisUntilFinished: Long) {
                quizViewModel.remainingTime = millisUntilFinished
                val minutes = millisUntilFinished / 1000 / 60
                val seconds = millisUntilFinished / 1000 % 60
                binding.timerTextView.text = "Time left: $minutes:${String.format("%02d", seconds)}"
            }

            override fun onFinish() {
                binding.questionTextView.text = "Time's up! You got ${quizViewModel.correctAnswers} out of ${quizViewModel.currentQuestions.size} correct."
                disableRadioButtons()
                resetValues()
            }
        }.start()
    }

    private fun resetValues() {
        with(sharedPref.edit()) {
            putBoolean("quizCompleted", true)
            putInt("correctAnswers", 0)
            putInt("questionsAnswered", 0)
            putLong("remainingTime", totalQuizTime)
            remove("currentQuestions")
            apply()
        }
        quizViewModel.correctAnswers = 0
        quizViewModel.questionsAnswered = 0
        quizViewModel.remainingTime = totalQuizTime
        quizViewModel.currentQuestions = emptyList()
        Log.d("triviaTag", "Values reset: correctAnswers=${quizViewModel.correctAnswers}, questionsAnswered=${quizViewModel.questionsAnswered}")
    }

    private fun loadSavedState() {
        val quizCompleted = sharedPref.getBoolean("quizCompleted", false)
        quizViewModel.correctAnswers = sharedPref.getInt("correctAnswers", 0)
        quizViewModel.questionsAnswered = sharedPref.getInt("questionsAnswered", 0)
        quizViewModel.remainingTime = sharedPref.getLong("remainingTime", totalQuizTime)
        quizViewModel.currentQuestions = loadQuestionsFromPreferences(sharedPref)

        Log.d("triviaTag", "Remaining Time --> ${quizViewModel.remainingTime / 1000 / 60}:${(quizViewModel.remainingTime / 1000) % 60}")
        Log.d("triviaTag", quizViewModel.questionsAnswered.toString())
        if(quizCompleted) {
            fetchQuizData()
        }
        else {
            if (quizViewModel.currentQuestions.isNotEmpty()) {
                updateProgressBar()
                loadNextQuestion()
                startTimer(quizViewModel.remainingTime)
            } else {
                fetchQuizData()
            }
        }
    }

    private fun loadQuestionsFromPreferences(sharedPref: SharedPreferences): List<Question> {
        val questionsJson = sharedPref.getString("currentQuestions", null) ?: return emptyList()
        val type = object : TypeToken<List<Question>>() {}.type
        return Gson().fromJson(questionsJson, type)
    }

    private fun saveQuizState() {
        with(sharedPref.edit()) {
            putInt("correctAnswers", quizViewModel.correctAnswers)
            putInt("questionsAnswered", quizViewModel.questionsAnswered)
            putLong("remainingTime", quizViewModel.remainingTime)
            putBoolean("quizCompleted", false)
            apply()
        }
    }

    override fun onResume() {
        super.onResume()
        timer?.cancel()
        loadSavedState()
        Log.d("triviaTag", quizViewModel.remainingTime.toString())
        Log.d("triviaTag", "OnResume Called")
    }

    override fun onPause() {
        super.onPause()
        saveQuizState()
        Log.d("triviaTag", quizViewModel.remainingTime.toString())
        Log.d("triviaTag", "OnPause Called")
        timer?.cancel()
    }

    override fun onDestroy() {
        super.onDestroy()
        saveQuizState()
        timer?.cancel()
    }
}

// Data class for Question
//data class Question(val questionText: String, val options: List<String>, val correctAnswerIndex: Int)

class QuizViewModel : ViewModel() {
    var currentQuestions: List<Question> = emptyList()
    var questionsAnswered = 0
    var correctAnswers = 0
    var remainingTime: Long = 10 * 60 * 1000L // Default to 10 minutes
}
