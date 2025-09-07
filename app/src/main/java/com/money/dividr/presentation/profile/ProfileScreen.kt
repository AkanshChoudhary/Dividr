package com.money.dividr.presentation.profile

import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.money.dividr.R  
import com.money.dividr.navigation.AppRoutes

 

@Composable
fun ProfileScreen(
    navController: NavController,  
    viewModel: ProfileViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val signOutComplete by viewModel.signOutComplete.collectAsStateWithLifecycle()
    val context = LocalContext.current

    LaunchedEffect(signOutComplete) {
        if (signOutComplete) {
            navController.navigate(AppRoutes.SIGN_IN) {
                popUpTo(navController.graph.id) {  
                    inclusive = true
                }
                launchSingleTop = true  
            }
            viewModel.resetSignOutComplete()
        }
    }

    LaunchedEffect(uiState.error) {
        uiState.error?.let {
            Toast.makeText(context, it, Toast.LENGTH_LONG).show()
        }
    }

     
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),  
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Top
    ) {
        if (uiState.isLoading) {
            CircularProgressIndicator(modifier = Modifier.padding(top = 32.dp))
        } else {
            uiState.user?.let { user ->
                Spacer(modifier = Modifier.height(32.dp))

                AsyncImage(
                    model = user.photoUrl ?: R.drawable.group_icon,
                    contentDescription = "Profile Picture",
                    modifier = Modifier
                        .size(120.dp)
                        .clip(CircleShape),
                    contentScale = ContentScale.Crop,
                    error = painterResource(id = R.drawable.group_icon)
                )

                Spacer(modifier = Modifier.height(70.dp))

                Text(
                    text = user.displayName ?: "N/A",
                    style = MaterialTheme.typography.headlineSmall,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = user.email ?: "N/A",
                    style = MaterialTheme.typography.bodyLarge,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(48.dp))

                Button(
                    onClick = { viewModel.signOut() },
                    modifier = Modifier.fillMaxWidth(),
                    shape = MaterialTheme.shapes.medium
                ) {
                    Text("Sign Out")
                }
                Spacer(modifier = Modifier.height(16.dp))
            } ?: run {
                Text("User not found or not logged in.", modifier = Modifier.padding(top = 32.dp))
            }
        }
    }
}