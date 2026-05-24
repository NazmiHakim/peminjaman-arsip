# AGENTS.md - BPKPAD Document Loan Management (Standalone Android App)

## 🤖 1. Agent Persona & General Directives
You are a Senior Android Engineer expert in Kotlin, Jetpack Compose, Clean Architecture, and Hilt. Your goal is to assist in building a **STANDALONE** Android application for BPKPAD Balangan's Document Loan Management (Modul Peminjaman Dokumen).

This is a **FULL STANDALONE APPLICATION** (single APK). It owns its own Authentication, Master Data, Loan Transactions, Reporting, and Notifications.

### Core Directives
- **Always** think step-by-step before generating code.
- **Always** adhere strictly to Clean Architecture boundaries (Domain, Data, Presentation).
- **Never** put business logic inside UI layer (Compose/Activity) or Data layer.
- **Never** expose Data/Network models (DTOs) directly to UI. Always map to Domain models.
- **Always** log every state-changing action to `AuditLogRepository` — NON-NEGOTIABLE for government compliance.
- **Always** enforce RBAC (Role-Based Access Control) before exposing any action button.
- **Always** check UI Component Registry (Section 5) BEFORE creating new `@Composable` — prioritize reuse.
- Prioritize code readability, immutability, and thread safety (Coroutines/Flow).

## 🎯 2. Project Context

### 2.1 Business Domain
Aplikasi ini melayani **Badan Pengelola Keuangan dan Pendapatan Aset Daerah (BPKPAD) Kabupaten Balangan** untuk mendigitalkan proses peminjaman dokumen fisik (SP2D, Surat Perintah, SPJ, dll) yang sebelumnya dicatat manual di buku.

### 2.2 Core Problems Solved (PIECES Analysis)
- **Performance:** Pencarian dokumen dari 15-30 menit → detik
- **Information:** Riwayat peminjaman terdokumentasi dengan timestamp
- **Economics:** Paperless, mengurangi risiko kehilangan dokumen audit
- **Control:** RBAC Arsiparis ↔ Kasubag + Audit Trail lengkap
- **Efficiency:** Auto-reminder overdue, export PDF/Excel otomatis
- **Service:** QR Code accelerator untuk pengembalian cepat

### 2.3 Actors
| Aktor | Role | Tanggung Jawab |
|---|---|---|
| **Arsiparis** | `arsiparis` | Operator: buat transaksi, upload foto, scan QR, proses pengembalian, bypass, perpanjangan |
| **Kasubag** | `kasubag` | Supervisor: approve/reject, acknowledge bypass, batalkan persetujuan, lihat statistik |

## 🛠 3. Tech Stack & Architecture
- **Language:** Kotlin (min SDK 24, target SDK 34)
- **UI Toolkit:** Jetpack Compose (Material Design 3)
- **Architecture:** Clean Architecture + MVVM (Unidirectional Data Flow)
- **Dependency Injection:** Dagger Hilt
- **Async:** Kotlin Coroutines & StateFlow/SharedFlow
- **Networking:** Retrofit + OkHttp + Kotlinx Serialization
- **Local Storage:** Room Database (offline-first + cache)
- **Session Storage:** DataStore Preferences
- **Image Storage:** Firebase Cloud Storage (surat pengantar, bypass proof)
- **QR Generator:** ZXing (`com.google.zxing`)
- **QR Scanner:** CameraX + ML Kit Barcode Scanning
- **OCR (Optional):** Google ML Kit Text Recognition
- **Push Notification:** Firebase Cloud Messaging (FCM)
- **Background Scheduler:** WorkManager (daily overdue check at 08:00 WITA)
- **Image Loading:** Coil
- **PDF Export:** Android `PdfDocument` API
- **Excel Export:** Apache POI
- **WhatsApp Integration:** Intent URI (`https://api.whatsapp.com/send?phone=...`)
- **Date/Time:** `java.time.*` with core library desugaring

## 📁 4. Directory Structure Enforcement

```
com.bpkpad.peminjaman/
├── core/
│   ├── common/              # ResultState, Constants, Extensions
│   ├── network/             # Retrofit client, AuthInterceptor, ApiResponse
│   ├── database/            # AppDatabase, DAOs, Entities, TypeConverters, DatabaseSeeder
│   ├── storage/             # Firebase Storage wrapper, FileRepository
│   ├── session/             # SessionManager (DataStore), SessionObject
│   ├── notification/        # FCM Service, NotificationHelper, OverdueWorker
│   ├── di/                  # Hilt modules
│   ├── theme/               # BpkpadTheme, colors, typography
│   └── ui/                  # Core reusable UI components (OWNED)
├── auth/                    # 🔐 Login, Logout, Session (Internal)
│   ├── data/                # AuthRepository, AuthApi, DTOs
│   ├── domain/              # LoginUseCase, LogoutUseCase, User model
│   └── presentation/        # LoginScreen, LoginViewModel
├── master/                  # 📚 Master Data (Owned by us)
│   ├── data/                # MasterDokumenRepo, MasterInstansiRepo, DTOs
│   ├── domain/              # Models, UseCases (CRUD)
│   └── presentation/        # ListDokumen, FormDokumen, ListInstansi
├── peminjaman/              # 📋 Loan Transactions (Core Business)
│   ├── data/
│   │   ├── remote/          # TransaksiApi, DTOs
│   │   ├── local/           # TransaksiEntity, DAO
│   │   ├── repository/      # TransaksiRepositoryImpl, AuditLogRepositoryImpl
│   │   └── mapper/          # DTO ↔ Entity ↔ Domain mappers
│   ├── domain/
│   │   ├── model/           # Transaksi, DetailPeminjaman, Perpanjangan, AuditLog
│   │   ├── repository/      # Repository interfaces
│   │   └── usecase/         # CreateTransaksiUseCase, ApproveTransaksiUseCase, etc.
│   └── presentation/
│       ├── dashboard/       # DashboardArsiparis, DashboardKasubag
│       ├── form/            # FormTransaksiBaru
│       ├── detail/          # DetailTransaksi
│       ├── approval/        # AntreanApproval (Kasubag)
│       ├── pengembalian/    # ScanQR, FormPengembalian
│       ├── perpanjangan/    # FormPerpanjangan
│       └── riwayat/         # ListRiwayat + Audit Trail Timeline
├── laporan/                 # 📊 Reporting & Export
│   ├── domain/              # ExportPdfUseCase, ExportExcelUseCase
│   └── presentation/        # FilterLaporan, ExportPreview
├── qr/                      # 📱 QR Code utilities
│   ├── QrGenerator.kt       # ZXing wrapper
│   └── QrScanner.kt         # CameraX + ML Kit wrapper
└── utils/                   # Extensions, Constants, Validators
```

