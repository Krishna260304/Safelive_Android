package com.safelive.app.domain.usecase.incident

import com.safelive.app.domain.model.DraftIncident
import com.safelive.app.domain.repository.IncidentRepository
import com.safelive.app.utils.Resource
import javax.inject.Inject

class GetIncidentsUseCase @Inject constructor(
    private val repository: IncidentRepository
) {
    operator fun invoke(
        page: Int = 1,
        status: String? = null,
        category: String? = null,
        priority: String? = null,
        search: String? = null
    ) = repository.getIncidents(page, status, category, priority, search)
}

class GetIncidentDetailUseCase @Inject constructor(
    private val repository: IncidentRepository
) {
    suspend operator fun invoke(id: String) = repository.getIncidentById(id)
}

class GetIncidentLogbookUseCase @Inject constructor(
    private val repository: IncidentRepository
) {
    suspend operator fun invoke(id: String) = repository.getIncidentLogbook(id)
}

class CreateIncidentUseCase @Inject constructor(
    private val repository: IncidentRepository
) {
    suspend operator fun invoke(
        title: String,
        description: String,
        category: String,
        priority: String,
        latitude: Double?,
        longitude: Double?,
        location: String?,
        pincode: String?,
        imagePaths: List<String>
    ): Resource<*> {
        if (title.trim().length < 5) return Resource.Error("Title must be at least 5 characters")
        if (description.trim().length < 20) return Resource.Error("Description must be at least 20 characters")
        if (category.isBlank()) return Resource.Error("Please select a category")
        if (priority.isBlank()) return Resource.Error("Please select priority")
        if (pincode.isNullOrBlank()) return Resource.Error("Please enter pincode")

        return repository.createIncident(title, description, category, priority, latitude, longitude, location, pincode, imagePaths)
    }
}

class UpdateIncidentStatusUseCase @Inject constructor(
    private val repository: IncidentRepository
) {
    suspend operator fun invoke(id: String, status: String, note: String?) =
        repository.updateIncidentStatus(id, status, note)
}

class GetDashboardStatsUseCase @Inject constructor(
    private val repository: IncidentRepository
) {
    suspend operator fun invoke() = repository.getDashboardStats()
}

class GetNearbyIncidentsUseCase @Inject constructor(
    private val repository: IncidentRepository
) {
    suspend operator fun invoke(latitude: Double, longitude: Double) =
        repository.getNearbyIncidents(latitude, longitude)
}

class SaveDraftUseCase @Inject constructor(
    private val repository: IncidentRepository
) {
    suspend operator fun invoke(draft: DraftIncident) = repository.saveDraft(draft)
}

class GetDraftsUseCase @Inject constructor(
    private val repository: IncidentRepository
) {
    operator fun invoke() = repository.getDrafts()
}

class DeleteDraftUseCase @Inject constructor(
    private val repository: IncidentRepository
) {
    suspend operator fun invoke(id: Int) = repository.deleteDraft(id)
}
