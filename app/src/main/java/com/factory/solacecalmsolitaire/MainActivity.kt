package com.factory.solacecalmsolitaire

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.factory.solacecalmsolitaire.ui.screens.GameScreen
import com.factory.solacecalmsolitaire.ui.theme.SolaceCalmSolitaireTheme
import com.factory.solacecalmsolitaire.util.SoundPlayer
import com.factory.solacecalmsolitaire.viewmodel.GameViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        setContent {
            val gameViewModel: GameViewModel = viewModel()
            val uiState by gameViewModel.uiState.collectAsStateWithLifecycle()
            SolaceCalmSolitaireTheme(dynamicColor = uiState.dynamicColorEnabled) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    GameScreen(gameViewModel = gameViewModel)
                }
            }
        }
    }

    override fun onDestroy() {
        SoundPlayer.release()
        super.onDestroy()
    }
}
