package com.odoo.odools.params

import org.eclipse.lsp4j.jsonrpc.validation.NonNull

class DisplayCrashNotificationParams {

    private var crashInfo: String
    private var pid: Long
    private var recentMessages: String

    constructor() {
        crashInfo = String()
        pid = 0
        recentMessages = String()
    }

    constructor(@NonNull crashInfo: String, @NonNull pid: Long, @NonNull recentMessages: String) {
        this.crashInfo = crashInfo
        this.pid = pid
        this.recentMessages = recentMessages
    }

    @NonNull
    fun getPid(): Long {
        return pid;
    }

    fun setPid(@NonNull pid: Long) {
        this.pid = pid
    }

    @NonNull
    fun getCrashInfo(): String {
        return this.crashInfo
    }

    fun setCrashInfo(@NonNull crashInfo: String) {
        this.crashInfo = crashInfo
    }

    @NonNull
    fun getRecentMessages(): String {
        return this.recentMessages
    }

    fun setRecentMessages(@NonNull recentMessages: String) {
        this.recentMessages = recentMessages
    }

    override fun toString(): String {
        return "DisplayCrashNotificationParams [crashInfo=$crashInfo, pid=$pid, recentMessages=$recentMessages]"
    }

    override fun equals(obj: Any?): Boolean {
        if (this === obj) {
            return true
        } else if (obj == null) {
            return false
        } else if (this.javaClass != obj.javaClass) {
            return false
        } else {
            val other = obj as DisplayCrashNotificationParams
            return this.pid == other.pid //TODO improve?
        }
    }

    override fun hashCode(): Int {
        return 31 + this.crashInfo.hashCode() + this.pid.hashCode()
    }
}