package com.example.kamil.flagquiz

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.annotation.SuppressLint
import android.app.AlertDialog
import android.app.Dialog
import android.content.DialogInterface
import android.content.SharedPreferences
import android.content.res.AssetManager
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
import android.widget.LinearLayout
import android.widget.TextView
import kotlinx.android.synthetic.main.fragment_main.*
import kotlinx.android.synthetic.main.fragment_main.view.*
import java.io.IOError
import java.io.IOException
import java.security.SecureRandom
import java.util.*


class MainActivityFragment : Fragment() {
    companion object {
        const val TAG: String = "FlagQuiz Activity"
        const val FLAGS_IN_QUIZ = 10
    }



    // nazwy plików flag
    lateinit var fileNameList: MutableList<String>
    // kraje biezącego quizu
    lateinit var quizCountriesList: MutableList<String>
    // obszary bieżącego quizu
    private lateinit var regionsSet: Set<String>
    // poprawna nazwa kraju przypisania do bieżącej flagi
    private lateinit var correctAnswer: String
    // liczba prób odpowiedzi
    private var totalGuesses: Int = 0
    // liczba poprawnych odpowiedzi
    private var correctAnswers: Int = 0
    // liczba wierszy przycisków odpowiedzi wyświetlanych na ekranie
    private var guesssRows: Int = 0
    // obiekt używany podczas losowania
    private lateinit var random: SecureRandom
    // zmienna uzywana podczas opozniania ładowania kolejnej flagi
    private lateinit var handler: Handler
    // animacja błędnej odpowiedzi
    private lateinit var shakeAnimation: Animation
    // wiersze przycisków odpowiedzi
    private var guessLinearLayouts: List<LinearLayout> = listOf(row1LinearLayout, row2LinearLayout, row3LinearLayout, row4LinearLayout)



    

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        val view = inflater.inflate(R.layout.fragment_main, container, false)


//        handler = Handler()
//        random = SecureRandom()


        shakeAnimation = AnimationUtils.loadAnimation(activity, R.anim.incorrect_shake)
        shakeAnimation.repeatCount = 3
//
//
//        // konfiguruje obiekty nasłuchujące przycisków odpowiedzi
//        for (item : LinearLayout in guessLinearLayouts) {
//           if(item == null){
//                Log.v("Jednak","Jednak null")
//           }
//            for (i in 0 until item.childCount) {
//                item.getChildAt(i).setOnClickListener(GuessButtonListener())
//            }
//        }
//
//
//        // określa tekst wyświetlany w polach questionNumberTextView

