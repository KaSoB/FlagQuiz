package com.example.kamil.flagquiz

import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.ActivityInfo
import android.os.Bundle
import android.support.design.widget.Snackbar
import android.support.v7.app.AppCompatActivity
import android.view.Menu
import android.view.MenuItem
import android.content.res.Configuration
import android.preference.PreferenceManager
import android.view.View
import android.widget.Toast

import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.content_main.*
import android.support.v4.app.Fragment

class MainActivity : AppCompatActivity() {
    companion object {
        const val CHOICES: String = "pref_numberOfChoices"
        const val REGIONS: String = "pref_regionsToInclude"
    }

    // Wymusza tryb portretowy
    private var phoneDevice = true
    // Informuje czy preferencje zostały zmienione
    private var preferencesChanged = true

    private var preferencesChangeListener = PreferencesListener()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        setSupportActionBar(toolbar)

        // przypisuje domyślne wartosci do sharedprefences
        PreferenceManager.setDefaultValues(this, R.xml.preferences, false)

        // rejestruje obiekt nasłuchujący zmian SharedPreferences
        PreferenceManager.getDefaultSharedPreferences(this).registerOnSharedPreferenceChangeListener(preferencesChangeListener)

        // określa rozmiar ekranu
        val screenSize = resources.configuration.screenLayout and Configuration.SCREENLAYOUT_SIZE_MASK // TODO: Upewnić się czy w kotlinie 'and' to operator &
        // jeżeli urządzenie jest tabletem, przypisuje wartości false zmiennej phoneDevice
        if (screenSize == Configuration.SCREENLAYOUT_SIZE_LARGE || screenSize == Configuration.SCREENLAYOUT_SIZE_XLARGE) {
            phoneDevice = false

        }
        // jeżeli aplikacja działa na urządzeniu mającym wymiary telefonu, to zezwalaj tylko na orientację pionową
        if (phoneDevice) {
            requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        }
    }

    override fun onStart() {
        super.onStart()
        if (preferencesChanged) {
            // Teraz gdy domyślne preferencje zostały ustawione
            // zainicjuj MainActivityFragment i uruchom quiz.
            val quizFragment : MainActivityFragment = supportFragmentManager.findFragmentById(R.id.quizFragment) as MainActivityFragment
            quizFragment.updateGuessRows(PreferenceManager.getDefaultSharedPreferences(this))
            quizFragment.updateRegions(PreferenceManager.getDefaultSharedPreferences(this))
            quizFragment.resetQuiz()
            // zmiany zostały wprowadzone -> resetuje flagę
            preferencesChanged = false
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        // ustal aktualną oreintację urządzenia
        if (resources.configuration.orientation == Configuration.ORIENTATION_PORTRAIT) {
            // Inflate the menu; this adds items to the action bar if it is present.
            menuInflater.inflate(R.menu.menu_main, menu)
            return true
        }
        return false
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        startActivity(Intent(this, SettingsActivity::class.java))
        return super.onOptionsItemSelected(item)
    }

    inner class PreferencesListener : SharedPreferences.OnSharedPreferenceChangeListener {
        override fun onSharedPreferenceChanged(p0: SharedPreferences?, p1: String?) {
            preferencesChanged = true // użytkownik zmienił ustawienia aplikacji
            val quizFragment: MainActivityFragment = supportFragmentManager.findFragmentById(R.id.quizFragment) as MainActivityFragment

            when (p1) {
            // zmiana liczby wyświetlnaych odpowiedzi
                CHOICES -> {
                    quizFragment.updateGuessRows(p0)
                    quizFragment.resetQuiz()
                }

                REGIONS -> {
                    val regions = p0!!.getStringSet(REGIONS, null)

                    if (regions != null && regions.any()) {
                        quizFragment.updateRegions(p0)
                        quizFragment.resetQuiz()
                    } else {
                        // przynajmniej jeden obszar musi zostać wybrany - domyślnie jest to Ameryka Północna
                        val editor: SharedPreferences.Editor = p0.edit()
                        regions.add(getString(R.string.default_region))
                        editor.putStringSet(REGIONS,regions)
                        editor.apply()
                        Toast.makeText(this@MainActivity, R.string.default_region_message, Toast.LENGTH_SHORT).show()
                    }
                }

            }
            Toast.makeText(this@MainActivity, R.string.restarting_quiz, Toast.LENGTH_SHORT).show()
        }
    }
}

