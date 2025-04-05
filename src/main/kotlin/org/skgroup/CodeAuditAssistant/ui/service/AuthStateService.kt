package org.skgroup.CodeAuditAssistant.ui.service

import com.intellij.openapi.components.*

/**
 * 类描述：AuthStateService 类用于。
 *
 * @author springkill
 * @version 1.0
 * @since 2025/4/1
 */
@Service
@State(name = "CodeAuditAssistantAuth", storages = [Storage("CodeAuditAssistantAuth.xml")])
class AuthStateService : PersistentStateComponent<AuthStateService.State> {
    companion object {
        val instance: AuthStateService
            get() = service()
    }

    data class State(
        var isAuthenticated: Boolean = false,
        var expireTime: Long = 0,
        var licenseKey: String = "",
        var authName: String = "",
    )

    var myState = State()

    override fun getState() = myState

    override fun loadState(state: State) {
        myState = state
    }

    fun isAuthenticated() = myState.isAuthenticated && System.currentTimeMillis() < myState.expireTime

    fun saveAuth(licenseKey: String, expireTime: Long, authName: String) {
        myState.isAuthenticated = true
        myState.expireTime = expireTime
        myState.licenseKey = licenseKey
        myState.authName = authName
    }

    fun clearAuth() {
        myState = State()
    }

}
