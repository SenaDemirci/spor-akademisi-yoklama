package com.senademirci.futbolyoklama.ui.theme

import androidx.compose.ui.graphics.Color

// Saha yeşili ana palet
val PitchGreen = Color(0xFF1B7A3E)
val PitchGreenDark = Color(0xFF0E5228)
val PitchGreenLight = Color(0xFFA8E6BC)

// Yoklama durum renkleri — dört durum da ilk bakışta ayırt edilebilmeli
val StatusPresent = Color(0xFF1B7A3E) // Var
val StatusAbsent = Color(0xFFC62828)  // Yok
val StatusExcused = Color(0xFF1565C0) // İzinli
val StatusLate = Color(0xFFE9820A)    // Geç geldi

// Devamsızlık oranı vurguları
val RiskHigh = Color(0xFFC62828)   // %30 üzeri
val RiskMedium = Color(0xFFE9820A) // %15-30
