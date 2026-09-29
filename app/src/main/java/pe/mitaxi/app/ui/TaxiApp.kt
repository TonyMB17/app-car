package pe.mitaxi.app.ui

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import pe.mitaxi.app.R
import pe.mitaxi.app.data.repository.DailySummaryRepository
import pe.mitaxi.app.data.repository.ServiceRepository
import pe.mitaxi.app.ui.services.ServicesRoute
import pe.mitaxi.app.data.repository.ExpenseRepository
import pe.mitaxi.app.ui.expenses.ExpensesRoute

private enum class Destination(val route: String, @param:StringRes val label: Int, val symbol: String) {
    Home("home", R.string.home, "⌂"),
    Services("services", R.string.services, "↗"),
    Expenses("expenses", R.string.expenses, "−"),
    Statistics("statistics", R.string.statistics, "▥"),
    Settings("settings", R.string.settings, "⚙"),
}

@Composable
fun TaxiApp(serviceRepository: ServiceRepository, summaryRepository: DailySummaryRepository, expenseRepository: ExpenseRepository, periodRepository: pe.mitaxi.app.data.repository.PeriodSummaryRepository, fareRepository: pe.mitaxi.app.data.repository.FareRepository) {
    val navController = rememberNavController()
    val entry by navController.currentBackStackEntryAsState()
    Scaffold(bottomBar = {
        NavigationBar {
            Destination.entries.forEach { destination ->
                NavigationBarItem(
                    selected = entry?.destination?.route == destination.route,
                    onClick = {
                        navController.navigate(destination.route) {
                            popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                            launchSingleTop = true
                            restoreState = true
                        }
                    },
                    icon = { Text(destination.symbol) },
                    label = { Text(stringResource(destination.label), maxLines = 1) },
                )
            }
        }
    }) { padding ->
        NavHost(navController, startDestination = Destination.Home.route, modifier = Modifier.padding(padding)) {
            composable(Destination.Home.route) {
                ServicesRoute(serviceRepository, fareRepository, quickEntry = true, dailyRepository = summaryRepository)
            }
            composable(Destination.Services.route) { ServicesRoute(serviceRepository, fareRepository) }
            composable(Destination.Expenses.route) { ExpensesRoute(expenseRepository) }
            composable(Destination.Statistics.route) { pe.mitaxi.app.ui.statistics.PeriodSummaryScreen(periodRepository) }
            composable(Destination.Settings.route) { pe.mitaxi.app.ui.settings.FaresScreen(fareRepository) }
        }
    }
}

