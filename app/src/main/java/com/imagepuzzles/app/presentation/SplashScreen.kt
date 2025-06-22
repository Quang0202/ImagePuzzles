package com.imagepuzzles.app.presentation

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.imagepuzzles.app.R
import com.imagepuzzles.app.navigation.Screen
import kotlinx.coroutines.delay

@Composable
fun SplashScreen(navController: NavController) {
    // State để điều khiển tiến trình của ProgressBar
    var progress by remember { mutableStateOf(0f) }

    // Animation cho progress bar
    val animatedProgress by animateFloatAsState(
        targetValue = progress,
        animationSpec = tween(durationMillis = 2000),
        label = "ProgressAnimation"
    )

    LaunchedEffect(Unit) {
        while (progress < 1f) {
            progress += 0.1f
            delay(200L)
        }
       navController.navigate(Screen.Game.route){
           popUpTo(navController.graph.startDestinationId) {
               inclusive = true
           }
           launchSingleTop = true
       }
    }

    // UI của Splash Screen
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White),
        contentAlignment = Alignment.Center
    ) {
        Image(
            painter = painterResource(id = R.drawable.splash),
            contentDescription = "App Logo",
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop // Đảm bảo hình ảnh lấp đầy màn hình
        )

        // ProgressBar ở dưới cùng
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Transparent),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(Modifier.weight(2f))
            LinearProgressIndicator(
                progress = { animatedProgress },
                modifier = Modifier
                    .size(width = 200.dp, height = 8.dp),
                color = MaterialTheme.colorScheme.primary,
                trackColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)
            )
            Spacer(Modifier.weight(1f))
        }
    }
}