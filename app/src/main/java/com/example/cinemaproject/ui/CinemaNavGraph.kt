import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.example.cinemaproject.ui.theme.CinemaProjectTheme
import com.example.cinemaproject.ui.RegisterScreen
import com.example.cinemaproject.ui.LoginScreen
import com.example.cinemaproject.ui.SessionsListScreen
import com.example.cinemaproject.ui.SessionDetailsScreen
import com.example.cinemaproject.ui.FilmDetailsScreen
import com.example.cinemaproject.ui.AddReviewScreen
import java.net.URLEncoder

@Composable
fun CinemaNavGraph(
    navController: NavHostController,
    tokenStorage: TokenStorage,
    startDestination: String
) {
    NavHost(
        navController = navController,
        startDestination = startDestination
    ) {
        authGraph(tokenStorage, navController)
        sessionsGraph(tokenStorage, navController)
        sessionDetailsGraph(tokenStorage, navController)
        filmDetailsGraph(tokenStorage, navController)
        addReviewGraph(tokenStorage, navController)
    }
}

private fun NavGraphBuilder.authGraph(
    tokenStorage: TokenStorage,
    navController: NavHostController
) {
    composable("auth") {
        var showLogin by remember { mutableStateOf(true) }
        
        if (showLogin) {
            LoginScreen(
                tokenStorage = tokenStorage,
                onLoggedIn = {
                    navController.navigate("sessions") {
                        popUpTo("auth") { inclusive = true }
                    }
                },
                onNavigateToRegister = { showLogin = false }
            )
        } else {
            RegisterScreen(
                tokenStorage = tokenStorage,
                onRegistered = {
                    navController.navigate("sessions") {
                        popUpTo("auth") { inclusive = true }
                    }
                },
                onNavigateToLogin = { showLogin = true }
            )
        }
    }
}

private fun NavGraphBuilder.sessionsGraph(
    tokenStorage: TokenStorage,
    navController: NavHostController
) {
    composable("sessions") {
        SessionsListScreen(
            tokenStorage = tokenStorage,
            onOpenDetails = { sessionId, hallId ->
                navController.navigate("details/$sessionId/$hallId")
            },
            onOpenFilm = { filmId, title, imageUrl ->
                val encodedTitle = URLEncoder.encode(title, Charsets.UTF_8.name())
                val encodedImage = URLEncoder.encode(imageUrl, Charsets.UTF_8.name())
                navController.navigate("film/$filmId?title=$encodedTitle&image=$encodedImage")
            }
        )
    }
}

private fun NavGraphBuilder.sessionDetailsGraph(
    tokenStorage: TokenStorage,
    navController: NavHostController
) {
    composable(
        route = "details/{sessionId}/{hallId}",
        arguments = listOf(
            navArgument("sessionId") { type = NavType.StringType },
            navArgument("hallId") { type = NavType.StringType }
        )
    ) { backStackEntry ->
        val sessionId = backStackEntry.arguments?.getString("sessionId") ?: return@composable
        val hallId = backStackEntry.arguments?.getString("hallId") ?: return@composable
        
        SessionDetailsScreen(
            tokenStorage = tokenStorage,
            sessionId = sessionId,
            hallId = hallId,
            onBack = { navController.popBackStack() }
        )
    }
}

private fun NavGraphBuilder.filmDetailsGraph(
    tokenStorage: TokenStorage,
    navController: NavHostController
) {
    composable(
        route = "film/{filmId}?title={title}&image={image}",
        arguments = listOf(
            navArgument("filmId") { type = NavType.StringType },
            navArgument("title") {
                type = NavType.StringType
                defaultValue = ""
                nullable = true
            },
            navArgument("image") {
                type = NavType.StringType
                defaultValue = ""
                nullable = true
            }
        )
    ) { backStackEntry ->
        val filmId = backStackEntry.arguments?.getString("filmId") ?: return@composable
        val title = backStackEntry.arguments?.getString("title") ?: filmId
        val image = backStackEntry.arguments?.getString("image") ?: ""
        
        FilmDetailsScreen(
            filmId = filmId,
            title = title,
            imageUrl = image,
            onAddReview = { fId, fTitle ->
                val encodedTitle = URLEncoder.encode(fTitle, Charsets.UTF_8.name())
                navController.navigate("addReview/$fId?title=$encodedTitle")
            }
        )
    }
}

private fun NavGraphBuilder.addReviewGraph(
    tokenStorage: TokenStorage,
    navController: NavHostController
) {
    composable(
        route = "addReview/{filmId}?title={title}",
        arguments = listOf(
            navArgument("filmId") { type = NavType.StringType },
            navArgument("title") {
                type = NavType.StringType
                defaultValue = ""
                nullable = true
            }
        )
    ) { backStackEntry ->
        val filmId = backStackEntry.arguments?.getString("filmId") ?: return@composable
        val title = backStackEntry.arguments?.getString("title") ?: filmId
        
        AddReviewScreen(
            filmId = filmId,
            title = title,
            onBack = { navController.popBackStack() }
        )
    }
}