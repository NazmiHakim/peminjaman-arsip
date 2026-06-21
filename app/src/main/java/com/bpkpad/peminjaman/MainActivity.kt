package com.bpkpad.peminjaman

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.lifecycleScope
import androidx.navigation.compose.rememberNavController
import com.bpkpad.peminjaman.core.database.DatabaseSeeder
import com.bpkpad.peminjaman.core.session.SessionManager
import com.bpkpad.peminjaman.core.theme.BpkpadTheme
import com.bpkpad.peminjaman.navigation.BpkpadNavGraph
import com.bpkpad.peminjaman.navigation.Screen
import com.bpkpad.peminjaman.peminjaman.domain.model.enums.UserRole
import com.bpkpad.peminjaman.peminjaman.domain.repository.UserRepository
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Single Activity entry point for the BPKPAD app.
 *
 * Responsibilities:
 * 1. Install splash screen (keeps it up until session check completes).
 * 2. Seed the local database with demo/seed data on first launch (debug builds).
 * 3. Determine the correct [startDestination] based on the persisted session:
 *    - No session → Login screen.
 *    - Session role == KASUBAG → DashboardKasubag.
 *    - Otherwise → DashboardArsiparis.
 * 4. Mount [BpkpadTheme] + [BpkpadNavGraph].
 *
 * Business logic lives in UseCases; ViewModel state in [StateFlow].
 * This Activity is intentionally thin.
 */
@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject lateinit var sessionManager: SessionManager
    @Inject lateinit var dbSeeder: DatabaseSeeder
    @Inject lateinit var userRepository: UserRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        // Install splash BEFORE super.onCreate so the system can manage it.
        val splashScreen = installSplashScreen()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Keep splash visible until we resolve the start destination.
        var isReady = false
        splashScreen.setKeepOnScreenCondition { !isReady }

        // Seed database asynchronously (no-op if already seeded).
        dbSeeder.seedIfEmpty(lifecycleScope)

        lifecycleScope.launch {
            // Resolve session once; DataStore guarantees a value or null.
            var session = sessionManager.session.firstOrNull()
            if (session != null) {
                val authenticatedUser = userRepository.getAuthenticatedUser()
                if (authenticatedUser == null || authenticatedUser.id != session.userId) {
                    sessionManager.clearSession()
                    session = null
                }
            }

            val startDestination = when {
                session == null -> Screen.Login.route
                session.role == UserRole.KASUBAG -> Screen.DashboardKasubag.route
                else -> Screen.DashboardArsiparis.route
            }

            // Allow splash to dismiss.
            isReady = true

            setContent {
                BpkpadTheme {
                    val navController = rememberNavController()
                    BpkpadNavGraph(
                        navController = navController,
                        sessionManager = sessionManager,
                        startDestination = startDestination
                    )
                }
            }
        }
    }
}
