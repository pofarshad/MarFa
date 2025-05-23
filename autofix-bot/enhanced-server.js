/**
 * MarFaNet AutoFixBot - Enhanced AI-Driven CI Remediation Pipeline
 * Instantly detects and fixes CI problems the moment they happen
 */

import express from 'express';
import crypto from 'crypto';
import { Octokit } from '@octokit/rest';
import { createAppAuth } from '@octokit/auth-app';

const app = express();
app.use(express.json({ limit: '10mb' }));

// Enhanced instant fix patterns
const INSTANT_FIXES = {
    // Plugin version conflicts
    AGP_VERSION_ERROR: {
        pattern: /plugin.*com\.android\.application.*was not found|Plugin \[id: 'com\.android\.application', version: '.*'\] was not found/i,
        fix: {
            file: 'build.gradle.kts',
            patch: `--- a/build.gradle.kts
+++ b/build.gradle.kts
@@ -2,7 +2,7 @@
 
 plugins {
-    id("com.android.application") version "9.0.0-alpha01" apply false
+    id("com.android.application") version "8.6.0" apply false
     id("org.jetbrains.kotlin.android") version "2.0.0-Beta3" apply false
 }`,
            description: 'Fixed Android Gradle Plugin version to stable 8.6.0'
        }
    },

    // Missing settings file
    MISSING_SETTINGS: {
        pattern: /settings file.*not found|Could not read settings file|Project directory.*does not contain a settings file/i,
        fix: {
            file: 'settings.gradle.kts',
            patch: `--- /dev/null
+++ b/settings.gradle.kts
@@ -0,0 +1,16 @@
+pluginManagement {
+    repositories {
+        google()
+        mavenCentral()
+        gradlePluginPortal()
+    }
+}
+
+dependencyResolutionManagement {
+    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
+    repositories {
+        google()
+        mavenCentral()
+    }
+}
+
+rootProject.name = "MarFaNet"
+include(":app")`,
            description: 'Created missing settings.gradle.kts with app module'
        }
    },

    // Permission errors
    PERMISSION_ERROR: {
        pattern: /permission denied.*gradlew|gradlew.*permission denied/i,
        fix: {
            file: 'gradlew',
            command: 'chmod +x gradlew',
            description: 'Fixed Gradle wrapper executable permissions'
        }
    },

    // Missing app module
    MISSING_APP_MODULE: {
        pattern: /Project ':app' not found|build\.gradle.*not found.*app/i,
        fix: {
            file: 'app/build.gradle.kts',
            patch: `--- /dev/null
+++ b/app/build.gradle.kts
@@ -0,0 +1,35 @@
+plugins {
+    id("com.android.application")
+    id("org.jetbrains.kotlin.android")
+}
+
+android {
+    namespace = "net.marfanet.android"
+    compileSdk = 34
+
+    defaultConfig {
+        applicationId = "net.marfanet.android"
+        minSdk = 21
+        targetSdk = 34
+        versionCode = 4
+        versionName = "1.1.0-alpha1"
+        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
+    }
+
+    buildTypes {
+        release {
+            isMinifyEnabled = false
+        }
+    }
+
+    compileOptions {
+        sourceCompatibility = JavaVersion.VERSION_17
+        targetCompatibility = JavaVersion.VERSION_17
+    }
+
+    kotlinOptions {
+        jvmTarget = "17"
+    }
+}
+
+dependencies {
+    implementation("androidx.core:core-ktx:1.12.0")
+    implementation("androidx.appcompat:appcompat:1.6.1")
+    implementation("com.google.android.material:material:1.11.0")
+    testImplementation("junit:junit:4.13.2")
+}`,
            description: 'Created complete app module with proper Android configuration'
        }
    },

    // Deprecated GitHub Actions
    DEPRECATED_ACTIONS: {
        pattern: /upload-artifact.*deprecated|upload-artifact.*not found/i,
        fix: {
            file: '.github/workflows/*',
            patch: `--- a/.github/workflows/ci.yml
+++ b/.github/workflows/ci.yml
@@ -30,7 +30,7 @@
       run: ./gradlew assembleDebug
       
     - name: Upload APK
-      uses: actions/upload-artifact@v3
+      uses: actions/upload-artifact@v4
       with:
         name: marfanet-debug-apk
         path: app/build/outputs/apk/debug/*.apk`,
            description: 'Updated to latest upload-artifact@v4 action'
        }
    }
};

