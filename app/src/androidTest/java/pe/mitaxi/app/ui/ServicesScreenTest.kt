package pe.mitaxi.app.ui

import android.content.Context
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.isEnabled
import java.time.Instant
import java.time.ZoneId
import pe.mitaxi.app.domain.model.DayWindow
import pe.mitaxi.app.domain.model.ExpenseCategory
import pe.mitaxi.app.domain.model.ServiceInput
import pe.mitaxi.app.data.local.entity.ServiceEntity
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.performTextReplacement
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.doubleClick
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import pe.mitaxi.app.data.local.database.TaxiDatabase
import pe.mitaxi.app.data.repository.DailySummaryRepository
import pe.mitaxi.app.data.repository.ServiceRepository
import pe.mitaxi.app.data.repository.ExpenseRepository
import pe.mitaxi.app.ui.theme.TaxiTheme

@RunWith(AndroidJUnit4::class)
class ServicesScreenTest {
    @get:Rule val compose = createComposeRule()
    private lateinit var database: TaxiDatabase

    @Before fun setup() {
        database = Room.inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext<Context>(), TaxiDatabase::class.java).addCallback(TaxiDatabase.INITIAL_FARES).build()
        compose.setContent {
            TaxiTheme { TaxiApp(ServiceRepository(database.serviceDao()), DailySummaryRepository(database.serviceDao(), database.expenseDao()), ExpenseRepository(database.expenseDao()), pe.mitaxi.app.data.repository.PeriodSummaryRepository(database.serviceDao(), database.expenseDao()), pe.mitaxi.app.data.repository.FareRepository(database)) }
        }
        compose.waitUntil(10_000) { compose.onAllNodes(hasTestTag("quickFare500")).fetchSemanticsNodes().isNotEmpty() }
    }

    @After fun close() { database.close() }

    private fun waitForCount(count: Long) {
        compose.waitUntil(10_000) { runBlocking { database.serviceDao().count() == count } }
        compose.waitForIdle()
    }

    @Test fun registerValidateEditAndConfirmDeletion() {
        compose.onNodeWithText("Servicios").performClick()
        compose.onNodeWithText("+ Registrar").performClick()
        compose.onNodeWithTag("serviceAmount").performTextInput("0")
        compose.onNodeWithTag("saveService").performClick()
        compose.onNodeWithText("Ingresa un monto mayor que cero, con hasta dos decimales y sin separadores de miles.").assertIsDisplayed()
        assertEquals(0L, runBlocking { database.serviceDao().count() })
        compose.onNodeWithTag("serviceAmount").performTextReplacement("12,50")
        compose.onNodeWithTag("serviceNote").assertDoesNotExist()
        compose.onNodeWithTag("saveService").performClick()
        waitForCount(1)
        compose.onNodeWithText("Editar").performClick()
        compose.onNodeWithTag("serviceAmount").performTextReplacement("15.75")
        compose.onNodeWithText("Más opciones").performClick()
        compose.onNodeWithTag("serviceNote").performTextReplacement("Servicio editado")
        compose.onNodeWithTag("serviceDate").performScrollTo().performTextReplacement("28/09/2026")
        compose.onNodeWithTag("serviceTime").performScrollTo().performTextReplacement("08:15")
        compose.onNodeWithTag("saveService").performClick()
        compose.waitUntil(10_000) {
            runBlocking { database.serviceDao().observeAll().first().single().amountCents == 1575L }
        }
        compose.waitForIdle()
        compose.onNodeWithText("Servicio editado").assertIsDisplayed()
        compose.onNodeWithText("28/09/2026 · 08:15").assertIsDisplayed()
        compose.onNodeWithText("Eliminar").performClick()
        compose.onNodeWithText("¿Eliminar este servicio?").assertIsDisplayed()
        compose.onNodeWithText("Cancelar").performClick()
        assertEquals(1L, runBlocking { database.serviceDao().count() })
        compose.onNodeWithText("Servicio editado").assertIsDisplayed()
        compose.onNodeWithText("Eliminar").performClick()
        compose.onNodeWithTag("confirmDelete").performClick()
        waitForCount(0)
        compose.onNodeWithText("Servicio editado").assertDoesNotExist()
        compose.onNodeWithText("Aún no hay registros").assertIsDisplayed()
    }

    @Test fun quickFareNeedsOneTapAndUndoPreservesPreviousServices() {
        val previousId = runBlocking { ServiceRepository(database.serviceDao()).add(300, "Anterior") }
        compose.onNodeWithTag("quickFare600").performClick()
        waitForCount(2)
        compose.onNodeWithTag("serviceAmount").assertDoesNotExist()
        val rows = runBlocking { database.serviceDao().observeAll().first() }
        assertEquals(600L, rows.first().amountCents)
        org.junit.Assert.assertNull(rows.first().note)
        compose.onNodeWithText("Deshacer").performClick()
        waitForCount(1)
        assertEquals(previousId, runBlocking { database.serviceDao().observeAll().first().single().id })
    }

    @Test fun quickDoubleTapDoesNotCreateTwoServices() {
        compose.onNodeWithTag("quickFare500").performTouchInput { doubleClick() }
        waitForCount(1)
        compose.onNodeWithText("S/ 5.00 registrado").assertIsDisplayed()
        assertEquals(1L, runBlocking { database.serviceDao().count() })
    }

    @Test fun otherAmountFromHomeOnlyRequestsAmount() {
        compose.onNodeWithText("Otro monto").performClick()
        compose.onNodeWithTag("serviceNote").assertDoesNotExist()
        compose.onNodeWithTag("serviceDate").assertDoesNotExist()
        compose.onNodeWithTag("serviceAmount").performTextInput("8.50")
        compose.onNodeWithTag("saveService").performClick()
        waitForCount(1)
        assertEquals(850L, runBlocking { database.serviceDao().observeAll().first().single().amountCents })
    }

    @Test fun dailySummaryReactsToServiceFuelAndUndoWithoutIncludingYesterday() {
        val day = DayWindow.at(Instant.now(), ZoneId.systemDefault())
        runBlocking {
            database.serviceDao().insert(ServiceEntity(occurredAt = day.start - 1, amountCents = 9999, createdAt = day.start - 1, updatedAt = day.start - 1))
        }
        fun waitValue(tag: String, value: String) {
            compose.waitUntil(10_000) { compose.onAllNodes(hasTestTag(tag) and hasText(value)).fetchSemanticsNodes().isNotEmpty() }
        }
        waitValue("todayServiceCount", "0")
        compose.onNodeWithTag("quickFare700").performClick()
        waitValue("todayServiceCount", "1")
        waitValue("todayIncome", "S/ 7.00")
        compose.onNodeWithTag("addFuel").performScrollTo().performClick()
        compose.onNodeWithTag("fuelAmount").performTextInput("30,50")
        compose.onNodeWithTag("saveFuel").performClick()
        waitValue("todayFuel", "S/ 30.50")
        compose.onNodeWithTag("todayIncome").assertTextEquals("S/ 7.00")
        compose.onNodeWithTag("undoFuel").performScrollTo().performClick()
        waitValue("todayFuel", "S/ 0.00")
        compose.onNodeWithTag("todayServiceCount").assertTextEquals("1")
    }

    @Test fun expenseCategoryAmountEditAndConfirmedDelete() {
        compose.onNodeWithText("Gastos").performClick()
        compose.onNodeWithTag("addExpenseWASH").performClick()
        compose.onNodeWithText("Nota (opcional)").assertDoesNotExist()
        compose.onNodeWithTag("expenseDate").assertDoesNotExist()
        compose.onNodeWithTag("expenseAmount").performTextInput("0")
        compose.onNodeWithTag("saveExpense").performClick()
        compose.onNodeWithText("Ingresa un monto mayor que cero, con hasta dos decimales y sin separadores de miles.").assertIsDisplayed()
        compose.onNodeWithTag("expenseAmount").performTextReplacement("20,50")
        compose.onNodeWithTag("saveExpense").performClick()
        compose.waitUntil(10_000) { runBlocking { database.expenseDao().observeAll().first().size == 1 } }
        val id = runBlocking { database.expenseDao().observeAll().first().single().id }
        compose.onNodeWithTag("editExpense$id").performScrollTo().performClick()
        compose.onNodeWithTag("expenseAmount").performTextReplacement("25")
        compose.onNodeWithText("Más opciones").performClick()
        compose.onNodeWithTag("expenseCategoryFUEL").performScrollTo().performClick()
        compose.onNodeWithTag("expenseDate").performScrollTo().performTextReplacement("31/02/2026")
        compose.onNodeWithTag("saveExpense").performClick()
        compose.onNodeWithText("Revisa la fecha y hora. Usa dd/mm/aaaa y HH:mm (24 horas).").performScrollTo().assertIsDisplayed()
        compose.onNodeWithTag("expenseDate").performScrollTo().performTextReplacement(ServiceInput.date(Instant.now().toEpochMilli(), ZoneId.systemDefault()))
        compose.onNodeWithTag("saveExpense").performClick()
        compose.waitUntil(10_000) { runBlocking { database.expenseDao().observeAll().first().single().category == "FUEL" } }
        compose.onNodeWithText("Inicio").performClick()
        compose.waitUntil(10_000) { compose.onAllNodes(hasTestTag("todayFuel") and hasText("S/ 25.00")).fetchSemanticsNodes().isNotEmpty() }
        compose.onNodeWithText("Gastos").performClick()
        compose.onNodeWithTag("deleteExpense$id").performScrollTo().performClick()
        compose.onNodeWithText("Cancelar").performClick()
        assertEquals(1, runBlocking { database.expenseDao().observeAll().first().size })
        compose.onNodeWithTag("deleteExpense$id").performClick()
        compose.onNodeWithTag("confirmExpenseDelete").performClick()
        compose.waitUntil(10_000) { runBlocking { database.expenseDao().observeAll().first().isEmpty() } }
        compose.onNodeWithText("Inicio").performClick()
        compose.waitUntil(10_000) { compose.onAllNodes(hasTestTag("todayFuel") and hasText("S/ 0.00")).fetchSemanticsNodes().isNotEmpty() }
    }

    @Test fun previouslySavedFuelCanBeEditedFromExpenses() {
        val id = runBlocking { DailySummaryRepository(database.serviceDao(), database.expenseDao()).addFuel(3000) }
        compose.onNodeWithText("Gastos").performClick()
        compose.waitUntil(10_000) { compose.onAllNodes(hasTestTag("editExpense$id")).fetchSemanticsNodes().isNotEmpty() }
        compose.onNodeWithTag("editExpense$id").performScrollTo().performClick()
        compose.onNodeWithTag("expenseAmount").performTextReplacement("40.50")
        compose.onNodeWithTag("saveExpense").performClick()
        compose.waitUntil(10_000) { runBlocking { database.expenseDao().observeAll().first().single().amountCents == 4050L } }
        compose.onNodeWithText("Inicio").performClick()
        compose.waitUntil(10_000) { compose.onAllNodes(hasTestTag("todayFuel") and hasText("S/ 40.50")).fetchSemanticsNodes().isNotEmpty() }
        assertEquals(id, runBlocking { database.expenseDao().observeAll().first().single().id })
    }

    @Test fun configureFaresAndPreserveRegisteredAmounts() {
        compose.onNodeWithText("Ajustes").performClick()
        compose.onNodeWithTag("editFare1").performScrollTo().performClick()
        compose.onNodeWithTag("fareAmount").performTextReplacement("0")
        compose.onNodeWithTag("saveFare").performClick()
        compose.onNodeWithText("Ingresa un monto mayor que cero, con hasta dos decimales y sin separadores de miles.").assertIsDisplayed()
        compose.onNodeWithTag("fareAmount").performTextReplacement("6")
        compose.onNodeWithTag("saveFare").performClick()
        compose.onNodeWithText("Ya tienes una tarifa con ese importe.").assertIsDisplayed()
        compose.onNodeWithTag("fareAmount").performTextReplacement("8,50")
        compose.onNodeWithTag("saveFare").performClick()
        compose.waitUntil(10_000) { runBlocking { database.fareDao().find(1)?.amountCents == 850L } }
        compose.onNodeWithTag("downFare1").performScrollTo().performClick()
        compose.waitUntil(10_000) { runBlocking { database.fareDao().all()[1].id == 1L } }
        compose.onNodeWithTag("addFare").performScrollTo().performClick()
        compose.onNodeWithTag("fareAmount").performTextInput("12.50")
        compose.onNodeWithTag("saveFare").performClick()
        compose.waitUntil(10_000) { runBlocking { database.fareDao().all().size == 5 } }
        compose.onNodeWithText("Inicio").performClick()
        compose.waitUntil(10_000) { compose.onAllNodes(hasTestTag("quickFare850")).fetchSemanticsNodes().isNotEmpty() }
        compose.onNodeWithTag("quickFare850").assertTextEquals("S/ 8.50").performClick()
        waitForCount(1)
        compose.onNodeWithText("Ajustes").performClick()
        compose.onNodeWithTag("deleteFare1").performScrollTo().performClick()
        compose.onNodeWithText("Cancelar").performClick()
        assertEquals(5, runBlocking { database.fareDao().all().size })
        compose.onNodeWithTag("deleteFare1").performClick()
        compose.onNodeWithTag("confirmFareDelete").performClick()
        compose.waitUntil(10_000) { runBlocking { database.fareDao().find(1) == null } }
        compose.onNodeWithText("Inicio").performClick()
        compose.waitUntil(10_000) { compose.onAllNodes(hasTestTag("quickFare850")).fetchSemanticsNodes().isEmpty() }
        assertEquals(850L, runBlocking { database.serviceDao().observeAll().first().single().amountCents })
    }

    @Test fun summaryFiltersAndSelectionSurviveNavigation() {
        runBlocking {
            ServiceRepository(database.serviceDao()).add(2000, "")
            ExpenseRepository(database.expenseDao()).add(ExpenseCategory.WASH, 1000)
            database.serviceDao().insert(ServiceEntity(occurredAt = 0, amountCents = 500, createdAt = 0, updatedAt = 0))
        }
        fun waitValue(tag: String, value: String) {
            compose.waitUntil(10_000) { compose.onAllNodes(hasTestTag(tag) and hasText(value)).fetchSemanticsNodes().isNotEmpty() }
        }
        compose.onNodeWithText("Estadísticas").performClick()
        waitValue("periodCount", "1")
        waitValue("periodProfit", "S/ 10.00")
        for (period in listOf("WEEK", "MONTH")) {
            compose.onNodeWithTag("period$period").performClick()
            waitValue("periodCount", "1")
        }
        compose.onNodeWithTag("periodALL").performClick()
        waitValue("periodCount", "2")
        waitValue("periodProfit", "S/ 15.00")
        compose.onNodeWithText("Inicio").performClick()
        compose.onNodeWithText("Estadísticas").performClick()
        waitValue("periodDates", "Todo el historial")
        runBlocking { ExpenseRepository(database.expenseDao()).add(ExpenseCategory.OTHER, 2000) }
        waitValue("periodProfit", "S/ -5.00")
        compose.onNodeWithTag("periodTODAY").performClick()
        waitValue("periodProfit", "S/ -10.00")
    }

    @Test fun quickExpensesAndProfitStates() {
        fun waitValue(tag: String, value: String) {
            compose.waitUntil(10_000) { compose.onAllNodes(hasTestTag(tag) and hasText(value)).fetchSemanticsNodes().isNotEmpty() }
        }
        waitValue("profitStatus", "En equilibrio")
        runBlocking { ServiceRepository(database.serviceDao()).add(20000, "") }
        waitValue("profitStatus", "Con ganancia")
        waitValue("todayProfit", "S/ 200.00")
        compose.onNodeWithText("Gastos").performClick()
        compose.onNodeWithTag("quickExpenseFUEL5000").performTouchInput { doubleClick() }
        compose.waitUntil(10_000) { runBlocking { database.expenseDao().observeAll().first().size == 1 } }
        compose.onNodeWithTag("expenseAmount").assertDoesNotExist()
        compose.waitUntil(10_000) { compose.onAllNodes(hasTestTag("quickExpenseFUEL10000") and isEnabled()).fetchSemanticsNodes().isNotEmpty() }
        compose.onNodeWithTag("quickExpenseFUEL10000").performClick()
        compose.waitUntil(10_000) { runBlocking { database.expenseDao().observeAll().first().size == 2 } }
        compose.waitUntil(10_000) { compose.onAllNodes(hasTestTag("quickExpenseWASH1000") and isEnabled()).fetchSemanticsNodes().isNotEmpty() }
        compose.onNodeWithTag("quickExpenseWASH1000").performClick()
        compose.waitUntil(10_000) { runBlocking { database.expenseDao().observeAll().first().size == 3 } }
        compose.onNodeWithText("Inicio").performClick()
        waitValue("todayExpenses", "S/ 160.00")
        waitValue("todayFuel", "S/ 150.00")
        waitValue("todayProfit", "S/ 40.00")
        runBlocking { ExpenseRepository(database.expenseDao()).add(ExpenseCategory.REPAIR, 4000) }
        waitValue("profitStatus", "En equilibrio")
        runBlocking { ExpenseRepository(database.expenseDao()).add(ExpenseCategory.OTHER, 100) }
        waitValue("profitStatus", "En pérdida")
        waitValue("todayProfit", "S/ -1.00")
    }
}
