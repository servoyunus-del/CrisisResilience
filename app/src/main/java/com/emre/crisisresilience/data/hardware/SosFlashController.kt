package com.emre.crisisresilience.data.hardware

import android.content.Context
import android.hardware.camera2.CameraManager
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import com.emre.crisisresilience.utils.helpers.MorseCodeHelper
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import kotlin.coroutines.coroutineContext

class SosFlashController @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val cameraManager = context.getSystemService(Context.CAMERA_SERVICE) as CameraManager
    private val cameraId = try { cameraManager.cameraIdList.firstOrNull { id ->
        cameraManager.getCameraCharacteristics(id).get(android.hardware.camera2.CameraCharacteristics.FLASH_INFO_AVAILABLE) == true
    } } catch (e: Exception) { null }

    private val vibrator: Vibrator? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
        vibratorManager.defaultVibrator
    } else {
        @Suppress("DEPRECATION")
        context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
    }

    suspend fun startSosSignal(text: String = "SOS") {
        if (cameraId == null) return

        val morsePattern = MorseCodeHelper.textToMorse(text)
        
        try {
            while (coroutineContext.isActive) {
                // Her bir kelime/harf/işaret kombinasyonunu dolaş
                for (charStr in morsePattern.split("|")) {
                    if (charStr == " ") {
                        // Kelime arası boşluk
                        delay(MorseCodeHelper.WORD_SPACE)
                        continue
                    }

                    // Harfin içindeki nokta ve çizgileri dolaş
                    for ((index, symbol) in charStr.withIndex()) {
                        if (!coroutineContext.isActive) throw CancellationException()

                        val duration = if (symbol == '.') MorseCodeHelper.DOT_DURATION else MorseCodeHelper.DASH_DURATION
                        
                        // Flaş ve Titreşimi AÇ
                        setTorchMode(true)
                        vibrate(duration)
                        delay(duration)
                        
                        // Flaş ve Titreşimi KAPAT
                        setTorchMode(false)
                        
                        // Son eleman değilse eleman arası boşluk, son elemansa harf arası boşluk
                        if (index < charStr.length - 1) {
                            delay(MorseCodeHelper.ELEMENT_SPACE)
                        } else {
                            delay(MorseCodeHelper.LETTER_SPACE)
                        }
                    }
                }
                // Kelime bittiğinde döngünün başına dönmeden önce uzun bir boşluk bırakılabilir (Örn: 2 saniye)
                delay(2000L)
            }
        } finally {
            // Coroutine iptal edildiğinde donanımı mutlak serbest bırak
            setTorchMode(false)
            vibrator?.cancel()
        }
    }

    private fun setTorchMode(enabled: Boolean) {
        try {
            cameraId?.let { cameraManager.setTorchMode(it, enabled) }
        } catch (e: Exception) {
            // Kamera başka bir uygulama tarafından kullanılıyor olabilir
        }
    }

    private fun vibrate(duration: Long) {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator?.vibrate(VibrationEffect.createOneShot(duration, VibrationEffect.DEFAULT_AMPLITUDE))
            } else {
                @Suppress("DEPRECATION")
                vibrator?.vibrate(duration)
            }
        } catch (e: Exception) {
            // Titreşim hatası
        }
    }
}