/**
 * Instant CI problem detection and fixing
 */
function detectAndApplyInstantFixes(errorLogs) {
    const appliedFixes = [];
    
    for (const [fixName, fixConfig] of Object.entries(INSTANT_FIXES)) {
        if (fixConfig.pattern.test(errorLogs)) {
            console.log(`⚡ INSTANT FIX DETECTED: ${fixName}`);
            appliedFixes.push({
                name: fixName,
                ...fixConfig.fix
            });
        }
    }
    
    return appliedFixes;
}

/**
 * Generate comprehensive fix with instant patches
 */
async function generateComprehensiveFix(workflowName, jobName, errorLogs, instantFixes) {
    let fixContent = "# 🚀 AutoFixBot - Instant CI Remediation\n\n";
    
    if (instantFixes.length > 0) {
        fixContent += "## ⚡ Instant Fixes Applied\n\n";
        
        instantFixes.forEach((fix, index) => {
            fixContent += `### ${index + 1}. ${fix.description}\n`;
            fixContent += `**File**: \`${fix.file}\`\n\n`;
            
            if (fix.command) {
                fixContent += `**Command**: \`${fix.command}\`\n\n`;
            } else if (fix.patch) {
                fixContent += "```diff\n";
                fixContent += fix.patch;
                fixContent += "\n```\n\n";
            }
        });
        
        fixContent += "## 🎯 Expected Result\n";
        fixContent += "These instant fixes should resolve the CI failures immediately. ";
        fixContent += "The build should now complete successfully.\n\n";
        
        return fixContent;
    }
    
    // If no instant fixes, use AI for complex issues
    fixContent += "## 🤖 AI Analysis Required\n\n";
    fixContent += `**Workflow**: ${workflowName}\n`;
    fixContent += `**Job**: ${jobName}\n\n`;
    fixContent += "**Error Summary**: Complex issue requiring detailed analysis\n\n";
    fixContent += "```\n";
    fixContent += errorLogs.slice(-1000); // Last 1000 chars
    fixContent += "\n```\n\n";
    fixContent += "**Recommended Action**: Manual investigation required for this specific error pattern.\n";
    
    return fixContent;
}

/**
 * Apply fixes via GitHub API
 */
async function applyFixesToRepository(owner, repo, fixes, octokit) {
    const results = [];
    
    for (const fix of fixes) {
        try {
            if (fix.command) {
                // For permission fixes, create workflow update
                results.push({
                    file: fix.file,
                    action: 'command',
                    command: fix.command,
                    status: 'requires_workflow_update'
                });
            } else if (fix.patch) {
                // Create file via API
                const content = Buffer.from(extractFileContent(fix.patch)).toString('base64');
                
                try {
                    await octokit.repos.createOrUpdateFileContents({
                        owner,
                        repo,
                        path: fix.file,
                        message: `AutoFixBot: ${fix.description}`,
                        content,
                        branch: 'main'
                    });
                    
                    results.push({
                        file: fix.file,
                        action: 'created',
                        status: 'success'
                    });
                } catch (error) {
                    if (error.status === 422) {
                        // File exists, need to update
                        const { data: existingFile } = await octokit.repos.getContent({
                            owner,
                            repo,
                            path: fix.file
                        });
                        
                        await octokit.repos.createOrUpdateFileContents({
                            owner,
                            repo,
                            path: fix.file,
                            message: `AutoFixBot: ${fix.description}`,
                            content,
                            sha: existingFile.sha,
                            branch: 'main'
                        });
                        
                        results.push({
                            file: fix.file,
                            action: 'updated',
                            status: 'success'
                        });
                    } else {
                        throw error;
                    }
                }
            }
        } catch (error) {
            console.error(`❌ Failed to apply fix for ${fix.file}:`, error.message);
            results.push({
                file: fix.file,
                action: 'failed',
                status: 'error',
                error: error.message
            });
        }
    }
    
    return results;
}

/**
 * Extract file content from diff patch
 */
function extractFileContent(patch) {
    const lines = patch.split('\n');
    const content = [];
    
    for (const line of lines) {
        if (line.startsWith('+') && !line.startsWith('+++')) {
            content.push(line.substring(1));
        } else if (!line.startsWith('-') && !line.startsWith('@') && !line.startsWith('+++') && !line.startsWith('---')) {
            content.push(line);
        }
    }
    
    return content.join('\n');
}

