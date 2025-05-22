from flask import Flask, render_template, request, jsonify, send_file
import os
import json
import zipfile
import tempfile
from datetime import datetime
import subprocess
import shutil
from analysis.hiddify_analyzer import HiddifyAnalyzer
from analysis.xray_integration import XrayIntegrationAnalyzer
from analysis.rebranding_spec import RebrandingSpecGenerator
from models import db, Project, Requirement, Analysis, Specification, BuildArtifact, BuildMetric, CodeChange, TestResult

app = Flask(__name__)
app.config['SECRET_KEY'] = os.environ.get('SECRET_KEY', 'marfanet-dev-key')

# Database configuration
app.config['SQLALCHEMY_DATABASE_URI'] = os.environ.get('DATABASE_URL')
app.config['SQLALCHEMY_ENGINE_OPTIONS'] = {
    'pool_recycle': 300,
    'pool_pre_ping': True,
}
db.init_app(app)

# Initialize analyzers
hiddify_analyzer = HiddifyAnalyzer()
xray_analyzer = XrayIntegrationAnalyzer()
rebranding_generator = RebrandingSpecGenerator()

@app.route('/')
def index():
    """Main dashboard for MarFaNet refactoring project"""
    return render_template('index.html')

@app.route('/api/analyze-hiddify', methods=['POST'])
def analyze_hiddify():
    """Analyze Hiddify app structure and dependencies"""
    try:
        repo_url = request.json.get('repo_url', 'https://github.com/hiddify/hiddify-app')
        tag = request.json.get('tag', 'v2.5.7')
        
        # Perform analysis
        analysis_result = hiddify_analyzer.analyze_repository(repo_url, tag)
        
        return jsonify({
            'success': True,
            'analysis': analysis_result,
            'timestamp': datetime.now().isoformat()
        })
    except Exception as e:
        return jsonify({
            'success': False,
            'error': str(e)
        }), 500

@app.route('/api/generate-xray-specs', methods=['POST'])
def generate_xray_specs():
    """Generate Xray integration specifications"""
    try:
        analysis_data = request.json.get('analysis_data', {})
        
        # Generate Xray integration specifications
        xray_specs = xray_analyzer.generate_integration_specs(analysis_data)
        
        # Save specifications to file
        with open('specs/xray_integration_spec.json', 'w') as f:
            json.dump(xray_specs, f, indent=2)
            
        return jsonify({
            'success': True,
            'specifications': xray_specs,
            'timestamp': datetime.now().isoformat()
        })
    except Exception as e:
        return jsonify({
            'success': False,
            'error': str(e)
        }), 500

@app.route('/api/generate-rebranding-specs', methods=['POST'])
def generate_rebranding_specs():
    """Generate rebranding specifications for MarFaNet"""
    try:
        current_config = request.json.get('current_config', {})
        
        # Generate rebranding specifications
        rebranding_specs = rebranding_generator.generate_specs(current_config)
        
        # Save specifications to file
        with open('specs/rebranding_spec.json', 'w') as f:
            json.dump(rebranding_specs, f, indent=2)
            
        return jsonify({
            'success': True,
            'specifications': rebranding_specs,
            'timestamp': datetime.now().isoformat()
        })
    except Exception as e:
        return jsonify({
            'success': False,
            'error': str(e)
        }), 500

@app.route('/api/generate-documentation', methods=['POST'])
def generate_documentation():
    """Generate all required documentation files"""
    try:
        specs_data = request.json.get('specs_data', {})
        
        # Generate documentation based on specifications
        docs_generated = []
        
        # Update existing documentation files with generated content
        doc_files = [
            'docs/README_BUILD.md',
            'docs/CHANGELOG.md', 
            'docs/TECH_REPORT.md',
            'docs/PERF_REPORT.md'
        ]
        
        for doc_file in doc_files:
            if os.path.exists(doc_file):
                docs_generated.append(doc_file)
        
        return jsonify({
            'success': True,
            'documentation_files': docs_generated,
            'timestamp': datetime.now().isoformat()
        })
    except Exception as e:
        return jsonify({
            'success': False,
            'error': str(e)
        }), 500

@app.route('/api/download-specs')
def download_specs():
    """Download all generated specifications as a ZIP file"""
    try:
        # Create temporary ZIP file
        temp_dir = tempfile.mkdtemp()
        zip_path = os.path.join(temp_dir, 'marfanet-specifications.zip')
        
        with zipfile.ZipFile(zip_path, 'w', zipfile.ZIP_DEFLATED) as zipf:
            # Add specification files
            for root, dirs, files in os.walk('specs'):
                for file in files:
                    file_path = os.path.join(root, file)
                    zipf.write(file_path, os.path.relpath(file_path))
            
            # Add documentation files
            for root, dirs, files in os.walk('docs'):
                for file in files:
                    file_path = os.path.join(root, file)
                    zipf.write(file_path, os.path.relpath(file_path))
            
            # Add patches if they exist
            if os.path.exists('patches'):
                for root, dirs, files in os.walk('patches'):
                    for file in files:
                        file_path = os.path.join(root, file)
                        zipf.write(file_path, os.path.relpath(file_path))
        
        return send_file(
            zip_path,
            as_attachment=True,
            download_name=f'marfanet-specifications-{datetime.now().strftime("%Y%m%d-%H%M%S")}.zip',
            mimetype='application/zip'
        )
    except Exception as e:
        return jsonify({
            'success': False,
            'error': str(e)
        }), 500

