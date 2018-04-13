package com.example.kamil.flagquiz

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.annotation.SuppressLint
import android.app.AlertDialog
import android.app.Dialog
import android.content.DialogInterface
import android.content.SharedPreferences
import android.graphics.drawable.Drawable
import android.support.v4.app.Fragment
import android.os.Bundle
import android.os.Handler
import android.support.constraint.ConstraintLayout
import android.support.v4.app.DialogFragment
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewAnimationUtils
import android.view.ViewGroup
import android.view.animation.Animation
import android.view.animation.AnimationUtils
import android.widget.Button
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import kotlinx.android.synthetic.main.content_settings.*
import java.io.IOException
import java.io.InputStream
import java.security.SecureRandom


class MainActivityFragment : Fragment() {
    companion object {
        const val TAG: String = "FlagQuiz Activity"
        const val FLAGS_IN_QUIZ = 10

        class QuizDialogFragment : DialogFragment() {
            override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
                val builder: AlertDialog.Builder = AlertDialog.Builder(activity)
                val totalGuesses = arguments.getInt("TotalGuesses", 1)
                builder.setMessage(getString(R.string.results, totalGuesses, (1000F / (totalGuesses * 1F))))
                builder.setPositiveButton(R.string.reset_quiz,
                        { _, _ -> ( fragmentManager.findFragmentById(R.id.quizFragment) as MainActivityFragment).resetQuiz() })
                return builder.create()
            }
        }
    }


    // nazwy plików flag
    private var fileNameList: MutableList<String> = ArrayList()
    // kraje biezącego quizu
    private var quizCountriesList: MutableList<String> = ArrayList()
    // obszary bieżącego quizu
    private lateinit var regionsSet: Set<String>
    // poprawna nazwa kraju przypisania do bieżącej flagi
    private lateinit var correctAnswer: String
    // liczba prób odpowiedzi
    private var totalGuesses: Int = 0
    // liczba poprawnych odpowiedzi
    private var correctAnswers: Int = 0
    // liczba wierszy przycisków odpowiedzi wyświetlanych na ekranie
    private var guessRows: Int = 0
    // obiekt używany podczas losowania
    private lateinit var random: SecureRandom
    // zmienna uzywana podczas opozniania ładowania kolejnej flagi
    private lateinit var handler: Handler
    // animacja błędnej odpowiedzi
    private lateinit var shakeAnimation: Animation
    // wiersze przycisków odpowiedzi
    private lateinit var guessLinearLayouts: List<LinearLayout>

    private lateinit var quizLinearLayout: LinearLayout
    private lateinit var guessCountryTextView: TextView
    private lateinit var flagImageView: ImageView
    private lateinit var answerTextView: TextView
    private lateinit var questionNumberTextView: TextView

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        val view = inflater.inflate(R.layout.fragment_main, container, false)
        guessCountryTextView = view.findViewById(R.id.guessCountryTextView) as TextView
        flagImageView = view.findViewById(R.id.flagImageView) as ImageView
        answerTextView = view.findViewById(R.id.answerTextView) as TextView
        questionNumberTextView = view.findViewById(R.id.questionNumberTextView) as TextView
        quizLinearLayout = view.findViewById(R.id.quizLinearLayout) as LinearLayout

        guessLinearLayouts = listOf(
                view.findViewById(R.id.row1LinearLayout) as LinearLayout,
                view.findViewById(R.id.row2LinearLayout) as LinearLayout,
                view.findViewById(R.id.row3LinearLayout) as LinearLayout,
                view.findViewById(R.id.row4LinearLayout) as LinearLayout)


        handler = Handler()
        random = SecureRandom()


        shakeAnimation = AnimationUtils.loadAnimation(activity, R.anim.incorrect_shake)
        shakeAnimation.repeatCount = 3


        // konfiguruje obiekty nasłuchujące przycisków odpowiedzi
        for (item: LinearLayout in guessLinearLayouts) {
            for (i in 0 until item.childCount) {
                item.getChildAt(i).setOnClickListener(GuessButtonListener())
            }
        }


        // określa tekst wyświetlany w polach questionNumberTextView
        questionNumberTextView.text = getString(R.string.question, 1, FLAGS_IN_QUIZ)
        return view
    }

    // aktualizuje zmienną guessRows na podstawie wartości SharedPreferences
    fun updateGuessRows(sharedPreferences: SharedPreferences?) {
        // ustal liczbę przycisków odpowiedzi, które mają zostać wyświetlone
        val choices = sharedPreferences!!.getString(MainActivity.CHOICES, null)
        guessRows = Integer.parseInt(choices) / 2

        // ukryj wszystkie obiekty LinearLayout przycisków odpowiedzi
        guessLinearLayouts.forEach { it -> it.visibility = View.GONE }

        // wyświetla właściwe obiekty LinearLayout przycisków odpowiedzi
        guessLinearLayouts.take(guessRows).forEach { it -> it.visibility = View.VISIBLE }

    }

    fun updateRegions(sharedPreferences: SharedPreferences?) {
        regionsSet = sharedPreferences!!.getStringSet(MainActivity.REGIONS, null)
    }

    fun resetQuiz() {
        // AssetManager jest używany do uzyskiwania nazw plików obrazów flag z wybranych obszarów
        val assets = activity.assets
        fileNameList.clear()

        try {
            for (region in regionsSet) {
                val paths = assets.list(region)
                paths.forEach { path -> fileNameList.add(path.replace(".png", "")) }
            }
        } catch (exception: IOException) {
            Log.e(TAG, "Błąd ładowania plików obrazów", exception)
        }

        correctAnswers = 0
        totalGuesses = 0
        quizCountriesList.clear()

        var flagCounter = 1
        val numberOfFlags = fileNameList.size

        while (flagCounter <= FLAGS_IN_QUIZ) {
            val randomIndex = random.nextInt(numberOfFlags)

            // uzyskaj losową nazwę pliku
            val fileName = fileNameList[randomIndex]

            // jeżeli obszar jest aktywny ale nie został jeszcze wybrany
            if (!quizCountriesList.contains(fileName)) {
                quizCountriesList.add(fileName) // dodaj pliki do listy
                flagCounter++
            }
        }
        loadNextFlag() // uruchom quiz, ładując pierwszą flagę
    }

    // załaduj kolejną flagę po udzieleniu przez użytkownika poprawnej odpowiedzi
    private fun loadNextFlag() {
        // ustal nazwę pliku kolejnej flagi i usuń ją z listy
        val nextImage: String = quizCountriesList[0]
        quizCountriesList.removeAt(0)
        correctAnswer = nextImage
        answerTextView.text = ""

        // wyświetl numer bieżącego pytania
        questionNumberTextView.text = getString(R.string.question, (correctAnswers + 1), FLAGS_IN_QUIZ)

        // odczytaj informację o obszarze z nazwy kolejnego pliku obrazu
        val region = nextImage.substring(0, nextImage.indexOf('-'))

        // skorzystaj z AssetManager w celu załadowania kolejnego obrazu z folderu assets
        val assets = activity.assets

        // uzyskaj InputStream zasobu kolejnej flagi
        // i spróbuj z niego skorzystać
        try {
            val stream: InputStream = assets.open("$region/$nextImage.png")
            val flag = Drawable.createFromStream(stream, nextImage)
            flagImageView.setImageDrawable(flag)
        } catch (exception: IOException) {
            Log.e(TAG, "Błąd ładowania $nextImage", exception)
        }
        fileNameList.shuffle()

        // prawidłową odpowiedź umieść na końcu listy fileNameList
        val correct: Int = fileNameList.indexOf(correctAnswer)
        fileNameList.add(fileNameList.removeAt(correct))

        // dodaj 2, 4, 6 lub 8 przycisków odpowiedzi w zależności od wartości zmiennej guessRows
        for (row in 0 until guessRows) {
            // umieść przyciski w currentTableRow
            for (column in 0 until guessLinearLayouts[row].childCount) {
                // uzyskaj odwołanie do przycisku w celu jego skonfigurowania
                val newGuessButton: Button = guessLinearLayouts[row].getChildAt(column) as Button
                newGuessButton.isEnabled = true

                // ustal nazwę kraju i przekształć ją na tekst wyświetlany w obiekcie newGuessButton
                val fileName = fileNameList[row * 2 + column]
                newGuessButton.text = getCountryName(fileName)
            }
        }
        val row = random.nextInt(guessRows) // losuj wiersz
        val column = random.nextInt(2)  // losuj kolumnę
        val randomRow = guessLinearLayouts[row]
        val countryName = getCountryName(correctAnswer)
        val button = randomRow.getChildAt(column) as Button
        button.text = countryName
    }

    private fun getCountryName(name: String) = name
            .substring(name.indexOf('-') + 1)
            .replace('_', ' ')

    private fun animate(animateOut: Boolean) {
        // zapobiegaj wyświetleniu animacji podczas umieszczania pierwszej flagi na ekranie
        if (correctAnswers == 0) {
            return
        }
        // oblicz współrzędne x i y środka
        val centerX = (quizLinearLayout.left + quizLinearLayout.right) / 2
        val centerY = (quizLinearLayout.top + quizLinearLayout.bottom) / 2
        // oblicz promień animacji
        val radius: Float = Math.max(quizLinearLayout.width, quizLinearLayout.height) * 1F // to float

        val animator: Animator?

        // jeżeli rozkład quizLinearLayout ma być umieszczony na ekranie, a nie z niego zdejmowany
        when (animateOut) {
            true -> {
                animator = ViewAnimationUtils.createCircularReveal(quizLinearLayout, centerX, centerY, radius, 0F)

                val animListener = object : AnimatorListenerAdapter() {
                    override fun onAnimationEnd(animation: Animator?) {
                        loadNextFlag()
                    }
                }
                animator.addListener(animListener)
            }
            false -> {
                animator = ViewAnimationUtils.createCircularReveal(quizLinearLayout, centerX, centerY, 0F, radius)
            }
        }
        animator.duration = 500
        animator.start()
    }

    // metoda narzędziowa dezaktywująca wszystkie przyciski odpowiedzi
    private fun disableButtons() {
        for (row in 0 until guessRows) {
            val guessRow = guessLinearLayouts[row]
            for (i in 0 until guessRow.childCount) {
                guessRow.getChildAt(i).isEnabled = false
            }
        }
    }

    inner class GuessButtonListener : View.OnClickListener {
        override fun onClick(p0: View?) {
            val guessButton = p0 as Button
            val guess = guessButton.text.toString()
            val answer = getCountryName(correctAnswer)
            ++totalGuesses
            if (guess == answer) {
                ++correctAnswers

                answerTextView.text = answer
                answerTextView.setTextColor(resources.getColor(R.color.correct_answer, context.theme))
                disableButtons() // dezaktywuj wszysktie przyciski odpowiedzi

                // jezeli uzytkownik zgadł wszysktie flagi
                if (correctAnswers == FLAGS_IN_QUIZ) {
                    // DialogFragment wyświetla status quizu i uruchamia nowy quiz
                    val quizResults = QuizDialogFragment()
                    val args = Bundle()
                    args.putInt("TotalGuesses", totalGuesses)
                    quizResults.arguments = args
                    quizResults.isCancelable = false
                    quizResults.show(fragmentManager, "quiz results")
                } else { // odpowiedź jest poprawna, ale quiz się jeszcze nie skończył
                    // odczekaj 2 sekundy i załaduj kolejną flagę

                    handler.postDelayed({ animate(true) }, 2000)
                }

            } else {   // odpowiedź jest niepoprawna
                // odtwórz animację trzęsącej się flagi
                flagImageView.startAnimation(shakeAnimation)
                answerTextView.setText(R.string.incorrect_answer)
                answerTextView.setTextColor(resources.getColor(R.color.incorrect_answer, context.theme))
                guessButton.isEnabled = false
            }
        }
    }
}
