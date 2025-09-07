package com.money.dividr.presentation.home

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.BottomAppBar
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemColors
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost  
import androidx.navigation.compose.composable  
import androidx.navigation.compose.currentBackStackEntryAsState  
import androidx.navigation.compose.rememberNavController
import com.money.dividr.R  
import com.money.dividr.domain.model.Group
import com.money.dividr.navigation.AppRoutes
import com.money.dividr.presentation.components.JoinGroupDialog
import com.money.dividr.presentation.components.CreateGroupDialog
import com.money.dividr.presentation.components.GroupCard
import com.money.dividr.presentation.profile.ProfileScreen  
import com.money.dividr.presentation.utils.roundToTwoDecimals

 
object BottomNavRoutes {
    const val HOME_CONTENT = "home_content"
    const val PROFILE_CONTENT = "profile_content"
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    navController: NavController,  
    homeViewModel: HomeViewModel = hiltViewModel()
) {
    val bottomBarNavController = rememberNavController()  
    var showCreateGroupDialog by remember { mutableStateOf(false) }
    var showJoinGroupDialog by remember { mutableStateOf(false) }
    val groupCreationState by homeViewModel.groupCreationStatus.collectAsStateWithLifecycle()
    val joinGroupState by homeViewModel.joinGroupStatus.collectAsStateWithLifecycle()
    val groupsState by homeViewModel.groupsState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val navBackStackEntry by bottomBarNavController.currentBackStackEntryAsState()
    val currentBottomNavRoute = navBackStackEntry?.destination?.route
    Scaffold(
        topBar = {

            TopAppBar(
                title = {
                    Text(
                        text = "Dividr",  
                        fontFamily = FontFamily.Cursive,
                        style = MaterialTheme.typography.headlineLarge
                    )
                },
                actions = {
                    if(currentBottomNavRoute == BottomNavRoutes.HOME_CONTENT){
                        TextButton(onClick = { showCreateGroupDialog = true }) {
                            Text("Create Group", fontSize = 16.sp)
                        }
                        TextButton(onClick = { showJoinGroupDialog = true }) {
                            Text("Join Group", fontSize = 16.sp)
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                    actionIconContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            )
        },
        bottomBar = {
            AppBottomNavigationBar(navController = bottomBarNavController)
        }
    ) { innerPadding ->
        NavHost(
            navController = bottomBarNavController,
            startDestination = BottomNavRoutes.HOME_CONTENT,
            modifier = Modifier.padding(innerPadding)  
        ) {
            composable(BottomNavRoutes.HOME_CONTENT) {
                GroupListContent(groupsState = groupsState,
                    onGroupClick = { groupId, groupJoinKey ->
                        navController.navigate(AppRoutes.groupDetailsRoute(groupId,groupJoinKey))
                    },
                    currUid = homeViewModel.auth.currentUser?.uid,
                )
            }
            composable(BottomNavRoutes.PROFILE_CONTENT) {
                ProfileScreen(navController = navController)  
            }
        }
    }

    if (showCreateGroupDialog) {
        CreateGroupDialog(
            onDismissRequest = {
                showCreateGroupDialog = false
                homeViewModel.resetGroupCreationStatus()
            },
            onCancel = {
                showCreateGroupDialog = false
                homeViewModel.resetGroupCreationStatus()
            },
            onCreate = { groupName ->
                homeViewModel.createGroup(groupName)
            }
        )
    }

    if (showJoinGroupDialog) {
        JoinGroupDialog(
            showDialog = showJoinGroupDialog,
            onDismissRequest = {
                if (joinGroupState !is JoinGroupUiState.Loading) {  
                    showJoinGroupDialog = false
                }
            },
            onJoinGroupClick = { groupCode ->
                if (groupCode.isNotBlank()) {
                    homeViewModel.joinGroup(groupCode)
                } else {
                    Toast.makeText(context, "Please enter a group code.", Toast.LENGTH_SHORT).show()
                }
            },
        )
    }

    LaunchedEffect(groupCreationState) {
        when (val state = groupCreationState) {
            is GroupCreationUiState.Success -> {
                Toast.makeText(context, "Group created: ${state.groupId}", Toast.LENGTH_SHORT).show()
                showCreateGroupDialog = false
                homeViewModel.resetGroupCreationStatus()
            }
            is GroupCreationUiState.Error -> {
                Toast.makeText(context, "Error: ${state.message}", Toast.LENGTH_LONG).show()
                homeViewModel.resetGroupCreationStatus()
            }
            is GroupCreationUiState.Loading -> {
                Toast.makeText(context, "Creating group...", Toast.LENGTH_SHORT).show()
            }
            is GroupCreationUiState.Idle -> { /* Do nothing */ }
        }
    }
    LaunchedEffect(joinGroupState) {
        when (val state = joinGroupState) {
            is JoinGroupUiState.Success -> {
                Toast.makeText(context, "Successfully joined group!", Toast.LENGTH_SHORT).show()
                showJoinGroupDialog = false
                homeViewModel.resetJoinGroupStatus()
            }
            is JoinGroupUiState.Error -> {
                Toast.makeText(context, "Error: ${state.message}", Toast.LENGTH_LONG).show()
                homeViewModel.resetJoinGroupStatus()
            }
            JoinGroupUiState.Loading -> { /* Handled by isLoading prop in Dialog or a global indicator */ }
            JoinGroupUiState.Idle -> { /* Do nothing */ }
        }
    }
}

@Composable
fun AppBottomNavigationBar(navController: NavHostController) {
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    BottomAppBar(
        containerColor = MaterialTheme.colorScheme.primaryContainer
    ) {
        BottomNavItem.values().forEach { item ->
            NavigationBarItem(
                selected = currentRoute == item.route,
                onClick = {
                    navController.navigate(item.route) {
                         
                         
                         
                        popUpTo(navController.graph.startDestinationId) {
                            saveState = true
                        }
                         
                         
                        launchSingleTop = true
                         
                        restoreState = true
                    }
                },
                icon = { Icon(item.icon, contentDescription = item.title) },
                label = { Text(item.title, fontSize = 16.sp) },
                colors = NavigationBarItemColors(
                    selectedIndicatorColor = MaterialTheme.colorScheme.primaryContainer,
                    selectedIconColor = MaterialTheme.colorScheme.onPrimaryContainer,
                    selectedTextColor = MaterialTheme.colorScheme.onPrimaryContainer,
                    unselectedIconColor = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.5f),
                    unselectedTextColor = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.5f),
                    disabledIconColor = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.0f),
                    disabledTextColor = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.0f),
                )
            )
        }
    }
}


