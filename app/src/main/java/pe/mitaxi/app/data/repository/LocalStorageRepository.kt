package pe.mitaxi.app.data.repository

import pe.mitaxi.app.data.local.database.TaxiDatabase

class LocalStorageRepository(private val database: TaxiDatabase) {
    suspend fun verifyAccess() {
        // Opens and validates the real database, without creating sample data.
        database.serviceDao().count()
    }
}
