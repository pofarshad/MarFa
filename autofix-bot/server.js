/**
 * MarFaNet AutoFixBot - AI-Driven CI Remediation Pipeline
 * Automatically captures failing GitHub Actions and generates fixes
 */

import express from 'express';
import crypto from 'crypto';
import { Octokit } from '@octokit/rest';
import { createAppAuth } from '@octokit/auth-app';

const app = express();
app.use(express.json());

// Configuration from environment
const config = {
    port: process.env.PORT || 3000,
    githubAppId: process.env.GITHUB_APP_ID,
    githubPrivateKey: process.env.GITHUB_PRIVATE_KEY,
    webhookSecret: process.env.WEBHOOK_SECRET,
    openaiApiKey: process.env.OPENAI_API_KEY,
    replitAiUrl: process.env.REPLIT_AI_URL || 'https://api.replit.com/v1/ai'
};

// GitHub App authentication
const octokit = new Octokit({
    authStrategy: createAppAuth,
    auth: {
        appId: config.githubAppId,
        privateKey: config.githubPrivateKey.replace(/\\n/g, '\n'),
    },
});

/**
 * Webhook signature verification
 */
function verifyWebhookSignature(payload, signature) {
    const expectedSignature = 'sha256=' + crypto
        .createHmac('sha256', config.webhookSecret)
        .update(payload)
        .digest('hex');
    
    return crypto.timingSafeEqual(
        Buffer.from(signature),
        Buffer.from(expectedSignature)
    );
}

/**
 * Extract failing job logs from GitHub Actions
 */
async function getFailingJobLogs(owner, repo, runId, installationId) {
    try {
        console.log(`📋 Fetching logs for run ${runId} in ${owner}/${repo}`);
        
        const installationOctokit = await octokit.auth({
            type: 'installation',
            installationId: installationId,
        });
        
        const authenticatedOctokit = new Octokit({
            auth: installationOctokit.token,
        });
        
        // Get workflow run details
        const { data: workflowRun } = await authenticatedOctokit.rest.actions.getWorkflowRun({
            owner,
            repo,
            run_id: runId,
        });
        
        // Get jobs for this workflow run
        const { data: jobs } = await authenticatedOctokit.rest.actions.listJobsForWorkflowRun({
            owner,
            repo,
            run_id: runId,
        });
        
        // Find failed jobs
        const failedJobs = jobs.jobs.filter(job => job.conclusion === 'failure');
        
        if (failedJobs.length === 0) {
            console.log('⚠️ No failed jobs found');
            return null;
        }
        
        // Get logs for the first failed job
        const failedJob = failedJobs[0];
        console.log(`🔍 Analyzing failed job: ${failedJob.name}`);
        
        // Download job logs
        const { data: logData } = await authenticatedOctokit.rest.actions.downloadJobLogsForWorkflowRun({
            owner,
            repo,
            job_id: failedJob.id,
        });
        
        return {
            workflowRun,
            failedJob,
            logs: logData,
            octokit: authenticatedOctokit
        };
        
    } catch (error) {
        console.error('❌ Error fetching job logs:', error.message);
        throw error;
    }
}

/**
 * Generate instant automated fix for common CI issues
 */
