package com.bpkpad.peminjaman.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.*
import androidx.navigation.compose.*
import com.bpkpad.peminjaman.auth.presentation.LoginScreen
import com.bpkpad.peminjaman.core.session.SessionManager
import com.bpkpad.peminjaman.laporan.presentation.LaporanScreen
import com.bpkpad.peminjaman.master.presentation.ListDokumenScreen
import com.bpkpad.peminjaman.master.presentation.ListInstansiScreen
import com.bpkpad.peminjaman.peminjaman.domain.model.enums.UserRole
import com.bpkpad.peminjaman.peminjaman.presentation.approval.AntreanApprovalScreen
import com.bpkpad.peminjaman.peminjaman.presentation.dashboard.DashboardArsiparisScreen
import com.bpkpad.peminjaman.peminjaman.presentation.dashboard.DashboardKasubagScreen
import com.bpkpad.peminjaman.peminjaman.presentation.detail.DetailTransaksiScreen
import com.bpkpad.peminjaman.peminjaman.presentation.form.FormTransaksiScreen
import com.bpkpad.peminjaman.peminjaman.presentation.pengembalian.ScanQrScreen
import com.bpkpad.peminjaman.peminjaman.presentation.riwayat.ListRiwayatScreen

sealed class Screen(val route: String) {
    data object Login               : Screen("login")
    data object DashboardArsiparis  : Screen("dashboard_arsiparis")
    data object DashboardKasubag    : Screen("dashboard_kasubag")
    data object FormTransaksi       : Screen("form_transaksi")
    data object AntreanApproval     : Screen("antrean_approval")
    data object ScanQr              : Screen("scan_qr")
    data object ListRiwayat         : Screen("list_riwayat")
    data object ListInstansi        : Screen("list_instansi")
    data object ListDokumen         : Screen("list_dokumen")
    data object Laporan             : Screen("laporan")
    data object DetailTransaksi     : Screen("detail_transaksi/{transaksiId}") {
        fun createRoute(id: Int) = "detail_transaksi/$id"
    }
}

@Composable
fun BpkpadNavGraph(
    navController: NavHostController,
    sessionManager: SessionManager,
    startDestination: String = Screen.Login.route
) {
    NavHost(navController = navController, startDestination = startDestination) {

        composable(Screen.Login.route) {
            LoginScreen(
                onLoginSuccess = { role ->
                    val dest = if (role == UserRole.KASUBAG)
                        Screen.DashboardKasubag.route
                    else
                        Screen.DashboardArsiparis.route
                    navController.navigate(dest) {
                        popUpTo(Screen.Login.route) { inclusive = true }
                    }
                }
            )
        }

        composable(Screen.DashboardArsiparis.route) {
            DashboardArsiparisScreen(
                onNavigate = { navController.navigate(it) },
                onLogout = {
                    navController.navigate(Screen.Login.route) {
                        popUpTo(0) { inclusive = true }
                    }
                }
            )
        }

        composable(Screen.DashboardKasubag.route) {
            DashboardKasubagScreen(
                onNavigate = { navController.navigate(it) },
                onLogout = {
                    navController.navigate(Screen.Login.route) {
                        popUpTo(0) { inclusive = true }
                    }
                }
            )
        }

        composable(Screen.FormTransaksi.route) {
            FormTransaksiScreen(onBack = { navController.popBackStack() })
        }

        composable(Screen.AntreanApproval.route) {
            AntreanApprovalScreen(
                onBack = { navController.popBackStack() },
                onNavigateToDetail = { id -> navController.navigate(Screen.DetailTransaksi.createRoute(id)) }
            )
        }

        composable(Screen.ScanQr.route) {
            ScanQrScreen(
                onBack = { navController.popBackStack() },
                onFound = { id -> navController.navigate(Screen.DetailTransaksi.createRoute(id)) }
            )
        }

        composable(
            route = Screen.DetailTransaksi.route,
            arguments = listOf(navArgument("transaksiId") { type = NavType.IntType }),
            deepLinks = listOf(navDeepLink { uriPattern = "bpkpad://transaksi/{transaksiId}" })
        ) { back ->
            val id = back.arguments?.getInt("transaksiId") ?: 0
            DetailTransaksiScreen(transaksiId = id, onBack = { navController.popBackStack() })
        }

        composable(
            route = "list_riwayat?status={status}", // Menggunakan query parameter opsional
            arguments = listOf(
                navArgument("status") {
                    type = NavType.StringType
                    nullable = true
                    defaultValue = null
                }
            )
        ) { backStackEntry ->
            val statusParam = backStackEntry.arguments?.getString("status")
            ListRiwayatScreen(
                statusFilter = statusParam, // Oper parameter ke Screen
                onBack = { navController.popBackStack() },
                onNavigateToDetail = { id -> navController.navigate(Screen.DetailTransaksi.createRoute(id)) }
            )
        }

        composable(Screen.ListInstansi.route) {
            ListInstansiScreen(onBack = { navController.popBackStack() })
        }

        composable(Screen.ListDokumen.route) {
            ListDokumenScreen(onBack = { navController.popBackStack() })
        }

        composable(Screen.Laporan.route) {
            LaporanScreen(onBack = { navController.popBackStack() })
        }
    }
}
