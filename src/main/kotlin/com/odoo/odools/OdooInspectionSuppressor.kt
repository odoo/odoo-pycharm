package com.odoo.odools

import com.intellij.codeInspection.InspectionSuppressor
import com.intellij.codeInspection.SuppressQuickFix
import com.intellij.openapi.components.service
import com.intellij.psi.PsiElement

// Suppress PyCharm Python inspections that overlap with OdooLS diagnostics, unless OdooLS is disabled
// or the selected profile has no odoo_path
class OdooInspectionSuppressor : InspectionSuppressor {
    private val suppressedIds = setOf(
        "PyUnresolvedReferences",
        "PyArgumentList",
        "PyTypeChecker",
        "PyCallingNonCallable",
        "PyAttributeOutsideInit",
    )

    override fun isSuppressedFor(element: PsiElement, toolId: String): Boolean {
        if (toolId !in suppressedIds) return false
        return element.project.service<OdooProjectSettingsService>().isOdooLsActive()
    }

    override fun getSuppressActions(element: PsiElement?, toolId: String): Array<SuppressQuickFix> =
        SuppressQuickFix.EMPTY_ARRAY
}
