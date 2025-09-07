package com.money.dividr.navigation

object AppRoutes {
    const val SIGN_IN = "sign_in"
    const val HOME = "home"
    const val PROFILE = "profile"  
    const val CREATE_GROUP = "create_group"  
    const val JOIN_GROUP = "join_group"    

     
    const val GROUP_DETAILS_BASE = "group_details"
    const val GROUP_DETAILS_ARG_ID = "groupId"
    const val GROUP_CODE_ARG_ID = "groupJoinKey"
    const val GROUP_DETAILS = "$GROUP_DETAILS_BASE/{$GROUP_DETAILS_ARG_ID}/{$GROUP_CODE_ARG_ID}"
    fun groupDetailsRoute(groupId: String, groupJoinKey: String) = "$GROUP_DETAILS_BASE/$groupId/$groupJoinKey"

     
    const val ADD_EXPENSE_BASE = "add_expense"
     
    const val ADD_EXPENSE = "$ADD_EXPENSE_BASE/{$GROUP_DETAILS_ARG_ID}"  
    fun addExpenseRoute(groupId: String) = "$ADD_EXPENSE_BASE/$groupId"

     
    const val EXPENSE_DETAILS_BASE = "expense_details"
    const val EXPENSE_DETAILS_ARG_ID = "expenseId"
    const val EXPENSE_DETAILS = "$EXPENSE_DETAILS_BASE/{$GROUP_DETAILS_ARG_ID}/{$EXPENSE_DETAILS_ARG_ID}"
    fun expenseDetailsRoute(groupId: String?, expenseId: String) = "$EXPENSE_DETAILS_BASE/$groupId/$expenseId"

     
    const val SHOW_BALANCES_BASE = "show_balances"
     
    const val SHOW_BALANCES = "$SHOW_BALANCES_BASE/{$GROUP_DETAILS_ARG_ID}"  
    fun showBalancesRoute(groupId: String?) = "$SHOW_BALANCES_BASE/$groupId"

}
