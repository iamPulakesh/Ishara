package com.isharaai.isl.feature.onboarding

import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import com.isharaai.isl.core.theme.*
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.isharaai.isl.core.language.LanguageManager

private const val PREF_NAME = "ishara_onboarding"
private const val KEY_COMPLETED = "onboarding_completed"
private const val KEY_TUTORIAL_PENDING = "tutorial_pending"

private fun prefs(context: Context) =
    context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)

fun isOnboardingCompleted(context: Context): Boolean =
    prefs(context).getBoolean(KEY_COMPLETED, false)

fun setOnboardingCompleted(context: Context) =
    prefs(context).edit().putBoolean(KEY_COMPLETED, true).apply()

fun isTutorialPending(context: Context): Boolean =
    prefs(context).getBoolean(KEY_TUTORIAL_PENDING, false)

fun setTutorialPending(context: Context, pending: Boolean) =
    prefs(context).edit().putBoolean(KEY_TUTORIAL_PENDING, pending).apply()

@Composable
fun OnboardingScreen(onComplete: (wantsTutorial: Boolean) -> Unit) {
    val context = LocalContext.current
    var step by remember { mutableIntStateOf(0) } // 0 = language, 1 = tutorial prompt

    Box(
        modifier = Modifier.fillMaxSize().background(AppGreenDark),
        contentAlignment = Alignment.Center
    ) {
        Card(
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(8.dp),
            modifier = Modifier.padding(32.dp).fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(28.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                if (step == 0) {
                    // Language Selection
                    Text("Welcome to Ishara", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = AppGreenDark)
                    Text("ইশারায় স্বাগতম", fontSize = 16.sp, color = AppGreenMedium)
                    Spacer(modifier = Modifier.height(24.dp))
                    Text("Please select your language", fontSize = 14.sp, color = Color.Gray, textAlign = TextAlign.Center)
                    Text("আপনার ভাষা নির্বাচন করুন", fontSize = 13.sp, color = Color.Gray, textAlign = TextAlign.Center)
                    Spacer(modifier = Modifier.height(20.dp))

                    LangCard("English", "🇺🇸") {
                        LanguageManager.saveLanguageOnly(context, LanguageManager.LANG_ENGLISH)
                        step = 1
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                    LangCard("Bengali (বাংলা)", "🇮🇳") {
                        LanguageManager.saveLanguageOnly(context, LanguageManager.LANG_BENGALI)
                        step = 1
                    }
                } else {
                    // Tutorial Prompt
                    val isBn = LanguageManager.getCurrentLanguage(context) == LanguageManager.LANG_BENGALI
                    Text(
                        if (isBn) "আপনি কি টিউটোরিয়াল চান?" else "Would you like to see tutorial?",
                        fontSize = 20.sp, fontWeight = FontWeight.Bold, color = AppGreenDark, textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(24.dp))
                    Button(
                        onClick = {
                            setOnboardingCompleted(context)
                            setTutorialPending(context, true)
                            LanguageManager.applyStoredLanguage(context)
                            onComplete(true)
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = AppGreen),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) { Text(if (isBn) "হ্যাঁ" else "Yes, show me", fontWeight = FontWeight.Bold, fontSize = 15.sp) }
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedButton(
                        onClick = {
                            setOnboardingCompleted(context)
                            LanguageManager.applyStoredLanguage(context)
                            onComplete(false)
                        },
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) { Text(if (isBn) "না" else "No, skip", fontSize = 15.sp, color = AppGreen) }
                }
            }
        }
    }
}

@Composable
private fun LangCard(label: String, flag: String, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(ISLCardBg)
            .clickable(onClick = onClick)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(flag, fontSize = 26.sp)
        Spacer(modifier = Modifier.width(14.dp))
        Text(label, fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = AppGreenDark)
    }
}