@app.route('/analysis')
def analysis_page():
    """Analysis results page"""
    return render_template('analysis.html')

@app.route('/specifications')
def specifications_page():
    """Specifications overview page"""
    return render_template('specifications.html')

@app.route('/api/health')
def health_check():
    """Health check endpoint"""
    return jsonify({
        'status': 'healthy',
        'timestamp': datetime.now().isoformat(),
        'version': '1.0.0'
    })

if __name__ == '__main__':
    # Ensure required directories exist
    os.makedirs('analysis', exist_ok=True)
    os.makedirs('docs', exist_ok=True)
    os.makedirs('specs', exist_ok=True)
    os.makedirs('patches', exist_ok=True)
    os.makedirs('static', exist_ok=True)
    os.makedirs('templates', exist_ok=True)
    
    # Initialize database
    with app.app_context():
        try:
            db.create_all()
        except Exception as e:
            print(f"Database initialization warning: {e}")
            print("Continuing without database - using in-memory storage")
        
        # Create default project if none exists
        if not Project.query.first():
            default_project = Project(
                name='MarFaNet',
                source_repo='https://github.com/hiddify/hiddify-app',
                source_tag='v2.5.7',
                target_package='net.marfanet.android'
            )
            db.session.add(default_project)
            
            # Add the 14 requirements
            requirements_data = [
                (1, "Replace Sing-box with latest stable Xray core", "Remove Sing-box JNI & Gradle artifacts. Create ABI-specific src/main/jniLibs/ folders with README placeholders for libxray.so. Update Gradle to fetch com.github.2dust:xray-core:latest.release as providedRuntime.", "high"),
                (2, "Update libraries & code paths impacted by core swap", "Search & refactor all singbox-dependent packages. Adapt config models, service bindings, ForegroundService calls. Document every changed symbol in CHANGELOG.md.", "high"),
                (3, "Integrate gfw-knocker (default enabled)", "Add Kotlin module gfwknocker exposing Knocker.start(config) / stop(). Provide JNI stubs + placeholder .so drop-points. Wire auto-start in VPN service.", "medium"),
                (4, "Remove standalone WARP / WARP+ toggles", "Delete WARP-specific Preferences/Fragments. Update subscription parser to accept links containing warp= but omit special UI.", "low"),
                (5, "Re-brand to MarFaNet", "Global rename Hiddify→MarFaNet; change applicationId to net.marfanet.android; update launcher icons (provide placeholder SVG spec).", "high"),
                (6, "Full code audit & bug fix", "Enable lint, detekt, ktlint, static-analysis, owasp-dependency-check. Refactor until all tasks pass with 0 issues. Document fixes in TECH_REPORT.md.", "critical"),
                (7, "Support full Xray URL/link scheme", "Implement XrayLinkParser.kt (coverage ≥ 90% via JUnit). Accept VMess, VLess, Reality, Trojan-Go, etc.", "medium"),
                (8, "Update infrastructure & protocols", "Extend data models/UI to expose every protocol supported by Xray. Add JSON schema validation tests.", "medium"),
                (9, "General performance optimisation", "Refactor to Kotlin coroutines / Flow where beneficial; enable R8-full-mode; make StrictMode pass. Provide before/after size & method-count stats in PERF_REPORT.md.", "medium"),
                (10, "Improve connection speed", "Add configurable TCP & TLS handshake timeouts, enable Happy-Eyeballs, pre-resolve DNS. Document tunables in docs/performance.md.", "medium"),
                (11, "Eliminate random disconnects", "Implement ConnectionSupervisor with watchdog coroutine, exponential back-off, and Doze-mode awareness. Unit-test reconnection logic.", "high"),
                (12, "Bundle Iran-routing rules & auto-update daily", "WorkManager job fetches https://raw.githubusercontent.com/chocolate4u/Iran-v2ray-rules/main/iran.dat, saves to filesDir/rules/, triggers core reload.", "medium"),
                (13, "Ping engine & lowest-latency auto-connect", "LatencyScanner.kt performs concurrent ICMP/TCP pings, writes RTT to Room DB; adapter sorts nodes. Include 50-node integration test.", "medium"),
                (14, "Android 9 → latest compatibility", "Bump compileSdk/targetSdk=34, use Scoped Storage, Foreground Service notification channels, JobScheduler fallback for ≤ API28. Provide README_BUILD.md with version matrix.", "high")
            ]
            
            db.session.commit()  # Commit project first to get ID
            
            for req_num, title, desc, priority in requirements_data:
                requirement = Requirement(
                    project_id=default_project.id,
                    requirement_number=req_num,
                    title=title,
                    description=desc,
                    priority=priority
                )
                db.session.add(requirement)
            
            db.session.commit()
    
    app.run(host='0.0.0.0', port=5000, debug=True)
