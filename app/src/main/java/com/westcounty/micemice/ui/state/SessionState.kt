package com.westcounty.micemice.ui.state

import com.westcounty.micemice.data.model.UserRole

data class SessionState(
    val isLoggedIn: Boolean,
    val username: String,
    val orgCode: String,
    val role: UserRole,
    val loginAtMillis: Long = 0L,
    val lastActionAtMillis: Long = 0L,
)

val LoggedOutSession = SessionState(
    isLoggedIn = false,
    username = "",
    orgCode = "",
    role = UserRole.Researcher,
    loginAtMillis = 0L,
    lastActionAtMillis = 0L,
)
