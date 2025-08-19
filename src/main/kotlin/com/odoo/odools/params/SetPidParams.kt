package com.odoo.odools.params

import org.eclipse.lsp4j.jsonrpc.validation.NonNull
import org.eclipse.lsp4j.util.ToStringBuilder
import org.eclipse.lsp4j.util.Preconditions

class SetPidParams {
    @NonNull
    private var server_pid: Int? = null

    constructor()

    constructor(@NonNull value: Int?) {
        this.server_pid = Preconditions.checkNotNull<Int?>(value, "server_pid")
    }

    @NonNull
    fun getServerPid(): Int? {
        return this.server_pid
    }

    fun setServerPid(@NonNull value: Int?) {
        this.server_pid = Preconditions.checkNotNull<Int?>(value, "server_pid")
    }

    override fun toString(): String {
        val b = ToStringBuilder(this)
        b.add("serverPid", this.server_pid)
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
            val other = obj as SetPidParams
            if (this.server_pid == null) {
                if (other.server_pid != null) {
                    return false
                }
            } else if (this.server_pid != other.server_pid) {
                return false
            }

            return true
        }
    }

    override fun hashCode(): Int {
        return 31 + (if (this.server_pid == null) 0 else this.server_pid.hashCode())
    }
}