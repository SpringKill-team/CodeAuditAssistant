package com.skgroup.securityinspector.visitors

import com.intellij.psi.PsiRecursiveElementWalkingVisitor

public abstract class BaseFixElementWalkingVisitor : PsiRecursiveElementWalkingVisitor() {

    private var isFix: Boolean = false

    fun setFix() {
        this.isFix = true
    }

    fun resetFix() {
        this.isFix = false
    }

    fun isFix(): Boolean {
        return isFix
    }
}