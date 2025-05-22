"""
Rebranding Specification Generator
Generates comprehensive specifications for rebranding Hiddify to MarFaNet
"""

import os
import json
import re
from typing import Dict, List, Any, Optional
from datetime import datetime

class RebrandingSpecGenerator:
    """Generates comprehensive rebranding specifications for MarFaNet transformation"""
    
    def __init__(self):
        self.source_brand = "Hiddify"
        self.target_brand = "MarFaNet"
        self.source_package = "com.hiddify.hiddify"
        self.target_package = "net.marfanet.android"
        
    def generate_specs(self, current_config: Dict[str, Any]) -> Dict[str, Any]:
        """
        Generate comprehensive rebranding specifications
        
        Args:
            current_config: Current application configuration
            
        Returns:
            Dictionary containing rebranding specifications
        """
        
        specs = {
            "metadata": {
                "generated_at": datetime.now().isoformat(),
                "version": "1.0.0",
                "source_brand": self.source_brand,
                "target_brand": self.target_brand,
                "source_package": self.source_package,
                "target_package": self.target_package
            },
            "package_changes": self._generate_package_changes(),
            "application_id": self._generate_application_id_changes(),
            "string_resources": self._generate_string_resource_changes(),
            "icon_specifications": self._generate_icon_specifications(),
            "manifest_changes": self._generate_manifest_changes(),
            "kotlin_code_changes": self._generate_kotlin_code_changes(),
            "gradle_changes": self._generate_gradle_changes(),
            "asset_updates": self._generate_asset_updates(),
            "legal_updates": self._generate_legal_updates(),
            "migration_checklist": self._generate_migration_checklist()
        }
        
        return specs
    
    def _generate_package_changes(self) -> Dict[str, Any]:
        """Generate package structure changes"""
        
        return {
            "package_mapping": {
                "com.hiddify.hiddify": "net.marfanet.android",
                "com.hiddify.hiddify.core": "net.marfanet.android.core",
                "com.hiddify.hiddify.service": "net.marfanet.android.service",
                "com.hiddify.hiddify.ui": "net.marfanet.android.ui",
                "com.hiddify.hiddify.data": "net.marfanet.android.data",
                "com.hiddify.hiddify.util": "net.marfanet.android.util",
                "com.hiddify.hiddify.config": "net.marfanet.android.config"
            },
            "directory_structure": {
                "before": "app/src/main/kotlin/com/hiddify/hiddify/",
                "after": "app/src/main/kotlin/net/marfanet/android/",
                "subdirectories": [
                    "core/",
                    "service/", 
                    "ui/",
                    "data/",
                    "util/",
                    "config/",
                    "xray/"
                ]
            },
            "migration_steps": [
                {
                    "step": 1,
                    "action": "Create new package directory structure",
                    "command": "mkdir -p app/src/main/kotlin/net/marfanet/android/{core,service,ui,data,util,config,xray}"
                },
                {
                    "step": 2,
                    "action": "Move Kotlin files to new package structure",
                    "script": "move_kotlin_files.sh"
                },
                {
                    "step": 3,
                    "action": "Update package declarations in all Kotlin files",
                    "regex_replace": {
                        "pattern": "package com\\.hiddify\\.hiddify",
                        "replacement": "package net.marfanet.android"
                    }
                },
                {
                    "step": 4,
                    "action": "Update import statements",
                    "regex_replace": {
                        "pattern": "import com\\.hiddify\\.hiddify",
                        "replacement": "import net.marfanet.android"
                    }
                }
            ]
        }
    
    def _generate_application_id_changes(self) -> Dict[str, Any]:
        """Generate application ID changes"""
        
        return {
            "gradle_config": {
                "file": "app/build.gradle",
                "changes": [
                    {
                        "property": "applicationId",
                        "old_value": "com.hiddify.hiddify",
                        "new_value": "net.marfanet.android",
                        "location": "android.defaultConfig"
                    },
                    {
                        "property": "namespace",
                        "old_value": "com.hiddify.hiddify", 
                        "new_value": "net.marfanet.android",
                        "location": "android"
                    }
                ]
            },
            "manifest_updates": {
                "file": "app/src/main/AndroidManifest.xml",
                "changes": [
                    {
                        "attribute": "package",
                        "old_value": "com.hiddify.hiddify",
                        "new_value": "net.marfanet.android"
                    }
                ]
            },
            "signing_config": {
                "keystore_alias": "marfanet-release",
                "keystore_file": "marfanet-release.keystore",
                "considerations": [
                    "Generate new signing key for MarFaNet",
                    "Update CI/CD pipeline with new keystore",
                    "Plan app update strategy for existing users"
                ]
            }
        }
    
    def _generate_string_resource_changes(self) -> Dict[str, Any]:
        """Generate string resource changes"""
        
        return {
            "string_mappings": {
                "app_name": {
                    "old_value": "Hiddify",
                    "new_value": "MarFaNet",
                    "files": ["app/src/main/res/values/strings.xml"]
                },
                "app_full_name": {
                    "old_value": "HiddifyNG",
                    "new_value": "MarFaNet",
                    "files": ["app/src/main/res/values/strings.xml"]
                },
                "app_description": {
                    "old_value": "Hiddify VPN Client",
                    "new_value": "MarFaNet VPN Client",
                    "files": ["app/src/main/res/values/strings.xml"]
                },
                "service_name": {
                    "old_value": "Hiddify VPN Service",
                    "new_value": "MarFaNet VPN Service", 
                    "files": ["app/src/main/res/values/strings.xml"]
                }
            },
            "localization_files": [
                {
                    "language": "en",
                    "file": "app/src/main/res/values/strings.xml",
                    "strings_to_update": [
                        "app_name",
                        "app_description", 
                        "notification_title",
                        "about_app_title",
                        "copyright_text"
                    ]
                },
                {
                    "language": "fa",
                    "file": "app/src/main/res/values-fa/strings.xml",
                    "strings_to_update": [
                        "app_name",
                        "app_description",
                        "notification_title",
                        "about_app_title"
                    ]
                }
            ],
            "update_script": {
                "script_name": "update_strings.py",
                "description": "Automated script to update all string resources",
                "functionality": [
                    "Parse all strings.xml files",
                    "Replace brand-specific strings",
                    "Maintain translation consistency",
                    "Generate change report"
                ]
            }
        }
    
    def _generate_icon_specifications(self) -> Dict[str, Any]:
        """Generate app icon specifications"""
        
        return {
            "icon_requirements": {
                "launcher_icon": {
                    "name": "ic_launcher",
                    "formats": ["PNG", "Vector (XML)"],
                    "densities": {
                        "mdpi": "48x48px",
                        "hdpi": "72x72px", 
                        "xhdpi": "96x96px",
                        "xxhdpi": "144x144px",
                        "xxxhdpi": "192x192px"
                    },
                    "adaptive_icon": {
                        "foreground": "ic_launcher_foreground.xml",
                        "background": "ic_launcher_background.xml",
                        "size": "108x108dp (with 72x72dp safe zone)"
                    }
                },
                "notification_icon": {
                    "name": "ic_notification",
                    "format": "Vector XML",
                    "color": "White (#FFFFFF)",
                    "style": "Simple, monochrome",
                    "size": "24x24dp"
                }
            },
            "design_guidelines": {
                "brand_colors": {
                    "primary": "#2196F3",
                    "primary_dark": "#1976D2",
                    "accent": "#03DAC6",
                    "background": "#FFFFFF",
                    "surface": "#F5F5F5"
                },
                "design_principles": [
                    "Clean and modern design",
                    "Network/connectivity theme",
                    "Professional appearance",
                    "High contrast for visibility"
                ],
                "logo_elements": [
                    "Network nodes or connection lines",
                    "Shield or security symbol",
                    "Globe or connectivity icon",
                    "Modern typography for 'MarFaNet'"
                ]
            },
            "file_locations": {
                "launcher_icons": [
                    "app/src/main/res/mipmap-mdpi/ic_launcher.png",
                    "app/src/main/res/mipmap-hdpi/ic_launcher.png",
                    "app/src/main/res/mipmap-xhdpi/ic_launcher.png", 
                    "app/src/main/res/mipmap-xxhdpi/ic_launcher.png",
                    "app/src/main/res/mipmap-xxxhdpi/ic_launcher.png"
                ],
                "adaptive_icons": [
                    "app/src/main/res/mipmap-anydpi-v26/ic_launcher.xml",
                    "app/src/main/res/drawable/ic_launcher_foreground.xml",
                    "app/src/main/res/drawable/ic_launcher_background.xml"
                ],
                "notification_icons": [
                    "app/src/main/res/drawable/ic_notification.xml"
                ]
            },
            "creation_tools": [
                "Android Studio Image Asset Studio",
                "Adobe Illustrator/Photoshop",
                "Figma or Sketch",
                "Online icon generators"
            ]
        }
    
    def _generate_manifest_changes(self) -> Dict[str, Any]:
        """Generate Android manifest changes"""
        
        return {
            "manifest_updates": {
                "file": "app/src/main/AndroidManifest.xml",
                "changes": [
                    {
                        "element": "manifest",
                        "attribute": "package",
                        "old_value": "com.hiddify.hiddify",
                        "new_value": "net.marfanet.android"
                    },
                    {
                        "element": "application",
                        "attribute": "android:label",
                        "old_value": "@string/app_name",
                        "new_value": "@string/app_name",
                        "note": "Update referenced string resource"
                    },
                    {
                        "element": "application", 
                        "attribute": "android:icon",
                        "old_value": "@mipmap/ic_launcher",
                        "new_value": "@mipmap/ic_launcher",
                        "note": "Replace icon resources"
                    }
                ]
            },
            "service_declarations": {
                "vpn_service": {
                    "old_name": "com.hiddify.hiddify.service.HiddifyVpnService",
                    "new_name": "net.marfanet.android.service.MarFaNetVpnService",
                    "label_update": "MarFaNet VPN Service"
                },
                "tile_service": {
                    "old_name": "com.hiddify.hiddify.service.QuickToggleService", 
                    "new_name": "net.marfanet.android.service.QuickToggleService",
                    "label_update": "MarFaNet Quick Toggle"
                }
            },
            "activity_declarations": {
                "main_activity": {
                    "old_name": "com.hiddify.hiddify.MainActivity",
                    "new_name": "net.marfanet.android.MainActivity",
                    "label_update": "@string/app_name"
                },
                "settings_activity": {
                    "old_name": "com.hiddify.hiddify.ui.SettingsActivity",
                    "new_name": "net.marfanet.android.ui.SettingsActivity", 
                    "label_update": "MarFaNet Settings"
                }
            },
            "provider_declarations": {
                "file_provider": {
                    "authorities": "net.marfanet.android.fileprovider",
                    "update_required": True
                }
            }
        }
    
    def _generate_kotlin_code_changes(self) -> Dict[str, Any]:
        """Generate Kotlin code changes"""
        
        return {
            "class_renames": {
                "HiddifyApplication": "MarFaNetApplication",
                "HiddifyVpnService": "MarFaNetVpnService", 
                "HiddifyConfig": "MarFaNetConfig",
                "HiddifyTunnel": "MarFaNetTunnel",
                "HiddifyPreferences": "MarFaNetPreferences"
            },
            "constant_updates": {
                "app_constants": {
                    "file": "app/src/main/kotlin/net/marfanet/android/Constants.kt",
                    "constants": [
                        {
                            "name": "APP_NAME",
                            "old_value": "\"Hiddify\"",
                            "new_value": "\"MarFaNet\""
                        },
                        {
                            "name": "APP_PACKAGE",
                            "old_value": "\"com.hiddify.hiddify\"",
                            "new_value": "\"net.marfanet.android\""
                        },
                        {
                            "name": "NOTIFICATION_CHANNEL_ID",
                            "old_value": "\"hiddify_vpn\"",
                            "new_value": "\"marfanet_vpn\""
                        }
                    ]
                }
            },
            "shared_preferences": {
                "preference_files": [
                    {
                        "old_name": "hiddify_prefs",
                        "new_name": "marfanet_prefs",
                        "migration_required": True
                    },
                    {
                        "old_name": "hiddify_config",
                        "new_name": "marfanet_config",
                        "migration_required": True
                    }
                ],
                "migration_strategy": {
                    "approach": "Copy old preferences to new files on first launch",
                    "implementation": "PreferencesMigration.kt",
                    "cleanup": "Remove old preference files after successful migration"
                }
            },
            "database_changes": {
                "room_database": {
                    "database_name": {
                        "old_value": "hiddify.db",
                        "new_value": "marfanet.db"
                    },
                    "migration_required": True,
                    "migration_version": "Bump database version and add migration"
                }
            },
            "notification_channels": {
                "vpn_channel": {
                    "channel_id": "marfanet_vpn_channel",
                    "channel_name": "MarFaNet VPN",
                    "channel_description": "MarFaNet VPN service notifications"
                },
                "update_channel": {
                    "channel_id": "marfanet_updates",
                    "channel_name": "MarFaNet Updates", 
                    "channel_description": "MarFaNet app and rule updates"
                }
            }
        }
    
    def _generate_gradle_changes(self) -> Dict[str, Any]:
        """Generate Gradle configuration changes"""
        
        return {
            "build_gradle_app": {
                "file": "app/build.gradle",
                "changes": [
                    {
                        "section": "android.defaultConfig",
                        "property": "applicationId",
                        "new_value": "\"net.marfanet.android\""
                    },
                    {
                        "section": "android.defaultConfig",
                        "property": "versionName", 
                        "new_value": "\"1.0.0\"",
                        "note": "Reset version for new brand"
                    },
                    {
                        "section": "android.defaultConfig",
                        "property": "versionCode",
                        "new_value": "1",
                        "note": "Reset version code for new brand"
                    }
                ]
            },
            "build_gradle_project": {
                "file": "build.gradle",
                "changes": [
                    {
                        "section": "project properties",
                        "property": "archivesBaseName",
                        "new_value": "\"marfanet-android\""
                    }
                ]
            },
            "gradle_properties": {
                "file": "gradle.properties",
                "additions": [
                    "android.useAndroidX=true",
                    "android.enableJetifier=true",
                    "kotlin.code.style=official"
                ]
            },
            "signing_config": {
                "release_config": {
                    "keyAlias": "marfanet-release",
                    "keyPassword": "{{ keystore_password }}",
                    "storeFile": "marfanet-release.keystore",
                    "storePassword": "{{ keystore_password }}"
                },
                "debug_config": {
                    "keyAlias": "marfanet-debug", 
                    "note": "Use debug keystore for development"
                }
            }
        }
    
    def _generate_asset_updates(self) -> Dict[str, Any]:
        """Generate asset update specifications"""
        
        return {
            "image_assets": {
                "splash_screen": {
                    "files": ["app/src/main/res/drawable/splash_background.xml"],
                    "updates": ["Replace Hiddify logo with MarFaNet logo"]
                },
                "about_page": {
                    "files": ["app/src/main/res/drawable/about_logo.xml"],
                    "updates": ["Update logo and branding elements"]
                },
                "tutorial_images": {
                    "directory": "app/src/main/res/drawable/",
                    "files": ["tutorial_*.png"],
                    "updates": ["Update screenshots with MarFaNet branding"]
                }
            },
            "text_assets": {
                "privacy_policy": {
                    "file": "app/src/main/assets/privacy_policy.html",
                    "updates": ["Replace Hiddify references with MarFaNet"]
                },
                "terms_of_service": {
                    "file": "app/src/main/assets/terms.html",
                    "updates": ["Update legal entity and app name references"]
                },
                "about_text": {
                    "file": "app/src/main/assets/about.html",
                    "updates": ["Update app description and credits"]
                }
            },
            "configuration_assets": {
                "default_configs": {
                    "directory": "app/src/main/assets/configs/",
                    "updates": ["Update default server configurations if needed"]
                },
                "help_files": {
                    "directory": "app/src/main/assets/help/",
                    "updates": ["Update help documentation with MarFaNet branding"]
                }
            }
        }
    
    def _generate_legal_updates(self) -> Dict[str, Any]:
        """Generate legal document updates"""
        
        return {
            "copyright_notices": {
                "source_files": {
                    "update_required": True,
                    "new_copyright": "Copyright © 2024 MarFaNet Team",
                    "files_to_update": [
                        "All Kotlin source files",
                        "README.md",
                        "LICENSE file",
                        "About dialog"
                    ]
                }
            },
            "license_compliance": {
                "open_source_licenses": {
                    "maintain_attribution": [
                        "Original Hiddify project attribution",
                        "Third-party library licenses",
                        "Xray core license compliance"
                    ],
                    "license_file": "app/src/main/assets/licenses.html"
                }
            },
            "legal_documents": {
                "privacy_policy": {
                    "file": "app/src/main/assets/privacy_policy.html",
                    "updates": [
                        "Update app name references",
                        "Update developer/company information",
                        "Ensure GDPR compliance",
                        "Update contact information"
                    ]
                },
                "terms_of_service": {
                    "file": "app/src/main/assets/terms.html", 
                    "updates": [
                        "Update legal entity information",
                        "Update app name and functionality",
                        "Review and update terms as needed"
                    ]
                }
            },
            "attribution_requirements": {
                "hiddify_attribution": {
                    "required": True,
                    "text": "Based on Hiddify VPN (https://github.com/hiddify/hiddify-app)",
                    "location": "About page and source code headers"
                },
                "xray_attribution": {
                    "required": True,
                    "text": "Powered by Xray Core (https://github.com/XTLS/Xray-core)",
                    "location": "About page and legal notices"
                }
            }
        }
    
    def _generate_migration_checklist(self) -> List[Dict[str, Any]]:
        """Generate comprehensive migration checklist"""
        
        return [
            {
                "category": "Package Structure",
                "tasks": [
                    {
                        "task": "Create new package directory structure",
                        "status": "pending",
                        "priority": "high",
                        "estimated_time": "30 minutes"
                    },
                    {
                        "task": "Move all Kotlin files to new package structure",
                        "status": "pending", 
                        "priority": "high",
                        "estimated_time": "2 hours"
                    },
                    {
                        "task": "Update package declarations in all files",
                        "status": "pending",
                        "priority": "high", 
                        "estimated_time": "1 hour"
                    },
                    {
                        "task": "Update import statements throughout codebase",
                        "status": "pending",
                        "priority": "high",
                        "estimated_time": "1 hour"
                    }
                ]
            },
            {
                "category": "Application Configuration",
                "tasks": [
                    {
                        "task": "Update applicationId in build.gradle",
                        "status": "pending",
                        "priority": "high",
                        "estimated_time": "5 minutes"
                    },
                    {
                        "task": "Update package attribute in AndroidManifest.xml",
                        "status": "pending",
                        "priority": "high",
                        "estimated_time": "5 minutes" 
                    },
                    {
                        "task": "Reset version codes and names",
                        "status": "pending",
                        "priority": "medium",
                        "estimated_time": "5 minutes"
                    }
                ]
            },
            {
                "category": "Resources and Assets",
                "tasks": [
                    {
                        "task": "Update string resources with new app name",
                        "status": "pending",
                        "priority": "high",
                        "estimated_time": "30 minutes"
                    },
                    {
                        "task": "Create new launcher icons",
                        "status": "pending",
                        "priority": "high",
                        "estimated_time": "4 hours"
                    },
                    {
                        "task": "Update notification icons",
                        "status": "pending",
                        "priority": "medium",
                        "estimated_time": "1 hour"
                    },
                    {
                        "task": "Update splash screen and about page assets",
                        "status": "pending",
                        "priority": "medium",
                        "estimated_time": "2 hours"
                    }
                ]
            },
            {
                "category": "Code Updates", 
                "tasks": [
                    {
                        "task": "Rename main classes (Application, Service, etc.)",
                        "status": "pending",
                        "priority": "high",
                        "estimated_time": "1 hour"
                    },
                    {
                        "task": "Update constants and configuration values",
                        "status": "pending",
                        "priority": "high",
                        "estimated_time": "30 minutes"
                    },
                    {
                        "task": "Update SharedPreferences names",
                        "status": "pending",
                        "priority": "medium",
                        "estimated_time": "30 minutes"
                    },
                    {
                        "task": "Update database names and implement migration",
                        "status": "pending",
                        "priority": "medium", 
                        "estimated_time": "2 hours"
                    }
                ]
            },
            {
                "category": "Legal and Documentation",
                "tasks": [
                    {
                        "task": "Update copyright notices in source files",
                        "status": "pending",
                        "priority": "medium",
                        "estimated_time": "1 hour"
                    },
                    {
                        "task": "Update privacy policy and terms of service",
                        "status": "pending",
                        "priority": "high",
                        "estimated_time": "2 hours"
                    },
                    {
                        "task": "Update README and documentation",
                        "status": "pending",
                        "priority": "medium",
                        "estimated_time": "1 hour"
                    },
                    {
                        "task": "Ensure proper attribution to original projects",
                        "status": "pending",
                        "priority": "high",
                        "estimated_time": "30 minutes"
                    }
                ]
            },
            {
                "category": "Testing and Validation",
                "tasks": [
                    {
                        "task": "Compile and build app with new configuration",
                        "status": "pending",
                        "priority": "high",
                        "estimated_time": "30 minutes"
                    },
                    {
                        "task": "Test app installation and basic functionality",
                        "status": "pending",
                        "priority": "high",
                        "estimated_time": "1 hour"
                    },
                    {
                        "task": "Verify all resources display correctly",
                        "status": "pending",
                        "priority": "high",
                        "estimated_time": "30 minutes"
                    },
                    {
                        "task": "Test migration from old version (if applicable)",
                        "status": "pending",
                        "priority": "medium",
                        "estimated_time": "1 hour"
                    }
                ]
            }
        ]

    def save_specifications(self, specs: Dict[str, Any], output_path: str):
        """Save rebranding specifications to JSON file"""
        try:
            os.makedirs(os.path.dirname(output_path), exist_ok=True)
            with open(output_path, 'w', encoding='utf-8') as f:
                json.dump(specs, f, indent=2, ensure_ascii=False)
            print(f"Rebranding specifications saved to: {output_path}")
        except Exception as e:
            print(f"Error saving specifications: {e}")
