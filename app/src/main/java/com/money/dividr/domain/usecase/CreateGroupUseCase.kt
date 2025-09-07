package com.money.dividr.domain.usecase

import com.money.dividr.domain.repository.GroupRepository
import com.money.dividr.domain.model.UserData
import javax.inject.Inject
import com.money.dividr.data.repository.Result
class CreateGroupUseCase @Inject constructor(
    private val groupRepository: GroupRepository
) {
    suspend operator fun invoke(groupName: String, creator: UserData): Result<String> {
        if (groupName.isBlank()) {
            return Result.Error(IllegalArgumentException("Group name cannot be blank."))
        }
        return groupRepository.createGroup(groupName, creator)
    }
}