        val questionNumberTextView = view.findViewById(R.id.questionNumberTextView) as TextView
        questionNumberTextView.text = getString(R.string.question,1, FLAGS_IN_QUIZ)
        return view
    }

    // aktualizuje zmienną guessRows na podstawie wartości SharedPreferences
    fun updateGuessRows(sharedPreferences: SharedPreferences?) {
        // ustal liczbę przycisków odpowiedzi, które mają zostać wyświetlone
        val choices = sharedPreferences!!.getString(MainActivity.CHOICES, null)
        val guesssRows = Integer.parseInt(choices) / 2

        // ukryj wszystkie obiekty LinearLayout przycisków odpowiedzi
        guessLinearLayouts.forEach { it -> it.visibility = View.GONE }

        // wyświetla właściwe obiekty LinearLayout przycisków odpowiedzi
        guessLinearLayouts.take(guesssRows).forEach { it -> it.visibility = View.VISIBLE }

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
        questionNumberTextView.text = getString(R.string.question, (correctAnswers + 1), FLAGS_IN_QUIZ);

        // odczytaj informację o obszarze z nazwy kolejnego pliku obrazu
        var region = nextImage.substring(0, nextImage.indexOf('-'))

        // skorzystaj z AssetManager w celu załadowania kolejnego obrazu z folderu assets
        var assets = activity.assets

        // uzyskaj InputStream zasobu kolejnej flagi
        // i spróbuj z niego skorzystać
        try {

        } catch (exception: IOException) {
            Log.e(TAG, "Błąd ładowania $nextImage", exception)
        }
        fileNameList.shuffle()

        // prawidłową odpowiedź umieść na końcu listy fileNameList
        val correct: Int = fileNameList.indexOf(correctAnswer)
        fileNameList.add(fileNameList.removeAt(correct))

        // dodaj 2, 4, 6 lub 8 przycisków odpowiedzi w zależności od wartości zmiennej guessRows
        for (row in 0 until guesssRows) {
            // umieść przyciski w currentTableRow
            for (column in 0 until guessLinearLayouts[row].childCount) {
                // uzyskaj odwołanie do przycisku w celu jego skonfigurowania
                val newGuessButton: Button = guessLinearLayouts[row].getChildAt(column) as Button
                newGuessButton.isEnabled = true

                // ustal nazwę kraju i przekształć ją na tekst wyświetlany w obiekcie newGuessButton
                var fileName = fileNameList[row * 2 + column]
                newGuessButton.text = getCountryName(fileName)
            }
        }
        val row = random.nextInt(guesssRows) // losuj wiersz
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
        val centerX = (quizConstraintLayout.left + quizConstraintLayout.right) / 2
        val centerY = (quizConstraintLayout.top + quizConstraintLayout.bottom) / 2
        // oblicz promień animacji
        val radius: Float = Math.max(quizConstraintLayout.width, quizConstraintLayout.height) * 1F // to float

        val animator: Animator?

        // jeżeli rozkład quizLinearLayout ma być umieszczony na ekranie, a nie z niego zdejmowany
        when (animateOut) {
            true -> {
                animator = ViewAnimationUtils.createCircularReveal(quizConstraintLayout, centerX, centerY, radius, 0F)
                animator.addListener(AnimatorListener())
            }

            false -> {
                animator = ViewAnimationUtils.createCircularReveal(quizConstraintLayout, centerX, centerY, 0F, radius)
            }

        }
        animator.duration = 500
        animator.start()
    }
    // metoda narzędziowa dezaktywująca wszystkie przyciski odpowiedzi
    private fun disableButtons(){
        for (row in 0 until guesssRows){
            val guessRow = guessLinearLayouts[row]
            for (i in 0 until guessRow.childCount){
                guessRow.getChildAt(i).isEnabled = true
            }
        }
    }

    inner class AnimatorListener : AnimatorListenerAdapter() {
        override fun onAnimationEnd(animation: Animator?) {
            loadNextFlag()
        }
    }
   @SuppressLint("ValidFragment")
   inner class QuizDialogFragment : DialogFragment() {
        override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
            val builder: AlertDialog.Builder = AlertDialog.Builder(activity)
            val totalGuesses =  arguments.getInt("TotalGuesses",0)
            builder.setMessage(getString(R.string.results, totalGuesses, (1000F / (totalGuesses * 1F))))

            builder.setPositiveButton(R.string.reset_quiz, PositiveButtonListener() )
            return builder.create()
        }

    }

    inner class GuessButtonListener : View.OnClickListener {
        override fun onClick(p0: View?) {
            val guessButton = p0 as Button
            var guess = guessButton.text.toString()
            var answer = getCountryName(correctAnswer)
            ++totalGuesses
            if (guess == answer) {
                ++correctAnswers;

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
                    handler.postDelayed(AnimateRunnable(),2000)
                }

            } else {   // odpowiedź jest niepoprawna
                // odtwórz animację trzęsącej się flagi
                flagImageView.startAnimation(shakeAnimation)
                answerTextView.setText(R.string.incorrect_answer)
                answerTextView.setTextColor(resources.getColor(R.color.incorrect_answer,context.theme))
                guessButton.isEnabled = false
            }
        }
    }
    inner class PositiveButtonListener :  DialogInterface.OnClickListener{
        override fun onClick(p0: DialogInterface?, p1: Int) {
            resetQuiz()
        }
    }
    inner class AnimateRunnable : Runnable{
        override fun run() {
            animate(true)
        }
    }
}
