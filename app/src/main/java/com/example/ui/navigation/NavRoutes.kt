package com.example.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.ui.graphics.vector.ImageVector

sealed class Screen(
    val route: String,
    val title: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector
) {
    data object Dashboard : Screen(
        route = "dashboard",
        title = "پیشخوان",
        selectedIcon = Icons.Filled.Dashboard,
        unselectedIcon = Icons.Outlined.Dashboard
    )

    data object Workouts : Screen(
        route = "workouts",
        title = "تمرینات",
        selectedIcon = Icons.Filled.FitnessCenter,
        unselectedIcon = Icons.Outlined.FitnessCenter
    )

    data object Nutrition : Screen(
        route = "nutrition",
        title = "تغذیه و آب",
        selectedIcon = Icons.Filled.Restaurant,
        unselectedIcon = Icons.Outlined.Restaurant
    )

    data object Analysis : Screen(
        route = "analysis",
        title = "آنالیز بدن",
        selectedIcon = Icons.Filled.Assessment,
        unselectedIcon = Icons.Outlined.Assessment
    )

    data object Profile : Screen(
        route = "profile",
        title = "پروفایل",
        selectedIcon = Icons.Filled.Person,
        unselectedIcon = Icons.Outlined.Person
    )

    companion object {
        val bottomNavItems: List<Screen>
            get() = listOf(
                Dashboard,
                Workouts,
                Nutrition,
                Analysis,
                Profile
            )
    }
}
