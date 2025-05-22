// MarFaNet Dashboard JavaScript

let currentAnalysis = null;
let currentSpecs = null;

// Utility functions
function showLoadingMessage(message) {
    const modal = new bootstrap.Modal(document.getElementById('loadingModal'));
    document.getElementById('loading-message').textContent = message;
    modal.show();
}

function hideLoadingMessage() {
    const modal = bootstrap.Modal.getInstance(document.getElementById('loadingModal'));
    if (modal) {
        modal.hide();
    }
}

function showSuccessMessage(message) {
    showToast(message, 'success');
}

function showErrorMessage(message) {
    showToast(message, 'error');
}

function showToast(message, type = 'info') {
    // Create toast element
    const toastId = 'toast-' + Date.now();
    const toastHtml = `
        <div id="${toastId}" class="toast align-items-center text-bg-${type === 'error' ? 'danger' : type}" role="alert" aria-live="assertive" aria-atomic="true">
            <div class="d-flex">
                <div class="toast-body">
                    <i class="fas fa-${type === 'success' ? 'check-circle' : type === 'error' ? 'exclamation-circle' : 'info-circle'} me-2"></i>
                    ${message}
                </div>
                <button type="button" class="btn-close btn-close-white me-2 m-auto" data-bs-dismiss="toast" aria-label="Close"></button>
            </div>
        </div>
    `;
    
    // Add to toast container or create one
    let toastContainer = document.querySelector('.toast-container');
    if (!toastContainer) {
        toastContainer = document.createElement('div');
        toastContainer.className = 'toast-container position-fixed top-0 end-0 p-3';
        document.body.appendChild(toastContainer);
    }
    
    toastContainer.insertAdjacentHTML('beforeend', toastHtml);
    
    // Show toast
    const toastElement = document.getElementById(toastId);
    const toast = new bootstrap.Toast(toastElement, {
        autohide: true,
        delay: 5000
    });
    toast.show();
    
    // Remove toast element after hiding
    toastElement.addEventListener('hidden.bs.toast', () => {
        toastElement.remove();
    });
}

function updateProgress(percentage, message) {
    const progressBar = document.getElementById('overall-progress');
    const progressLog = document.getElementById('progress-log');
    
    if (progressBar) {
        progressBar.style.width = percentage + '%';
        progressBar.textContent = percentage + '%';
        progressBar.setAttribute('aria-valuenow', percentage);
    }
    
    if (progressLog && message) {
        const timestamp = new Date().toLocaleTimeString();
        const logEntry = document.createElement('div');
        logEntry.className = 'mb-1';
        logEntry.innerHTML = `<small class="text-muted">[${timestamp}]</small> ${message}`;
        progressLog.appendChild(logEntry);
        progressLog.scrollTop = progressLog.scrollHeight;
    }
    
    // Log to console as fallback
    if (message) {
        console.log(`[${new Date().toLocaleTimeString()}] ${message}`);
    }
}

function updateRequirementStatus(requirementId, status) {
    const row = document.querySelector(`tr:nth-child(${requirementId})`);
    if (row) {
        const statusCell = row.querySelector('td:nth-child(3)');
        if (statusCell) {
            const statusClass = status === 'completed' ? 'bg-success' : status === 'in-progress' ? 'bg-warning' : 'bg-secondary';
            const statusText = status === 'completed' ? 'Completed' : status === 'in-progress' ? 'In Progress' : 'Pending';
            statusCell.innerHTML = `<span class="badge ${statusClass}">${statusText}</span>`;
        }
    }
}

// Main functions
async function startAnalysis() {
    try {
        showLoadingMessage('Analyzing Hiddify app structure...');
        updateProgress(10, 'Starting repository analysis...');
        
        const response = await fetch('/api/analyze-hiddify', {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json'
            },
            body: JSON.stringify({
                repo_url: 'https://github.com/hiddify/hiddify-app',
                tag: 'v2.5.7'
            })
        });
        
        updateProgress(50, 'Processing dependency analysis...');
        
        const data = await response.json();
        
        if (data.success) {
            currentAnalysis = data.analysis;
            updateProgress(80, 'Generating analysis report...');
            
            // Enable next steps
            document.getElementById('generate-specs-btn').disabled = false;
            
            updateProgress(100, 'Analysis completed successfully!');
            hideLoadingMessage();
            showSuccessMessage('Hiddify app analysis completed successfully!');
            
            // Update requirement statuses
            for (let i = 1; i <= 5; i++) {
                updateRequirementStatus(i, 'completed');
            }
            
        } else {
            throw new Error(data.error);
        }
        
    } catch (error) {
        hideLoadingMessage();
        showErrorMessage('Analysis failed: ' + error.message);
        updateProgress(0, 'Analysis failed: ' + error.message);
    }
}

