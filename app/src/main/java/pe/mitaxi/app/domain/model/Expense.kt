package pe.mitaxi.app.domain.model

enum class ExpenseCategory { FUEL, MAINTENANCE, REPAIR, WASH, OTHER }

data class Expense(val id: Long, val occurredAt: Long, val amountCents: Long, val category: ExpenseCategory)
