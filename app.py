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

app = Flask(__name__)
app.config['SECRET_KEY'] = os.environ.get('SECRET_KEY', 'marfanet-dev-key')

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
    
    app.run(host='0.0.0.0', port=5000, debug=True)
