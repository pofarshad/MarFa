# 🤖 MarFaNet AutoFixBot - Production Deployment Guide

**Revolutionary AI-driven CI remediation pipeline that automatically fixes failing GitHub Actions!**

## 🚀 Quick Start

### 1. Create GitHub App
1. Go to **Settings → Developer settings → GitHub Apps → New GitHub App**
2. Configure these settings:
   - **App name**: `MarFaNet-AutoFixBot`
   - **Webhook URL**: `https://your-replit-app.replit.app/webhook`
   - **Webhook secret**: Generate and save for later
   
3. **Permissions needed**:
   - Actions: **Read**
   - Contents: **Read & Write**
   - Pull requests: **Read & Write**  
   - Issues: **Read & Write**

4. **Generate private key** → Download `marfanet-autofix.pem`
5. **Install app** on your MarFaNet repository

### 2. Deploy to Replit
1. **Create new Node.js project** in Replit
2. **Upload these files** to your Replit project
3. **Set environment variables**:
   ```
   GITHUB_APP_ID=your_app_id
   GITHUB_PRIVATE_KEY="-----BEGIN RSA PRIVATE KEY-----\n...\n-----END RSA PRIVATE KEY-----"
   WEBHOOK_SECRET=your_webhook_secret
   OPENAI_API_KEY=your_openai_key
   PORT=3000
   ```

4. **Install dependencies**:
   ```bash
   npm install
   ```

5. **Start the bot**:
   ```bash
   npm start
   ```

### 3. Configure Repository
1. **Add repository secret**:
   - Name: `AUTOFIXBOT_URL`
   - Value: `https://your-replit-app.replit.app`

2. **The dispatcher workflow** is already configured and will automatically trigger AutoFixBot on CI failures!

## 🎯 How It Works

### Automatic Flow
1. **CI Fails** → GitHub Actions workflow completes with failure
2. **Dispatcher Triggers** → `dispatch-autofix.yml` detects failure and calls AutoFixBot
3. **AI Analysis** → AutoFixBot downloads logs and sends to OpenAI for analysis
4. **Fix Generation** → AI creates either a git patch or explanatory guidance
5. **PR Creation** → Bot automatically creates draft pull request with the fix
6. **Human Review** → Team reviews and merges the automated fix

### Safety Features
- ✅ **Draft PRs only** - Human approval required
- ✅ **File scope limits** - Won't touch sensitive files
- ✅ **Rate limiting** - One fix per workflow run
- ✅ **Comprehensive logging** - Full audit trail

## 🔧 Supported Fix Types

AutoFixBot specializes in common Android CI issues:
- **Missing Gradle wrapper files**
- **Incorrect file permissions** 
- **Deprecated GitHub Actions**
- **Android SDK/NDK configuration**
- **Dependency version conflicts**
- **Build script syntax errors**

## 📊 Monitoring

### Health Check
Visit: `https://your-replit-app.replit.app/health`

### Logs
Check your Replit console for real-time AutoFixBot activity:
- 🚨 Failure detection
- 🤖 AI analysis progress
- 🔧 Fix application status
- ✅ PR creation confirmation

## 🛡️ Security & Governance

### Access Control
- GitHub App permissions scope to minimum required
- Private key stored securely in Replit environment
- Webhook signature verification for all requests

### Quality Gates
- All fixes create **draft PRs** requiring human review
- Bot won't modify sensitive files (keystores, secrets)
- Rate limiting prevents spam
- Comprehensive audit logging

## 🚀 Roll-out Strategy

### Stage 1: Testing (Recommended)
1. **Fork repository** for testing
2. **Trigger failing CI** run intentionally
3. **Validate AutoFix PR** creation and quality
4. **Review AI-generated patches** for accuracy

### Stage 2: Pilot Deployment
1. **Enable on main repository**
2. **Restrict to develop branch** initially
3. **Monitor for 1 week** incident-free operation
4. **Gather team feedback** on fix quality

### Stage 3: Full Production
1. **Enable for all branches** after successful pilot
2. **Monitor OpenAI usage** and costs
3. **Iterate on prompts** based on fix success rate
4. **Scale to additional repositories**

## 💡 Maintenance

### Regular Tasks
- **Rotate GitHub App private key** annually
- **Monitor OpenAI API usage** and limits
- **Update dependencies** for security patches
- **Review and optimize AI prompts** based on success rate

### Troubleshooting
- **Webhook not receiving events**: Check GitHub App installation and permissions
- **AI not generating good fixes**: Review and refine the prompt in `generateFix()`
- **PRs not being created**: Verify repository write permissions and branch protection rules

---

**🎉 Ready to revolutionize your CI pipeline with AI-powered automatic fixes!**

*Your AutoFixBot will now catch every CI failure and generate intelligent fixes within minutes!*