## 🎨 5. UI Component System & Reuse Rules

### 5.1 Component Ownership Classification
Setiap `@Composable` function **WAJIB** diklasifikasikan ke salah satu kategori berikut. AI **HARUS** mengecek ini SEBELUM membuat component baru.

| Kategori | Package Path | Ownership | Aturan |
|---|---|---|---|
| 🔒 **SHARED** | `com.bpkpad.shared.ui.*` | Tim Design System / Tim Lain | **JANGAN MODIFIKASI**. Hanya pakai via import. |
| 🤝 **CONTRACT** | `com.bpkpad.core.ui.contract.*` | Interface bersama | Implementasi di modul kita, signature tidak boleh berubah. |
| ⭐ **OWNED** | `com.bpkpad.peminjaman.core.ui.*` | Tim Peminjaman | Bebas modifikasi, tapi backward-compatible. |
| 🏠 **LOCAL** | `com.bpkpad.peminjaman.{feature}.components.*` | Fitur tertentu | Hanya dipakai di 1 screen/feature. |

### 5.2 Component Discovery Protocol (WAJIB DIIKUTI AI)

**SEBELUM** membuat `@Composable` baru, AI **HARUS** menjalankan checklist ini:

```
□ STEP 1: Cek apakah component sudah ada di SHARED layer?
   → Search: `com.bpkpad.shared.ui.components.*`
   → Jika ADA → PAKAI, jangan buat baru.
   
□ STEP 2: Cek apakah component sudah ada di modul kita (OWNED)?
   → Search: `com.bpkpad.peminjaman.core.ui.*`
   → Jika ADA → REUSE atau EXTEND (jangan duplicate).
   
□ STEP 3: Cek apakah component mirip dengan yang ada tapi butuh variasi?
   → Jika YA → Tambah parameter opsional ke component existing.
   → Jika TIDAK → Lanjut ke Step 4.
   
□ STEP 4: Tentukan scope component baru:
   → Dipakai >2 screen? → Taruh di `core.ui` (OWNED)
   → Dipakai 1 screen? → Taruh di `{feature}.components` (LOCAL)
   
□ STEP 5: Cek COMPATIBILITY (lihat section 5.3)
```

### 5.3 Compatibility Checklist

Saat AI merekomendasikan reuse component, **WAJIB** verifikasi:

```kotlin
// ✅ CHECKLIST COMPATIBILITY
// 1. Signature Match - parameter cocok dengan kebutuhan?
// 2. Theme Compliance - pakai BpkpadTheme / BpkpadColorScheme?
// 3. State Handling - support StateFlow/UiState pattern kita?
// 4. RBAC Aware - bisa conditional render berdasarkan role?
// 5. Accessibility - punya contentDescription / semantics?
// 6. Preview - punya @Preview dengan mock data?
```

### 5.4 UI Component Registry (Living Document)

**AI HARUS maintain daftar ini** saat menambahkan component baru:

#### 🔒 SHARED Components (Milik Tim Lain - READ ONLY)
```kotlin
// Import dari: com.bpkpad.shared.ui.components.*
// JANGAN MODIFIKASI - hanya gunakan

@Composable fun BpkpadTopBar(title: String, onBack: (() -> Unit)? = null)
@Composable fun BpkpadPrimaryButton(text: String, enabled: Boolean = true, onClick: () -> Unit)
@Composable fun BpkpadSecondaryButton(text: String, onClick: () -> Unit)
@Composable fun BpkpadTextField(value: String, onValueChange: (String) -> Unit, label: String, error: String? = null)
@Composable fun BpkpadCard(content: @Composable () -> Unit)
@Composable fun BpkpadLoadingIndicator()
@Composable fun BpkpadErrorView(message: String, onRetry: () -> Unit)
@Composable fun BpkpadEmptyState(message: String, icon: ImageVector? = null)
```

#### ⭐ OWNED Components (Milik Modul Kita - BISA DIMODIFIKASI)
```kotlin
// Import dari: com.bpkpad.peminjaman.core.ui.*

@Composable fun TransaksiCard(transaksi: Transaksi, showOverdueBadge: Boolean = false, onCardClick: () -> Unit, onActionClick: ((TransaksiAction) -> Unit)? = null)
@Composable fun StatusBadge(status: TransaksiStatus)
@Composable fun AuditTimelineItem(log: AuditLog)
@Composable fun DokumenListItem(detail: DetailPeminjaman, onConditionSelect: ((Kondisi) -> Unit)? = null)
@Composable fun QrCodeDisplay(token: String, size: Dp = 200.dp)
@Composable fun FotoSuratViewer(path: String, onZoom: () -> Unit)
@Composable fun BypassIndicator(isAcknowledged: Boolean)
@Composable fun RoleBasedContent(arsiparisContent: @Composable () -> Unit, kasubagContent: @Composable () -> Unit)
@Composable fun InstansiAutocompleteField(value: String, onValueChange: (String) -> Unit, suggestions: List<Instansi>)
@Composable fun KondisiSelectionDialog(onConfirm: (Map<Int, Kondisi>) -> Unit, onDismiss: () -> Unit)
```

#### 🏠 LOCAL Components (Spesifik Feature)
```kotlin
// presentation/form/components/
@Composable fun DokumenSearchResultItem(dokumen: MasterDokumen, onClick: () -> Unit)

// presentation/pengembalian/components/
@Composable fun QrScannerOverlay()
```

### 5.5 Comment Convention untuk UI Components

**WAJIB** tambahkan header comment ini di setiap `@Composable` baru:

```kotlin
/**
 * [OWNED] TransaksiCard
 * 
 * Ownership: Modul Peminjaman (Tim Kami)
 * Scope: Global (dipakai di Dashboard, Riwayat, Detail)
 * Theme: BpkpadTheme compliant
 * RBAC: Neutral (tidak role-specific)
 * 
 * Changelog:
 * - v1.0 (2026-05-01): Initial version
 * - v1.1 (2026-05-10): Added onActionClick callback
 * - v1.2 (2026-05-20): Added showOverdueBadge parameter
 * 
 * Dependencies:
 * - com.bpkpad.shared.ui.components.BpkpadCard (SHARED)
 * - com.bpkpad.shared.ui.components.BpkpadPrimaryButton (SHARED)
 */
@Composable
fun TransaksiCard(
    transaksi: Transaksi,
    showOverdueBadge: Boolean = false,
    onCardClick: () -> Unit,
    onActionClick: ((TransaksiAction) -> Unit)? = null
) {
    // Implementasi...
}

@Preview(showBackground = true, name = "Normal State")
@Composable
private fun TransaksiCard_Preview_Normal() {
    BpkpadTheme { TransaksiCard(transaksi = PreviewData.transaksiNormal, onCardClick = {}) }
}
```

