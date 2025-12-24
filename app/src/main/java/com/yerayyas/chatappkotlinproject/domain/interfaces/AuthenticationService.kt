package com.yerayyas.chatappkotlinproject.domain.interfaces

/**
 * Abstraction layer for authentication-related operations.
 *
 * This interface resides in the Domain layer and serves as a contract that any
 * authentication provider (Data layer) must implement. By using this abstraction,
 * the domain logic (Use Cases) and presentation logic (ViewModels) remain agnostic
 * to the specific authentication framework being used.
 *
 * This follows the **Dependency Inversion Principle**, allowing the app to switch
 * or mock authentication providers without affecting business logic.
 */
interface AuthenticationService {

    /**
     * Verifies if there is a currently authenticated user in the system.
     *
     * This is primarily used as a security gate in navigation flows to decide whether
     * to allow access to protected screens or redirect the user to the authentication
     * onboarding (Login/SignUp).
     *
     * @return `true` if a valid session exists; `false` if the user is anonymous or signed out.
     */
    fun isUserAuthenticated(): Boolean
}
