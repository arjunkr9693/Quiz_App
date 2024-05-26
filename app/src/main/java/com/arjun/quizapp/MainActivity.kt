package com.arjun.quizapp

import android.animation.Animator
import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.os.Bundle
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import com.arjun.quizapp.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    private var currentQuestionIndex = 0
    private var questionsAnswered = 0
    private var correctAnswers = 0

    private val questions = listOf(
        Question("What is the capital of France?", listOf("Paris", "London", "Berlin", "Madrid"), 0),
        Question("What is 2 + 2?", listOf("3", "4", "5", "6"), 1),
        Question("Who wrote 'Hamlet'?", listOf("Shakespeare", "Dickens", "Hemingway", "Austen"), 0),
        Question("What is the capital of Spain?", listOf("Madrid", "Barcelona", "Valencia", "Seville"), 0),
        Question("What is the largest planet?", listOf("Earth", "Mars", "Jupiter", "Saturn"), 2),
        Question("What is the chemical symbol for water?", listOf("H2O", "O2", "CO2", "N2"), 0),
        Question("Who painted the Mona Lisa?", listOf("Leonardo da Vinci", "Vincent van Gogh", "Pablo Picasso", "Claude Monet"), 0),
        Question("What is the capital of Japan?", listOf("Tokyo", "Osaka", "Kyoto", "Nagoya"), 0),
        Question("What is the fastest land animal?", listOf("Cheetah", "Lion", "Tiger", "Leopard"), 0),
        Question("Who developed the theory of relativity?", listOf("Isaac Newton", "Galileo Galilei", "Albert Einstein", "Nikola Tesla"), 2)
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        supportActionBar?.hide()
        setContentView(binding.root)

        binding.nextButton.setOnClickListener {
            if (binding.answersRadioGroup.checkedRadioButtonId != -1) {
                onAnswerSelected()
                animateQuestionChange()
            } else {
                Toast.makeText(this, "Please select an answer", Toast.LENGTH_SHORT).show()
            }
        }

        // Set initial question
        loadNextQuestion()
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

            if (selectedAnswerIndex == questions[currentQuestionIndex - 1].correctAnswerIndex) {
                correctAnswers++
            }
            questionsAnswered++
            updateProgressBar()
        }
    }

    private fun updateProgressBar() {
        val progress = (questionsAnswered.toFloat() / questions.size) * 100
        binding.progressBar.progress = progress.toInt()
    }

    private fun loadNextQuestion() {
        if (currentQuestionIndex < questions.size) {
            val question = questions[currentQuestionIndex]
            binding.questionTextView.text = question.questionText
            binding.answerButton1.text = question.options[0]
            binding.answerButton2.text = question.options[1]
            binding.answerButton3.text = question.options[2]
            binding.answerButton4.text = question.options[3]
            binding.answersRadioGroup.clearCheck()
            binding.questionCounterTextView.text = "Question ${currentQuestionIndex + 1}/${questions.size}"
            currentQuestionIndex++
        } else {
            // Quiz finished
            binding.questionTextView.text = "Quiz Completed! You got $correctAnswers out of ${questions.size} correct."
            // Disable answer buttons or show a completion message
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
}
