package com.odoo.odools.params

import org.eclipse.lsp4j.jsonrpc.validation.NonNull

class SetConfigurationParams {

    private var configFile: List<Map<String, Any>>;
    private var html: Map<String, Any>;

    constructor() {
        configFile = ArrayList()
        html = HashMap()
    }

    constructor(@NonNull configFile: List<Map<String, Any>>, @NonNull html: Map<String, Any>) {
        this.configFile = configFile
        this.html = html
    }

    @NonNull
    fun getHtml(): Map<String, Any> {
        return html;
    }

    fun setHtml(@NonNull html: Map<String, Any>) {
        this.html = html
    }

    @NonNull
    fun getConfigFile(): List<Map<String, Any>> {
        return this.configFile
    }

    fun setConfigFile(@NonNull configFile: List<Map<String, Any>>) {
        this.configFile = configFile
    }

    override fun toString(): String {
        return "SetConfigurationParams [configFile=$configFile, html=$html]"
    }

    override fun equals(obj: Any?): Boolean {
        if (this === obj) {
            return true
        } else if (obj == null) {
            return false
        } else if (this.javaClass != obj.javaClass) {
            return false
        } else {
            val other = obj as SetConfigurationParams
            return this.html.keys == other.html.keys //TODO improve?
        }
    }

    override fun hashCode(): Int {
        return 31 + this.configFile.hashCode() + this.html.hashCode()
    }
}