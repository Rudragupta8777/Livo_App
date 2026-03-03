<div align="center">
  <img src="https://raw.githubusercontent.com/Rudragupta8777/Livo_App/master/app/src/main/res/drawable/livo_readme_logo.png" alt="Livo Logo" width="60%" />

  # Livo: Premium Hotel Booking & Management

  <p align="center">
    <img src="https://img.shields.io/badge/Android-3DDC84?style=for-the-badge&logo=android&logoColor=white" />
    <img src="https://img.shields.io/badge/Kotlin-7F52FF?style=for-the-badge&logo=kotlin&logoColor=white" />
    <img src="https://img.shields.io/badge/Dagger_Hilt-02569B?style=for-the-badge&logo=android&logoColor=white" />
    <img src="https://img.shields.io/badge/Retrofit2-FF6F00?style=for-the-badge&logo=square&logoColor=white" />
    <img src="https://img.shields.io/badge/Cloudinary-3448C5?style=for-the-badge&logo=cloudinary&logoColor=white" />
    <img src="https://img.shields.io/badge/Mapbox-000000?style=for-the-badge&logo=mapbox&logoColor=white" />
    <img src="https://img.shields.io/badge/Razorpay-02042B?style=for-the-badge&logo=razorpay&logoColor=3395FF" />
  </p>

  <p align="center"><b>A secure, seamless bridge between travelers seeking premium stays and managers overseeing property operations.</b></p>
</div>

<h3 align="center">Dark Theme</h3>
<p align="center">
  <img src="https://raw.githubusercontent.com/Rudragupta8777/Livo_App/master/app/src/main/res/drawable/dark_app_readme.png" alt="Livo App Dark Theme Screenshots" width="80%" />
</p>

<br>

<h3 align="center">Light Theme</h3>
<p align="center">
  <img src="https://raw.githubusercontent.com/Rudragupta8777/Livo_App/master/app/src/main/res/drawable/light_app_readme.png" alt="Livo App Light Theme Screenshots" width="80%" />
</p>

## 🌟 Overview

**Livo** is a highly secure, production-ready Android application designed for modern hospitality. Built with a strict **security-first mindset** and MAD (Modern Android Development) architecture, it features a dual-role system. It provides an intuitive, highly animated booking experience for guests and a powerful, data-driven financial dashboard for property managers.

<div align="center">
  <table>
    <tr>
      <td width="50%">
        <h3 align="center">🤵 For Guests (Users)</h3>
        <ul>
          <li>Smart hotel search & discovery via Mapbox</li>
          <li>Advanced booking engine with guest allocation</li>
          <li>Premium UI/UX with fluid object animations</li>
          <li>Secure OTP authentication & session handling</li>
          <li>Seamless & secure checkout via Razorpay</li>
          <li>Track upcoming and past reservations</li>
        </ul>
      </td>
      <td width="50%">
        <h3 align="center">🏢 For Property Managers</h3>
        <ul>
          <li>Complete Hotel & Room CRUD capabilities</li>
          <li>Real-time Financial Dashboard (Revenue/Refunds)</li>
          <li>Comprehensive booking & cancellation oversight</li>
          <li>Cloudinary-integrated property media uploads</li>
          <li>Admin-verified role authorization</li>
        </ul>
      </td>
    </tr>
  </table>
</div>

## ✨ Key Features & Security

### 🔒 Enterprise-Grade Security
Livo is built to withstand vulnerabilities common in mobile applications:
* **Encrypted Storage:** User emails, Access Tokens, and Refresh Tokens are strictly encrypted using Jetpack Security's `MasterKey` (AES256_GCM).
* **Automated Token Refresh:** A synchronized, thread-safe `AuthAuthenticator` intercepts `401 Unauthorized` responses and silently refreshes JWT tokens without disrupting the user.
* **Process Resilience:** Utilizing a custom `PersistentCookieJar` and `SharedPreferences` cache, the app survives background OS RAM kills, ensuring no session drops or lost data when switching apps.

### 🧭 User Experience Flow
1. `Discover` - Dynamic hotel search with geolocation and animated UI transitions.
2. `Book` - Seamless selection of dates using Material Date Range Pickers and interactive room allocation.
3. `Manage` - Property managers can easily add properties, upload images, and control room inventories.
4. `Analyze` - Managers access a real-time analytics grid displaying Total Revenue, Cancellation Rates, and Refund metrics.

## 🛠️ Tech Stack