### 5.6 AI Decision Flow untuk UI Tasks

```
User minta component baru
         │
         ▼
┌─────────────────────────┐
│ Apakah ada di SHARED?   │──YA──▶ "Pakai [SharedComponent]"
└─────────────────────────┘
         │ TIDAK
         ▼
┌─────────────────────────┐
│ Apakah ada di OWNED     │──YA──▶ "Reuse [OwnedComponent]. 
│ yang mirip?             │        Perlu tambah parameter?"
└─────────────────────────┘
         │ TIDAK
         ▼
┌─────────────────────────┐
│ Butuh di >2 screen?     │──YA──▶ Buat di core.ui
└─────────────────────────┘
         │ TIDAK
         ▼
Buat di {feature}.components (LOCAL)
```

### 5.7 Anti-Pattern yang HARUS Ditolak AI

```kotlin
// ❌ ANTI-PATTERN 1: Duplicate SHARED component
@Composable
fun MyCustomButton(text: String, onClick: () -> Unit) { /* ... */ }
// ✅ SOLUSI: Pakai BpkpadPrimaryButton dari SHARED

// ❌ ANTI-PATTERN 2: Hardcode color/typography
@Composable
fun StatusTag(status: String) {
    Text(status, color = Color.Red, fontSize = 14.sp)  // ❌ Hardcode!
}
// ✅ SOLUSI: Pakai BpkpadColorScheme & BpkpadTypography

// ❌ ANTI-PATTERN 3: Component tanpa @Preview
// ✅ SOLUSI: Wajib ada minimal 1 @Preview

// ❌ ANTI-PATTERN 4: Modify SHARED component
// ✅ SOLUSI: Buat wrapper di OWNED layer

// ❌ ANTI-PATTERN 5: LOCAL component dipakai lintas feature
// ✅ SOLUSI: Pindahkan ke core.ui (OWNED)
```

### 5.8 Versioning & Breaking Changes

```kotlin
// ✅ BACKWARD-COMPATIBLE: Tambah param dengan default value
@Composable
fun TransaksiCard(
    transaksi: Transaksi,
    onCardClick: () -> Unit,
    showOverdueBadge: Boolean = false  // New param with default
)

// ✅ BREAKING CHANGE: Gunakan @Deprecated + overload
@Deprecated("Use new signature with showOverdueBadge", ReplaceWith("..."))
@Composable
fun TransaksiCard(transaksi: Transaksi, onCardClick: () -> Unit) {
    TransaksiCard(transaksi, onCardClick, false)
}
```

## 🚀 6. Development Phases & Execution Guide

### Phase 1: Foundation & Base Setup
1. Setup `BpkpadApplication` with `@HiltAndroidApp`.
2. Define `ResultState<T>` (sealed class: Loading, Success, Error).
3. Setup Hilt Modules: `NetworkModule`, `DatabaseModule`, `FirebaseModule`, `RepositoryModule`.
4. Setup `SessionManager` (DataStore) dan `AuthInterceptor` (OkHttp).
5. Setup `BpkpadTheme`, typography, dan core UI components.
6. Setup `DatabaseSeeder` untuk dummy data (debug only).

### Phase 2: Domain Layer (The Core)
*Rule: ZERO Android Framework dependencies.*
1. Define pure Kotlin data classes: `Transaksi`, `DetailPeminjaman`, `Perpanjangan`, `AuditLog`, `MasterDokumen`, `Instansi`, `User`.
2. Define `enum class`: `TransaksiStatus`, `DokumenStatus`, `KondisiPengembalian`, `UserRole`, `MetodePersetujuan`, `AuditAction`.
3. Define Repository interfaces.
4. Create UseCases — setiap UseCase state-changing **MUST** call `AuditLogRepository.log()`.
5. All UseCases return `Flow<ResultState<T>>` or `ResultState<T>`.

### Phase 3: Data Layer (Implementation)
*Rule: Map everything. Remote/Local → Domain Model.*
1. Define DTOs for API communication.
2. Define Room Entities with proper relations (`@Relation`, `@ForeignKey`).
3. Implement Retrofit interfaces.
4. Implement `FileRepositoryImpl` using Firebase Storage SDK (compress images before upload).
5. Implement all Repository implementations with proper transaction handling (`@Transaction`).

### Phase 4: Presentation Layer (UI & ViewModel)
*Rule: Stateless UI, Hoist State, UDF.*
1. **ViewModels:** `@HiltViewModel`, expose state via `StateFlow`, handle user intents via sealed `UiEvent`.
2. **Compose UI:**
    - Separate `Screen` (stateful) dan `Content` (stateless) composables.
    - Example: `DashboardScreen(vm) → DashboardContent(state, onEvent)`.
    - Never pass ViewModel into child composables.
3. Use Jetpack Navigation Compose with type-safe routes.
4. Always show Loading state, Error state with retry, dan Empty state where applicable.

### Phase 5: Feature Pipelines

#### 5.1 Loan Creation Pipeline (UC-01, UC-02, UC-03)
1. `FormTransaksiScreen` → fill instansi, PIC, surat, tenggat.
2. Capture photo via `CameraX` → upload to Firebase Storage → get URL.
3. Search master dokumen manually → select → add to list.
4. Submit → `CreateTransaksiUseCase` → save to Room + POST API + `AuditLog(TRANSAKSI_DIBUAT)`.
5. Trigger FCM to Kasubag devices.

#### 5.2 Approval Pipeline (UC-05)
1. Kasubag buka antrean Pending.
2. Approve → generate QR token via ZXing → status `disetujui` → `AuditLog(DISETUJUI_ONLINE)`.
3. Reject → wajib isi alasan → status `ditolak` → `AuditLog(DITOLAK)`.

#### 5.3 Bypass Pipeline (UC-06)
1. Arsiparis clicks "Bypass Persetujuan" on pending transaksi.
2. Capture photo of physical ACC memo → upload to Firebase.
3. Fill catatan bypass (mandatory).
4. `BypassTransaksiUseCase` → status = `disetujui`, `metode_persetujuan = bypass`, `is_bypass_acknowledged = false`.
5. `AuditLog(DISETUJUI_BYPASS)`.
6. Later, Kasubag clicks "Ketahui (Acknowledge)" → `is_bypass_acknowledged = true` → `AuditLog(BYPASS_DIAKUI_KASUBAG)`.

#### 5.4 QR-Based Return Pipeline (UC-07, UC-08)
1. `ScanQrScreen` (CameraX + ML Kit) → detect QR token.
2. `FindTransaksiByQrTokenUseCase` → lookup transaksi.
3. Navigate to `DetailTransaksiScreen`.
4. Arsiparis clicks "Selesaikan Peminjaman" → dialog shows list of dokumen.
5. Arsiparis picks condition (Baik/Rusak/Hilang) per dokumen.
6. If Rusak/Hilang → wajib fill catatan.
7. `ReturnTransaksiUseCase` → update transaksi + update master status + `AuditLog(DIKEMBALIKAN_*)`.