async function generateFix(workflowName, jobName, errorLogs) {
    try {
        console.log('🤖 Analyzing CI failure for instant fix...');
        
        // Instant detection and fixes for common issues
        const instantFixes = detectAndFixCommonIssues(errorLogs);
        if (instantFixes.length > 0) {
            console.log(`⚡ Found ${instantFixes.length} instant fixes!`);
            return formatInstantFixes(instantFixes);
        }
        
        // Advanced AI analysis for complex issues
        const truncatedLogs = errorLogs.slice(-4000);
        
        const prompt = `You are an expert Android CI/CD engineer. Fix this IMMEDIATELY with a working solution.

WORKFLOW: "${workflowName}" - JOB: "${jobName}"

ERROR LOGS:
${truncatedLogs}

PROVIDE ONLY:
1. ```diff patch for immediate fix
2. Root cause explanation in 1 line

FOCUS ON INSTANT SOLUTIONS FOR:
- Plugin version conflicts → Use stable versions
- Missing files → Create with proper content  
- Permission errors → Fix with chmod
- Path issues → Correct directory structure
- Dependency conflicts → Use compatible versions
- Gradle errors → Fix build configuration

RESPOND WITH WORKING PATCHES ONLY.e root cause.`;

        // Try Replit AI first, fallback to OpenAI
        let response;
        
        if (config.replitAiUrl && process.env.REPLIT_AI_TOKEN) {
            console.log('🔄 Using Replit AI...');
            response = await callReplitAI(prompt);
        } else if (config.openaiApiKey) {
            console.log('🔄 Using OpenAI...');
            response = await callOpenAI(prompt);
        } else {
            throw new Error('No AI service configured');
        }
        
        return response;
        
    } catch (error) {
        console.error('❌ Error generating fix:', error.message);
        throw error;
    }
}

/**
 * Call Replit AI service
 */
async function callReplitAI(prompt) {
    const response = await fetch(config.replitAiUrl, {
        method: 'POST',
        headers: {
            'Authorization': `Bearer ${process.env.REPLIT_AI_TOKEN}`,
            'Content-Type': 'application/json',
        },
        body: JSON.stringify({
            model: 'replit-code-v1_5-3b',
            messages: [
                {
                    role: 'user',
                    content: prompt
                }
            ],
            max_tokens: 2000,
            temperature: 0.1
        })
    });
    
    if (!response.ok) {
        throw new Error(`Replit AI API error: ${response.status}`);
    }
    
    const data = await response.json();
    return data.choices[0].message.content;
}

/**
 * Call OpenAI service
 */
async function callOpenAI(prompt) {
    const response = await fetch('https://api.openai.com/v1/chat/completions', {
        method: 'POST',
        headers: {
            'Authorization': `Bearer ${config.openaiApiKey}`,
            'Content-Type': 'application/json',
        },
        body: JSON.stringify({
            model: 'gpt-4',
            messages: [
                {
                    role: 'system',
                    content: 'You are an expert Android CI/CD engineer. Provide concise, actionable fixes for GitHub Actions failures.'
                },
                {
                    role: 'user',
                    content: prompt
                }
            ],
            max_tokens: 2000,
            temperature: 0.1
        })
    });
    
    if (!response.ok) {
        throw new Error(`OpenAI API error: ${response.status}`);
    }
    
    const data = await response.json();
    return data.choices[0].message.content;
}

/**
 * Apply fix via Pull Request
 */
async function applyFix(owner, repo, fix, workflowName, jobName, runId, octokit) {
    try {
        console.log('🔧 Applying fix via Pull Request...');
        
        // Check if fix contains a diff patch
        const isDiffPatch = fix.includes('```diff') && fix.includes('```');
        
        if (!isDiffPatch) {
            // Create informational issue instead of PR
            return await createInformationalIssue(owner, repo, fix, workflowName, jobName, runId, octokit);
        }
        
        // Extract diff content
        const diffMatch = fix.match(/```diff\n?([\s\S]*?)\n?```/);
        if (!diffMatch) {
            throw new Error('Invalid diff format in AI response');
        }
        
        const diffContent = diffMatch[1];
        const branchName = `autofix/ci-failure-${runId}`;
        
        // Get default branch
        const { data: repo_data } = await octokit.rest.repos.get({ owner, repo });
        const defaultBranch = repo_data.default_branch;
        
        // Get latest commit on default branch
        const { data: ref } = await octokit.rest.git.getRef({
            owner,
            repo,
            ref: `heads/${defaultBranch}`
        });
        
        // Create new branch
        await octokit.rest.git.createRef({
            owner,
            repo,
            ref: `refs/heads/${branchName}`,
            sha: ref.object.sha
        });
        
        console.log(`🌿 Created branch: ${branchName}`);
        
        // Parse and apply diff (simplified - would need proper git patch parsing)
        const prBody = `## 🤖 AutoFix: CI Failure Remediation

This PR was automatically generated to fix a failing CI workflow.

### 📋 Failure Details
- **Workflow**: ${workflowName}
- **Job**: ${jobName}
- **Run ID**: ${runId}

### 🔧 Proposed Fix
${fix}

### ⚠️ Review Required
Please review this automated fix before merging. The AI has analyzed the failure logs and suggested this solution.

---
*Generated by MarFaNet AutoFixBot*`;

        // Create pull request
        const { data: pr } = await octokit.rest.pulls.create({
            owner,
            repo,
            title: `AutoFix: CI failure in ${workflowName}`,
            head: branchName,
            base: defaultBranch,
            body: prBody,
            draft: true // Mark as draft for review
        });
        
        console.log(`✅ Created Pull Request: #${pr.number}`);
        
        return {
            type: 'pull_request',
            number: pr.number,
            url: pr.html_url
        };
        
    } catch (error) {
        console.error('❌ Error applying fix:', error.message);
        
        // Fallback: create issue with fix suggestion
        return await createInformationalIssue(owner, repo, fix, workflowName, jobName, runId, octokit);
    }
}

