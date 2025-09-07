# Dividr - Expense Splitting App

## Summary

Dividr is a native Android application designed to simplify expense splitting and management for groups. Whether it's for roommates, trips with friends, or any shared financial activity, 
Dividr helps users easily track who paid for what, calculate individual shares (including handling complex splitting scenarios and rounding), and see who owes whom, ensuring everyone gets settled up fairly. 
The app leverages modern Android development practices and a real-time backend for a seamless user experience.

## Tech Stack

*   **Programming Language:** Kotlin
*   **UI Toolkit:** Jetpack Compose (Android's next-generation declarative UI toolkit)
*   **Architecture:** Clean Architecture + MVVM
*   **Asynchronous Programming:** Kotlin Coroutines & Flow (StateFlow)
*   **Dependency Injection:** Hilt
*   **Backend & Database:** Firebase Firestore (for real-time data synchronization)
*   **Authentication:** Firebase Google Sign-In
*   **Navigation:** Jetpack Navigation Compose
*   **Android Jetpack Libraries:** ViewModel, LiveData/StateFlow, Navigation, etc.

## Features

*   **User Authentication:** Secure sign-in/sign-up using Google Sign-In.
*   **Group Creation & Management:**
    *   Create new groups for different purposes.
    *   Join existing groups using a unique group code.
*   **Expense Tracking:**
    *   Add new expenses with details: description, amount, payer.
    *   View a list of all expenses within a group.
    *   View detailed information for each expense, including participants and their shares.
*   **Advanced Splitting Logic:**
    *   Accurately splits expenses equally among group members.
    *   Handles rounding discrepancies intelligently to ensure the total amount is always accounted for (penny problem).
*   **Balance Viewing:**
    *   Clearly shows who owes whom within a group.
    *   Displays a summary of individual debts and credits.
*   **Real-time Updates:** Data syncs across all group members' devices in real-time using Firestore.
*   **Modern & Intuitive UI:** Built with Jetpack Compose for a clean and responsive user experience.
*   **Future Scope** Add "Smart Simplify" feature using OpenAI's API to simplify multiple debts and balances.



