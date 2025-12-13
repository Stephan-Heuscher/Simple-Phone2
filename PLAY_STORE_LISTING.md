# Google Play Store Publication Checklist & Content

This document contains all the information and text you need to publish "Simple Phone" on the Google Play Store.

## 1. The Application Bundle (The Code)

*   **Action:** You need to generate a Signed App Bundle (`.aab`).
*   **How to do it in Android Studio:**
    1.  Go to **Build > Generate Signed Bundle / APK**.
    2.  Select **Android App Bundle**.
    3.  Create a new **Key Store** (keep this file safe! If you lose it, you can't update the app).
    4.  Fill in the details (password, alias, etc.).
    5.  Select **Release** build variant.
    6.  Click **Finish**.
    7.  The `.aab` file will be generated in `app/release/`.

## 2. Store Listing Assets (Visuals)

You need to create these images. Since I cannot generate image files, here are the specifications and descriptions for what you should create:

*   **App Icon:**
    *   **Size:** 512px x 512px (PNG with alpha).
    *   **Design:** A simple, high-contrast phone handset icon. Green background, white phone symbol. Matches the app's theme.
*   **Feature Graphic:**
    *   **Size:** 1024px x 500px (JPEG or PNG, no alpha).
    *   **Design:** A clean banner showing the app running on a phone held by an elderly hand, or just the app interface with the text "Simple Phone: Senior Friendly" in large, readable font.
*   **Phone Screenshots:** (Min 2, Max 8)
    *   **Screenshot 1 (Home):** Show the main screen with large contact photos (use the Demo Mode!). Caption: "Large Photos & Text".
    *   **Screenshot 2 (Dialer):** Show the large number pad. Caption: "Easy to Read Numbers".
    *   **Screenshot 3 (In-Call):** Show the incoming call screen with big "Answer" button. Caption: "No Confusion".
    *   **Screenshot 4 (Settings):** Show the settings with "Huge Text" enabled. Caption: "Customizable for Seniors".

## 3. Store Listing Information (Text)

### App Name
`Simple Phone: Senior Friendly`

### Short Description
`Easy to use phone app for seniors. Large text, photos, and no confusion.`

### Full Description
```text
Simple Phone is a phone dialer designed specifically for seniors and people with poor eyesight or dexterity issues. We believe technology should be accessible to everyone.

**Key Features:**

*   **Large Photos & Text:** Contacts are displayed with large, clear photos and names. No more squinting at tiny lists.
*   **Simple Interface:** No confusing menus or swipe gestures. Everything is one tap away.
*   **Senior Friendly:** High contrast colors and large buttons make it easy to see and use.
*   **Privacy First:** Completely offline. No internet connection required. Your contacts and call history never leave your phone.
*   **No Ads & Free:** We are committed to affordability. This app is completely free and contains no advertisements.
*   **Emergency Ready:** Easy access to emergency contacts.

**Why Simple Phone?**

Modern smartphones can be overwhelming. Simple Phone strips away the complexity and brings back the basic functionality of a phone, but with a modern, accessible interface. It replaces your default dialer with an experience that just works.
```

## 4. Content & Policy Declarations

*   **Privacy Policy URL:** You need to host the `PRIVACY_POLICY.md` file somewhere (e.g., GitHub Pages, Google Sites, or a simple free host).
    *   *URL Example:* `https://your-username.github.io/Simple-Phone/privacy_policy.html`
*   **Content Rating:**
    *   Select "Utility/Productivity" category.
    *   Does the app contain violence? **No**.
    *   Does the app contain sexuality? **No**.
    *   Does the app contain offensive language? **No**.
    *   Does the app allow users to interact? **Yes** (via phone calls, but usually "No" for app-hosted interaction). *Note: Strictly speaking, a dialer facilitates communication, but usually this question refers to social media features. Select "No" for "Does the app allow users to interact or exchange content with other users through the app?" as it uses standard carrier services, not app-specific servers.*
    *   **Result:** PEGI 3 / ESRB E (Everyone).
*   **Target Audience:**
    *   Select **Everyone** (or 3+ / 5+ depending on the options).
    *   Appeal to children? **No**.
*   **News App:** **No**.
*   **COVID-19:** **My app is not a publicly available COVID-19 contact tracing or status app.**
*   **Data Safety:**
    *   Does your app collect or share any of the required user data types? **No**. (Since it's offline).
    *   *Note: Even though you access Contacts, if it never leaves the device (no internet), you typically declare "No" for collection/sharing off-device. However, read the specific wording carefully. If asked if you "access" or "collect" (even locally), say Yes to Contacts, but No to "Shared" and Yes to "Ephemeral/On-device only".*
    *   **Recommendation:** Declare that you collect **Contacts** and **App Info/Performance** (crash logs if you use them, but you don't have internet, so No).
    *   **Purpose:** App Functionality.
*   **Advertising ID:** **No**.
*   **Government Apps:** **No**.

## 5. Categorization & Contact Details

*   **App Category:** `Communication` or `Tools`.
*   **Tags:** `Senior`, `Accessibility`, `Offline`, `Dialer`, `Simple`.
*   **Email:** `your-email@example.com` (Create a dedicated email for this).
*   **Website:** (Optional) Link to your GitHub repo or landing page.

## 6. Pricing & Distribution

*   **Pricing:** **Free**.
*   **Countries:** Select **All countries** (or specific ones if you prefer).
