package br.com.devcapu.remy.conversation

import android.app.Activity
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.util.Log
import java.util.Locale
import java.util.TimeZone
import kotlin.collections.any
import kotlin.collections.filter
import kotlin.collections.forEach
import kotlin.collections.isNotEmpty
import kotlin.let
import kotlin.ranges.until
import kotlin.text.appendLine
import kotlin.text.equals
import kotlin.text.startsWith
import kotlin.text.uppercase

class LanguageConfigChecker {

    companion object {
        private const val TAG = "LanguageConfigChecker"

        fun checkPortugueseConfiguration(context: Context) {
            Log.i(TAG, "=== VERIFICAÇÃO COMPLETA DE CONFIGURAÇÃO DE PORTUGUÊS ===")

            checkSystemLanguage(context)
            checkAvailableLocales()
            checkSpeechRecognitionLanguages(context)
            checkRegionalSettings(context)
            checkGoogleServices(context)
        }

        private fun checkSystemLanguage(context: Context) {
            Log.i(TAG, "--- 1. IDIOMA DO SISTEMA ---")

            val defaultLocale = Locale.getDefault()
            Log.i(TAG, "Idioma padrão: ${defaultLocale.language}")
            Log.i(TAG, "País padrão: ${defaultLocale.country}")
            Log.i(TAG, "Locale completo: $defaultLocale")
            Log.i(TAG, "Nome do idioma: ${defaultLocale.displayLanguage}")
            Log.i(TAG, "Nome do país: ${defaultLocale.displayCountry}")

            val isPortuguese = defaultLocale.language.equals("pt", ignoreCase = true)
            Log.i(TAG, "É português? $isPortuguese")

            if (isPortuguese) {
                when (defaultLocale.country.uppercase()) {
                    "BR" -> Log.i(TAG, "✅ Português do Brasil configurado")
                    "PT" -> Log.i(TAG, "✅ Português de Portugal configurado")
                    else -> Log.i(TAG, "⚠️ Português genérico configurado")
                }
            } else {
                Log.w(TAG, "❌ Português NÃO está configurado como idioma principal")
            }

            val config = context.resources.configuration
            val appLocale = config.locales.get(0)
            Log.i(TAG, "Locale da aplicação: $appLocale")
        }

        private fun checkAvailableLocales() {
            Log.i(TAG, "--- 2. LOCALES DISPONÍVEIS ---")

            val availableLocales = Locale.getAvailableLocales()
            val portugueseLocales = availableLocales.filter {
                it.language.equals("pt", ignoreCase = true)
            }

            Log.i(TAG, "Total de locales disponíveis: ${availableLocales.size}")
            Log.i(TAG, "Locales de português encontrados: ${portugueseLocales.size}")

            portugueseLocales.forEach { locale ->
                Log.i(TAG, "  • $locale - ${locale.displayName}")
            }

            if (portugueseLocales.isNotEmpty()) {
                Log.i(TAG, "✅ Português está disponível no sistema")
            } else {
                Log.w(TAG, "❌ Português NÃO encontrado nos locales disponíveis")
            }
        }

        fun checkSpeechRecognitionLanguages(context: Context) {
            Log.i(TAG, "--- 3. RECONHECIMENTO DE VOZ ---")

            val isAvailable = SpeechRecognizer.isRecognitionAvailable(context)
            Log.i(TAG, "Reconhecimento de voz disponível: $isAvailable")

            if (!isAvailable) {
                Log.e(TAG, "❌ Reconhecimento de voz NÃO está disponível")
                return
            }

            val intent = Intent(RecognizerIntent.ACTION_GET_LANGUAGE_DETAILS)
            context.sendOrderedBroadcast(
                intent,
                null,
                object : BroadcastReceiver() {
                    override fun onReceive(context: Context?, intent: Intent?) {
                        val supportedLanguages =
                            intent?.getStringArrayListExtra(RecognizerIntent.EXTRA_SUPPORTED_LANGUAGES)
                        val preferredLanguage =
                            intent?.getStringExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE)

                        Log.i(TAG, "Idioma preferido: $preferredLanguage")
                        Log.i(TAG, "Total de idiomas suportados: ${supportedLanguages?.size ?: 0}")

                        supportedLanguages?.let { languages ->
                            val portugueseLanguages = languages.filter {
                                it.startsWith("pt", ignoreCase = true)
                            }

                            Log.i(
                                TAG,
                                "Idiomas portugueses suportados: ${portugueseLanguages.size}"
                            )
                            portugueseLanguages.forEach { lang ->
                                Log.i(TAG, "  • $lang")
                            }

                            if (portugueseLanguages.isNotEmpty()) {
                                Log.i(TAG, "✅ Português está disponível para reconhecimento de voz")
                            } else {
                                Log.w(
                                    TAG,
                                    "❌ Português NÃO está disponível para reconhecimento de voz"
                                )
                            }

                            val hasBrazilian =
                                portugueseLanguages.any { it.equals("pt-BR", ignoreCase = true) }
                            Log.i(TAG, "pt-BR disponível: $hasBrazilian")
                        }
                    }
                },
                null,
                Activity.RESULT_OK,
                null,
                null
            )
        }

