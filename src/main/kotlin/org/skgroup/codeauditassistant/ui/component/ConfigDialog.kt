package org.skgroup.codeauditassistant.ui.component

import com.intellij.openapi.ui.DialogWrapper
import com.intellij.ui.components.JBLabel
import com.intellij.ui.components.JBPanel
import com.intellij.ui.components.JBTextField
import com.intellij.uiDesigner.core.GridConstraints
import com.intellij.uiDesigner.core.GridLayoutManager
import kotlinx.serialization.json.*
import org.skgroup.codeauditassistant.i18n.CAMessage
import org.skgroup.codeauditassistant.ui.service.AuthStateService
import org.skgroup.codeauditassistant.utils.RSAUtil
import java.awt.Dimension
import java.util.*
import javax.swing.JButton
import javax.swing.JComponent

class ConfigDialog : DialogWrapper(true) {
    private val licenseLabel = JBLabel(CAMessage.message("config.license.label"))
    private val licenseKeyField = JBTextField()
    private val authButton = JButton(CAMessage.message("config.register.button"))
    private val resultLabel = JBLabel(CAMessage.message("config.result.label"))
    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
    }

    init {
        title = CAMessage.message("config.dialog.title")
        init()
        checkExistingAuth()
    }

    private fun checkExistingAuth() {
        if (AuthStateService.instance.isAuthenticated()) {
            licenseKeyField.text = AuthStateService.instance.state.licenseKey
            resultLabel.text = CAMessage.message(
                "config.authenticated.message",
                Date(AuthStateService.instance.state.expireTime)
            )
        }
    }

    override fun createCenterPanel(): JComponent {
        licenseKeyField.emptyText.text = CAMessage.message("config.license.placeholder")

        // 使用1行3列的布局，使组件横向排列
        val panel = JBPanel<JBPanel<*>>(GridLayoutManager(2, 3)).apply {
            // 设置较小的垂直尺寸和较大的最小宽度
            preferredSize = Dimension(600, 60)
            minimumSize = Dimension(400, 60)
        }

        // 添加标签
        panel.add(licenseLabel, GridConstraints().apply {
            row = 0
            column = 0
            anchor = GridConstraints.ANCHOR_WEST
            fill = GridConstraints.FILL_NONE
            hSizePolicy = GridConstraints.SIZEPOLICY_FIXED
        })

        // 添加文本框（会拉伸）
        panel.add(licenseKeyField, GridConstraints().apply {
            row = 0
            column = 1
            anchor = GridConstraints.ANCHOR_WEST
            fill = GridConstraints.FILL_HORIZONTAL
            hSizePolicy = GridConstraints.SIZEPOLICY_WANT_GROW
        })

        // 添加按钮
        panel.add(authButton, GridConstraints().apply {
            row = 0
            column = 2
            anchor = GridConstraints.ANCHOR_WEST
            fill = GridConstraints.FILL_NONE
            hSizePolicy = GridConstraints.SIZEPOLICY_FIXED
        })

        panel.add(resultLabel, GridConstraints().apply {
            row = 1
            column = 0
            colSpan = 3
            anchor = GridConstraints.ANCHOR_WEST
            fill = GridConstraints.FILL_NONE
            hSizePolicy = GridConstraints.SIZEPOLICY_FIXED
        })

        authButton.addActionListener {
            authenticate()
        }

        return panel
    }

    private fun authenticate() {
        val licenseKey = licenseKeyField.text.trim()

        if (licenseKey.isBlank()) {
            resultLabel.text = CAMessage.message("config.error.empty.license")
            return
        }

        try {
            // Split the license into data and signature parts
            val parts = licenseKey.split("|")
            if (parts.size != 2) {
                resultLabel.text = CAMessage.message("config.error.invalid.format")
                return
            }

            // 解码Base64的数据部分
            val licenseData = Base64.getDecoder().decode(parts[0])
            // 获取Base64编码的签名字符串（不解码）
            val base64Signature = parts[1]

            // Verify the signature - 使用现有的verifySignature方法
            if (!RSAUtil.verifySignature(licenseData, base64Signature)) {
                resultLabel.text = CAMessage.message("config.error.signature.failed")
                return
            }

            // Parse the license data
            val licenseJson = String(licenseData, Charsets.UTF_8)
            if (validateAndSaveAuth(licenseJson, licenseKey)) {
                resultLabel.text = CAMessage.message(
                    "config.success.authenticated",
                    Date(AuthStateService.instance.state.expireTime)
                )
            } else {
                resultLabel.text = CAMessage.message("config.error.expired")
            }
        } catch (e: Exception) {
            resultLabel.text = CAMessage.message("config.error.general", e.message ?: "")
            e.printStackTrace() // 添加这行以获取更详细的错误信息
        }
    }

    private fun validateAndSaveAuth(decryptedData: String, license: String): Boolean {
        val jsonElement = json.parseToJsonElement(decryptedData)
        val isExpired = jsonElement.jsonObject["licenseExpireTime"]?.jsonPrimitive?.longOrNull ?: 0L
        val expireTime = jsonElement.jsonObject["expireTime"]?.jsonPrimitive?.longOrNull ?: 0L
        val authName = jsonElement.jsonObject["authName"]?.jsonPrimitive?.contentOrNull ?: ""
        if (isExpired < System.currentTimeMillis() || expireTime < System.currentTimeMillis()) {
            AuthStateService.instance.clearAuth()
            return false
        } else {
            AuthStateService.instance.saveAuth(license, expireTime, authName)
        }
        return true
    }
}