package com.money.dividr.presentation.signin

import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.money.dividr.R

@Composable
fun SignInScreen(
    state: SignInState,
    onSignInClick: () -> Unit
) {
    val context = LocalContext.current

    LaunchedEffect(key1 = state.signInError) {
        state.signInError?.let {
            Toast.makeText(
                context,
                it,
                Toast.LENGTH_LONG
            ).show()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {

        Text(text = stringResource(id = R.string.app_name), modifier = Modifier.fillMaxWidth().padding(vertical = 32.dp), style = TextStyle(fontSize = 80.sp), textAlign = TextAlign.Center, fontFamily = FontFamily.Cursive)
        Spacer(modifier = Modifier.height(12.dp))
        Text(text = "Welcome to Dividr - Split your expenses!", modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp), style = TextStyle(fontSize = 24.sp), textAlign = TextAlign.Center, fontFamily = FontFamily.Cursive)
        Spacer(modifier = Modifier.weight(1f))
        Box(
            modifier = Modifier
                .fillMaxHeight(0.50f),
             
            contentAlignment = Alignment.Center
        ) {
            Image(
                painter = painterResource(id = R.drawable.wallet_ills),  
                contentDescription = ""  
            )
        }

         
        Spacer(modifier = Modifier.weight(1f))

         
        Button(
            onClick = onSignInClick,
            modifier = Modifier.fillMaxWidth(0.8f),
            shape = RoundedCornerShape(12.dp)  
        ) {
            Icon(
                painter = painterResource(id = R.drawable.google_g_logo),  
                contentDescription = "Google Icon"
            )
            Spacer(Modifier.width(8.dp))  
            Text(text = "Sign In With Google")
        }
        Spacer(modifier = Modifier.height(16.dp))  
    }
}
