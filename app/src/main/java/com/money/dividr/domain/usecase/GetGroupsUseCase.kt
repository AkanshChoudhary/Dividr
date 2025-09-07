package com.money.dividr.domain.usecase

import com.money.dividr.domain.model.Group
import com.money.dividr.domain.repository.GroupRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetGroupsUseCase @Inject constructor(
    private val groupRepository: GroupRepository
) {
    operator fun invoke(): Flow<List<Group>> {
        return groupRepository.getUserGroups()
    }
}
