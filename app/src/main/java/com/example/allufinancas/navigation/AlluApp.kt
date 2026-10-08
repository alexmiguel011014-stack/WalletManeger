package com.example.allufinancas.navigation

import android.app.Activity
import android.content.Context
import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.*
import androidx.core.view.WindowCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.allufinancas.AlluApplication
import com.example.allufinancas.designsystem.*
import com.example.allufinancas.feature.alerts.Alerts
import com.example.allufinancas.feature.alerts.AlertsViewModel
import com.example.allufinancas.feature.alerts.ReviewInbox
import com.example.allufinancas.feature.assistant.Assistant
import com.example.allufinancas.feature.assistant.AssistantViewModel
import com.example.allufinancas.feature.budgets.Budgets
import com.example.allufinancas.feature.budgets.BudgetsViewModel
import com.example.allufinancas.feature.home.Home
import com.example.allufinancas.feature.home.HomeViewModel
import com.example.allufinancas.feature.imports.ImportInbox
import com.example.allufinancas.feature.imports.ImportInboxViewModel
import com.example.allufinancas.feature.onboarding.PrivacyEducation
import com.example.allufinancas.feature.onboarding.Welcome
import com.example.allufinancas.feature.settings.Settings
import com.example.allufinancas.feature.settings.SettingsViewModel
import com.example.allufinancas.feature.transactions.TransactionDetail
import com.example.allufinancas.feature.transactions.Transactions
import com.example.allufinancas.feature.transactions.TransactionsViewModel
import com.example.allufinancas.feature.transactions.transactionById
import java.time.Instant

internal enum class Page(val label: String, val route: String, val icon: String) {
  HOME("Início", "home", "⌂"),
  TRANSACTIONS("Extrato", "transactions", "≡"),
  BUDGETS("Planos", "budgets", "◎"),
  ALERTS("Avisos", "alerts", "◇"),
  ASSISTANT("Assistente", "assistant", "✦"),
  SETTINGS("Ajustes", "settings", "⚙"),
}

private val tabs = listOf(Page.HOME, Page.TRANSACTIONS, Page.BUDGETS, Page.ALERTS, Page.ASSISTANT)
private const val PRIVACY_ROUTE = "privacy"
private const val REVIEW_ROUTE = "alerts/review"
private const val IMPORTS_ROUTE = "imports"
private const val TRANSACTION_DETAIL_ROUTE = "transaction/{transactionId}"

