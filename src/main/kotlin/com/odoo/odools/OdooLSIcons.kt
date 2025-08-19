package com.odoo.odools

import com.intellij.icons.AllIcons
import com.intellij.util.PlatformIcons
import javax.swing.Icon
import com.intellij.openapi.util.IconLoader

internal object OdooLSIcons {

    val ODOO: Icon = IconLoader.getIcon("/icons/pluginIcon.svg", OdooLSIcons::class.java)

    val PARAMETER: Icon = PlatformIcons.PARAMETER_ICON
    val FIELD: Icon = PlatformIcons.FIELD_ICON
    val ATTRIBUTE: Icon = PlatformIcons.ANNOTATION_TYPE_ICON
    val TYPE: Icon = AllIcons.Nodes.Type
    val ENUM: Icon = PlatformIcons.ENUM_ICON
    val FUNCTION: Icon = PlatformIcons.FUNCTION_ICON
    val KEY_VALUE: Icon = AllIcons.Json.Object
    val ALIAS: Icon = AllIcons.Nodes.ObjectTypeAttribute
}