@Composable
fun GroupListContent(
    modifier: Modifier = Modifier,
    onGroupClick: (groupId: String, groupJoinKey: String) -> Unit,
    groupsState: GroupsUiState,
    currUid: String?
) {
    Box(
        modifier = modifier  
            .fillMaxSize()
            .padding(16.dp),
        contentAlignment = Alignment.Center
    ) {
        when (groupsState) {
            is GroupsUiState.Loading -> {
                CircularProgressIndicator()
            }
            is GroupsUiState.Success -> {
                if (groupsState.groups.isEmpty()) {
                     Text("No groups found. Create one!", style = MaterialTheme.typography.bodyLarge, modifier = Modifier.align(Alignment.Center))
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        contentPadding = PaddingValues(vertical = 8.dp)
                    ) {
                        items(groupsState.groups, key = { it.groupId }) { group ->
                            val owedMap=group.balances[currUid]
                            val amountOwed=owedMap?.values?.sum()?.roundToTwoDecimals()
                            GroupCard(
                                groupName = group.groupName,
                                logo = R.drawable.group_icon, 
                                amountOwed = "$ $amountOwed",  
                                onClick = {
                                    onGroupClick(group.groupId,group.groupJoinKey)
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(color = Color.Transparent)
                            )
                        }
                    }
                }
            }
            is GroupsUiState.Error -> {
                Text("Error: ${groupsState.message}", style = MaterialTheme.typography.bodyLarge)
            }
            is GroupsUiState.Empty -> {
                Text("No groups found. Create one!", style = MaterialTheme.typography.bodyLarge)
            }
            is GroupsUiState.Idle -> {
                 Text("Fetching groups...", style = MaterialTheme.typography.bodyLarge)
            }
        }
    }
}

 
enum class BottomNavItem(val title: String, val icon: ImageVector, val route: String) {
    Home("Home", Icons.Filled.Home, BottomNavRoutes.HOME_CONTENT),
    Profile("Profile", Icons.Filled.Person, BottomNavRoutes.PROFILE_CONTENT)
}

@Preview(showBackground = true)
@Composable
fun HomeScreenPreview() {
    MaterialTheme {
        HomeScreen(
            navController = rememberNavController()
        )
    }
}