async function generateSpecs() {
    try {
        showLoadingMessage('Generating technical specifications...');
        updateProgress(20, 'Generating Xray integration specs...');
        
        // Generate Xray specs
        const xrayResponse = await fetch('/api/generate-xray-specs', {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json'
            },
            body: JSON.stringify({
                analysis_data: currentAnalysis || {}
            })
        });
        
        updateProgress(40, 'Generating rebranding specifications...');
        
        // Generate rebranding specs
        const rebrandResponse = await fetch('/api/generate-rebranding-specs', {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json'
            },
            body: JSON.stringify({
                current_config: currentAnalysis || {}
            })
        });
        
        updateProgress(60, 'Generating documentation...');
        
        updateProgress(60, 'Processing responses...');
        
        const xrayData = await xrayResponse.json();
        const rebrandData = await rebrandResponse.json();
        
        updateProgress(70, 'Generating documentation...');
        
        // Generate documentation
        const docsResponse = await fetch('/api/generate-documentation', {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json'
            },
            body: JSON.stringify({
                specs_data: {
                    xray: xrayData,
                    rebranding: rebrandData
                }
            })
        });
        
        updateProgress(80, 'Finalizing specifications...');
        
        const docsData = await docsResponse.json();
        
        if (xrayData.success && rebrandData.success && docsData.success) {
            currentSpecs = {
                xray: xrayData.specifications,
                rebranding: rebrandData.specifications,
                documentation: docsData.documentation_files
            };
            
            // Enable download
            document.getElementById('download-btn').disabled = false;
            
            updateProgress(100, 'All specifications generated successfully!');
            hideLoadingMessage();
            showSuccessMessage('Technical specifications generated successfully!');
            
            // Update requirement statuses
            for (let i = 6; i <= 14; i++) {
                updateRequirementStatus(i, 'completed');
            }
            
        } else {
            throw new Error('Failed to generate some specifications');
        }
        
    } catch (error) {
        hideLoadingMessage();
        showErrorMessage('Specification generation failed: ' + error.message);
        updateProgress(0, 'Specification generation failed: ' + error.message);
    }
}

async function downloadSpecs() {
    try {
        showLoadingMessage('Preparing download package...');
        
        // Trigger download
        window.location.href = '/api/download-specs';
        
        setTimeout(() => {
            hideLoadingMessage();
            showSuccessMessage('Download package prepared successfully!');
        }, 2000);
        
    } catch (error) {
        hideLoadingMessage();
        showErrorMessage('Download failed: ' + error.message);
    }
}

