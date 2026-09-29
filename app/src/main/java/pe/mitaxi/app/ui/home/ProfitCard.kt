package pe.mitaxi.app.ui.home

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import java.math.BigDecimal
import pe.mitaxi.app.R
import pe.mitaxi.app.utils.formatSoles

@Composable
fun ProfitCard(profit: BigDecimal, title: String, tag: String) {
    val sign = profit.signum()
    val background = when { sign > 0 -> Color(0xFFD8F3DC); sign < 0 -> Color(0xFFFFDAD6); else -> Color(0xFFFFE9A8) }
    val foreground = when { sign > 0 -> Color(0xFF14532D); sign < 0 -> Color(0xFF8C1D18); else -> Color(0xFF594400) }
    Card(Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = background, contentColor = foreground)) {
        Column(Modifier.padding(12.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium)
            Text(formatSoles(profit), Modifier.testTag(tag), style = MaterialTheme.typography.headlineLarge)
            Text(stringResource(when { sign > 0 -> R.string.profit_positive; sign < 0 -> R.string.profit_negative; else -> R.string.profit_even }),
                Modifier.testTag("profitStatus"), style = MaterialTheme.typography.labelLarge)
        }
    }
}
