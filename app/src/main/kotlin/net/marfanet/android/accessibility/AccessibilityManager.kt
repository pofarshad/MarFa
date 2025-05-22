package net.marfanet.android.accessibility

import android.content.Context
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.core.view.ViewCompat
import androidx.core.view.accessibility.AccessibilityNodeInfoCompat

/**
 * MarFaNet Accessibility Manager
 * Implements P2-05: Accessibility Scanner fixes for WCAG compliance
 * Target: ≥95% accessibility score
 */
class AccessibilityManager(private val context: Context) {
    
    companion object {
        private const val MIN_TOUCH_TARGET_SIZE_DP = 48
        private const val MIN_TEXT_SIZE_SP = 12
        private const val MIN_CONTRAST_RATIO = 4.5
    }
    
    /**
     * Scan view hierarchy for accessibility issues
     */
    fun scanAccessibility(rootView: View): AccessibilityReport {
        val issues = mutableListOf<AccessibilityIssue>()
        val suggestions = mutableListOf<String>()
        
        scanViewRecursively(rootView, issues, suggestions)
        
        val score = calculateAccessibilityScore(issues.size, getTotalViewCount(rootView))
        
        return AccessibilityReport(
            score = score,
            issues = issues,
            suggestions = suggestions,
            totalViews = getTotalViewCount(rootView)
        )
    }
    
    /**
     * Apply accessibility fixes to view hierarchy
     */
    fun applyAccessibilityFixes(rootView: View) {
        fixViewRecursively(rootView)
    }
    
    private fun scanViewRecursively(
        view: View, 
        issues: MutableList<AccessibilityIssue>,
        suggestions: MutableList<String>
    ) {
        // Check touch target size
        checkTouchTargetSize(view, issues)
        
        // Check content descriptions
        checkContentDescription(view, issues, suggestions)
        
        // Check text accessibility
        checkTextAccessibility(view, issues)
        
        // Check focus handling
        checkFocusability(view, issues)
        
        // Scan child views
        if (view is ViewGroup) {
            for (i in 0 until view.childCount) {
                scanViewRecursively(view.getChildAt(i), issues, suggestions)
            }
        }
    }
    
    private fun fixViewRecursively(view: View) {
        // Apply touch target size fixes
        fixTouchTargetSize(view)
        
        // Apply content description fixes
        fixContentDescription(view)
        
        // Apply text accessibility fixes
        fixTextAccessibility(view)
        
        // Apply focus fixes
        fixFocusability(view)
        
        // Apply semantic fixes
        applySemanticEnhancements(view)
        
        // Fix child views
        if (view is ViewGroup) {
            for (i in 0 until view.childCount) {
                fixViewRecursively(view.getChildAt(i))
            }
        }
    }
    
    private fun checkTouchTargetSize(view: View, issues: MutableList<AccessibilityIssue>) {
        if (view.isClickable || view.isFocusable) {
            val density = context.resources.displayMetrics.density
            val minSizePx = (MIN_TOUCH_TARGET_SIZE_DP * density).toInt()
            
            if (view.width < minSizePx || view.height < minSizePx) {
                issues.add(
                    AccessibilityIssue(
                        type = "TOUCH_TARGET_SIZE",
                        severity = "HIGH",
                        description = "Touch target too small: ${view.width}x${view.height}px (minimum: ${minSizePx}px)",
                        viewId = view.id.toString(),
                        fix = "Increase minimum touch target to 48dp"
                    )
                )
            }
        }
    }
    
    private fun checkContentDescription(
        view: View, 
        issues: MutableList<AccessibilityIssue>,
        suggestions: MutableList<String>
    ) {
        when (view) {
            is ImageView -> {
                if (view.contentDescription.isNullOrBlank() && view.drawable != null) {
                    issues.add(
                        AccessibilityIssue(
                            type = "MISSING_CONTENT_DESCRIPTION",
                            severity = "HIGH",
                            description = "ImageView missing content description",
                            viewId = view.id.toString(),
                            fix = "Add meaningful contentDescription"
                        )
                    )
                    suggestions.add("Add content description for image: ${view.id}")
                }
            }
            is Button -> {
                if (view.contentDescription.isNullOrBlank() && view.text.isNullOrBlank()) {
                    issues.add(
                        AccessibilityIssue(
                            type = "MISSING_BUTTON_LABEL",
                            severity = "CRITICAL",
                            description = "Button has no accessible label",
                            viewId = view.id.toString(),
                            fix = "Add text or contentDescription to button"
                        )
                    )
                }
            }
        }
    }
    
    private fun checkTextAccessibility(view: View, issues: MutableList<AccessibilityIssue>) {
        if (view is TextView) {
            // Check text size
            val textSizeSp = view.textSize / context.resources.displayMetrics.scaledDensity
            if (textSizeSp < MIN_TEXT_SIZE_SP) {
                issues.add(
                    AccessibilityIssue(
                        type = "TEXT_SIZE_TOO_SMALL",
                        severity = "MEDIUM",
                        description = "Text size ${textSizeSp}sp is below minimum ${MIN_TEXT_SIZE_SP}sp",
                        viewId = view.id.toString(),
                        fix = "Increase text size to at least ${MIN_TEXT_SIZE_SP}sp"
                    )
                )
            }
            
            // Check if text is selectable for long content
            if (view.text.length > 50 && !view.isTextSelectable) {
                issues.add(
                    AccessibilityIssue(
                        type = "LONG_TEXT_NOT_SELECTABLE",
                        severity = "LOW",
                        description = "Long text content should be selectable",
                        viewId = view.id.toString(),
                        fix = "Make long text selectable"
                    )
                )
            }
        }
    }
    