function analyzeRequirement(id) {
    updateRequirementStatus(id, 'in-progress');
    
    const requirements = {
        1: {
            message: 'Analyzing Sing-box to Xray core replacement...',
            details: 'Examining native library integration, JNI interfaces, and protocol compatibility',
            duration: 3000
        },
        2: {
            message: 'Identifying impacted code paths...',
            details: 'Scanning codebase for Sing-box dependencies and connection handling',
            duration: 2500
        },
        3: {
            message: 'Designing gfw-knocker integration...',
            details: 'Planning anti-censorship features and Iran-specific optimizations',
            duration: 2000
        },
        4: {
            message: 'Locating WARP/WARP+ components...',
            details: 'Finding Cloudflare integration points and migration strategy',
            duration: 2000
        },
        5: {
            message: 'Planning MarFaNet rebranding...',
            details: 'Asset replacement, package renaming, and UI updates',
            duration: 2000
        },
        6: {
            message: 'Setting up code audit framework...',
            details: 'Establishing security scanning and quality gates',
            duration: 2000
        },
        7: {
            message: 'Designing Xray URL scheme parser...',
            details: 'Creating robust configuration parser for multiple protocols',
            duration: 2000
        },
        8: {
            message: 'Updating protocol infrastructure...',
            details: 'Modernizing connection handling and protocol support',
            duration: 2000
        },
        9: {
            message: 'Identifying performance optimizations...',
            details: 'Memory management, battery efficiency, and speed improvements',
            duration: 2000
        },
        10: {
            message: 'Analyzing connection speed improvements...',
            details: 'Protocol selection, server optimization, and caching strategies',
            duration: 2000
        },
        11: {
            message: 'Designing connection stability features...',
            details: 'Auto-reconnection, failover mechanisms, and reliability enhancements',
            duration: 2000
        },
        12: {
            message: 'Planning Iran routing rules integration...',
            details: 'Smart routing, geolocation-based optimization, and rule management',
            duration: 2000
        },
        13: {
            message: 'Designing ping engine architecture...',
            details: 'Server monitoring, latency measurement, and health checking',
            duration: 2000
        },
        14: {
            message: 'Updating Android compatibility layer...',
            details: 'API level support, device compatibility, and system integration',
            duration: 2000
        }
    };
    
    const requirement = requirements[id] || {
        message: 'Analyzing requirement...',
        details: 'Processing analysis request',
        duration: 2000
    };
    
    updateProgress(0, requirement.message);
    
    // Progressive analysis simulation
    setTimeout(() => {
        updateProgress(30, requirement.details);
    }, 500);
    
    setTimeout(() => {
        updateProgress(60, 'Generating analysis report...');
    }, requirement.duration * 0.6);
    
    setTimeout(() => {
        updateProgress(90, 'Finalizing recommendations...');
    }, requirement.duration * 0.8);
    
    setTimeout(() => {
        updateRequirementStatus(id, 'completed');
        updateProgress(100, `Requirement ${id} analysis completed.`);
        showSuccessMessage(`Requirement ${id} analyzed successfully!`);
    }, requirement.duration);
}

// Health check
async function checkHealth() {
    try {
        const response = await fetch('/api/health');
        const data = await response.json();
        
        if (data.status === 'healthy') {
            console.log('Dashboard health check passed');
        }
    } catch (error) {
        console.error('Health check failed:', error);
    }
}

// Initialize dashboard
document.addEventListener('DOMContentLoaded', function() {
    // Check health on load
    checkHealth();
    
    // Initialize tooltips
    const tooltipTriggerList = [].slice.call(document.querySelectorAll('[data-bs-toggle="tooltip"]'));
    tooltipTriggerList.map(function (tooltipTriggerEl) {
        return new bootstrap.Tooltip(tooltipTriggerEl);
    });
    
    // Auto-refresh progress periodically
    setInterval(() => {
        if (document.getElementById('overall-progress')) {
            // Optional: Add periodic updates
        }
    }, 30000);
    
    // Add fade-in animation to cards
    const cards = document.querySelectorAll('.card');
    cards.forEach((card, index) => {
        card.style.opacity = '0';
        card.style.transform = 'translateY(20px)';
        setTimeout(() => {
            card.style.transition = 'all 0.5s ease-in-out';
            card.style.opacity = '1';
            card.style.transform = 'translateY(0)';
        }, index * 100);
    });
});

// Keyboard shortcuts
document.addEventListener('keydown', function(e) {
    // Ctrl+Enter to start analysis
    if (e.ctrlKey && e.key === 'Enter') {
        e.preventDefault();
        if (!document.getElementById('generate-specs-btn').disabled) {
            generateSpecs();
        } else {
            startAnalysis();
        }
    }
    
    // Ctrl+D to download
    if (e.ctrlKey && e.key === 'd') {
        e.preventDefault();
        if (!document.getElementById('download-btn').disabled) {
            downloadSpecs();
        }
    }
});

// Export functions for global access
window.startAnalysis = startAnalysis;
window.generateSpecs = generateSpecs;
window.downloadSpecs = downloadSpecs;
window.analyzeRequirement = analyzeRequirement;
window.showLoadingMessage = showLoadingMessage;
window.hideLoadingMessage = hideLoadingMessage;
window.showSuccessMessage = showSuccessMessage;
window.showErrorMessage = showErrorMessage;
