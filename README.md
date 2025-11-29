# H2C Smuggling Prober (Burp Suite Extension)

**A Reconnaissance Tool for detecting HTTP/2 over Cleartext (h2c) Tunneling Vulnerabilities**

![Java](https://img.shields.io/badge/Java-21-orange)
![Burp Suite](https://img.shields.io/badge/Burp%20Suite-Montoya%20API-blue)
![License](https://img.shields.io/badge/License-MIT-green)

## Overview
**H2C Prober** is a Burp Suite extension designed to detect a specific misconfiguration in reverse proxies: **Unintended h2c Upgrades**.

The `h2c` protocol (HTTP/2 over Cleartext) is technically obsolete, but many backends still support it. If a Reverse Proxy blindly forwards the `Upgrade: h2c` header (especially over an encrypted TLS channel), the backend may switch protocols. This creates a persistent **TCP Tunnel** through the proxy, allowing an attacker to bypass access controls (ACLs) and route arbitrary traffic to the backend.

**This extension automates the discovery of this vulnerability.** It attempts the upgrade handshake and listens for the `101 Switching Protocols` response.

## Features

* **Context Menu Integration:** Right-click any request in Repeater or Proxy to launch a probe.
* **Dedicated UI Tab:** A clean "H2C Prober" dashboard tab that logs all attempts and successful findings.
* **Smart Detection:** Automatically constructs the specific `HTTP2-Settings` payload required to trigger a compliant server upgrade.
* **Non-Intrusive:** Performs a safe handshake check without sending malicious payloads.

## Installation

### Prerequisites
* Java Development Kit (JDK) 21.
* Burp Suite (Community or Professional).
* Gradle.

### Build from Source
1.  Clone the repository:
    ```bash
    git clone https://github.com/tobiasGuta/h2csmuggler-mod.git
    cd h2csmuggler-mod
    ```
2.  Build the JAR file:
    ```bash
    ./gradlew clean jar
    ```
3.  Load into Burp Suite:
    * Navigate to **Extensions** -> **Installed**.
    * Click **Add** -> Select `build/libs/H2CProber.jar`.

## Usage Guide

1.  **Select a Target:** Identify a request to a server behind a Reverse Proxy.
2.  **Launch Probe:** Right-click the request -> **Probe for h2c Tunneling**.
3.  **Check Results:** Open the **"H2C Prober"** tab in Burp Suite.
    * **Success:** You will see `!!! VULNERABILITY CONFIRMED !!!` and `Status: 101 Switching Protocols`.
    * **Failure:** You will see a standard 200 OK or 400 Bad Request.

<img width="1912" height="268" alt="image" src="https://github.com/user-attachments/assets/ddbc2747-d5dd-45dd-a032-f79d7e65c6d7" />

## Exploitation (The "Next Step")

**Note:** This extension is a **Scanner**. It identifies the open door. To walk through that door and actually tunnel traffic (e.g., to access `/private` or `/admin` endpoints), you require a specialized HTTP/2 client that can handle raw socket framing.

I recommend using [`h2csmuggler`](https://github.com/BishopFox/h2csmuggler) by BishopFox, or you can use my version included in this same repository for the actual exploitation phase.

### Workflow
1.  **Identify:** Use this Burp Extension to confirm the target returns a `101 Switching Protocols`.
2.  **Exploit:** Use `h2csmuggler.py` to tunnel a request through that endpoint.

**Example Command:**
If this extension flags `https://vulnerable.com/` as vulnerable:

```bash
python3 h2csmuggler.py -x https://10.65.133.242:8200/ https://10.65.133.242:8200/private --insecure 
```

<img width="1895" height="488" alt="image" src="https://github.com/user-attachments/assets/082aec3f-a4b5-4623-9319-613467b1be99" />

# Support
If my tool helped you land a bug bounty, consider buying me a coffee ☕️ as a small thank-you! Everything I build is free, but a little support helps me keep improving and creating more cool stuff ❤️
---

<div align="center">
  <h3>☕ Support My Journey</h3>
</div>


<div align="center">
  <a href="https://www.buymeacoffee.com/tobiasguta">
    <img src="https://cdn.buymeacoffee.com/buttons/v2/default-yellow.png" width="200" />
  </a>
</div>