internal fun transactionDetailRoute(id: String) = "transaction/${Uri.encode(id)}"

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AlluApp(model: AlluViewModel? = null) {
  val context = LocalContext.current
  val app = context.applicationContext as AlluApplication
  val appFactory = remember(app) { AlluViewModelFactory(app.demoDataSource) }
  val stateModel = model ?: viewModel(factory = appFactory)
  val factory = remember(stateModel) { AlluViewModelFactory(stateModel.dataSource) }
  val local = remember { context.getSharedPreferences("allu_welcome", Context.MODE_PRIVATE) }
  var entered by rememberSaveable { mutableStateOf(local.getBoolean("entered", false)) }
  var privacy by rememberSaveable { mutableStateOf(false) }
  val settings by stateModel.preferences.collectAsStateWithLifecycle()
  val materializedAt by
      stateModel.dataSource.materializedAt.collectAsStateWithLifecycle(initialValue = null)
  val notice by stateModel.notice.collectAsStateWithLifecycle()
  BackHandler(enabled = !entered && privacy) { privacy = false }
  SideEffect {
    (context as? Activity)?.let { activity ->
      val bars = WindowCompat.getInsetsController(activity.window, activity.window.decorView)
      bars.isAppearanceLightStatusBars = entered && !settings.darkTheme
      bars.isAppearanceLightNavigationBars = entered && !settings.darkTheme
    }
  }
  AlluTheme(settings.darkTheme, settings.reduceMotion) {
    if (!entered) {
      if (privacy) PrivacyEducation(materializedAt, onBack = { privacy = false })
      else
          Welcome(
              onExplore = {
                local.edit().putBoolean("entered", true).apply()
                entered = true
              },
              onPrivacy = { privacy = true },
          )
    } else AlluNavigation(stateModel, factory, materializedAt, notice) { stateModel.clearNotice() }
  }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AlluNavigation(
    stateModel: AlluViewModel,
    factory: AlluViewModelFactory,
    materializedAt: Instant?,
    notice: String?,
    clearNotice: () -> Unit,
) {
  val navController = rememberNavController()
  val backStackEntry by navController.currentBackStackEntryAsState()
  val route = backStackEntry?.destination?.route
  var selectedTab by rememberSaveable { mutableStateOf(Page.HOME) }
  LaunchedEffect(route) { tabs.firstOrNull { it.route == route }?.let { selectedTab = it } }

  fun selectTab(tab: Page) {
    if (
        route == Page.SETTINGS.route ||
            route == PRIVACY_ROUTE ||
            route == REVIEW_ROUTE ||
            route == IMPORTS_ROUTE ||
            route?.startsWith("transaction/") == true
    ) {
      navController.popBackStack(selectedTab.route, inclusive = false)
    }
    selectedTab = tab
    navController.navigate(tab.route) {
      popUpTo(Page.HOME.route) { saveState = true }
      launchSingleTop = true
      restoreState = true
    }
  }

  val title =
      when (route) {
        Page.HOME.route -> "Visão geral"
        Page.SETTINGS.route -> Page.SETTINGS.label
        PRIVACY_ROUTE -> "Privacidade e limitações"
        REVIEW_ROUTE -> "Revisão de lançamentos"
        IMPORTS_ROUTE -> "Importações pendentes"
        TRANSACTION_DETAIL_ROUTE -> "Detalhes do lançamento"
        else -> tabs.firstOrNull { it.route == route }?.label ?: "ALLU Finanças"
      }
  val canGoBack =
      route == Page.SETTINGS.route ||
          route == PRIVACY_ROUTE ||
          route == REVIEW_ROUTE ||
          route == IMPORTS_ROUTE ||
          route?.startsWith("transaction/") == true

  BoxWithConstraints(Modifier.fillMaxSize()) {
    val useRail = maxWidth >= 600.dp
    AlluAppScaffold(
        topBar = {
          TopAppBar(
              title = { Text(title, fontWeight = FontWeight.SemiBold) },
              actions = {
                TextButton(
                    onClick = {
                      if (canGoBack) navController.popBackStack()
                      else navController.navigate(Page.SETTINGS.route) { launchSingleTop = true }
                    }
                ) {
                  Text(if (canGoBack) "Voltar" else "Ajustes")
                }
              },
          )
        },
        bottomBar = {
          if (!useRail)
              AlluBottomNavigation(
                  tabs.map { tab ->
                    AlluNavigationItem(
                        label = tab.label,
                        selected = selectedTab == tab,
                        onClick = { selectTab(tab) },
                        icon = { Text(tab.icon, fontSize = 20.sp) },
                    )
                  },
                  Modifier.testTag("bottom-navigation"),
              )
        },
    ) { insets ->
      Row(Modifier.fillMaxSize().padding(insets)) {
        if (useRail)
            NavigationRail(Modifier.testTag("navigation-rail")) {
              tabs.forEach { tab ->
                NavigationRailItem(
                    selected = selectedTab == tab,
                    onClick = { selectTab(tab) },
                    icon = { Text(tab.icon, fontSize = 20.sp) },
                    label = { Text(tab.label) },
                    modifier = Modifier.semantics { contentDescription = tab.label },
                )
              }
            }
        Box(Modifier.weight(1f).fillMaxHeight()) {
          NavHost(navController, startDestination = Page.HOME.route, Modifier.fillMaxSize()) {
            composable(Page.HOME.route) {
              val screenModel: HomeViewModel = viewModel(factory = factory)
              val screen by screenModel.state.collectAsStateWithLifecycle()
              ScreenContent(screen) { data ->
                Home(
                    data.transactions,
                    data.categories,
                    data.budgets,
                    data.alerts,
                    data.goals,
                    data.preferences,
                    materializedAt,
                    ::selectTab,
                    onOpenTransaction = { id ->
                      selectTab(Page.TRANSACTIONS)
                      navController.navigate(transactionDetailRoute(id))
                    },
                )
              }
            }
            composable(Page.TRANSACTIONS.route) {
              val screenModel: TransactionsViewModel = viewModel(factory = factory)
              val screen by screenModel.state.collectAsStateWithLifecycle()
              ScreenContent(screen) { data ->
                Transactions(
                    data.transactions,
                    data.categories,
                    data.preferences,
                    materializedAt,
                    data.pendingImportCount,
                    stateModel::saveTransaction,
                    onOpenImports = {
                      navController.navigate(IMPORTS_ROUTE) { launchSingleTop = true }
                    },
                ) { id ->
                  navController.navigate(transactionDetailRoute(id))
                }
              }
            }
            composable(Page.BUDGETS.route) {
              val screenModel: BudgetsViewModel = viewModel(factory = factory)
              val screen by screenModel.state.collectAsStateWithLifecycle()
              ScreenContent(screen) { data ->
                Budgets(
                    data.transactions,
                    data.categories,
                    data.budgets,
                    data.goals,
                    data.preferences,
                    materializedAt,
                    stateModel::updateBudget,
                )
              }
            }
            composable(Page.ALERTS.route) {
              val screenModel: AlertsViewModel = viewModel(factory = factory)
              val screen by screenModel.state.collectAsStateWithLifecycle()
              ScreenContent(screen) {
                Alerts(it.alerts, materializedAt, stateModel::setAlertDismissed) {
                  navController.navigate(REVIEW_ROUTE)
                }
              }
            }
            composable(REVIEW_ROUTE) {
              val screenModel: AlertsViewModel = viewModel(factory = factory)
              val screen by screenModel.reviewState.collectAsStateWithLifecycle()
              ScreenContent(screen) { data ->
                ReviewInbox(
                    data.cases,
                    data.transactions,
                    data.categories,
                    materializedAt,
                    stateModel::decideReview,
                    stateModel::setCategory,
                    stateModel::reopenReview,
                )
              }
            }
            composable(IMPORTS_ROUTE) {
              val screenModel: ImportInboxViewModel = viewModel(factory = factory)
              val screen by screenModel.state.collectAsStateWithLifecycle()
              ScreenContent(screen) { data ->
                ImportInbox(
                    data = data,
                    materializedAt = materializedAt,
                    onOpenSettings = {
                      navController.navigate(Page.SETTINGS.route) { launchSingleTop = true }
                    },
                    onConfirm = stateModel::confirmImport,
                    onResolve = stateModel::resolveImport,
                )
              }
            }
            composable(Page.ASSISTANT.route) {
              val screenModel: AssistantViewModel = viewModel(factory = factory)
              val screen by screenModel.state.collectAsStateWithLifecycle()
              ScreenContent(screen) {
                Assistant(
                    it.messages,
                    it.transactions,
                    it.categories,
                    materializedAt,
                    stateModel::sendMessage,
                )
              }
            }
            composable(Page.SETTINGS.route) {
              val screenModel: SettingsViewModel = viewModel(factory = factory)
              val screen by screenModel.state.collectAsStateWithLifecycle()
              ScreenContent(screen) {
                Settings(
                    it.preferences,
                    it.enabledImportSource,
                    it.importFeedSuffix,
                    materializedAt,
                    stateModel,
                ) {
                  navController.navigate(PRIVACY_ROUTE) { launchSingleTop = true }
                }
              }
            }
            composable(PRIVACY_ROUTE) {
              PrivacyEducation(materializedAt, onBack = { navController.popBackStack() })
            }
            composable(
                TRANSACTION_DETAIL_ROUTE,
                arguments = listOf(navArgument("transactionId") { type = NavType.StringType }),
            ) { entry ->
              val transactionsEntry =
                  remember(entry) { navController.getBackStackEntry(Page.TRANSACTIONS.route) }
              val screenModel: TransactionsViewModel =
                  viewModel(transactionsEntry, factory = factory)
              val screen by screenModel.state.collectAsStateWithLifecycle()
              val id = entry.arguments?.getString("transactionId").orEmpty()
              ScreenContent(screen) { data ->
                TransactionDetail(
                    transactionById(data.transactions, id),
                    data.categories,
                    data.preferences,
                    onBack = { navController.popBackStack() },
                    onCategory = { transactionId, categoryId ->
                      stateModel.setCategory(transactionId, categoryId)
                      navController.popBackStack()
                    },
                    onSave = stateModel::saveTransaction,
                    onDelete = stateModel::deleteTransaction,
                    materializedAt = materializedAt,
                )
              }
            }
          }
          if (notice != null)
              Surface(
                  modifier =
                      Modifier.align(Alignment.BottomCenter).padding(16.dp).clickable {
                        clearNotice()
                      },
                  color = AlluNavy,
                  shape = RoundedCornerShape(10.dp),
              ) {
                Text(notice, Modifier.padding(14.dp), color = Color.White)
              }
        }
      }
    }
  }
}

@Composable
private fun <T> ScreenContent(state: ScreenUiState<T>, content: @Composable (T) -> Unit) {
  when (state) {
    ScreenUiState.Loading ->
        Box(Modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
          LoadingSkeleton()
        }
    is ScreenUiState.Error ->
        Box(Modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
          ErrorNotice("Não foi possível carregar esta tela", state.message)
        }
    is ScreenUiState.Empty -> content(state.value)
    is ScreenUiState.Content -> content(state.value)
  }
}