        private fun checkRegionalSettings(context: Context) {
            Log.i(TAG, "--- 4. CONFIGURAÇÕES REGIONAIS ---")

            val config = context.resources.configuration

            val locales = config.locales
            Log.i(TAG, "Número de locales configurados: ${locales.size()}")

            for (i in 0 until locales.size()) {
                val locale = locales.get(i)
                Log.i(TAG, "  Locale $i: $locale")

                if (locale.language.equals("pt", ignoreCase = true)) {
                    Log.i(TAG, "  ✅ Português encontrado na posição $i")
                }
            }


            val timeZone = TimeZone.getDefault()
            Log.i(TAG, "Timezone: ${timeZone.id}")

            val brazilianTimezones = listOf(
                "America/Sao_Paulo", "America/Manaus", "America/Fortaleza",
                "America/Recife", "America/Bahia", "America/Cuiaba"
            )

            if (brazilianTimezones.contains(timeZone.id)) {
                Log.i(TAG, "✅ Timezone brasileiro detectado")
            }
        }

        private fun checkGoogleServices(context: Context) {
            Log.i(TAG, "--- 5. GOOGLE SERVICES ---")

            val googleAppPackages = listOf(
                "com.google.android.googlequicksearchbox", // Google App
                "com.google.android.gms", // Google Play Services
                "com.google.android.tts", // Google Text-to-Speech
                "com.google.android.apps.speechservices" // Google Speech Services
            )

            val packageManager = context.packageManager

            googleAppPackages.forEach { packageName ->
                try {
                    val packageInfo = packageManager.getPackageInfo(packageName, 0)
                    Log.i(TAG, "✅ $packageName instalado (versão: ${packageInfo.versionName})")
                } catch (e: Exception) {
                    Log.w(TAG, "❌ $packageName NÃO instalado")
                }
            }
        }

        fun showConfigurationSummary(context: Context): String {
            val defaultLocale = Locale.getDefault()
            val isPortuguese = defaultLocale.language.equals("pt", ignoreCase = true)
            val isRecognitionAvailable = SpeechRecognizer.isRecognitionAvailable(context)

            val summary = buildString {
                appendLine("📱 RESUMO DAS CONFIGURAÇÕES")
                appendLine("════════════════════════")
                appendLine("Idioma do sistema: ${defaultLocale.displayLanguage}")
                appendLine("País: ${defaultLocale.displayCountry}")
                appendLine("Português configurado: ${if (isPortuguese) "✅ Sim" else "❌ Não"}")
                appendLine("Reconhecimento disponível: ${if (isRecognitionAvailable) "✅ Sim" else "❌ Não"}")
                appendLine("Locale completo: $defaultLocale")

                if (!isPortuguese) {
                    appendLine()
                    appendLine("⚠️ AÇÃO NECESSÁRIA:")
                    appendLine("Vá em Configurações > Idioma e região")
                    appendLine("Adicione ou selecione Português")
                }

                if (!isRecognitionAvailable) {
                    appendLine()
                    appendLine("⚠️ PROBLEMA:")
                    appendLine("Reconhecimento de voz não disponível")
                    appendLine("Instale o Google App ou Google Speech Services")
                }
            }

            Log.i(TAG, summary)
            return summary
        }

        fun setPortugueseLocale(context: Context, countryCode: String = "BR") {
            Log.i(TAG, "Tentando configurar locale para pt-$countryCode")

            val locale = Locale("pt", countryCode)
            Locale.setDefault(locale)

            val config = Configuration()
            config.locale = locale

            context.resources.updateConfiguration(config, context.resources.displayMetrics)

            Log.i(TAG, "Locale configurado para: ${Locale.getDefault()}")
        }
    }
}