package dev.lucasnlm.antimine.language

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.preference.Preference
import androidx.preference.PreferenceCategory
import androidx.preference.PreferenceFragmentCompat
import dev.lucasnlm.antimine.R
import dev.lucasnlm.antimine.preferences.IPreferencesRepository
import dev.lucasnlm.antimine.ui.ext.ThematicActivity
import kotlinx.android.synthetic.main.activity_language.*
import org.koin.android.ext.android.inject
import java.util.Locale

class LanguageSelectorActivity : ThematicActivity(R.layout.activity_language) {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        section.bind(
            text = R.string.select_language,
            startButton = R.drawable.back_arrow,
            startAction = {
                finish()
            },
        )

        open_crowdin.bind(
            theme = usingTheme,
            text = R.string.crowdin,
            invert = true,
            startIcon = R.drawable.translate,
            onAction = {
                try {
                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(CROWDIN_URL))
                    startActivity(intent)
                } catch (e: Exception) {
                    Toast.makeText(applicationContext, R.string.unknown_error, Toast.LENGTH_SHORT).show()
                }
            },
        )

        placePreferenceFragment()
    }

    private fun placePreferenceFragment() {
        supportFragmentManager.apply {
            popBackStack()

            beginTransaction().apply {
                replace(
                    R.id.preference_fragment,
                    LanguageListFragment(),
                    LanguageListFragment.TAG,
                )
                commitAllowingStateLoss()
            }
        }
    }

    class LanguageListFragment : PreferenceFragmentCompat() {
        private val preferenceRepository: IPreferencesRepository by inject()

        private val languagesMap = mapOf(
            "Afrikaans" to "af-rZA",
            "??????????????" to "ar-rSA",
            "Catal??" to "ca-rES",
            "??e??tina" to "cs-rCZ",
            "Dansk" to "da-rDK",
            "Deutsch" to "de-rDE",
            "????????????????" to "el-rGR",
            "English" to "en-rUS",
            "Espa??ol" to "es-rES",
            "Suomi" to "fi-rFI",
            "Fran??ais" to "fr-rFR",
            "??????????????????" to "hi-rIN",
            "Latvie??u valoda" to "lv-rLV",
            "Lietuvi?? kalba" to "lt-rLT",
            "Magyar" to "hu-rHU",
            "Italiano" to "it-rIT",
            "??????????" to "iw-rIL",
            "?????????" to "ja-rJP",
            "?????????" to "ko-rKR",
            "Nederlands" to "nl-rNL",
            "Bokm??l" to "no-rNO",
            "Polski" to "pl-rPL",
            "Portugu??s (BR)" to "pt-rBR",
            "Portugu??s (PT)" to "pt-rPT",
            "Rom??n??" to "ro-rRO",
            "P????????????" to "ru-rRU",
            "??l??nsko" to "szl",
            "Svenska" to "sv-rSE",
            "Slovensko" to "sl-rSI",
            "?????????" to "th-rTH",
            "T??rk??e" to "tr-rTR",
            "Y??????????????????" to "uk-rUA",
            "Ti???ng Vi???t" to "vi-rVN",
            "??????" to "zh-rCN",
            "??????????????????" to "bg-rBG",
            "Bahasa Indonesia" to "in-rID",
            "V??neto" to "vec-rIT",
            "?????????? ??????????" to "ku-rTR",
        )

        override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
            addPreferencesFromResource(R.xml.language_preferences)

            findPreference<PreferenceCategory>(LANGUAGE_LIST)?.apply {
                languagesMap
                    .mapValues {
                        val language = it.value.split("-")
                        Locale(language.first(), language.last())
                    }
                    .toList()
                    .sortedBy {
                        it.first
                    }
                    .forEach { (language, locale) ->
                        addPreference(
                            Preference(context).apply {
                                title = language
                                isIconSpaceReserved = false
                                setOnPreferenceClickListener {
                                    preferenceRepository.setPreferredLocale("${locale.language}-${locale.country}")
                                    activity?.finish()
                                    true
                                }
                            },
                        )
                    }
            }
        }

        companion object {
            val TAG = LanguageListFragment::class.simpleName
            private const val LANGUAGE_LIST = "language_list"
        }
    }

    companion object {
        private const val CROWDIN_URL = "https://crowdin.com/project/antimine-android"
    }
}