    private fun checkFocusability(view: View, issues: MutableList<AccessibilityIssue>) {
        if (view.isClickable && !view.isFocusable) {
            issues.add(
                AccessibilityIssue(
                    type = "CLICKABLE_NOT_FOCUSABLE",
                    severity = "HIGH",
                    description = "Clickable view is not keyboard focusable",
                    viewId = view.id.toString(),
                    fix = "Make clickable views focusable"
                )
            )
        }
    }
    
    private fun fixTouchTargetSize(view: View) {
        if (view.isClickable || view.isFocusable) {
            val density = context.resources.displayMetrics.density
            val minSizePx = (MIN_TOUCH_TARGET_SIZE_DP * density).toInt()
            
            if (view.width < minSizePx || view.height < minSizePx) {
                view.minimumWidth = minSizePx
                view.minimumHeight = minSizePx
                
                // Apply padding to maintain visual appearance
                val paddingDiff = (minSizePx - maxOf(view.width, view.height)) / 2
                if (paddingDiff > 0) {
                    view.setPadding(
                        view.paddingLeft + paddingDiff,
                        view.paddingTop + paddingDiff,
                        view.paddingRight + paddingDiff,
                        view.paddingBottom + paddingDiff
                    )
                }
            }
        }
    }
    
    private fun fixContentDescription(view: View) {
        when (view) {
            is ImageView -> {
                if (view.contentDescription.isNullOrBlank() && view.drawable != null) {
                    // Generate semantic content description based on context
                    view.contentDescription = generateImageDescription(view)
                }
            }
            is Button -> {
                if (view.contentDescription.isNullOrBlank() && view.text.isNullOrBlank()) {
                    view.contentDescription = generateButtonDescription(view)
                }
            }
        }
    }
    
    private fun fixTextAccessibility(view: View) {
        if (view is TextView) {
            // Fix text size if too small
            val textSizeSp = view.textSize / context.resources.displayMetrics.scaledDensity
            if (textSizeSp < MIN_TEXT_SIZE_SP) {
                view.textSize = MIN_TEXT_SIZE_SP.toFloat()
            }
            
            // Make long text selectable
            if (view.text.length > 50 && !view.isTextSelectable) {
                view.setTextIsSelectable(true)
            }
        }
    }
    
    private fun fixFocusability(view: View) {
        if (view.isClickable && !view.isFocusable) {
            view.isFocusable = true
            view.isFocusableInTouchMode = false // Keyboard focus only
        }
    }
    
    private fun applySemanticEnhancements(view: View) {
        // Apply semantic roles for better screen reader experience
        when (view) {
            is Button -> {
                ViewCompat.setAccessibilityDelegate(view, object : androidx.core.view.AccessibilityDelegateCompat() {
                    override fun onInitializeAccessibilityNodeInfo(host: View, info: AccessibilityNodeInfoCompat) {
                        super.onInitializeAccessibilityNodeInfo(host, info)
                        info.roleDescription = "Button"
                        info.addAction(AccessibilityNodeInfoCompat.AccessibilityActionCompat.ACTION_CLICK)
                    }
                })
            }
        }
        
        // Add state descriptions for dynamic content
        if (view.hasOnClickListeners()) {
            ViewCompat.setStateDescription(view, "Clickable")
        }
    }
    
    private fun generateImageDescription(imageView: ImageView): String {
        // Generate context-aware descriptions
        return when (imageView.id) {
            // Connection status icons
            android.R.id.icon -> "Connection status indicator"
            // Server flags
            else -> {
                val parent = imageView.parent
                if (parent is ViewGroup) {
                    // Look for nearby text to provide context
                    for (i in 0 until parent.childCount) {
                        val sibling = parent.getChildAt(i)
                        if (sibling is TextView && !sibling.text.isNullOrBlank()) {
                            return "Icon for ${sibling.text}"
                        }
                    }
                }
                "Image"
            }
        }
    }
    
    private fun generateButtonDescription(button: Button): String {
        // Generate button descriptions based on context
        return when (button.id) {
            // Connection buttons
            android.R.id.button1 -> "Connect button"
            android.R.id.button2 -> "Disconnect button"
            else -> "Button"
        }
    }
    
    private fun calculateAccessibilityScore(issueCount: Int, totalViews: Int): Int {
        if (totalViews == 0) return 100
        
        val issueRate = issueCount.toDouble() / totalViews
        return ((1 - issueRate) * 100).coerceIn(0.0, 100.0).toInt()
    }
    
    private fun getTotalViewCount(rootView: View): Int {
        var count = 1
        if (rootView is ViewGroup) {
            for (i in 0 until rootView.childCount) {
                count += getTotalViewCount(rootView.getChildAt(i))
            }
        }
        return count
    }
}

data class AccessibilityReport(
    val score: Int,
    val issues: List<AccessibilityIssue>,
    val suggestions: List<String>,
    val totalViews: Int
) {
    val isCompliant: Boolean get() = score >= 95
    val criticalIssues: List<AccessibilityIssue> get() = issues.filter { it.severity == "CRITICAL" }
    val highIssues: List<AccessibilityIssue> get() = issues.filter { it.severity == "HIGH" }
}

data class AccessibilityIssue(
    val type: String,
    val severity: String, // CRITICAL, HIGH, MEDIUM, LOW
    val description: String,
    val viewId: String,
    val fix: String
)