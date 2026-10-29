package com.hirojonn.bedduu.ui.designsystem

import android.provider.Settings
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.DurationBasedAnimationSpec
import androidx.compose.animation.core.FiniteAnimationSpec
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

object MotionSpec {
    const val Short = 150
    const val Medium = 250
    const val Long = 350
    const val TabFadeOut = 90
    val StandardEaseOut = CubicBezierEasing(0f, 0f, 0.2f, 1f)

    @Composable
    fun <T> tween(durationMillis: Int): DurationBasedAnimationSpec<T> =
        tween(
            durationMillis = if (animationsDisabled()) 0 else durationMillis,
            easing = StandardEaseOut
        )

    @Composable
    fun pressSpring(): FiniteAnimationSpec<Float> =
        if (animationsDisabled()) {
            tween(durationMillis = 0)
        } else {
            spring(dampingRatio = 0.6f, stiffness = Spring.StiffnessMedium)
        }

    @Composable
    private fun animationsDisabled(): Boolean {
        val context = LocalContext.current
        return Settings.Global.getFloat(
            context.contentResolver,
            Settings.Global.ANIMATOR_DURATION_SCALE,
            1f
        ) == 0f
    }
}
