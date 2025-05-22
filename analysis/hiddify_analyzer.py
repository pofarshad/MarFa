"""
Hiddify App Source Code Analyzer
Analyzes the Hiddify VPN app structure and dependencies for MarFaNet transformation
"""

import os
import json
import re
import subprocess
import tempfile
import shutil
from typing import Dict, List, Any, Optional
from pathlib import Path
import git
from datetime import datetime

class HiddifyAnalyzer:
    """Analyzes Hiddify app source code structure and dependencies"""
    
    def __init__(self):
        self.analysis_cache = {}
        self.temp_dir = None
        
    def analyze_repository(self, repo_url: str, tag: str = "v2.5.7") -> Dict[str, Any]:
        """
        Analyze the Hiddify repository structure and dependencies
        
        Args:
            repo_url: GitHub repository URL
            tag: Git tag to analyze
            
        Returns:
            Dictionary containing analysis results
        """
        try:
            # Clone repository to temporary directory
            self.temp_dir = tempfile.mkdtemp()
            repo_path = os.path.join(self.temp_dir, "hiddify-app")
            
            print(f"Cloning repository: {repo_url}")
            repo = git.Repo.clone_from(repo_url, repo_path, branch=tag, depth=1)
            
            # Perform comprehensive analysis
            analysis_result = {
                "timestamp": datetime.now().isoformat(),
                "repository": {
                    "url": repo_url,
                    "tag": tag,
                    "commit": str(repo.head.commit)
                },
                "structure": self._analyze_structure(repo_path),
                "dependencies": self._analyze_dependencies(repo_path),
                "singbox_references": self._find_singbox_references(repo_path),
                "kotlin_analysis": self._analyze_kotlin_code(repo_path),
                "resources": self._analyze_resources(repo_path),
                "gradle_config": self._analyze_gradle_config(repo_path),
                "manifest_analysis": self._analyze_manifest(repo_path),
                "refactoring_plan": self._generate_refactoring_plan(repo_path)
            }
            
            return analysis_result
            
        except Exception as e:
            return {
                "error": str(e),
                "timestamp": datetime.now().isoformat()
            }
        finally:
            # Cleanup temporary directory
            if self.temp_dir and os.path.exists(self.temp_dir):
                shutil.rmtree(self.temp_dir, ignore_errors=True)
    
    def _analyze_structure(self, repo_path: str) -> Dict[str, Any]:
        """Analyze project directory structure"""
        structure = {
            "total_files": 0,
            "kotlin_files": 0,
            "java_files": 0,
            "xml_files": 0,
            "gradle_files": 0,
            "directories": [],
            "modules": []
        }
        
        for root, dirs, files in os.walk(repo_path):
            # Skip hidden directories and build outputs
            dirs[:] = [d for d in dirs if not d.startswith('.') and d not in ['build', 'node_modules']]
            
            rel_root = os.path.relpath(root, repo_path)
            if rel_root != '.':
                structure["directories"].append(rel_root)
            
            for file in files:
                structure["total_files"] += 1
                
                if file.endswith('.kt'):
                    structure["kotlin_files"] += 1
                elif file.endswith('.java'):
                    structure["java_files"] += 1
                elif file.endswith('.xml'):
                    structure["xml_files"] += 1
                elif file.endswith('.gradle') or file.endswith('.gradle.kts'):
                    structure["gradle_files"] += 1
        
        # Detect modules
        for item in os.listdir(repo_path):
            item_path = os.path.join(repo_path, item)
            if os.path.isdir(item_path) and os.path.exists(os.path.join(item_path, 'build.gradle')):
                structure["modules"].append(item)
        
        return structure
    
    def _analyze_dependencies(self, repo_path: str) -> Dict[str, Any]:
        """Analyze project dependencies from Gradle files"""
        dependencies = {
            "sing_box_dependencies": [],
            "android_dependencies": [],
            "kotlin_dependencies": [],
            "other_dependencies": [],
            "gradle_plugins": []
        }
        
        # Find all gradle files
        gradle_files = []
        for root, dirs, files in os.walk(repo_path):
            for file in files:
                if file.endswith('.gradle') or file.endswith('.gradle.kts'):
                    gradle_files.append(os.path.join(root, file))
        
        for gradle_file in gradle_files:
            try:
                with open(gradle_file, 'r', encoding='utf-8') as f:
                    content = f.read()
                    
                # Find dependencies
                dep_pattern = r'implementation\s+["\']([^"\']+)["\']'
                matches = re.findall(dep_pattern, content)
                
                for dep in matches:
                    if 'sing-box' in dep or 'singbox' in dep:
                        dependencies["sing_box_dependencies"].append({
                            "dependency": dep,
                            "file": os.path.relpath(gradle_file, repo_path)
                        })
                    elif 'androidx' in dep or 'android' in dep:
                        dependencies["android_dependencies"].append(dep)
                    elif 'kotlin' in dep:
                        dependencies["kotlin_dependencies"].append(dep)
                    else:
                        dependencies["other_dependencies"].append(dep)
                
                # Find plugins
                plugin_pattern = r'id\s+["\']([^"\']+)["\']'
                plugin_matches = re.findall(plugin_pattern, content)
                dependencies["gradle_plugins"].extend(plugin_matches)
                        
            except Exception as e:
                print(f"Error reading gradle file {gradle_file}: {e}")
        
        return dependencies
    
    def _find_singbox_references(self, repo_path: str) -> Dict[str, Any]:
        """Find all references to Sing-box in the codebase"""
        references = {
            "total_references": 0,
            "files_with_references": [],
            "import_statements": [],
            "class_references": [],
            "method_calls": [],
            "configuration_references": []
        }
        
        # Search patterns
        patterns = [
            r'import.*sing.*box',
            r'import.*singbox',
            r'SingBox\w*',
            r'singbox\w*',
            r'libbox',
            r'BoxService',
            r'sing-box'
        ]
        
        # Search in Kotlin and Java files
        for root, dirs, files in os.walk(repo_path):
            dirs[:] = [d for d in dirs if not d.startswith('.') and d not in ['build']]
            
            for file in files:
                if file.endswith(('.kt', '.java', '.xml', '.json')):
                    file_path = os.path.join(root, file)
                    rel_path = os.path.relpath(file_path, repo_path)
                    
                    try:
                        with open(file_path, 'r', encoding='utf-8') as f:
                            content = f.read()
                            
                        file_references = []
                        for pattern in patterns:
                            matches = re.findall(pattern, content, re.IGNORECASE)
                            if matches:
                                file_references.extend(matches)
                                
                                # Categorize references
                                if 'import' in pattern:
                                    references["import_statements"].extend(matches)
                                elif pattern.endswith(r'\w*'):
                                    references["class_references"].extend(matches)
                        
                        if file_references:
                            references["files_with_references"].append({
                                "file": rel_path,
                                "references": list(set(file_references)),
                                "count": len(file_references)
                            })
                            references["total_references"] += len(file_references)
                            
                    except Exception as e:
                        print(f"Error reading file {file_path}: {e}")
        
        return references
    
    def _analyze_kotlin_code(self, repo_path: str) -> Dict[str, Any]:
        """Analyze Kotlin source code structure"""
        kotlin_analysis = {
            "packages": set(),
            "classes": [],
            "interfaces": [],
            "services": [],
            "activities": [],
            "fragments": [],
            "total_lines": 0,
            "complexity_metrics": {}
        }
        
        for root, dirs, files in os.walk(repo_path):
            dirs[:] = [d for d in dirs if not d.startswith('.') and d not in ['build']]
            
            for file in files:
                if file.endswith('.kt'):
                    file_path = os.path.join(root, file)
                    rel_path = os.path.relpath(file_path, repo_path)
                    
                    try:
                        with open(file_path, 'r', encoding='utf-8') as f:
                            content = f.read()
                            lines = content.split('\n')
                            kotlin_analysis["total_lines"] += len(lines)
                        
                        # Extract package
                        package_match = re.search(r'package\s+([\w.]+)', content)
                        if package_match:
                            kotlin_analysis["packages"].add(package_match.group(1))
                        
                        # Find classes, interfaces, services, etc.
                        class_matches = re.findall(r'class\s+(\w+)', content)
                        interface_matches = re.findall(r'interface\s+(\w+)', content)
                        
                        for class_name in class_matches:
                            kotlin_analysis["classes"].append({
                                "name": class_name,
                                "file": rel_path
                            })
                            
                            # Check for specific types
                            if 'Service' in class_name or ': Service' in content:
                                kotlin_analysis["services"].append(class_name)
                            elif 'Activity' in class_name or ': Activity' in content:
                                kotlin_analysis["activities"].append(class_name)
                            elif 'Fragment' in class_name or ': Fragment' in content:
                                kotlin_analysis["fragments"].append(class_name)
                        
                        for interface_name in interface_matches:
                            kotlin_analysis["interfaces"].append({
                                "name": interface_name,
                                "file": rel_path
                            })
                            
                    except Exception as e:
                        print(f"Error analyzing Kotlin file {file_path}: {e}")
        
        kotlin_analysis["packages"] = list(kotlin_analysis["packages"])
        return kotlin_analysis
    
    def _analyze_resources(self, repo_path: str) -> Dict[str, Any]:
        """Analyze Android resources"""
        resources = {
            "strings": {},
            "layouts": [],
            "drawables": [],
            "values": [],
            "manifests": [],
            "icons": []
        }
        
        res_path = os.path.join(repo_path, 'app', 'src', 'main', 'res')
        if os.path.exists(res_path):
            for root, dirs, files in os.walk(res_path):
                for file in files:
                    file_path = os.path.join(root, file)
                    rel_path = os.path.relpath(file_path, res_path)
                    
                    if file.endswith('.xml'):
                        if 'layout' in root:
                            resources["layouts"].append(rel_path)
                        elif 'values' in root:
                            resources["values"].append(rel_path)
                            
                            # Parse strings.xml for app names
                            if file == 'strings.xml':
                                try:
                                    with open(file_path, 'r', encoding='utf-8') as f:
                                        content = f.read()
                                    
                                    string_matches = re.findall(r'<string name="([^"]+)">([^<]+)</string>', content)
                                    for name, value in string_matches:
                                        resources["strings"][name] = value
                                        
                                except Exception as e:
                                    print(f"Error parsing strings.xml: {e}")
                    
                    elif file.endswith(('.png', '.jpg', '.jpeg', '.svg', '.webp')):
                        if 'drawable' in root or 'mipmap' in root:
                            resources["drawables"].append(rel_path)
                            if 'ic_launcher' in file or 'icon' in file:
                                resources["icons"].append(rel_path)
        
        return resources
    
    def _analyze_gradle_config(self, repo_path: str) -> Dict[str, Any]:
        """Analyze Gradle configuration"""
        gradle_config = {
            "application_id": None,
            "version_name": None,
            "version_code": None,
            "compile_sdk": None,
            "target_sdk": None,
            "min_sdk": None,
            "build_types": [],
            "flavors": [],
            "signing_configs": []
        }
        
        app_gradle = os.path.join(repo_path, 'app', 'build.gradle')
        if not os.path.exists(app_gradle):
            app_gradle = os.path.join(repo_path, 'app', 'build.gradle.kts')
        
        if os.path.exists(app_gradle):
            try:
                with open(app_gradle, 'r', encoding='utf-8') as f:
                    content = f.read()
                
                # Extract configuration values
                config_patterns = {
                    "application_id": r'applicationId\s+["\']([^"\']+)["\']',
                    "version_name": r'versionName\s+["\']([^"\']+)["\']',
                    "version_code": r'versionCode\s+(\d+)',
                    "compile_sdk": r'compileSdk\s+(\d+)',
                    "target_sdk": r'targetSdk\s+(\d+)',
                    "min_sdk": r'minSdk\s+(\d+)'
                }
                
                for key, pattern in config_patterns.items():
                    match = re.search(pattern, content)
                    if match:
                        gradle_config[key] = match.group(1)
                
                # Find build types
                build_types = re.findall(r'(\w+)\s*\{[^}]*\}', content)
                gradle_config["build_types"] = build_types
                
            except Exception as e:
                print(f"Error analyzing Gradle config: {e}")
        
        return gradle_config
    
    def _analyze_manifest(self, repo_path: str) -> Dict[str, Any]:
        """Analyze Android manifest"""
        manifest_analysis = {
            "package": None,
            "permissions": [],
            "activities": [],
            "services": [],
            "receivers": [],
            "application_name": None,
            "main_activity": None
        }
        
        manifest_path = os.path.join(repo_path, 'app', 'src', 'main', 'AndroidManifest.xml')
        if os.path.exists(manifest_path):
            try:
                with open(manifest_path, 'r', encoding='utf-8') as f:
                    content = f.read()
                
                # Extract package
                package_match = re.search(r'package="([^"]+)"', content)
                if package_match:
                    manifest_analysis["package"] = package_match.group(1)
                
                # Extract permissions
                permission_matches = re.findall(r'<uses-permission android:name="([^"]+)"', content)
                manifest_analysis["permissions"] = permission_matches
                
                # Extract components
                activity_matches = re.findall(r'<activity[^>]*android:name="([^"]+)"', content)
                service_matches = re.findall(r'<service[^>]*android:name="([^"]+)"', content)
                receiver_matches = re.findall(r'<receiver[^>]*android:name="([^"]+)"', content)
                
                manifest_analysis["activities"] = activity_matches
                manifest_analysis["services"] = service_matches
                manifest_analysis["receivers"] = receiver_matches
                
                # Find main activity
                if 'android.intent.action.MAIN' in content:
                    main_activity_pattern = r'<activity[^>]*android:name="([^"]+)"[^>]*>.*?android.intent.action.MAIN.*?</activity>'
                    main_match = re.search(main_activity_pattern, content, re.DOTALL)
                    if main_match:
                        manifest_analysis["main_activity"] = main_match.group(1)
                
            except Exception as e:
                print(f"Error analyzing manifest: {e}")
        
        return manifest_analysis
    
    def _generate_refactoring_plan(self, repo_path: str) -> Dict[str, Any]:
        """Generate comprehensive refactoring plan"""
        plan = {
            "phases": [
                {
                    "phase": 1,
                    "name": "Dependency Replacement",
                    "tasks": [
                        "Remove Sing-box dependencies from build.gradle",
                        "Add Xray core dependencies",
                        "Update native library configurations",
                        "Clean up unused imports"
                    ],
                    "estimated_effort": "High",
                    "risk": "Medium"
                },
                {
                    "phase": 2,
                    "name": "Core Integration",
                    "tasks": [
                        "Create Xray JNI wrapper",
                        "Implement configuration bridge",
                        "Update service bindings",
                        "Modify VPN service implementation"
                    ],
                    "estimated_effort": "High",
                    "risk": "High"
                },
                {
                    "phase": 3,
                    "name": "Feature Updates",
                    "tasks": [
                        "Add GFW Knocker integration",
                        "Remove WARP components",
                        "Update protocol parsers",
                        "Implement latency scanner"
                    ],
                    "estimated_effort": "Medium",
                    "risk": "Medium"
                },
                {
                    "phase": 4,
                    "name": "Rebranding",
                    "tasks": [
                        "Update application ID",
                        "Change app name and strings",
                        "Replace launcher icons",
                        "Update about page"
                    ],
                    "estimated_effort": "Low",
                    "risk": "Low"
                },
                {
                    "phase": 5,
                    "name": "Testing & Optimization",
                    "tasks": [
                        "Add unit tests",
                        "Performance optimization",
                        "Code quality improvements",
                        "Security audit"
                    ],
                    "estimated_effort": "Medium",
                    "risk": "Low"
                }
            ],
            "critical_files": [
                "app/build.gradle",
                "app/src/main/AndroidManifest.xml",
                "app/src/main/kotlin/com/hiddify/hiddify/core/",
                "app/src/main/kotlin/com/hiddify/hiddify/service/",
                "app/src/main/res/values/strings.xml"
            ],
            "backup_recommendations": [
                "Create git branch before starting",
                "Document all configuration changes",
                "Keep reference to original Sing-box integration",
                "Test each phase independently"
            ]
        }
        
        return plan

    def save_analysis(self, analysis: Dict[str, Any], output_path: str):
        """Save analysis results to JSON file"""
        try:
            with open(output_path, 'w', encoding='utf-8') as f:
                json.dump(analysis, f, indent=2, ensure_ascii=False)
            print(f"Analysis saved to: {output_path}")
        except Exception as e:
            print(f"Error saving analysis: {e}")
