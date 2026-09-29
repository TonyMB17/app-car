package pe.mitaxi.app.widget

import android.content.Context
import android.util.Log
import android.widget.Toast
import java.math.BigDecimal
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import pe.mitaxi.app.R
import pe.mitaxi.app.utils.formatSoles

object WidgetFeedback {
    private var current: Toast? = null

    suspend fun saved(context: Context, amountCents: Long) = withContext(Dispatchers.Main) {
        // Replace the previous notice so repeating an amount gives fresh feedback.
        try {
            current?.cancel()
            current = Toast.makeText(context.applicationContext,
                context.getString(R.string.widget_save_confirmation, formatSoles(BigDecimal.valueOf(amountCents, 2))),
                Toast.LENGTH_SHORT).also { it.show() }
        } catch (error: RuntimeException) {
            // A notification failure must not report a committed service as failed.
            Log.e("WidgetFeedback", "Unable to show saved notice", error)
        }
    }
}
