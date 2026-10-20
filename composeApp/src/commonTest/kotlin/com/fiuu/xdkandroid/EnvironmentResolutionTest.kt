package com.fiuu.xdkandroid

import com.fiuu.xdkandroid.data.remote.ApiConfig
import com.fiuu.xdkandroid.domain.entity.resolveBuildMode
import com.fiuu.xdkandroid.domain.entity.resolveCoreEnv
import com.fiuu.xdkandroid.domain.entity.resolveSandboxMode
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class EnvironmentResolutionTest {

    @Test
    fun `baseUrl selects sandbox host when buildMode is 4`() {
        val url = ApiConfig.baseUrl(buildMode = 4)
        assertTrue(url.contains("sandbox-mobile.fiuu.com"))
    }

    @Test
    fun `baseUrl selects a different host for production buildMode`() {
        val sandbox = ApiConfig.baseUrl(buildMode = 4)
        val production = ApiConfig.baseUrl(buildMode = 2)
        assertTrue(sandbox != production)
    }

    @Test
    fun `resolveSandboxMode accepts a real Boolean`() {
        assertEquals(true, mapOf("mp_sandbox_mode" to true).resolveSandboxMode())
        assertEquals(false, mapOf("mp_sandbox_mode" to false).resolveSandboxMode())
    }

    @Test
    fun `resolveSandboxMode accepts a String, case-insensitively`() {
        assertEquals(true, mapOf("mp_sandbox_mode" to "true").resolveSandboxMode())
        assertEquals(true, mapOf("mp_sandbox_mode" to "TRUE").resolveSandboxMode())
        assertEquals(false, mapOf("mp_sandbox_mode" to "false").resolveSandboxMode())
    }

    @Test
    fun `resolveSandboxMode defaults to sandbox when absent or unrecognized`() {
        assertEquals(true, emptyMap<String, Any?>().resolveSandboxMode())
        assertEquals(true, mapOf("mp_sandbox_mode" to 123).resolveSandboxMode())
        assertEquals(true, mapOf("mp_sandbox_mode" to null).resolveSandboxMode())
    }

    @Test
    fun `resolveSandboxMode fails safe on a malformed string, only explicit false selects production`() {
        assertEquals(true, mapOf("mp_sandbox_mode" to "sandbox").resolveSandboxMode())
        assertEquals(true, mapOf("mp_sandbox_mode" to "1").resolveSandboxMode())
        assertEquals(false, mapOf("mp_sandbox_mode" to "FALSE").resolveSandboxMode())
    }

    @Test
    fun `resolveBuildMode does not crash on a malformed mp_build_mode string`() {
        assertEquals(4, mapOf("mp_build_mode" to "not-a-number").resolveBuildMode())
    }

    @Test
    fun `resolveBuildMode falls back to a valid mode for unsupported values instead of production`() {
        assertEquals(4, mapOf("mp_build_mode" to 0).resolveBuildMode())
        assertEquals(4, mapOf("mp_build_mode" to 5).resolveBuildMode())
        assertEquals(2, mapOf("mp_build_mode" to 5, "mp_sandbox_mode" to false).resolveBuildMode())
    }

    @Test
    fun `resolveBuildMode accepts the three known modes`() {
        assertEquals(2, mapOf("mp_build_mode" to 2).resolveBuildMode())
        assertEquals(3, mapOf("mp_build_mode" to 3).resolveBuildMode())
        assertEquals(4, mapOf("mp_build_mode" to 4).resolveBuildMode())
    }

    @Test
    fun `resolveCoreEnv accepts 1 to 4 as Int or String`() {
        for (env in 1..4) {
            assertEquals(env, mapOf("mp_core_env" to env).resolveCoreEnv())
            assertEquals(env, mapOf("mp_core_env" to env.toString()).resolveCoreEnv())
        }
    }

    @Test
    fun `resolveCoreEnv defaults to 1 when absent, malformed or out of range`() {
        assertEquals(1, emptyMap<String, Any?>().resolveCoreEnv())
        assertEquals(1, mapOf("mp_core_env" to null).resolveCoreEnv())
        assertEquals(1, mapOf("mp_core_env" to "uat").resolveCoreEnv())
        assertEquals(1, mapOf("mp_core_env" to 0).resolveCoreEnv())
        assertEquals(1, mapOf("mp_core_env" to 5).resolveCoreEnv())
        assertEquals(1, mapOf("mp_core_env" to "99").resolveCoreEnv())
        assertEquals(1, mapOf("mp_core_env" to -2).resolveCoreEnv())
    }

    @Test
    fun `resolveBuildMode lets mp_core_env 2 to 4 override mp_build_mode`() {
        assertEquals(3, mapOf("mp_core_env" to 3, "mp_build_mode" to 4).resolveBuildMode())
        assertEquals(2, mapOf("mp_core_env" to "2", "mp_sandbox_mode" to true).resolveBuildMode())
    }

    @Test
    fun `resolveBuildMode ignores mp_core_env 1 or an invalid value`() {
        assertEquals(3, mapOf("mp_core_env" to 1, "mp_build_mode" to 3).resolveBuildMode())
        assertEquals(3, mapOf("mp_core_env" to 99, "mp_build_mode" to 3).resolveBuildMode())
        assertEquals(2, mapOf("mp_core_env" to 0, "mp_sandbox_mode" to false).resolveBuildMode())
    }

    @Test
    fun `resolveBuildMode is not affected by mp_dev_mode`() {
        assertEquals(4, mapOf("mp_dev_mode" to true).resolveBuildMode())
        assertEquals(2, mapOf("mp_dev_mode" to true, "mp_sandbox_mode" to false).resolveBuildMode())
    }

    @Test
    fun `baseUrl fails safe to sandbox for an unrecognized build mode`() {
        val url = ApiConfig.baseUrl(buildMode = 99)
        assertTrue(url.contains("sandbox-mobile.fiuu.com"))
    }
}