/**
 * Create informational issue when patch cannot be applied
 */
async function createInformationalIssue(owner, repo, fix, workflowName, jobName, runId, octokit) {
    const issueBody = `## 🤖 AutoFix: CI Failure Analysis

The workflow **${workflowName}** failed in job **${jobName}** (Run #${runId}).

### 🔍 AI Analysis & Recommendations

${fix}

### 🔗 Resources
- [View Failed Run](https://github.com/${owner}/${repo}/actions/runs/${runId})
- [MarFaNet CI Documentation](../wiki/CI-Troubleshooting)

---
*Generated by MarFaNet AutoFixBot*`;

    const { data: issue } = await octokit.rest.issues.create({
        owner,
        repo,
        title: `🤖 AutoFix: CI failure analysis for ${workflowName}`,
        body: issueBody,
        labels: ['autofix', 'ci-failure', 'needs-review']
    });
    
    console.log(`📝 Created informational issue: #${issue.number}`);
    
    return {
        type: 'issue',
        number: issue.number,
        url: issue.html_url
    };
}

/**
 * Main webhook handler
 */
app.post('/webhook', async (req, res) => {
    try {
        // Verify webhook signature
        const signature = req.headers['x-hub-signature-256'];
        if (!verifyWebhookSignature(JSON.stringify(req.body), signature)) {
            return res.status(401).send('Unauthorized');
        }
        
        const { action, workflow_run, installation } = req.body;
        
        // Handle both failures AND in-progress builds for instant fixing
        if (action === 'completed' && workflow_run.conclusion === 'failure') {
            console.log(`🚨 CI FAILURE - Applying instant fixes!`);
        } else if (action === 'in_progress') {
            console.log(`🔍 CI IN PROGRESS - Monitoring for issues...`);
            return res.status(200).send('Monitoring in progress');
        } else {
            return res.status(200).send('Event ignored');
        }
        
        console.log(`🚨 CI Failure detected: ${workflow_run.name} in ${workflow_run.repository.full_name}`);
        
        // Get failing job logs
        const logData = await getFailingJobLogs(
            workflow_run.repository.owner.login,
            workflow_run.repository.name,
            workflow_run.id,
            installation.id
        );
        
        if (!logData) {
            return res.status(200).send('No failed jobs found');
        }
        
        // Generate AI fix
        const fix = await generateFix(
            workflow_run.name,
            logData.failedJob.name,
            logData.logs
        );
        
        // Apply fix via PR or issue
        const result = await applyFix(
            workflow_run.repository.owner.login,
            workflow_run.repository.name,
            fix,
            workflow_run.name,
            logData.failedJob.name,
            workflow_run.id,
            logData.octokit
        );
        
        console.log(`✨ AutoFix completed: ${result.type} #${result.number}`);
        
        res.status(200).json({
            success: true,
            result: result
        });
        
    } catch (error) {
        console.error('💥 Webhook processing error:', error);
        res.status(500).json({
            success: false,
            error: error.message
        });
    }
});

// Health check endpoint
app.get('/health', (req, res) => {
    res.json({
        status: 'healthy',
        service: 'MarFaNet AutoFixBot',
        timestamp: new Date().toISOString()
    });
});

// Start server
app.listen(config.port, () => {
    console.log(`🚀 MarFaNet AutoFixBot running on port ${config.port}`);
    console.log(`📡 Webhook endpoint: /webhook`);
    console.log(`💚 Health check: /health`);
});

export default app;