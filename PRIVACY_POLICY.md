# Privacy Policy for DebtTrack

**Effective Date:** June 23, 2026

At DebtTrack, we are committed to protecting your privacy. This Privacy Policy describes how we handle information in our mobile application ("DebtTrack" or "the App"). 

## 1. Summary (Offline-First Commitment)
DebtTrack is designed to be a **100% Offline-First** application. All data processing, transaction storage, and smart SMS parsing occur **locally on your device**. 
- **NO personal data, financial transactions, or SMS contents are sent to external servers or cloud services.**
- **NO background servers are maintained to store or process your personal information.**

---

## 2. Information Collection and Use

### A. SMS Permissions (`READ_SMS` and `RECEIVE_SMS`)
- **What We Process:** If you grant the SMS permission, DebtTrack monitors incoming SMS messages to identify automated alerts from financial services (e.g., banks, transactional utilities, UPI, and debit cards) indicating transactions, debt, or expenses.
- **How It Is Used:** We use local algorithms to extract the merchant/biller, transaction amount, and timestamp, converting them into structured records within your local database.
- **Local Sandbox Storage:** The raw contents of the parsed SMS messages are processed purely in-memory and are never stored anywhere other than as structured transactions in your local, sandboxed SQLite database.
- **No Transmission:** These SMS messages are **never transmitted** over the network.

### B. Notification Listener Permission (`NotificationListenerService`)
- **What We Process:** If enabled by you, our Notification Listener intercepts system notifications from transactional or financial apps (e.g., TrueCaller, Paytm, Google Pay, or banking apps).
- **How It Is Used:** Just like SMS, notification details are parsed locally to log transactions.
- **No Transmission:** Notification details are **never transmitted** over the network and remain strictly local on your device.

### C. Biometric Data (`BiometricManager` / Fingerprint / Face Unlock)
- **What We Process:** DebtTrack supports standard Android Biometric prompt APIs to secure your records.
- **How It Is Used:** The App never accesses, stores, or transmits your actual biometric characteristics (fingerprints or face scans). Authentication is handled entirely by the Android Operating System's secure enclave, which simply returns a success/failure confirmation token to the App.

### D. Device Storage (SQLite/Room Database)
- Your data is stored locally in an encrypted/sandboxed database directory on your device. Clearing app storage or uninstalling the App will completely erase all transactions.

---

## 3. Third-Party Services
DebtTrack does not share any data with third-party tracking tools, advertisers, analytics networks, or cloud providers. 

---

## 4. Children’s Privacy
Our App does not knowingly collect any personal data from anyone, including children under 13, as no data is collected or sent online.

---

## 5. Permissions Revocation
You can revoke SMS permissions, Notification permissions, or Biometric authentication at any time through your Android System Settings. Once revoked, the App will gracefully disable the associated features and operate in manual-entry mode.

---

## 6. Contact Us
If you have any questions or concerns regarding this Privacy Policy or local data storage practices, please contact us at:
- **Email:** sayyedsahilttp@gmail.com
