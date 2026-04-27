package com.example.android_calc.data

import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.tasks.await

class HistoryRepository {
    private val db = FirebaseFirestore.getInstance()
    private val historyCollection = db.collection("calculator_history")

    suspend fun saveCalculation(expression: String, result: String) {
        val item = HistoryItem(expression, result)
        try {
            historyCollection.add(item).await()
        } catch (e: Exception) {
        }
    }

    suspend fun getHistory(): List<HistoryItem> {
        return try {
            historyCollection
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .limit(20)
                .get()
                .await()
                .toObjects(HistoryItem::class.java)
        } catch (e: Exception) {
            emptyList()
        }
    }
}