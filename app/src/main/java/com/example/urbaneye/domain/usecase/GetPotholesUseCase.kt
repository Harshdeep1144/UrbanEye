package com.example.urbaneye.domain.usecase

import com.example.urbaneye.domain.model.Pothole
import com.example.urbaneye.domain.repository.PotholeRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetPotholesUseCase @Inject constructor(
    private val repository: PotholeRepository
) {
    operator fun invoke(): Flow<List<Pothole>> {
        return repository.getPotholes()
    }
}