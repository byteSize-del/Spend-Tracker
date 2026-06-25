# Google Play Console Data Safety Questionnaire Guide

When submitting DebtTrack to the Google Play Console, you must complete the **Data Safety** section. Use the following answers to fill out the questionnaire to accurately represent the offline-first architecture of DebtTrack and resolve any automated warnings regarding SMS permissions.

---

## Section 1: Data Collection and Security

### 1. Does your app collect or share any of the required user data types?
*   **Answer:** **Yes** (Even though the app is offline-first, Google defines "collection" as reading or accessing data from the device, even if processed locally in memory. Since we read SMS messages locally, we must declare "Yes").

### 2. Is all of the user data collected by your app encrypted in transit?
*   **Answer:** **Yes** (Declare "Yes" because we do not transmit any data over the network, so no data is sent unencrypted).

### 3. Do you provide a way for users to request that their data be deleted?
*   **Answer:** **Yes** (Since all data is local, users can delete their data at any time by clearing the app data in Android Settings or using the "Clear local data" option in the Settings screen of the app).

---

## Section 2: Data Types (What Data is "Collected"?)

Select the following data types:

### 1. Personal Info
*   **No** (We do not collect names, email addresses, phone numbers, etc. offline or online).

### 2. Messages
*   **SMS:** Check **Collected** (and **NOT** Shared).
    *   *Why?* The app parses transactional alerts using the SMS permission.

### 3. Financial Info
*   **Financial transaction history:** Check **Collected** (and **NOT** Shared).
    *   *Why?* The app stores transaction history locally in the Room database to track debts and expenses.

---

## Section 3: Data Usage and Handling

For each data type declared above, answer the follow-up questions as follows:

### SMS Messages
1.  **Is this data shared with third parties?**
    *   *Answer:* **No, this data is not shared.**
2.  **Is this data collected, processed, or both?**
    *   *Answer:* **Collected** (which includes local processing in Android API terminology).
3.  **Is this data processed ephemerally?**
    *   *Answer:* **Yes.** The raw SMS message content is processed ephemerally in-memory to extract debt details and is not saved/sent anywhere.
4.  **Is this data required for your app, or can users choose to handle it?**
    *   *Answer:* **Users can choose whether this data is collected.** (Users can toggle the permission off in Settings and manually enter their records).
5.  **Why is this data collected? (Select all that apply)**
    *   *Answer:* Check **App functionality** (to extract transactions and auto-fill records).

### Financial Info (Financial transaction history)
1.  **Is this data shared with third parties?**
    *   *Answer:* **No, this data is not shared.**
2.  **Is this data collected, processed, or both?**
    *   *Answer:* **Collected**.
3.  **Is this data processed ephemerally?**
    *   *Answer:* **No.** The parsed transaction records are saved locally on the user's phone database for long-term tracking.
4.  **Is this data required for your app, or can users choose to handle it?**
    *   *Answer:* **Users can choose whether this data is collected.** (Users can delete transactions or clear database records).
5.  **Why is this data collected? (Select all that apply)**
    *   *Answer:* Check **App functionality** (to display user's expenses/debts over time).

---

## Developer Declaration for Prominent Disclosure
When requesting the `READ_SMS` / `RECEIVE_SMS` permissions on-device, Google requires a prominent in-app disclosure. DebtTrack includes this directly in the onboarding and settings UI to ensure full transparency. 

*Sample description to submit to the Play Console reviewer:*
> "DebtTrack is a utility designed to help users automatically track and categorize their personal debts and expenses by locally analyzing transactional SMS notifications from banks and UPI apps. The app operates 100% offline-first. All SMS parsing and transaction logging occur locally in the Android sandbox environment on the user's device. No data is shared with third parties, uploaded to external servers, or collected for advertising."
