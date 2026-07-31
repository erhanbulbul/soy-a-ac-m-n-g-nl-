package com.example.ui.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.ui.MainViewModel
import com.example.ui.components.PaywallDialog
import com.example.ui.screens.AdvancedFeaturesScreen
import com.example.ui.screens.AuthScreen
import com.example.ui.screens.ChatDetailScreen
import com.example.ui.screens.ChatListScreen
import com.example.ui.screens.EmergencySosScreen
import com.example.ui.screens.FamilyTreeScreen
import com.example.ui.screens.HomeScreen
import com.example.ui.screens.JournalScreen
import com.example.ui.screens.ProfileSettingsScreen
import com.example.ui.screens.WillVaultScreen

object NavRoutes {
    const val AUTH = "auth"
    const val HOME = "home"
    const val FAMILY_TREE = "family_tree"
    const val JOURNAL = "journal"
    const val WILL_VAULT = "will_vault"
    const val CHAT_LIST = "chat_list"
    const val CHAT_DETAIL = "chat_detail"
    const val SETTINGS = "settings"
    const val EMERGENCY_SOS = "emergency_sos"
    const val ADVANCED_FEATURES = "advanced_features"
}

@Composable
fun NavGraph(viewModel: MainViewModel) {
    val navController = rememberNavController()

    val isLoggedIn by viewModel.isLoggedIn.collectAsState()
    val userProfile by viewModel.userProfile.collectAsState()
    val familyMembers by viewModel.familyMembers.collectAsState()
    val journalEntries by viewModel.journalEntries.collectAsState()
    val willDocuments by viewModel.willDocuments.collectAsState()
    val currentLanguageCode by viewModel.currentLanguage.collectAsState()
    val selectedMemberForChat by viewModel.selectedMemberForChat.collectAsState()

    val isPremium by viewModel.isPremium.collectAsState()
    val showPaywallDialog by viewModel.showPaywallDialog.collectAsState()
    val paywallFeatureName by viewModel.paywallFeatureName.collectAsState()

    Box(modifier = Modifier.fillMaxSize()) {
        NavHost(
            navController = navController,
            startDestination = if (isLoggedIn) NavRoutes.HOME else NavRoutes.AUTH
        ) {
            composable(NavRoutes.AUTH) {
                AuthScreen(
                    currentLanguageCode = currentLanguageCode,
                    onLanguageSelected = { viewModel.setLanguage(it) },
                    onLoginSuccess = { name, email ->
                        viewModel.login(name, email)
                        if (name.isNotBlank()) {
                            viewModel.updateProfileInfo(
                                name = name,
                                email = if (email.isNotBlank()) email else userProfile?.email ?: "",
                                bio = userProfile?.bio ?: "",
                                phone = userProfile?.phone ?: "",
                                avatarUri = userProfile?.avatarUri ?: "",
                                preferredColorHex = userProfile?.preferredTextColorHex ?: "#FFB800"
                            )
                        }
                        navController.navigate(NavRoutes.HOME) {
                            popUpTo(NavRoutes.AUTH) { inclusive = true }
                        }
                    }
                )
            }

            composable(NavRoutes.HOME) {
                HomeScreen(
                    userProfile = userProfile,
                    languageCode = currentLanguageCode,
                    onConfirmSafetyCheck = { viewModel.confirmSafetyCheck() },
                    onToggleDeceasedSimulation = { viewModel.toggleDeceasedSimulation(it) },
                    onNavigateToFamilyTree = { navController.navigate(NavRoutes.FAMILY_TREE) },
                    onNavigateToJournal = { navController.navigate(NavRoutes.JOURNAL) },
                    onNavigateToWillVault = { navController.navigate(NavRoutes.WILL_VAULT) },
                    onNavigateToChat = { navController.navigate(NavRoutes.CHAT_LIST) },
                    onNavigateToSettings = { navController.navigate(NavRoutes.SETTINGS) },
                    onNavigateToEmergencySos = { navController.navigate(NavRoutes.EMERGENCY_SOS) },
                    onNavigateToAdvancedFeatures = { navController.navigate(NavRoutes.ADVANCED_FEATURES) },
                    onNavigateToAuth = {
                        viewModel.logout()
                        navController.navigate(NavRoutes.AUTH) {
                            popUpTo(0) { inclusive = true }
                        }
                    },
                    onSelectLanguage = { viewModel.setLanguage(it) }
                )
            }

            composable(NavRoutes.ADVANCED_FEATURES) {
                AdvancedFeaturesScreen(
                    familyMembers = familyMembers,
                    languageCode = currentLanguageCode,
                    onBack = { navController.popBackStack() }
                )
            }

            composable(NavRoutes.EMERGENCY_SOS) {
                EmergencySosScreen(
                    currentLanguageCode = currentLanguageCode,
                    onBack = { navController.popBackStack() }
                )
            }

            composable(NavRoutes.FAMILY_TREE) {
                FamilyTreeScreen(
                    familyMembers = familyMembers,
                    languageCode = currentLanguageCode,
                    isPremium = isPremium,
                    onTriggerPaywall = { viewModel.triggerPaywall(it) },
                    onAddMemberByCode = { code, name, rel, notes, isDeceased, birthYear, avatarUri ->
                        viewModel.addFamilyMemberByCode(code, name, rel, notes, isDeceased, birthYear, avatarUri)
                    },
                    onDeleteMember = { viewModel.deleteFamilyMember(it) },
                    onSaveTreePositions = { members, callback ->
                        viewModel.saveFamilyTreePositions(members, callback)
                    },
                    onReportMemberDeath = { memberId, callback ->
                        viewModel.reportMemberDeath(memberId, callback)
                    },
                    onSelectMemberForChat = { member ->
                        viewModel.selectMemberForChat(member)
                        navController.navigate(NavRoutes.CHAT_DETAIL)
                    },
                    onBack = { navController.popBackStack() }
                )
            }

            composable(NavRoutes.JOURNAL) {
                JournalScreen(
                    journalEntries = journalEntries,
                    languageCode = currentLanguageCode,
                    userName = userProfile?.name ?: "Kullanıcı",
                    userCode = userProfile?.userCode?.ifBlank { com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.uid ?: "" } ?: "",
                    onAddEntry = { title, content, colorHex, imageUris ->
                        viewModel.addJournalEntry(title, content, colorHex, imageUris)
                    },
                    onUpdateEntry = { id, title, content, colorHex, imageUris ->
                        viewModel.updateJournalEntry(id, title, content, colorHex, imageUris)
                    },
                    onDeleteEntry = { viewModel.deleteJournalEntry(it) },
                    onBack = { navController.popBackStack() }
                )
            }

            composable(NavRoutes.WILL_VAULT) {
                WillVaultScreen(
                    willDocuments = willDocuments,
                    familyMembers = familyMembers,
                    languageCode = currentLanguageCode,
                    isPremium = isPremium,
                    onTriggerPaywall = { viewModel.triggerPaywall(it) },
                    onAddWillDocument = { title, category, uri, desc, recipients ->
                        viewModel.addWillDocument(title, category, uri, desc, recipients)
                    },
                    onDeleteWillDocument = { viewModel.deleteWillDocument(it) },
                    onBack = { navController.popBackStack() }
                )
            }

            composable(NavRoutes.CHAT_LIST) {
                ChatListScreen(
                    familyMembers = familyMembers,
                    languageCode = currentLanguageCode,
                    onSelectMember = { member ->
                        viewModel.selectMemberForChat(member)
                        navController.navigate(NavRoutes.CHAT_DETAIL)
                    },
                    onBack = { navController.popBackStack() }
                )
            }

            composable(NavRoutes.CHAT_DETAIL) {
                selectedMemberForChat?.let { member ->
                    ChatDetailScreen(
                        member = member,
                        viewModel = viewModel,
                        languageCode = currentLanguageCode,
                        onBack = { navController.popBackStack() }
                    )
                }
            }

            composable(NavRoutes.SETTINGS) {
                ProfileSettingsScreen(
                    userProfile = userProfile,
                    currentLanguageCode = currentLanguageCode,
                    isPremium = isPremium,
                    onTriggerPaywall = { viewModel.triggerPaywall(it) },
                    onTogglePremium = { viewModel.setPremiumStatus(it) },
                    onLanguageSelected = { viewModel.setLanguage(it) },
                    onSaveProfile = { name, email, bio, phone, avatarUri, preferredColorHex, address, bloodType ->
                        viewModel.updateProfileInfo(name, email, bio, phone, avatarUri, preferredColorHex, address, bloodType)
                    },
                    onDeleteAccount = {
                        viewModel.deleteAccount()
                        navController.navigate(NavRoutes.AUTH) {
                            popUpTo(0) { inclusive = true }
                        }
                    },
                    onNavigateToAuth = {
                        viewModel.logout()
                        navController.navigate(NavRoutes.AUTH) {
                            popUpTo(0) { inclusive = true }
                        }
                    },
                    onBack = { navController.popBackStack() }
                )
            }
        }

        if (showPaywallDialog) {
            PaywallDialog(
                triggeredFeatureName = paywallFeatureName,
                isPremium = isPremium,
                onDismiss = { viewModel.dismissPaywall() },
                onUpgradeSuccess = { viewModel.setPremiumStatus(true) },
                onCancelPremium = { viewModel.setPremiumStatus(false) }
            )
        }
    }
}