#### 5.5 Perpanjangan Pipeline (UC-09)
1. Arsiparis clicks "Perpanjang" on borrowed transaksi.
2. Pick new date (must be > today).
3. Capture new surat pengantar photo (mandatory).
4. Fill alasan.
5. `CreatePerpanjanganUseCase` → status = `pending`, wait for Kasubag.
6. Kasubag approves/rejects from tab "Perpanjangan".

## 📋 7. Business Rules & State Machine

### 7.1 Transaksi State Machine (STRICT)
```
[menunggu_persetujuan] 
    ├── Approve        → [disetujui]
    │                      ├── Konfirmasi Serah → [dipinjam]
    │                      │                        ├── Return       → [dikembalikan] (terminal)
    │                      │                        ├── Perpanjang   → (new Perpanjangan entity)
    │                      │                        └── Overdue      → (triggers WorkManager reminder)
    │                      └── Cancel by Kasubag → [dibatalkan] (terminal, master unlock)
    ├── Reject         → [ditolak] (terminal, dead-end)
    ├── Bypass         → [disetujui] (flag bypass, butuh acknowledge)
    └── Cancel         → [dibatalkan] (terminal)
```

### 7.2 Perpanjangan State Machine
```
[pending] 
    ├── Approve → [approved] (tenggat diupdate di parent transaksi)
    └── Reject  → [rejected] (tenggat tetap, dead-end)
```

### 7.3 Mandatory Validations
- `tanggal_kembali_rencana` **MUST** be > `tanggal_pinjam`
- `pic_no_hp` **MUST** be numeric, min 10 digits
- `foto_surat_pengantar_path` **MUST** be non-null for new transactions
- `alasan_penolakan` **MUST** be filled when status = `ditolak`
- `bukti_bypass_path` + `catatan_bypass` **MUST** be filled when bypass
- `catatan_kondisi` **MUST** be filled when kondisi = `rusak` or `hilang`
- `foto_surat_perpanjangan_path` **MUST** be filled for perpanjangan
- QR Token is **One-Time Use** — invalid after transaksi = `dikembalikan`

## 👥 8. RBAC Matrix (Enforced at UI & UseCase Level)

| Action | Arsiparis | Kasubag | UseCase |
|---|---|---|---|
| Login/Logout | ✅ | ✅ | `LoginUseCase` |
| Dashboard Ringkasan | ✅ (operasional) | ✅ (statistik + grafik) | - |
| CRUD Master Instansi | ✅ | ❌ | `CreateInstansiUseCase` |
| CRUD Master Dokumen | ✅ | ✅ (view only) | `CreateDokumenUseCase` |
| Buat Transaksi Peminjaman | ✅ | ❌ | `CreateTransaksiUseCase` |
| Upload Foto Surat | ✅ | ❌ | - |
| Edit Pengajuan (Pending) | ✅ | ❌ | `EditTransaksiUseCase` |
| Batal Transaksi (Pending/Approved) | ✅ | ✅ (override) | `CancelTransaksiUseCase` |
| Approve/Reject Peminjaman | ❌ | ✅ | `ApproveTransaksiUseCase` |
| Approve/Reject Perpanjangan | ❌ | ✅ | `ApprovePerpanjanganUseCase` |
| Bypass Persetujuan | ✅ | ❌ | `BypassTransaksiUseCase` |
| Acknowledge Bypass | ❌ | ✅ | `AcknowledgeBypassUseCase` |
| Scan QR Pengembalian | ✅ | ❌ | `FindTransaksiByQrTokenUseCase` |
| Selesaikan Peminjaman | ✅ | ❌ | `ReturnTransaksiUseCase` |
| Ajukan Perpanjangan | ✅ | ❌ | `CreatePerpanjanganUseCase` |
| Kirim WhatsApp (Struk/Reminder) | ✅ | ❌ | - |
| Lihat Audit Trail | ✅ | ✅ | - |
| Export PDF/Excel | ✅ | ✅ | `ExportPdfUseCase`, `ExportExcelUseCase` |

**Enforcement Pattern:**
```kotlin
@Composable
fun ApproveButton(onClick: () -> Unit) {
    val session by sessionManager.session.collectAsState()
    if (session?.role == UserRole.KASUBAG) {
        BpkpadPrimaryButton("Approve", onClick)
    }
    // Else: do not render at all (not just disable)
}
```

## 📝 9. Audit Trail Enforcement (CRITICAL)

**EVERY state-changing UseCase MUST call AuditLogRepository.** No exceptions.

### 9.1 Audit Action Codes
| Code | Trigger | Actor |
|---|---|---|
| `LOGIN` | User berhasil login | All |
| `LOGOUT` | User logout | All |
| `TRANSAKSI_DIBUAT` | New loan created | Arsiparis |
| `TRANSAKSI_DIEDIT` | Edit pending | Arsiparis |
| `PENGAJUAN_DIKIRIM` | Submit to Kasubag | Arsiparis |
| `DISETUJUI_ONLINE` | Kasubag approves via app | Kasubag |
| `DISETUJUI_BYPASS` | Arsiparis bypass with proof | Arsiparis |
| `BYPASS_DIAKUI_KASUBAG` | Kasubag acknowledge bypass | Kasubag |
| `DITOLAK` | Kasubag rejects | Kasubag |
| `DOKUMEN_DISERAHKAN` | Physical handover confirmed | Arsiparis |
| `PERPANJANGAN_DIAJUKAN` | Perpanjangan submitted | Arsiparis |
| `PERPANJANGAN_DISETUJUI` | Kasubag approves perpanjangan | Kasubag |
| `PERPANJANGAN_DITOLAK` | Kasubag rejects perpanjangan | Kasubag |
| `DIKEMBALIKAN_BAIK` | Return with baik condition | Arsiparis |
| `DIKEMBALIKAN_RUSAK` | Return with rusak condition | Arsiparis |
| `DIKEMBALIKAN_HILANG` | Return with hilang condition | Arsiparis |
| `DIBATALKAN` | Cancelled | Arsiparis/Kasubag |
| `MASTER_DOKUMEN_DITAMBAH` / `_DIEDIT` | Master CRUD | Arsiparis |
| `MASTER_INSTANSI_DITAMBAH` / `_DIEDIT` | Master CRUD | Arsiparis |

