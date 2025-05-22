# MarFaNet Android Refactoring Dashboard

## Overview

This repository contains a web dashboard for managing the comprehensive refactoring of the Hiddify VPN application into MarFaNet. The project involves replacing the Sing-box core with Xray core, complete rebranding, and implementing additional features like GFW knocker integration and Iran-specific routing rules.

## User Preferences

Preferred communication style: Simple, everyday language.

## System Architecture

### Frontend Architecture
- **Framework**: Bootstrap 5.3.0 for responsive UI components
- **Styling**: Custom CSS with CSS variables for consistent theming
- **JavaScript**: Vanilla JavaScript for client-side interactions and API calls
- **Template Engine**: Jinja2 templates via Flask for server-side rendering
- **UI Components**: Toast notifications, modals, progress indicators, and tabbed interfaces

### Backend Architecture
- **Framework**: Flask 3.1.1 web application framework
- **Structure**: Modular design with separate analysis modules
- **API Design**: RESTful endpoints returning JSON responses
- **Error Handling**: Centralized exception handling with structured error responses
- **Request Processing**: JSON-based request/response format for API endpoints

### Analysis Engine Architecture
- **Hiddify Analyzer**: Analyzes source repository structure, dependencies, and Sing-box references
- **Xray Integration Analyzer**: Generates specifications for replacing Sing-box with Xray core
- **Rebranding Spec Generator**: Creates comprehensive rebranding guidelines for Hiddify to MarFaNet transformation
- **Git Integration**: Uses GitPython for repository cloning and analysis

## Key Components

### Analysis Modules
1. **HiddifyAnalyzer** (`analysis/hiddify_analyzer.py`)
   - Clones and analyzes Hiddify repository structure
   - Identifies dependencies and Sing-box references
   - Generates comprehensive analysis reports

2. **XrayIntegrationAnalyzer** (`analysis/xray_integration.py`)
   - Creates specifications for Xray core integration
   - Defines JNI interface requirements
   - Generates protocol support configurations

3. **RebrandingSpecGenerator** (`analysis/rebranding_spec.py`)
   - Produces complete rebranding specifications
   - Handles package name changes and asset updates
   - Generates migration checklists

### Web Interface Components
1. **Dashboard** (`templates/index.html`)
   - Project overview and quick access to tools
   - Repository information and target specifications
   - Navigation to analysis and specification sections

2. **Analysis Interface** (`templates/analysis.html`)
   - Hiddify app analysis results display
   - Dependency and reference counting
   - Interactive analysis controls

3. **Specifications Interface** (`templates/specifications.html`)
   - Technical specification generation and display
   - Xray integration specifications
   - Rebranding guidelines

## Data Flow

### Analysis Workflow
1. User initiates analysis via web interface
2. Backend clones specified Hiddify repository tag
3. Analysis modules process source code structure
4. Results are aggregated and cached
5. Frontend displays analysis results with metrics

### Specification Generation
1. Analysis data is processed by specification generators
2. Xray integration specifications are created
3. Rebranding specifications are generated
4. Complete transformation guidelines are compiled
5. Results are made available for download

### Repository Processing
1. Git repository is cloned to temporary directory
2. Source tree is analyzed for structure and dependencies
3. Sing-box references are identified and cataloged
4. Configuration files and build scripts are examined
5. Cleanup occurs automatically after analysis

## External Dependencies

### Python Dependencies
- **Flask 3.1.1**: Web application framework
- **GitPython 3.1.44**: Git repository manipulation
- **Requests 2.32.3**: HTTP client for external API calls

### Frontend Dependencies (CDN)
- **Bootstrap 5.3.0**: UI framework and components
- **Font Awesome 6.4.0**: Icon library
- **Vanilla JavaScript**: No additional frameworks

### Development Dependencies
- **Python 3.11+**: Runtime environment
- **Git**: Version control and repository cloning
- **Temporary filesystem**: For repository analysis workspace

## Deployment Strategy

### Development Environment
- **Runtime**: Python 3.11 with Nix package management
- **Development Server**: Flask development server on port 5000
- **Dependency Management**: pip with requirements.txt
- **Auto-restart**: Workflow configured for automatic restart on changes

### Production Considerations
- Application runs as single Flask process
- Temporary directories are created and cleaned up automatically
- No persistent database required (analysis results are ephemeral)
- Static assets served directly by Flask in development

### Environment Configuration
- **Secret Key**: Configurable via environment variable or default
- **Port**: Default 5000, configurable via Flask settings
- **Debug Mode**: Enabled in development environment
- **Logging**: Standard Flask logging to console

### Repository Integration
- Git repositories are cloned to temporary directories
- Analysis workspace is automatically cleaned up
- No persistent storage of cloned repositories
- Source code analysis is performed in isolated environment