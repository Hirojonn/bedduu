package com.hirojonn.bedduu

import android.os.Bundle
import android.graphics.Color
import android.os.Build
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.runtime.SideEffect
import androidx.lifecycle.lifecycleScope
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.hirojonn.bedduu.ui.MangaReaderApp
import com.hirojonn.bedduu.ui.AppViewModel
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    private val viewModel: AppViewModel by viewModels()
    private lateinit var insetsController: WindowInsetsControllerCompat

    override fun onResume() {
        super.onResume()
        lifecycleScope.launch {
            viewModel.checkBookmarkedChapterUpdates(applicationContext)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        window.statusBarColor = Color.TRANSPARENT
        window.navigationBarColor = Color.TRANSPARENT
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            window.attributes = window.attributes.apply {
                layoutInDisplayCutoutMode =
                    WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
            }
        }
        insetsController = WindowInsetsControllerCompat(window, window.decorView).apply {
            systemBarsBehavior =
                WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        }
        insetsController.show(
            androidx.core.view.WindowInsetsCompat.Type.systemBars()
        )
        setContent {
            SideEffect {
                insetsController.isAppearanceLightStatusBars = !viewModel.isDarkMode
                insetsController.isAppearanceLightNavigationBars = !viewModel.isDarkMode
            }
            MangaReaderApp(viewModel)
        }
    }
}