### 9.2 Enforcement Pattern in UseCase
```kotlin
class ApproveTransaksiUseCase(
    private val transaksiRepo: TransaksiRepository,
    private val masterRepo: MasterDokumenRepository,
    private val auditRepo: AuditLogRepository
) {
    suspend operator fun invoke(transaksiId: Int, approverId: Int, catatan: String?): ResultState<Unit> {
        return try {
            // 1. Atomic transaction
            transaksiRepo.approveInTransaction(transaksiId, approverId)
            
            // 2. Update master dokumen status to DIPINJAM
            val details = transaksiRepo.getDetails(transaksiId)
            details.forEach { masterRepo.updateStatus(it.dokumenId, DokumenStatus.DIPINJAM) }
            
            // 3. MANDATORY audit log
            auditRepo.log(
                transaksiId = transaksiId,
                userId = approverId,
                aksi = AuditAction.DISETUJUI_ONLINE,
                detail = "Status berubah dari menunggu_persetujuan ke disetujui",
                catatan = catatan
            )
            
            ResultState.Success(Unit)
        } catch (e: Exception) {
            ResultState.Error(e.message ?: "Gagal menyetujui transaksi")
        }
    }
}
```

## 🎯 10. Background Tasks

### 10.1 Daily Overdue Checker (WorkManager)
- **Schedule:** Every day at 08:00 WITA
- **Logic:** Find all transaksi where `status = dipinjam` AND `tanggal_kembali_rencana < today`
- **Action:** Trigger local notification to Arsiparis with deep link to overdue list
- **Implementation:** `OverdueWorker` with `PeriodicWorkRequest`

### 10.2 FCM Push for New Approval
- **Trigger:** When Arsiparis submits new transaksi (status = `menunggu_persetujuan`)
- **Target:** All devices with Kasubag role
- **Payload:** Transaksi ID, Instansi name, PIC name
- **Action:** Notification → deep link to `DetailTransaksiScreen`

### 10.3 FCM Push for Perpanjangan Approval
- **Trigger:** When Arsiparis submits perpanjangan
- **Target:** All Kasubag devices
- **Action:** Notification → deep link to Antrean Perpanjangan

## ⚠️ 11. Strict Coding Conventions (Aligned with Other Teams)

- **Naming:** `CamelCase` for classes/composables, `camelCase` for variables/functions. UseCases must end with `UseCase` (verb-noun pattern).
- **ResultState:** Use `ResultState<T>` sealed class (Loading, Success, Error) — **do not use** `Result<T>` or custom wrappers.
- **Compose Preview:** Always add `@Preview` to stateless components with mock data.
- **Error Handling:** Never swallow exceptions. Always catch in Repository/UseCase and emit `ResultState.Error` with meaningful messages.
- **Hardcoding:** No hardcoded strings in Compose. Use `stringResource(R.string.x)`.
- **Secrets:** API endpoints/keys from `BuildConfig`. Never commit Firebase service account JSON.
- **Date Handling:** Always use `java.time.LocalDate` / `LocalDateTime`. Never `java.util.Date`.
- **Audit Log:** Every state-changing operation **MUST** call `AuditLogRepository.log()`.
- **RBAC Check:** Always verify `session.role` before showing role-specific UI.
- **Atomic Operations:** Use Room `@Transaction` for operations involving multiple tables (Transaksi + Detail + Audit + Master).
- **UI Component Reuse:** Always check Registry (Section 5.4) before creating new `@Composable`.

## 🧪 12. Testing Strategy (Aligned with Other Teams)

### 12.1 Domain/UseCases
- Pure JUnit tests
- Mock repositories using **MockK**
- Verify business rules (date validation, RBAC, mandatory fields)
- **Verify audit log is called** for state-changing operations

### 12.2 ViewModels
- Test Coroutines using `runTest` dan `TestDispatcher`
- Assert `StateFlow` emissions using **Turbine**
- Test RBAC-based UI state differences

### 12.3 Data Layer
- Unit tests for Mappers (DTO ↔ Entity ↔ Domain)
- Room in-memory database tests for DAOs
- Mock Retrofit responses

### 12.4 Presentation/Compose
- UI tests for stateless components using `compose-test-rule`
- Verify semantic nodes, text existence, click behavior
- Test RBAC: Arsiparis should NOT see Approve button; Kasubag should NOT see FAB

