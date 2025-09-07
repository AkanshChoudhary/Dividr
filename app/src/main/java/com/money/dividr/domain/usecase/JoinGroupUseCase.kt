package com.money.dividr.domain.usecase

import com.money.dividr.domain.model.UserData
import com.money.dividr.domain.repository.GroupRepository
import javax.inject.Inject
import com.money.dividr.data.repository.Result
class JoinGroupUseCase @Inject constructor(
    private val groupRepository: GroupRepository
) {
    suspend operator fun invoke(groupCode: String, joiningUser: UserData): Result<String> {
        if (groupCode.isBlank()) {
            return Result.Error(IllegalArgumentException("Group code cannot be blank."))
        }
        return groupRepository.joinGroupWithCode(groupCode, joiningUser)
    }
}