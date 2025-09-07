package com.money.dividr.navigation

import android.app.Activity
import android.util.Log
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner  
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope  
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.money.dividr.presentation.add_expense.AddExpenseScreen
import com.money.dividr.presentation.expense_details.ExpenseDetailsScreen
import com.money.dividr.presentation.group_details.GroupDetailsScreen
import com.money.dividr.presentation.home.HomeScreen
import com.money.dividr.presentation.show_balances.ShowBalancesScreen
 
import com.money.dividr.presentation.signin.GoogleAuthUiClient
import com.money.dividr.presentation.signin.SignInScreen
import com.money.dividr.presentation.signin.SignInViewModel
import kotlinx.coroutines.launch

@Composable
fun AppNavHost(
    navController: NavHostController = rememberNavController(),
    googleAuthUiClient: GoogleAuthUiClient
) {
    val currentContext = LocalContext.current

    LaunchedEffect(key1 = Unit) {
        if (googleAuthUiClient.getSignedInUser() != null) {
            navController.navigate(AppRoutes.HOME) {
                popUpTo(navController.graph.id) {
                    inclusive = true
                }
            }
        }
    }

    NavHost(navController = navController, startDestination = AppRoutes.SIGN_IN) {
        composable(AppRoutes.SIGN_IN) {
            val viewModel: SignInViewModel = hiltViewModel()
            val state by viewModel.state.collectAsStateWithLifecycle()
            val lifecycleOwner = LocalLifecycleOwner.current  


            val launcher = rememberLauncherForActivityResult(
                contract = ActivityResultContracts.StartIntentSenderForResult(),
                onResult = { result ->
                    if (result.resultCode == Activity.RESULT_OK) {
                        lifecycleOwner.lifecycleScope.launch {  
                            val signInResult = googleAuthUiClient.signInWithIntent(
                                intent = result.data ?: return@launch
                            )
                            viewModel.onSignInResult(signInResult)
                        }
                    }
                }
            )

            LaunchedEffect(key1 = state.isSignInSuccessful) {
                if (state.isSignInSuccessful) {
                    Toast.makeText(
                        currentContext.applicationContext,
                        "Sign in successful",
                        Toast.LENGTH_LONG
                    ).show()
                    navController.navigate(AppRoutes.HOME) {
                        popUpTo(navController.graph.id) {
                            inclusive = true
                        }
                    }
                    viewModel.resetState()
                }
            }
            SignInScreen(state = state, onSignInClick = {
                lifecycleOwner.lifecycleScope.launch {  
                    val signInIntentSender = googleAuthUiClient.signIn()
                    launcher.launch(
                        IntentSenderRequest.Builder(
                            signInIntentSender ?: return@launch
                        ).build()
                    )
                }
            })
        }
        composable(AppRoutes.HOME) {
            HomeScreen(navController = navController)  
        }

        composable(
            route = AppRoutes.GROUP_DETAILS,  
            arguments = listOf(navArgument(AppRoutes.GROUP_DETAILS_ARG_ID) {  
                type = NavType.StringType
                 
            })
        ) { backStackEntry ->
            val groupId = backStackEntry.arguments?.getString(AppRoutes.GROUP_DETAILS_ARG_ID)
            val groupJoinKey = backStackEntry.arguments?.getString(AppRoutes.GROUP_CODE_ARG_ID)
            if (groupId != null) {
                GroupDetailsScreen(
                    navController = navController,  
                    groupId = groupId,
                    groupJoinKey = groupJoinKey
                )
            } else {
                 
                 
                Text("Error: Group ID missing")
                Log.e("AppNavigation", "Group ID is null for GroupDetailsScreen")
            }
        }

        composable(
            route = AppRoutes.ADD_EXPENSE,
            arguments = listOf(navArgument(AppRoutes.GROUP_DETAILS_ARG_ID) {  
                type = NavType.StringType
            })
        ) { backStackEntry ->
             
             
             
            AddExpenseScreen(
                navController = navController  
            )
        }

        composable(
            route = AppRoutes.EXPENSE_DETAILS,
            arguments = listOf(
                navArgument(AppRoutes.GROUP_DETAILS_ARG_ID) {
                    type = NavType.StringType
                     
                },
                navArgument(AppRoutes.EXPENSE_DETAILS_ARG_ID) {
                    type = NavType.StringType
                     
                }
            )
        ) {
            ExpenseDetailsScreen(
                onNavigateBack = { navController.popBackStack() }
                 
            )
        }

        composable(
            route = AppRoutes.SHOW_BALANCES,
            arguments = listOf(
                navArgument(AppRoutes.GROUP_DETAILS_ARG_ID) {
                    type = NavType.StringType
                     
                },
            )
        ) {
            ShowBalancesScreen (
                onNavigateBack = { navController.popBackStack() }

            )
        }
         
         
         
    }
}
