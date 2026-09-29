package pe.mitaxi.app.ui.statistics

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import java.time.format.DateTimeFormatter
import pe.mitaxi.app.R
import pe.mitaxi.app.data.repository.PeriodSummaryRepository
import pe.mitaxi.app.domain.model.SummaryPeriod
import pe.mitaxi.app.ui.home.ProfitCard
import pe.mitaxi.app.utils.formatSoles

@Composable
fun PeriodSummaryScreen(repository: PeriodSummaryRepository) {
    val model: PeriodSummaryViewModel = viewModel(factory = PeriodSummaryViewModel.factory(repository))
    val state by model.state.collectAsStateWithLifecycle()
    LifecycleResumeEffect(model) { model.refresh(); onPauseOrDispose { model.stop() } }
    Column(Modifier.verticalScroll(rememberScrollState()).padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(stringResource(R.string.period_summary), style = MaterialTheme.typography.headlineMedium)
        SummaryPeriod.entries.chunked(2).forEach { periods ->
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                periods.forEach { period ->
                    FilterChip(selected = state.period == period, onClick = { model.select(period) },
                        modifier = Modifier.weight(1f).heightIn(min = 56.dp).testTag("period${period.name}"),
                        label = { Text(stringResource(when (period) {
                            SummaryPeriod.TODAY -> R.string.period_today
                            SummaryPeriod.WEEK -> R.string.period_week
                            SummaryPeriod.MONTH -> R.string.period_month
                            SummaryPeriod.ALL -> R.string.period_all
                        })) })
                }
            }
        }
        when {
            state.loading -> Text(stringResource(R.string.summary_loading))
            state.error -> {
                Text(stringResource(R.string.summary_error))
                TextButton(onClick = model::refresh) { Text(stringResource(R.string.retry)) }
            }
            else -> state.summary?.let { summary ->
                val format = DateTimeFormatter.ofPattern("dd/MM/yyyy")
                Text(summary.window?.let { stringResource(R.string.period_dates, it.first.format(format), it.until.minusDays(1).format(format)) }
                    ?: stringResource(R.string.period_all_dates), Modifier.testTag("periodDates"))
                ProfitCard(summary.profit, stringResource(R.string.period_profit), "periodProfit")
                Card(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        Metric(stringResource(R.string.today_services), summary.count.toString(), "periodCount")
                        Metric(stringResource(R.string.today_income), formatSoles(summary.income), "periodIncome")
                        Metric(stringResource(R.string.today_expenses), formatSoles(summary.expenses), "periodExpenses")
                    }
                }
                Text(stringResource(R.string.profit_formula), style = MaterialTheme.typography.bodyMedium)
            }
        }
    }
}

@Composable
private fun Metric(label: String, value: String, tag: String) {
    Row(Modifier.fillMaxWidth()) {
        Text(label, Modifier.weight(1f))
        Text(value, Modifier.testTag(tag), style = MaterialTheme.typography.titleMedium)
    }
}
