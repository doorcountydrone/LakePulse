package com.lakepulse.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import com.lakepulse.R

@Composable
fun SplashScreen() {
    Image(
        painter = painterResource(id = R.drawable.lakepulse_background),
        contentDescription = "LakePulse",
        contentScale = ContentScale.Crop,
        modifier = Modifier.fillMaxSize(),
    )
}