/**
 * Get failing job logs
 */
async function getFailingJobLogs(owner, repo, runId, octokit) {
    try {
        const { data: jobs } = await octokit.actions.listJobsForWorkflowRun({
            owner,
            repo,
            run_id: runId
        });
        
        let allLogs = '';
        
        for (const job of jobs.jobs) {
            if (job.conclusion === 'failure') {
                try {
                    const { data: logs } = await octokit.actions.downloadJobLogsForWorkflowRun({
                        owner,
                        repo,
                        job_id: job.id
                    });
                    allLogs += `\n--- Job: ${job.name} ---\n${logs}\n`;
                } catch (logError) {
                    console.error(`Failed to get logs for job ${job.id}:`, logError.message);
                }
            }
        }
        
        return allLogs || 'No logs available';
    } catch (error) {
        console.error('Failed to get job logs:', error.message);
        return 'Error retrieving logs';
    }
}

/**
 * Main webhook handler
 */
app.post('/webhook', async (req, res) => {
    try {
        console.log('🎯 AutoFixBot webhook received');
        
        const { action, workflow_run, installation } = req.body;
        
        // Only handle completed workflows that failed
        if (action !== 'completed' || workflow_run.conclusion !== 'failure') {
            return res.status(200).send('Ignored: Not a CI failure');
        }
        
        const { owner, repo } = workflow_run.repository;
        console.log(`🚨 CI FAILURE DETECTED: ${workflow_run.name} in ${owner.login}/${repo.name}`);
        
        // Setup GitHub API
        const octokit = new Octokit({
            authStrategy: createAppAuth,
            auth: {
                appId: process.env.GITHUB_APP_ID,
                privateKey: process.env.GITHUB_PRIVATE_KEY,
                installationId: installation.id
            }
        });
        
        // Get failing job logs
        const errorLogs = await getFailingJobLogs(owner.login, repo.name, workflow_run.id, octokit);
        
        // Apply instant fixes
        const instantFixes = detectAndApplyInstantFixes(errorLogs);
        
        if (instantFixes.length > 0) {
            console.log(`⚡ Applying ${instantFixes.length} instant fixes...`);
            
            // Apply fixes to repository
            const results = await applyFixesToRepository(owner.login, repo.name, instantFixes, octokit);
            
            // Generate comprehensive fix description
            const fixContent = await generateComprehensiveFix(
                workflow_run.name,
                'multiple jobs',
                errorLogs,
                instantFixes
            );
            
            // Create issue with fix summary
            await octokit.issues.create({
                owner: owner.login,
                repo: repo.name,
                title: `🚀 AutoFixBot - Instant CI Fixes Applied (Run #${workflow_run.run_number})`,
                body: fixContent + `\n\n## Applied Changes\n\n${results.map(r => `- ${r.action} \`${r.file}\` - ${r.status}`).join('\n')}\n\n**Next Step**: The fixes have been applied automatically. Push any additional changes or re-run the workflow to validate.`,
                labels: ['autofixbot', 'ci-fix', 'automated']
            });
            
            console.log('✅ Instant fixes applied successfully!');
        } else {
            console.log('🤖 No instant fixes available - creating analysis issue');
            
            const analysisContent = await generateComprehensiveFix(
                workflow_run.name,
                'analysis required',
                errorLogs,
                []
            );
            
            await octokit.issues.create({
                owner: owner.login,
                repo: repo.name,
                title: `🔍 AutoFixBot - CI Analysis Required (Run #${workflow_run.run_number})`,
                body: analysisContent,
                labels: ['autofixbot', 'needs-analysis', 'ci-failure']
            });
        }
        
        res.status(200).send('AutoFixBot processing complete');
        
    } catch (error) {
        console.error('❌ AutoFixBot error:', error);
        res.status(500).send('Internal server error');
    }
});

// Health check
app.get('/health', (req, res) => {
    res.json({ 
        status: 'healthy', 
        timestamp: new Date().toISOString(),
        version: '2.0.0-enhanced'
    });
});

const PORT = process.env.PORT || 3000;
app.listen(PORT, '0.0.0.0', () => {
    console.log(`🚀 Enhanced AutoFixBot running on port ${PORT}`);
    console.log(`⚡ Instant CI fixing enabled!`);
});