### 12.5 End-to-End Scenarios (with Dummy Data)
1. Login Arsiparis (`budi/budi123`) → Create transaksi → Logout
2. Login Kasubag (`siti/siti123`) → Approve → Logout
3. Login Arsiparis → Konfirmasi serah → Scan QR → Return
4. Login Arsiparis → Bypass → Login Kasubag → Acknowledge
5. Login Arsiparis → Perpanjangan → Login Kasubag → Approve perpanjangan
6. Test Overdue reminder (Transaksi #1)
7. Test Rejected dead-end (Transaksi #5)

## 📦 13. Database Schema (SQL DDL)

```sql
-- Tabel Users
CREATE TABLE users(
    id INT AUTO_INCREMENT PRIMARY KEY,
    username VARCHAR(50) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    nama_lengkap VARCHAR(100) NOT NULL,
    nip VARCHAR(20) UNIQUE,
    role ENUM('arsiparis','kasubag') NOT NULL,
    no_hp VARCHAR(20),
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_users_username(username),
    INDEX idx_users_role(role)
);

-- Tabel Instansi Peminjam
CREATE TABLE instansi_peminjam(
    id INT AUTO_INCREMENT PRIMARY KEY,
    nama_instansi VARCHAR(150) NOT NULL UNIQUE,
    alamat VARCHAR(255),
    kode_instansi VARCHAR(20) UNIQUE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_instansi_nama(nama_instansi)
);

-- Tabel Master Dokumen (Owned by this standalone app)
CREATE TABLE master_dokumen(
    id INT AUTO_INCREMENT PRIMARY KEY,
    nomor_dokumen VARCHAR(100) NOT NULL UNIQUE,
    perihal VARCHAR(255) NOT NULL,
    nominal DECIMAL(15,2) DEFAULT 0,
    tahun VARCHAR(4) NOT NULL,
    jenis_dokumen VARCHAR(50) NOT NULL,
    status ENUM('tersedia','dipinjam','rusak','hilang') NOT NULL DEFAULT 'tersedia',
    lokasi_rak VARCHAR(50),
    lokasi_box VARCHAR(50),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_dokumen_nomor(nomor_dokumen),
    INDEX idx_dokumen_status(status),
    INDEX idx_dokumen_tahun(tahun),
    INDEX idx_dokumen_jenis(jenis_dokumen)
);

-- Tabel Transaksi Peminjaman
CREATE TABLE transaksi_peminjaman(
    id INT AUTO_INCREMENT PRIMARY KEY,
    instansi_peminjam_id INT NOT NULL,
    pic_nama VARCHAR(100) NOT NULL,
    pic_no_hp VARCHAR(20) NOT NULL,
    nomor_surat_pengantar VARCHAR(100) NOT NULL,
    foto_surat_pengantar_path VARCHAR(500) NOT NULL,
    qr_code_token VARCHAR(255) UNIQUE,
    tanggal_pinjam DATE NOT NULL,
    tanggal_kembali_rencana DATE NOT NULL,
    tanggal_kembali_aktual DATE,
    status ENUM('menunggu_persetujuan','disetujui','ditolak','dipinjam','dikembalikan','dibatalkan') NOT NULL DEFAULT 'menunggu_persetujuan',
    metode_persetujuan ENUM('online','bypass'),
    bukti_bypass_path VARCHAR(500),
    catatan_bypass TEXT,
    is_bypass_acknowledged BOOLEAN NOT NULL DEFAULT FALSE,
    alasan_penolakan TEXT,
    created_by INT NOT NULL,
    approved_by INT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY(instansi_peminjam_id) REFERENCES instansi_peminjam(id),
    FOREIGN KEY(created_by) REFERENCES users(id),
    FOREIGN KEY(approved_by) REFERENCES users(id),
    INDEX idx_transaksi_instansi(instansi_peminjam_id),
    INDEX idx_transaksi_status(status),
    INDEX idx_transaksi_tanggal_pinjam(tanggal_pinjam),
    INDEX idx_transaksi_tanggal_kembali(tanggal_kembali_rencana),
    INDEX idx_transaksi_created_by(created_by),
    INDEX idx_transaksi_qr_token(qr_code_token)
);

-- Tabel Detail Peminjaman
CREATE TABLE detail_peminjaman(
    id INT AUTO_INCREMENT PRIMARY KEY,
    transaksi_id INT NOT NULL,
    dokumen_id INT NOT NULL,
    nomor_dokumen VARCHAR(100) NOT NULL,
    perihal_dokumen VARCHAR(255),
    tahun_dokumen VARCHAR(4),
    lokasi_rak VARCHAR(100),
    kondisi_pengembalian ENUM('baik','rusak','hilang'),
    catatan_kondisi TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY(transaksi_id) REFERENCES transaksi_peminjaman(id),
    INDEX idx_detail_transaksi(transaksi_id),
    INDEX idx_detail_dokumen(dokumen_id)
);

-- Tabel Perpanjangan
CREATE TABLE perpanjangan(
    id INT AUTO_INCREMENT PRIMARY KEY,
    transaksi_id INT NOT NULL,
    tanggal_kembali_lama DATE NOT NULL,
    tanggal_kembali_baru DATE NOT NULL,
    foto_surat_perpanjangan_path VARCHAR(255) NOT NULL,
    alasan TEXT NOT NULL,
    status ENUM('pending','approved','rejected') NOT NULL DEFAULT 'pending',
    alasan_penolakan TEXT,
    created_by INT NOT NULL,
    approved_by INT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY(transaksi_id) REFERENCES transaksi_peminjaman(id),
    FOREIGN KEY(created_by) REFERENCES users(id),
    FOREIGN KEY(approved_by) REFERENCES users(id),
    INDEX idx_perpanjangan_transaksi(transaksi_id)
);

-- Tabel Audit Log
CREATE TABLE audit_log(
    id INT AUTO_INCREMENT PRIMARY KEY,
    transaksi_id INT NOT NULL,
    user_id INT NOT NULL,
    aksi VARCHAR(50) NOT NULL,
    detail TEXT,
    catatan TEXT,
    timestamp TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY(transaksi_id) REFERENCES transaksi_peminjaman(id),
    FOREIGN KEY(user_id) REFERENCES users(id),
    INDEX idx_audit_transaksi(transaksi_id),
    INDEX idx_audit_user(user_id),
    INDEX idx_audit_aksi(aksi),
    INDEX idx_audit_timestamp(timestamp)
);
```

## 🌱 14. Dummy Data Seeder (Kotlin Room)

Simpan sebagai `core/database/DatabaseSeeder.kt`. Hanya berjalan di debug build.

**Test Credentials:**
- `budi` / `budi123` (Arsiparis)
- `siti` / `siti123` (Kasubag)
- `andi` / `andi123` (Arsiparis)

```kotlin
package com.bpkpad.peminjaman.core.database

import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import java.time.LocalDate

class DatabaseSeeder(
    private val db: AppDatabase,
    private val scope: CoroutineScope
) : RoomDatabase.Callback() {

    override fun onCreate(db: SupportSQLiteDatabase) {
        super.onCreate(db)
        scope.launch { seedAll() }
    }

    private suspend fun seedAll() {
        seedUsers()
        seedInstansi()
        seedMasterDokumen()
        seedTransaksi()
        seedAuditLog()
    }

    private suspend fun seedUsers() {
        db.userDao().insertAll(listOf(
            UserEntity(1, "budi", hashPw("budi123"), "Budi Santoso", "198501012010011001", "arsiparis", "081234567890", true),
            UserEntity(2, "siti", hashPw("siti123"), "Siti Aminah, S.AP", "198002022005012002", "kasubag", "081298765432", true),
            UserEntity(3, "andi", hashPw("andi123"), "Andi Wijaya", "199003032015011003", "arsiparis", "081355566677", true)
        ))
    }

    private suspend fun seedInstansi() {
        db.instansiDao().insertAll(listOf(
            InstansiEntity(1, "Dinas Pendidikan Balangan", "Jl. Merdeka No. 1, Paringin", "DINDIK"),
            InstansiEntity(2, "Dinas Kesehatan Balangan", "Jl. Sehat No. 2, Paringin", "DINKES"),
            InstansiEntity(3, "Badan Perencanaan Daerah", "Jl. Pembangunan No. 3", "BAPPEDA"),
            InstansiEntity(4, "Inspektorat Daerah", "Jl. Pengawasan No. 4", "INSPEKTORAT"),
            InstansiEntity(5, "Dinas Pekerjaan Umum", "Jl. Karya No. 5", "DPU")
        ))
    }

    private suspend fun seedMasterDokumen() {
        db.masterDokumenDao().insertAll(listOf(
            // DIPINJAM (terkait Transaksi #1 Overdue)
            MasterDokumenEntity(101, "SP2D-2023-001", "SP2D Belanja Modal Dindik", 15000000.0, "2023", "SP2D", "dipinjam", "Rak A", "Box 2023-01"),
            // TERSEDIA
            MasterDokumenEntity(102, "SP2D-2023-002", "SP2D Belanja Jasa Dinkes", 25000000.0, "2023", "SP2D", "tersedia", "Rak A", "Box 2023-02"),
            MasterDokumenEntity(103, "SP-2024-055", "Surat Perintah Tugas Bappeda", 0.0, "2024", "Surat Perintah", "tersedia", "Rak B", "Box 2024-01"),
            MasterDokumenEntity(104, "SPJ-2024-112", "SPJ Perjalanan Dinas Inspektorat", 8500000.0, "2024", "SPJ", "tersedia", "Rak C", "Box 2024-05"),
            MasterDokumenEntity(105, "SP2D-2024-088", "SP2D Belanja Pegawai DPU", 120000000.0, "2024", "SP2D", "tersedia", "Rak A", "Box 2024-08"),
            // DIPINJAM (terkait Transaksi #4 Normal)
            MasterDokumenEntity(106, "SP2D-2025-012", "SP2D Belanja Barang Dinkes", 7500000.0, "2025", "SP2D", "dipinjam", "Rak A", "Box 2025-01"),
            // RUSAK (riwayat)
            MasterDokumenEntity(107, "SP-2022-033", "Surat Perintah Lama", 0.0, "2022", "Surat Perintah", "rusak", "Rak D", "Box 2022-03"),
            // HILANG (riwayat)
            MasterDokumenEntity(108, "SPJ-2021-007", "SPJ Kegiatan 2021", 3000000.0, "2021", "SPJ", "hilang", "-", "-"),
            // TERSEDIA
            MasterDokumenEntity(109, "SP2D-2025-025", "SP2D Honorarium Guru", 45000000.0, "2025", "SP2D", "tersedia", "Rak A", "Box 2025-02"),
            MasterDokumenEntity(110, "SP2D-2025-030", "SP2D Belanja Modal Jalan", 250000000.0, "2025", "SP2D", "tersedia", "Rak A", "Box 2025-03")
        ))
    }

    private suspend fun seedTransaksi() {
        val today = LocalDate.now()
        
        db.transaksiDao().insertAll(listOf(
            // 🚨 SKENARIO 1: OVERDUE (4 hari) - trigger WorkManager
            TransaksiEntity(
                id = 1, instansiPeminjamId = 1, picNama = "Pak Joko Widodo", picNoHp = "628111222333",
                nomorSuratPengantar = "005/DINDIK/V/2026", fotoSuratPengantarPath = "surat_1.jpg",
                qrCodeToken = "QR-OVERDUE-001",
                tanggalPinjam = today.minusDays(10), tanggalKembaliRencana = today.minusDays(4),
                tanggalKembaliAktual = null, status = "dipinjam", metodePersetujuan = "online",
                buktiBypassPath = null, catatanBypass = null, isBypassAcknowledged = false,
                alasanPenolakan = null, createdBy = 1, approvedBy = 2
            ),
            // ⏳ SKENARIO 2: PENDING - antrean Kasubag
            TransaksiEntity(
                id = 2, instansiPeminjamId = 2, picNama = "Dr. Andi Rahman", picNoHp = "628222333444",
                nomorSuratPengantar = "010/DINKES/V/2026", fotoSuratPengantarPath = "surat_2.jpg",
                qrCodeToken = null,
                tanggalPinjam = today, tanggalKembaliRencana = today.plusDays(7),
                tanggalKembaliAktual = null, status = "menunggu_persetujuan", metodePersetujuan = null,
                buktiBypassPath = null, catatanBypass = null, isBypassAcknowledged = false,
                alasanPenolakan = null, createdBy = 1, approvedBy = null
            ),
            // ⚡ SKENARIO 3: BYPASS - perlu acknowledge Kasubag
            TransaksiEntity(
                id = 3, instansiPeminjamId = 3, picNama = "Ibu Rina Susanti", picNoHp = "628333444555",
                nomorSuratPengantar = "015/BAPPEDA/V/2026", fotoSuratPengantarPath = "surat_3.jpg",
                qrCodeToken = "QR-BYPASS-003",
                tanggalPinjam = today, tanggalKembaliRencana = today.plusDays(5),
                tanggalKembaliAktual = null, status = "disetujui", metodePersetujuan = "bypass",
                buktiBypassPath = "memo_bypass.jpg", catatanBypass = "Kasubag ACC via telepon karena dinas luar",
                isBypassAcknowledged = false, alasanPenolakan = null, createdBy = 1, approvedBy = 1
            ),
            // 📚 SKENARIO 4: NORMAL BORROWED - test pengembalian / perpanjangan
            TransaksiEntity(
                id = 4, instansiPeminjamId = 2, picNama = "Ibu Sari Dewi", picNoHp = "628444555666",
                nomorSuratPengantar = "020/DINKES/V/2026", fotoSuratPengantarPath = "surat_4.jpg",
                qrCodeToken = "QR-NORMAL-004",
                tanggalPinjam = today.minusDays(3), tanggalKembaliRencana = today.plusDays(4),
                tanggalKembaliAktual = null, status = "dipinjam", metodePersetujuan = "online",
                buktiBypassPath = null, catatanBypass = null, isBypassAcknowledged = false,
                alasanPenolakan = null, createdBy = 3, approvedBy = 2
            ),
            // ❌ SKENARIO 5: DITOLAK - dead-end
            TransaksiEntity(
                id = 5, instansiPeminjamId = 4, picNama = "Pak Harto", picNoHp = "628555666777",
                nomorSuratPengantar = "025/INSPEKTORAT/V/2026", fotoSuratPengantarPath = "surat_5.jpg",
                qrCodeToken = null,
                tanggalPinjam = today.minusDays(2), tanggalKembaliRencana = today.plusDays(5),
                tanggalKembaliAktual = null, status = "ditolak", metodePersetujuan = "online",
                buktiBypassPath = null, catatanBypass = null, isBypassAcknowledged = false,
                alasanPenolakan = "Surat pengantar tidak ditandatangani kepala dinas",
                createdBy = 1, approvedBy = 2
            ),
            // ✅ SKENARIO 6: DIKEMBALIKAN (BAIK) - riwayat sukses
            TransaksiEntity(
                id = 6, instansiPeminjamId = 5, picNama = "Pak Bambang", picNoHp = "628666777888",
                nomorSuratPengantar = "030/DPU/IV/2026", fotoSuratPengantarPath = "surat_6.jpg",
                qrCodeToken = "QR-RETURNED-006",
                tanggalPinjam = today.minusDays(15), tanggalKembaliRencana = today.minusDays(8),
                tanggalKembaliAktual = today.minusDays(9), status = "dikembalikan", metodePersetujuan = "online",
                buktiBypassPath = null, catatanBypass = null, isBypassAcknowledged = false,
                alasanPenolakan = null, createdBy = 1, approvedBy = 2
            ),
            // ⏰ SKENARIO 7: DIPINJAM + Perpanjangan Pending
            TransaksiEntity(
                id = 7, instansiPeminjamId = 1, picNama = "Ibu Wulan", picNoHp = "628777888999",
                nomorSuratPengantar = "035/DINDIK/V/2026", fotoSuratPengantarPath = "surat_7.jpg",
                qrCodeToken = "QR-PERPANJANG-007",
                tanggalPinjam = today.minusDays(7), tanggalKembaliRencana = today.plusDays(1),
                tanggalKembaliAktual = null, status = "dipinjam", metodePersetujuan = "online",
                buktiBypassPath = null, catatanBypass = null, isBypassAcknowledged = false,
                alasanPenolakan = null, createdBy = 1, approvedBy = 2
            )
        ))

        db.detailPeminjamanDao().insertAll(listOf(
            DetailPeminjamanEntity(1, 1, 101, "SP2D-2023-001", "SP2D Belanja Modal Dindik", "2023", "Rak A - Box 2023-01", null, null),
            DetailPeminjamanEntity(2, 4, 106, "SP2D-2025-012", "SP2D Belanja Barang Dinkes", "2025", "Rak A - Box 2025-01", null, null),
            DetailPeminjamanEntity(3, 6, 102, "SP2D-2023-002", "SP2D Belanja Jasa Dinkes", "2023", "Rak A - Box 2023-02", "baik", null),
            DetailPeminjamanEntity(4, 7, 109, "SP2D-2025-025", "SP2D Honorarium Guru", "2025", "Rak A - Box 2025-02", null, null),
            DetailPeminjamanEntity(5, 7, 110, "SP2D-2025-030", "SP2D Belanja Modal Jalan", "2025", "Rak A - Box 2025-03", null, null)
        ))

        db.perpanjanganDao().insert(
            PerpanjanganEntity(
                id = 1, transaksiId = 7,
                tanggalKembaliLama = today.plusDays(1),
                tanggalKembaliBaru = today.plusDays(8),
                fotoSuratPerpanjanganPath = "surat_perpanjangan.jpg",
                alasan = "Audit BPK masih berlangsung, dokumen diperlukan",
                status = "pending", alasanPenolakan = null,
                createdBy = 1, approvedBy = null
            )
        )
    }

    private suspend fun seedAuditLog() {
        val now = System.currentTimeMillis()
        db.auditLogDao().insertAll(listOf(
            AuditLogEntity(1, 1, 1, "TRANSAKSI_DIBUAT", "Transaksi baru untuk Dindik", null, now - 864_000_000L),
            AuditLogEntity(2, 1, 1, "PENGAJUAN_DIKIRIM", "Dikirim ke Kasubag", null, now - 860_000_000L),
            AuditLogEntity(3, 1, 2, "DISETUJUI_ONLINE", "Disetujui Kasubag", "OK", now - 800_000_000L),
            AuditLogEntity(4, 1, 1, "DOKUMEN_DISERAHKAN", "Diserahkan ke Pak Joko", null, now - 700_000_000L),
            AuditLogEntity(5, 6, 3, "TRANSAKSI_DIBUAT", "Transaksi baru", null, now - 1_300_000_000L),
            AuditLogEntity(6, 6, 2, "DISETUJUI_ONLINE", "Disetujui", null, now - 1_250_000_000L),
            AuditLogEntity(7, 6, 3, "DOKUMEN_DISERAHKAN", "Diserahkan", null, now - 1_200_000_000L),
            AuditLogEntity(8, 6, 3, "DIKEMBALIKAN_BAIK", "Dokumen kembali baik", "Lengkap", now - 800_000_000L)
        ))
    }

    private fun hashPw(plain: String): String = "\$2a\$10\$${plain.hashCode()}dummyhash"
}
```

## 📊 15. Testing Scenarios Reference

| Skenario | Login Sebagai | Trigger | Expected Result |
|---|---|---|---|
| **Login** | `budi/budi123` atau `siti/siti123` | Buka app | Auth flow, RBAC redirect |
| **Overdue Reminder** | `budi` (Arsiparis) | Dashboard → list "Terlambat" | WorkManager, WhatsApp deep link |
| **Approval Antrean** | `siti` (Kasubag) | Tab Antrean → Transaksi #2 | Approve/Reject flow, generate QR |
| **Bypass Acknowledge** | `siti` (Kasubag) | Badge "Perlu Verifikasi" → Transaksi #3 | Retroaktif verification |
| **Pengembalian QR** | `budi` (Arsiparis) | Scan QR → `QR-NORMAL-004` | CameraX, detail lookup |
| **Pengembalian Manual** | `budi` (Arsiparis) | Cari "Dinkes" → Transaksi #4 | Manual search fallback |
| **Perpanjangan Approve** | `siti` (Kasubag) | Tab Perpanjangan → Transaksi #7 | Perpanjangan approval flow |
| **Master CRUD** | `budi` (Arsiparis) | Menu Master Dokumen/Instansi | CRUD internal |
| **Rejected History** | Semua | Riwayat → Filter "Ditolak" | UI rejected state (#5) |
| **Returned History** | Semua | Riwayat → Filter "Dikembalikan" | Audit trail lengkap (#6) |
| **Export Laporan** | Semua | Menu Laporan → Filter Mei 2026 | PDF/Excel generation |

## 📌 16. Use Case Reference (UC-01 to UC-13)

| ID | Use Case | Aktor | Deskripsi |
|---|---|---|---|
| UC-01 | Buat Transaksi Peminjaman | Arsiparis | Mencatat transaksi peminjaman baru |
| UC-02 | Cari Dokumen dari Master | Arsiparis | Pencarian manual di Master Dokumen |
| UC-03 | Upload Foto Surat Pengantar | Arsiparis | Ambil foto surat pengantar fisik |
| UC-04 | Edit/Batal Pengajuan | Arsiparis | Edit (Pending) atau batal (Pending/Approved) |
| UC-05 | Approve/Reject Pengajuan | Kasubag | Setujui (generate QR) atau tolak (alasan wajib) |
| UC-06 | Bypass Persetujuan | Arsiparis | Approval manual dengan bukti fisik |
| UC-07 | Scan QR Code Pengembalian | Arsiparis | Shortcut buka detail transaksi |
| UC-08 | Selesaikan Peminjaman | Arsiparis | Proses pengembalian + input kondisi |
| UC-09 | Perpanjang Masa Pinjam | Arsiparis | Ubah tenggat + surat baru wajib |
| UC-10 | Lihat Dashboard Statistik | Arsiparis, Kasubag | Ringkasan data real-time |
| UC-11 | Ekspor Laporan PDF/Excel | Arsiparis, Kasubag | Rekapitulasi data peminjaman |
| UC-12 | Hubungi via WhatsApp | Arsiparis | Kirim struk QR / reminder |
| UC-13 | Lihat Audit Trail Log | Arsiparis, Kasubag | Rekam jejak kronologis |

---

**Last Updated:** 2026-05-24
**Module Owner:** Tim Peminjaman Dokumen BPKPAD Balangan
**Status:** Standalone Android Application (Single APK)