<div align="center">
  <table>
    <tr>
      <td align="center"><img src="https://cdn.simpleicons.org/kotlin" width="50px"/><br/>Kotlin</td>
      <td align="center"><img src="https://cdn.simpleicons.org/android" width="50px"/><br/>Android SDK</td>
      <td align="center"><img src="https://cdn.simpleicons.org/jsonwebtokens" width="50px"/><br/>JWT Auth</td>
      <td align="center"><img src="https://cdn.simpleicons.org/square" width="50px"/><br/>Retrofit/OkHttp</td>
      <td align="center"><img src="https://cdn.simpleicons.org/cloudinary" width="50px"/><br/>Cloudinary</td>
      <td align="center"><img src="https://cdn.simpleicons.org/mapbox" width="50px"/><br/>Mapbox</td>
      <td align="center"><img src="https://cdn.simpleicons.org/razorpay" width="50px"/><br/>Razorpay</td>
    </tr>
  </table>
</div>

---

## 📱 Application Architecture

### System Architecture
```text
┌─────────────────┐      ┌─────────────────┐      ┌─────────────────┐
│                 │      │                 │      │ Cloudinary /    │
│   Livo Mobile   │◄────►│    Livo API     │◄────►│ Mapbox /        │
│   Application   │      │   (Backend)     │      │ Razorpay APIs   │
│                 │      │                 │      │                 │
└─────────────────┘      └────────┬────────┘      └─────────────────┘
                                  │
                                  ▼
                         ┌─────────────────┐
                         │   Production    │
                         │    Database     │
                         └─────────────────┘


```

### Mobile MVVM Architecture

Livo strictly adheres to the **Model-View-ViewModel (MVVM)** design pattern, powered by Kotlin Coroutines, StateFlow, and Dagger-Hilt.

```text
 ┌────────────────────────────────┐
 │        UI Layer (Views)        │  <-- Activities, Fragments, Adapters (Observes StateFlow)
 └───────────────┬────────────────┘
                 │ (Triggers Events / Observes UiState)
 ┌───────────────▼────────────────┐
 │           ViewModel            │  <-- Manages UI State & Business Logic (StateFlow / Coroutines)
 └───────────────┬────────────────┘
                 │ (Fetches Data)
 ┌───────────────▼────────────────┐
 │      Data Layer (Repository)   │  <-- Single Source of Truth
 └───────┬───────────────┬────────┘
         │               │
 ┌───────▼───────┐ ┌─────▼────────┐
 │   Network     │ │ Local Storage│  <-- Retrofit API / EncryptedSharedPreferences
 └───────────────┘ └──────────────┘


```

---

## 📁 Project Structure

```text
com.livo.works
 ┣ 📂 Api             # Retrofit Interfaces for all network routes
 ┣ 📂 Auth            # Authentication Data & Repositories
 ┣ 📂 Booking         # Guest booking logic, cart, and checkout (Razorpay)
 ┣ 📂 Manager         # Property management, dashboard, and revenue data
 ┣ 📂 Room            # Room creation and inventory management
 ┣ 📂 Search          # Dynamic hotel search and Mapbox integrations
 ┣ 📂 ViewModel       # MVVM ViewModels utilizing StateFlow
 ┣ 📂 di              # Dagger-Hilt Dependency Injection Modules
 ┣ 📂 screens         # UI Layer (Activities, Fragments, Adapters)
 ┣ 📂 security        # TokenManager, Authenticators, Interceptors
 ┗ 📂 util            # Helper classes (Cloudinary, UiState)


```

---

## ⚙️ Implementation Status

| Component | Status | Notes |
| --- | --- | --- |
| **Architecture** | ✅ Complete | Robust MVVM Pattern with StateFlow & Coroutines |
| **Dependency Injection** | ✅ Complete | Fully integrated using Dagger-Hilt |
| **User Authentication** | ✅ Complete | OTP Flow, Token Interceptors, and Automatic Refresh |
| **Booking Engine** | ✅ Complete | Material Date Pickers, Guest Validation, Cart Logic |
| **Payment Gateway** | ✅ Complete | Secure transactions and order processing via Razorpay |
| **Manager Dashboard** | ✅ Complete | Revenue analytics, bookings overview, and hotel CRUD |
| **Media Handling** | ✅ Complete | Image compression and remote Cloudinary URL integration |
| **Process Resilience** | ✅ Complete | Survives background OS kills via persistent secure storage |

---
