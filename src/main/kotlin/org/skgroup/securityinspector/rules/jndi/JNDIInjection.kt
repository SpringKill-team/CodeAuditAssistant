package org.skgroup.securityinspector.rules.jndi

import com.intellij.codeInspection.ProblemHighlightType
import com.intellij.codeInspection.ProblemsHolder
import com.intellij.psi.*
import org.skgroup.securityinspector.inspectors.BaseLocalInspectionTool
import org.skgroup.securityinspector.utils.InspectionBundle
import org.skgroup.securityinspector.utils.SecExpressionUtils

class JNDIInjection : BaseLocalInspectionTool() {

    companion object {
        private val MESSAGE = InspectionBundle.message("vuln.massage.JNDIInjection")

        private val JNDIINJECTION_METHOS_SINKS = mapOf(
            "javax.management.remote.JMXServiceURL" to emptyList(),
            "java.rmi.registry.Registry" to listOf("lookup"),
            "javax.naming.Context" to listOf("lookup", "list", "listBindings", "lookupLink", "rename"),
            "javax.naming.InitialContext" to listOf("doLookup", "lookup", "rename", "list", "listBindings"),
            "javax.naming.directory.InitialDirContext" to listOf("lookup", "rename", "list", "listBindings"),
            "javax.naming.ldap.InitialLdapContext" to listOf("lookup", "rename", "list", "listBindings", "lookupLink"),
            "javax.management.remote.JMXConnector" to listOf("connect"),
            "javax.management.remote.JMXConnectorFactory" to listOf("connect"),
            "org.springframework.ldap.LdapOperations" to listOf(
                "findByDn", "list", "listBindings", "lookup", "lookupContext", "rename", "search"
            ),
            "org.springframework.ldap.core.LdapOperation" to listOf(
                "findByDn", "list", "listBindings", "lookup", "lookupContext", "rename", "search", "searchForObject"
            ),
            "org.springframework.ldap.core.LdapTemplate" to listOf(
                "lookup", "lookupContext", "findByDn", "rename", "list", "listBindings"
            ),
            "org.apache.shiro.jndi.JndiTemplate" to listOf("lookup"),
            "org.springframework.jndi.JndiTemplate" to listOf("lookup")
        )

        private val JNDIINJECTION_NEWEXPRESSION_SINKS = listOf(
            "javax.management.remote.JMXServiceURL"
        )
    }

    override fun buildVisitor(holder: ProblemsHolder, isOnTheFly: Boolean): PsiElementVisitor {
        return object : JavaElementVisitor() {

            override fun visitNewExpression(expression: PsiNewExpression) {
                if (SecExpressionUtils.isNewExpressionSink(expression, JNDIINJECTION_NEWEXPRESSION_SINKS)) {
                    holder.registerProblem(
                        expression,
                        MESSAGE,
                        ProblemHighlightType.GENERIC_ERROR_OR_WARNING
                    )
                }
            }

            override fun visitMethodCallExpression(expression: PsiMethodCallExpression) {
                if (SecExpressionUtils.isMethodSink(expression, JNDIINJECTION_METHOS_SINKS)) {
                    holder.registerProblem(
                        expression,
                        MESSAGE,
                        ProblemHighlightType.GENERIC_ERROR_OR_WARNING
                    )
                }
            }
        }
    }
}
