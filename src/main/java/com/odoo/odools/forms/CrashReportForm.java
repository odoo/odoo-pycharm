package com.odoo.odools.forms;

import com.intellij.ide.plugins.IdeaPluginDescriptor;
import com.intellij.ide.plugins.PluginManagerCore;
import com.intellij.openapi.extensions.PluginId;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.DialogWrapper;
import com.intellij.openapi.vfs.VirtualFile;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Base64;
import java.util.UUID;

public class CrashReportForm extends DialogWrapper {
    private JPanel panel;
    private JTextArea tDescr;
    private JTextArea tLog;
    private JTextArea tDoc;
    private JButton bQuit;
    private JButton bCrashReport;
    private JScrollPane scrollPaneCurrentDocument;
    private JScrollPane scrollPaneLog;
    private JScrollPane scrollPaneDescr;
    private JTextField tEmail;
    private JTextArea thankYouForReportingTextArea;

    private VirtualFile currentFile;
    private String currentFileContent;
    private String logsPath;
    private String logContent;
    private String crashInfo;
    private String currentConfig;

    public CrashReportForm(Project project, VirtualFile currentFile, String crash_info, String currentConfig, String logs_path) {
        super(project);
        this.currentFile = currentFile;
        this.logsPath = logs_path;
        this.crashInfo = crash_info;
        this.currentConfig = currentConfig;
        if (currentConfig == null) {
            this.currentConfig = "Config not found";
        }
        setTitle("OdooLS Crash Report");
        init(); // required!
    }

    @Override
    protected Action @NotNull [] createActions() {
        return new Action[0]; //No Ok/Cancel buttons
    }

    @Override
    protected @Nullable JComponent createCenterPanel() {
        pack();

        // Read the log content
        logContent = "";
        try {
            logContent = Files.readString(Path.of(this.logsPath), StandardCharsets.UTF_8);
        } catch (IOException e) {
            logContent = "Failed to read log file at " + this.logsPath + ":\n" + e.getMessage();
        }

        tLog.setText(logContent);

        //read the current file content

        currentFileContent = "";
        try {
            // Option 1: read as bytes and convert to string
            byte[] bytes = this.currentFile.contentsToByteArray();
            currentFileContent = new String(bytes, StandardCharsets.UTF_8);

        } catch (IOException e) {
            currentFileContent = "Unable to read current File: " + e.getMessage();
        }

        tDoc.setText(currentFileContent);

        bQuit.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                dispose();
            }
        });

        bCrashReport.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                sendReport(currentFile, currentFileContent, logsPath);
                dispose();
            }
        });
        return panel;
    }

    private void sendReport(VirtualFile currentFile, String currentFileContent, String logsPath) {
        String url = "https://iap-services.odoo.com/api/odools/vscode/2/crash_report";
        String json = buildJson(currentFile, currentFileContent, logsPath);
        try (HttpClient client = HttpClient.newHttpClient()) {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(json))
                    .build();

            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        } catch (Exception e) {
            JOptionPane.showMessageDialog(null, "Unable to send crash report: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private String buildJson(VirtualFile currentFile, String currentFileContent, String logsPath) {
        String uid = UUID.randomUUID().toString();
        String pluginVersion = "unknown";
        IdeaPluginDescriptor descriptor =
                PluginManagerCore.getPlugin(PluginId.getId("com.odoo.odools"));
        if (descriptor != null) {
            pluginVersion = descriptor.getVersion();
        }

        // Read file content as bytes
        byte[] bytes = null;
        try {
            bytes = Files.readAllBytes(Path.of(logsPath));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        // Encode as Base64
        String base64 = Base64.getEncoder().encodeToString(bytes);

        return """
            {
              "data": {
                "uid": "%s",
                "ide": "pycharm",
                "email": "%s",
                "document": "%s",
                "document_path": "%s",
                "lsp_log": "%s",
                "error": "%s",
                "additional_info": "%s",
                "version": "%s",
                "python_version": "%s",
                "configuration": "%s"
              }
            }
            """.formatted(
                uid,
                escapeForJson(this.tEmail.getText()),
                escapeForJson(currentFileContent),
                currentFile.getPath(),
                base64,
                escapeForJson(this.crashInfo),
                escapeForJson(this.tDescr.getText()),
                pluginVersion,
                "See configuration",
                escapeForJson(this.currentConfig)
        );
    }

    private static String escapeForJson(String s) {
        if (s == null) return "null";
        return s
            .replace("\\", "\\\\")
            .replace("\"", "\\\"")
            .replace("\n", "\\n")
            .replace("\r", "\\r")
            .replace("\t", "\\t");
    }

}
