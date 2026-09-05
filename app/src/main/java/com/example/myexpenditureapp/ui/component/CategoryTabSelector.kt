package com.example.myexpenditureapp.ui.component

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.myexpenditureapp.data.entity.Category

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun CategoryTabSelector(
    categories: List<Category>,
    selectedCategoryId: Long?,
    onCategorySelected: (Long?) -> Unit,
    modifier: Modifier = Modifier
) {
    val parentCategories = remember(categories) {
        categories.filter { it.parentId == null }
    }

    val currentSelectedCategory = categories.find { it.id == selectedCategoryId }
    val initialParentId = currentSelectedCategory?.parentId ?: if (parentCategories.any { it.id == selectedCategoryId }) selectedCategoryId else null
    
    var selectedParentId by remember(initialParentId) { 
        mutableStateOf(initialParentId ?: parentCategories.firstOrNull()?.id) 
    }

    val selectedTabIndex = parentCategories.indexOfFirst { it.id == selectedParentId }.coerceAtLeast(0)

    Column(modifier = modifier.fillMaxWidth()) {
        ScrollableTabRow(
            selectedTabIndex = selectedTabIndex,
            edgePadding = 16.dp,
            containerColor = Color.Transparent,
            contentColor = MaterialTheme.colorScheme.primary,
            divider = {},
            indicator = { tabPositions ->
                if (selectedTabIndex >= 0 && selectedTabIndex < tabPositions.size) {
                    TabRowDefaults.SecondaryIndicator(
                        Modifier.tabIndicatorOffset(tabPositions[selectedTabIndex]),
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        ) {
            parentCategories.forEachIndexed { index, parent ->
                Tab(
                    selected = selectedTabIndex == index,
                    onClick = { selectedParentId = parent.id },
                    text = {
                        Text(
                            text = "${parent.icon ?: ""} ${parent.name}".trim(),
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = if (selectedTabIndex == index) FontWeight.Bold else FontWeight.Normal
                        )
                    }
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        AnimatedContent(
            targetState = selectedParentId,
            transitionSpec = {
                fadeIn() togetherWith fadeOut()
            },
            label = "CategoryTransition",
            modifier = Modifier.fillMaxWidth().heightIn(min = 100.dp)
        ) { parentId ->
            val subCategories = categories.filter { it.parentId == parentId }
            val parent = parentCategories.find { it.id == parentId }

            Column(modifier = Modifier.padding(horizontal = 16.dp).fillMaxWidth()) {
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Parent itself option (All)
                    if (parent != null) {
                        FilterChip(
                            selected = selectedCategoryId == parent.id,
                            onClick = { onCategorySelected(parent.id) },
                            label = { Text("All ${parent.name}", style = MaterialTheme.typography.labelSmall) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        )
                    }

                    subCategories.forEach { child ->
                        FilterChip(
                            selected = selectedCategoryId == child.id,
                            onClick = { onCategorySelected(child.id) },
                            label = {
                                Text(
                                    text = "${child.icon ?: ""} ${child.name}".trim(),
                                    style = MaterialTheme.typography.labelSmall
                                )
                            }
                        )
                    }
                }
                
                if (subCategories.isEmpty() && parent == null) {
                    Text(
                        "Select a category group above",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(vertical = 16.dp)
                    )
                }
                
                Spacer(modifier = Modifier.height(24.dp))
                
                // Clear selection option
                TextButton(
                    onClick = { onCategorySelected(null) },
                    modifier = Modifier.align(Alignment.CenterHorizontally).fillMaxWidth()
                ) {
                    Text("None / Clear Category", color = MaterialTheme.colorScheme.error)
                }
            }
        }
    }
}
