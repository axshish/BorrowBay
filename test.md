# 📦 BorrowBay: Comprehensive System Architecture & Design

BorrowBay is a community-centric, peer-to-peer (P2P) rental ecosystem. This document serves as the technical source of truth for the application's architecture, data flow, and integration points.

---

## 🏗️ Eraser.io Architecture Diagram (Diagram-as-Code)

*Copy the block below into [Eraser.io](https://app.eraser.io/)*

```text
title BorrowBay - Feature-Driven MVVM Architecture

direction right

// 📱 Android Application
App [icon: smartphone, color: green] {
  
  // 🧭 Navigation & Orchestration
  Navigation [icon: terminal, color: gray] {
    NavGraph [icon: share-2, label: "Main Navigation Controller"]
  }

  // 🧩 Feature Modules (UI + ViewModel)
  Features [icon: grid, color: purple] {
    Home_Feature [label: "Home (Nearby Search)"]
    Auth_Feature [label: "Auth (Google/Email/OTP)"]
    Listing_Feature [label: "Create Listing (Multi-step)"]
    Profile_Feature [label: "Profile (History/Editing)"]
    Detail_Feature [label: "Item Details & Booking"]
    Registration_Feature [label: "User Registration"]
  }

  // 🛠️ Core & Utils (Shared Logic)
  Core [icon: settings, color: gray] {
    Location_Utils [icon: map-pin]
    Razorpay_Helper [icon: credit-card]
    Supabase_Client [icon: zap]
  }

  // 💾 Data Layer (Repositories & Models)
  Data_Layer [icon: hard-drive, color: blue] {
    Rental_Repo [icon: folder, label: "RentalRepository"]
    User_Repo [icon: folder, label: "UserRepository"]
    Models [icon: file-text, label: "Domain Models (RentalItem, User)"]
  }
}

// ☁️ External Services
Cloud [icon: cloud, color: blue] {
  Firebase [icon: firebase, color: orange] {
    FB_Auth [icon: lock, label: "Identity Management"]
    Firestore [icon: database, label: "Metadata (NoSQL)"]
  }
  Supabase [icon: hard-drive, color: green, label: "Object Storage"]
  Razorpay_API [icon: credit-card, label: "Payment Gateway"]
  OSM_Maps [icon: map, label: "OpenStreetMap (OSMDroid)"]
}

// 🔗 Logic Connections
NavGraph > Features: coordinates transitions
Features > Data_Layer: requests data via ViewModels
Home_Feature > Location_Utils: detects coordinates
Listing_Feature > Supabase_Client: uploads item images
Detail_Feature > Razorpay_Helper: initializes checkout

// 📡 Data Sync Connections
User_Repo <> FB_Auth: manages sessions
Data_Layer <> Firestore: syncs items/user documents
Listing_Feature > Supabase: binary storage (images)
Razorpay_Helper > Razorpay_API: transaction processing
Detail_Feature > OSM_Maps: privacy-centric mapping
```

---

## 📂 1. Layered & Feature-Based Architecture

### 🎨 UI Layer (Jetpack Compose)
- **Feature-Based Packaging**: UI components are grouped by feature (e.g., `features.home`, `features.profile`), promoting modularity and easier maintenance.
- **Theming**: A centralized Material 3 theme (`ui.theme`) ensures a consistent design language across all features.
- **Navigation**: `NavGraph.kt` acts as the brain of the app, managing the backstack and passing arguments (like `productId`) between screens.

### 🧠 ViewModel Layer (MVVM)
ViewModels handle screen-specific state and side effects.
- **State Management**: Uses `MutableStateFlow` to expose reactive UI states (e.g., `HomeUiState`).
- **Context Awareness**: ViewModels interact with `LocationUtils` for geo-fencing and `RazorpayHelper` for financial transactions.
- **Concurrency**: Leverages Kotlin Coroutines for non-blocking I/O operations.

### 💾 Data Layer (Repository Pattern)
Abstracts the source of data from the UI features.
- **`RentalRepository`**: Single point of contact for fetching categories, nearby rentals, and global listings.
- **`UserRepository`**: Handles user profile synchronization, location updates, and session management.

---

## ⚙️ 2. Cloud Integration & Data Lifecycle

### 🔥 Firebase (Core Backend)
- **Firestore**: Our primary NoSQL database. 
    - Real-time updates for rental status changes.
    - Complex queries for nearby search based on latitude/longitude.
- **Auth**: Multi-provider authentication (Google, Email, Phone/OTP).

### ⚡ Supabase (Image Infrastructure)
- **Storage**: Handles all binary assets. 
- **Local-First Pattern**: The app displays local URIs immediately while uploading to Supabase in the background, ensuring a high-performance "feel" for users.
- **Cache Invalidation**: Appends timestamps to public URLs to force cache refreshes when images are updated.

### 💳 Razorpay (Escrow & Payments)
- **Integration**: Secure checkout via the Razorpay Android SDK.
- **Payouts**: Uses the lender's stored `razorpayId` for routing payments and security deposits.

---

## 🛠️ 3. Key Technical Strategies

### 📍 Location & Search
- **Geo-Querying**: BorrowBay calculates distances between user coordinates and item locations to show "Nearby First" results.
- **Reverse Geocoding**: Uses `Geocoder` to convert coordinates into human-readable addresses.

### 🛡️ Privacy & Security
- **OSMDroid Mapping**: Uses OpenStreetMap to show item locations without the heavy tracking overhead of proprietary map providers.
- **Security Rules**: Firestore rules ensure that only item owners can modify their listings.

---

## 📋 4. Technology Stack Summary
- **Language**: Kotlin 1.9.0
- **UI Architecture**: Jetpack Compose (Declarative UI)
- **State Management**: Kotlin Flow & StateFlow
- **Backend**: Firebase Firestore & Auth
- **Storage**: Supabase Storage
- **Payments**: Razorpay SDK
- **Maps**: OSMDroid
- **Image Loading**: Coil
