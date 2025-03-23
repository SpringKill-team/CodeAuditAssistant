package org.skgroup.securityinspector.ui.component

data class SystemStatus(
    var SystemPlatform: String = "Unknown",
    var graphStatus: String = "Not Loaded",
    var methodCount: Int = 0,
    var callGraphSize: String = "0 MB",
    var buildInfo: String,
    var searchInfo: String,
    var errorInfo: String
)