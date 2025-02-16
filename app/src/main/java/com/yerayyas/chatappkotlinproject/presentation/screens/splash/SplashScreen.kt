package com.yerayyas.chatappkotlinproject.presentation.screens.splash

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.airbnb.lottie.compose.LottieAnimation
import com.airbnb.lottie.compose.LottieCompositionSpec
import com.airbnb.lottie.compose.LottieConstants
import com.airbnb.lottie.compose.animateLottieCompositionAsState
import com.airbnb.lottie.compose.rememberLottieComposition
import com.yerayyas.chatappkotlinproject.R
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay

@Composable
fun SplashScreen(
    modifier: Modifier = Modifier,
    onNavigateToMain: () -> Unit
) {
    var shouldNavigate by remember { mutableStateOf(true) }

    LaunchedEffect(Unit) {
        if (shouldNavigate) {
            delay(3000) // Solo espera en primer inicio
            onNavigateToMain()
        }
    }

    Column(
        modifier = modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Loader()
        Text(
            text = stringResource(id = R.string.app_name),
            modifier = Modifier.padding(top = 10.dp),
            fontSize = 25.sp,
            style = TextStyle(fontWeight = FontWeight.Bold)
        )
        Text(
            text = stringResource(id = R.string.developer_name),
            modifier = Modifier.padding(top = 10.dp),
            fontSize = 25.sp,
            style = TextStyle(fontWeight = FontWeight.Bold)
        )
    }
}


@Composable
fun Loader() {
    val composition by rememberLottieComposition(LottieCompositionSpec.RawRes(R.raw.chat_animation))
    val progress by animateLottieCompositionAsState(
        composition = composition,
        iterations = LottieConstants.IterateForever
    )
    LottieAnimation(
        composition = composition,
        progress = { progress },
        modifier = Modifier
            .fillMaxWidth()
            .size(600.dp),
        contentScale = ContentScale.Fit
    )
}

