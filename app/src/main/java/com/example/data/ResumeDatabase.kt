package com.example.data

import android.content.Context

typealias ResumeDatabase = AppDatabase

class ResumeRepository(private val dao: ResumeDao) {
    val allDrafts = dao.getAllDrafts()

    suspend fun getLatestDraft(): ResumeDraft? = dao.getLatestDraft()

    suspend fun saveDraft(draft: ResumeDraft): Long = dao.upsertDraft(draft)

    suspend fun deleteDraft(id: Long) = dao.deleteDraftById(id)
}
