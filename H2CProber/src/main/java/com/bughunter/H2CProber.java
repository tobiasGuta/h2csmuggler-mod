package com.bughunter;

import burp.api.montoya.BurpExtension;
import burp.api.montoya.MontoyaApi;
import burp.api.montoya.http.message.HttpHeader;
import burp.api.montoya.http.message.HttpRequestResponse;
import burp.api.montoya.http.message.requests.HttpRequest;
import burp.api.montoya.http.message.responses.HttpResponse;
import burp.api.montoya.ui.UserInterface;
import burp.api.montoya.ui.contextmenu.ContextMenuEvent;
import burp.api.montoya.ui.contextmenu.ContextMenuItemsProvider;
import burp.api.montoya.ui.contextmenu.MessageEditorHttpRequestResponse;
import burp.api.montoya.ui.editor.EditorOptions;
import burp.api.montoya.ui.editor.extension.ExtensionProvidedHttpRequestEditor;

import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;

public class H2CProber implements BurpExtension, ContextMenuItemsProvider {

    private MontoyaApi api;
    private JTextArea logArea; // The text area for our new tab

    private static final String HTTP2_SETTINGS_PAYLOAD = "AAMAAABkAAQAAP__";

    @Override
    public void initialize(MontoyaApi api) {
        this.api = api;
        api.extension().setName("H2C Smuggling Prober (With UI)");

        // 1. Setup the UI Component
        SwingUtilities.invokeLater(() -> {
            // Create the text area
            logArea = new JTextArea();
            logArea.setEditable(false);
            logArea.setFont(new Font("Monospaced", Font.PLAIN, 12));
            logArea.setText("H2C Prober Log Started...\n-------------------------\n");

            // Wrap it in a scroll pane
            JScrollPane scrollPane = new JScrollPane(logArea);

            // Register the tab in Burp
            api.userInterface().registerSuiteTab("H2C Prober", scrollPane);
        });

        // 2. Register Menu
        api.userInterface().registerContextMenuItemsProvider(this);

        api.logging().logToOutput("H2C Prober Loaded. Check the 'H2C Prober' tab for results.");
    }

    @Override
    public List<Component> provideMenuItems(ContextMenuEvent event) {
        if (event.messageEditorRequestResponse().isEmpty()) return null;

        JMenuItem probeItem = new JMenuItem("Probe for h2c Tunneling");
        MessageEditorHttpRequestResponse editor = event.messageEditorRequestResponse().get();

        probeItem.addActionListener(l -> {
            new Thread(() -> performAttack(editor.requestResponse())).start();
        });

        List<Component> menuList = new ArrayList<>();
        menuList.add(probeItem);
        return menuList;
    }

    private void performAttack(HttpRequestResponse baseRequestResponse) {
        try {
            HttpRequest originalRequest = baseRequestResponse.request();
            String targetHost = baseRequestResponse.httpService().host();
            String targetUrl = originalRequest.url();

            logToTab("[-] Probing: " + targetUrl);

            // Construct Upgrade Request
            HttpRequest upgradeRequest = originalRequest
                    .withService(baseRequestResponse.httpService())
                    .withHeader(HttpHeader.httpHeader("Connection", "Upgrade, HTTP2-Settings"))
                    .withHeader(HttpHeader.httpHeader("Upgrade", "h2c"))
                    .withHeader(HttpHeader.httpHeader("HTTP2-Settings", HTTP2_SETTINGS_PAYLOAD))
                    .withMethod("GET")
                    .withBody("");

            HttpRequestResponse response = api.http().sendRequest(upgradeRequest);
            HttpResponse serverResponse = response.response();
            short statusCode = serverResponse.statusCode();

            if (statusCode == 101) {
                // SUCCESS LOGGING
                StringBuilder alert = new StringBuilder();
                alert.append("\n!!! VULNERABILITY CONFIRMED !!!\n");
                alert.append("Target: ").append(targetHost).append("\n");
                alert.append("Status: 101 Switching Protocols\n");
                alert.append("The proxy forwarded the 'h2c' upgrade.\n");
                alert.append("Next Step: Use an external tool (e.g. h2csmuggler) to tunnel traffic.\n");
                alert.append("--------------------------------------------------\n");

                logToTab(alert.toString());

                // Highlight the event in Burp's dashboard
                api.logging().raiseErrorEvent("H2C Tunneling Found: " + targetHost);
            }
            else {
                logToTab("[-] Failed (" + statusCode + ") on " + targetHost + "\n");
            }

        } catch (Exception e) {
            logToTab("[!] Error: " + e.getMessage() + "\n");
            api.logging().logToError("Error probing: " + e.getMessage());
        }
    }

    // Helper to write to the Swing UI safely
    private void logToTab(String message) {
        SwingUtilities.invokeLater(() -> {
            if (logArea != null) {
                logArea.append(message + "\n");
                // Auto-scroll to bottom
                logArea.setCaretPosition(logArea.getDocument().getLength());
            }
        });
    }
}