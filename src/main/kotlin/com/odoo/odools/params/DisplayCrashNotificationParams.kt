package com.odoo.odools.params

import org.eclipse.lsp4j.jsonrpc.validation.NonNull
import org.eclipse.lsp4j.util.ToStringBuilder
import org.eclipse.lsp4j.util.Preconditions

class DisplayCrashNotificationParams {

    private var crashInfo: String
    private var pid: Long

    constructor() {
        crashInfo = String()
        pid = 0
    }

    constructor(@NonNull crashInfo: String, @NonNull pid: Long) {
        this.crashInfo = Preconditions.checkNotNull<String>(crashInfo, "crashInfo")
        this.pid = Preconditions.checkNotNull(pid, "pid")
    }

    @NonNull
    fun getPid(): Long {
        return pid;
    }

    fun setPid(@NonNull pid: Long) {
        this.pid = Preconditions.checkNotNull(pid, "pid")
    }

    @NonNull
    fun getCrashInfo(): String {
        return this.crashInfo
    }

    fun setCrashInfo(@NonNull configFile: List<Map<String, Any>>) {
        this.crashInfo = Preconditions.checkNotNull<String>(crashInfo, "crashInfo")
    }

    override fun toString(): String {
        val b = ToStringBuilder(this)
        b.add("configFile", this.crashInfo)
        b.add("html", this.pid)
        return b.toString()
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