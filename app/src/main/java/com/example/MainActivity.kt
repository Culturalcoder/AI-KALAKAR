package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.ui.screens.DashboardScreen
import com.example.ui.screens.ProductDetailScreen
import com.example.ui.screens.WalkthroughScreen
import com.example.ui.screens.WizardScreen
import com.example.ui.theme.AIKalakarTheme
import com.example.ui.theme.NaturalLinen
import com.example.ui.viewmodel.ArtisanViewModel
import com.example.ui.viewmodel.WizardStep

object Routes {
    const val ROUTE_DASHBOARD = "dashboard"
    const val ROUTE_WIZARD = "wizard"
    const val ROUTE_WALKTHROUGH = "walkthrough"
    const val ROUTE_DETAIL = "detail/{productId}"
    fun detailRoute(productId: Long) = "detail/$productId"
}

class MainActivity : ComponentActivity() {
    private val viewModel: ArtisanViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            AIKalakarTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = NaturalLinen
                ) {
                    AIKalakarApp(viewModel = viewModel)
                }
            }
        }
    }
}

@Composable
fun AIKalakarApp(viewModel: ArtisanViewModel) {
    val navController = rememberNavController()

    NavHost(
        navController = navController,
        startDestination = Routes.ROUTE_DASHBOARD
    ) {
        composable(Routes.ROUTE_DASHBOARD) {
            DashboardScreen(
                viewModel = viewModel,
                onNavigateToCreate = {
                    viewModel.resetWizard()
                    navController.navigate(Routes.ROUTE_WIZARD)
                },
                onProductClick = { product ->
                    navController.navigate(Routes.detailRoute(product.id))
                },
                onOpenWalkthrough = {
                    navController.navigate(Routes.ROUTE_WALKTHROUGH)
                }
            )
        }

        composable(Routes.ROUTE_WIZARD) {
            WizardScreen(
                viewModel = viewModel,
                onNavigateBack = {
                    navController.popBackStack()
                },
                onProductPublished = { productId ->
                    navController.navigate(Routes.detailRoute(productId)) {
                        popUpTo(Routes.ROUTE_DASHBOARD) { inclusive = false }
                    }
                }
            )
        }

        composable(
            route = Routes.ROUTE_DETAIL,
            arguments = listOf(navArgument("productId") { type = NavType.LongType })
        ) { backStackEntry ->
            val productId = backStackEntry.arguments?.getLong("productId") ?: 0L
            ProductDetailScreen(
                productId = productId,
                viewModel = viewModel,
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }

        composable(Routes.ROUTE_WALKTHROUGH) {
            WalkthroughScreen(
                viewModel = viewModel,
                onDismiss = {
                    navController.popBackStack()
                }
            )
        }
    }
}

