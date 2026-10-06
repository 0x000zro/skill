package com.cdp.learningapp.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.cdp.learningapp.CDPApplication
import com.cdp.learningapp.data.model.ManifestResponse
import com.cdp.learningapp.data.remote.AppConfig
import com.cdp.learningapp.data.repository.ContentRepository
import com.cdp.learningapp.domain.model.Resource
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class HomeUiState(
    val manifestResource: Resource<ManifestResponse> = Resource.Loading(),
    val selectedTab: Int = 0, // 0 = Study, 1 = Revision, 2 = Practice, 3 = Test
    val searchQuery: String = "",
    val isRefreshing: Boolean = false,
    val showSettingsDialog: Boolean = false,
    val currentOwner: String = "",
    val currentRepo: String = "",
    val currentBranch: String = ""
)

class HomeViewModel(
    private val repository: ContentRepository = CDPApplication.instance.repository,
    private val appConfig: AppConfig = CDPApplication.instance.appConfig
) : ViewModel() {

    private val _uiState = MutableStateFlow(
        HomeUiState(
            currentOwner = appConfig.githubOwner,
            currentRepo = appConfig.githubRepo,
            currentBranch = appConfig.githubBranch
        )
    )
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    init {
        loadManifest(forceRefresh = false)
    }

    fun selectTab(index: Int) {
        _uiState.value = _uiState.value.copy(selectedTab = index)
    }

    fun updateSearchQuery(query: String) {
        _uiState.value = _uiState.value.copy(searchQuery = query)
    }

    fun openSettingsDialog() {
        _uiState.value = _uiState.value.copy(showSettingsDialog = true)
    }

    fun closeSettingsDialog() {
        _uiState.value = _uiState.value.copy(showSettingsDialog = false)
    }

    fun updateGitHubConfig(owner: String, repo: String, branch: String) {
        appConfig.githubOwner = owner
        appConfig.githubRepo = repo
        appConfig.githubBranch = branch
        _uiState.value = _uiState.value.copy(
            currentOwner = owner,
            currentRepo = repo,
            currentBranch = branch,
            showSettingsDialog = false
        )
        // Refresh with new coordinates
        loadManifest(forceRefresh = true)
    }

    fun loadManifest(forceRefresh: Boolean = false) {
        viewModelScope.launch {
            if (forceRefresh) {
                _uiState.value = _uiState.value.copy(isRefreshing = true)
            }
            repository.getManifest(forceRefresh = forceRefresh).collect { resource ->
                _uiState.value = _uiState.value.copy(
                    manifestResource = resource,
                    isRefreshing = false
                )
            }
        }
    }
}
