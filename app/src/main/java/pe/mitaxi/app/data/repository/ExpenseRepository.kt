package pe.mitaxi.app.data.repository

import java.time.Clock
import kotlinx.coroutines.flow.map
import pe.mitaxi.app.data.local.dao.ExpenseDao
import pe.mitaxi.app.data.local.entity.ExpenseEntity
import pe.mitaxi.app.domain.model.Expense
import pe.mitaxi.app.domain.model.ExpenseCategory

class ExpenseRepository(private val dao: ExpenseDao, private val clock: Clock = Clock.systemUTC()) {
    val expenses = dao.observeAll().map { rows -> rows.map {
        Expense(it.id, it.occurredAt, it.amountCents, ExpenseCategory.valueOf(it.category))
    } }

    suspend fun add(category: ExpenseCategory, amountCents: Long): Long {
        require(amountCents > 0)
        val now = clock.millis()
        return dao.insert(ExpenseEntity(occurredAt = now, category = category.name, amountCents = amountCents, createdAt = now, updatedAt = now))
    }

    suspend fun update(id: Long, category: ExpenseCategory, amountCents: Long, occurredAt: Long) {
        require(amountCents > 0)
        check(dao.update(id, category.name, amountCents, occurredAt, clock.millis()) == 1)
    }

    suspend fun delete(id: Long) { check(dao.delete(id) == 1) }
}
