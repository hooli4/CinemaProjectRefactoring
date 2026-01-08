import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.navigation.compose.rememberNavController

@Composable
fun CinemaApp(
    tokenStorage: TokenStorage
) {
    val navController = rememberNavController()
    val startDestination = remember(tokenStorage) {
        if (tokenStorage.getAccessToken() != null) "sessions" else "auth"
    }
    
    CinemaProjectTheme {
        Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
            Box(modifier = Modifier.padding(innerPadding)) {
                CinemaNavGraph(
                    navController = navController,
                    tokenStorage = tokenStorage,
                    startDestination = startDestination
                )
            }
        }
    }
}