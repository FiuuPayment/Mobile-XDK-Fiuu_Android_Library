package com.fiuu.xdkandroid

import com.fiuu.xdkandroid.domain.entity.NATIVE_PARAM_KEYS
import com.fiuu.xdkandroid.domain.entity.WEBCORE_ONLY_PARAM_KEYS
import com.fiuu.xdkandroid.domain.entity.isBillDescriptionEditable
import com.fiuu.xdkandroid.domain.entity.isBillEmailEditable
import com.fiuu.xdkandroid.domain.entity.isBillMobileEditable
import com.fiuu.xdkandroid.domain.entity.isBillNameEditable
import com.fiuu.xdkandroid.domain.entity.paramWarnings
import com.fiuu.xdkandroid.domain.entity.toPaymentRequest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ParamValidationTest {

    private fun Map<String, Any?>.singleWarning(): String {
        val warnings = paramWarnings()
        assertEquals(1, warnings.size, "expected one warning, got $warnings")
        return warnings.single()
    }

    @Test
    fun `valid params produce no warnings`() {
        val params = mapOf<String, Any?>(
            "mp_merchant_ID" to "m", "mp_amount" to "1.00", "mp_core_env" to 1,
            "mp_build_mode" to "3", "mp_sandbox_mode" to "FALSE", "mp_express_mode" to true,
            "mp_dev_mode" to "false", "mp_closebutton_display" to false, "device_info" to "{}"
        )
        assertEquals(emptyList(), params.paramWarnings())
    }

    @Test
    fun `absent and null flags produce no warnings`() {
        assertEquals(emptyList(), emptyMap<String, Any?>().paramWarnings())
        assertEquals(emptyList(), mapOf("mp_build_mode" to null, "mp_express_mode" to null).paramWarnings())
    }

    @Test
    fun `out of range mp_core_env is reported`() {
        assertTrue(mapOf("mp_core_env" to "9").singleWarning().startsWith("mp_core_env=\"9\""))
        assertTrue(mapOf("mp_core_env" to 0).singleWarning().startsWith("mp_core_env=0"))
    }

    @Test
    fun `invalid mp_build_mode reports the fallback it actually uses`() {
        assertTrue(mapOf("mp_build_mode" to "5").singleWarning().endsWith("4 (sandbox)."))
        assertTrue(
            mapOf("mp_build_mode" to 5, "mp_sandbox_mode" to false).singleWarning()
                .endsWith("2 (production).")
        )
    }

    @Test
    fun `mp_build_mode is not checked when mp_core_env overrides it`() {
        assertEquals(emptyList(), mapOf("mp_core_env" to 3, "mp_build_mode" to "5").paramWarnings())
    }

    @Test
    fun `malformed mp_sandbox_mode is reported as sandbox`() {
        val warning = mapOf("mp_sandbox_mode" to "flase").singleWarning()
        assertTrue(warning.contains("treating it as true (sandbox)"))
    }

    @Test
    fun `malformed boolean flags are reported with their default`() {
        assertTrue(mapOf("mp_express_mode" to "yes").singleWarning().endsWith("using false."))
        assertTrue(mapOf("mp_dev_mode" to 1).singleWarning().endsWith("using false."))
        assertTrue(mapOf("mp_closebutton_display" to "no").singleWarning().endsWith("using true."))
        assertTrue(mapOf("mp_editing_enabled" to "yes").singleWarning().endsWith("using false."))
    }

    @Test
    fun `warnings never change how a flag resolves`() {
        assertFalse(mapOf<String, Any?>("mp_express_mode" to "yes").toPaymentRequest().expressMode)
        assertFalse(mapOf<String, Any?>("mp_express_mode" to "TRUE").toPaymentRequest().expressMode)
        assertTrue(mapOf<String, Any?>("mp_express_mode" to "true").toPaymentRequest().expressMode)
        assertTrue(mapOf<String, Any?>("mp_closebutton_display" to "no").toPaymentRequest().closeButtonDisplay)
        assertFalse(mapOf<String, Any?>().toPaymentRequest().editingEnabled)
        assertTrue(mapOf<String, Any?>("mp_editing_enabled" to "true").toPaymentRequest().editingEnabled)
    }

    @Test
    fun `per-field edit flags only apply while editing is enabled`() {
        val locked = mapOf<String, Any?>("mp_bill_email_edit_disabled" to false).toPaymentRequest()
        assertFalse(locked.isBillNameEditable)
        assertFalse(locked.isBillEmailEditable)

        val request = mapOf<String, Any?>(
            "mp_editing_enabled" to true,
            "mp_bill_name_edit_disabled" to true,
            "mp_bill_mobile_edit_disabled" to "true"
        ).toPaymentRequest()
        assertFalse(request.isBillNameEditable)
        assertTrue(request.isBillEmailEditable)
        assertFalse(request.isBillMobileEditable)
        assertTrue(request.isBillDescriptionEditable)
    }

    @Test
    fun `unknown mp keys are reported with a case-insensitive suggestion`() {
        assertEquals(
            "Unknown param mp_merchant_id is ignored. Did you mean mp_merchant_ID?",
            mapOf("mp_merchant_id" to "m").singleWarning()
        )
        assertEquals(
            "Unknown param mp_foo is ignored.",
            mapOf("mp_foo" to "x").singleWarning()
        )
    }

    @Test
    fun `unknown key warnings never include the value`() {
        assertFalse(mapOf("mp_pasword" to "secret").singleWarning().contains("secret"))
    }

    @Test
    fun `non mp keys are not reported`() {
        assertEquals(emptyList(), mapOf("device_info" to "{}", "is_submodule" to true).paramWarnings())
    }

    @Test
    fun `unknown keys are not reported for the Webcore engine`() {
        assertEquals(emptyList(), mapOf("mp_core_env" to "3", "mp_webcore_only" to "x").paramWarnings())
    }

    @Test
    fun `Webcore-only params are reported in one warning on the native engine`() {
        assertEquals(
            "Webcore-only params are ignored by the native engine (mp_core_env 1): " +
                "mp_bin_lock, mp_language. They only take effect with mp_core_env 2-4.",
            mapOf("mp_language" to "en", "mp_bin_lock" to listOf("411111")).singleWarning()
        )
    }

    @Test
    fun `Webcore-only params are not reported for the Webcore engine`() {
        assertEquals(emptyList(), mapOf("mp_core_env" to "2", "mp_bin_lock" to "411111").paramWarnings())
    }

    @Test
    fun `Webcore-only warning never includes the values`() {
        assertFalse(mapOf("mp_metadata" to "secret").singleWarning().contains("secret"))
    }

    @Test
    fun `native and Webcore-only key sets do not overlap`() {
        assertEquals(emptySet(), NATIVE_PARAM_KEYS intersect WEBCORE_ONLY_PARAM_KEYS)
    }
}
