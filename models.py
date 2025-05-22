"""
Database models for MarFaNet refactoring dashboard
"""

from flask_sqlalchemy import SQLAlchemy
from sqlalchemy.orm import DeclarativeBase
from datetime import datetime
import json

class Base(DeclarativeBase):
    pass

db = SQLAlchemy(model_class=Base)

class Project(db.Model):
    """Main project tracking"""
    __tablename__ = 'projects'
    
    id = db.Column(db.Integer, primary_key=True)
    name = db.Column(db.String(100), nullable=False, default='MarFaNet')
    source_repo = db.Column(db.String(255), nullable=False)
    source_tag = db.Column(db.String(50), nullable=False)
    target_package = db.Column(db.String(100), nullable=False, default='net.marfanet.android')
    created_at = db.Column(db.DateTime, default=datetime.utcnow)
    updated_at = db.Column(db.DateTime, default=datetime.utcnow, onupdate=datetime.utcnow)
    
    # Relationships
    analyses = db.relationship('Analysis', backref='project', lazy=True)
    requirements = db.relationship('Requirement', backref='project', lazy=True)
    build_artifacts = db.relationship('BuildArtifact', backref='project', lazy=True)

class Requirement(db.Model):
    """Individual requirements from the 14-point matrix"""
    __tablename__ = 'requirements'
    
    id = db.Column(db.Integer, primary_key=True)
    project_id = db.Column(db.Integer, db.ForeignKey('projects.id'), nullable=False)
    requirement_number = db.Column(db.Integer, nullable=False)
    title = db.Column(db.String(200), nullable=False)
    description = db.Column(db.Text, nullable=False)
    status = db.Column(db.String(20), default='pending')  # pending, in_progress, completed, failed
    priority = db.Column(db.String(10), default='medium')  # low, medium, high, critical
    completion_percentage = db.Column(db.Integer, default=0)
    implementation_notes = db.Column(db.Text)
    commit_hash = db.Column(db.String(40))  # Git commit implementing this requirement
    test_results = db.Column(db.Text)  # JSON string of test results
    created_at = db.Column(db.DateTime, default=datetime.utcnow)
    updated_at = db.Column(db.DateTime, default=datetime.utcnow, onupdate=datetime.utcnow)

class Analysis(db.Model):
    """Repository analysis results"""
    __tablename__ = 'analyses'
    
    id = db.Column(db.Integer, primary_key=True)
    project_id = db.Column(db.Integer, db.ForeignKey('projects.id'), nullable=False)
    analysis_type = db.Column(db.String(50), nullable=False)  # hiddify, xray_integration, rebranding
    version = db.Column(db.String(20), nullable=False)
    status = db.Column(db.String(20), default='pending')  # pending, running, completed, failed
    results = db.Column(db.Text)  # JSON string of analysis results
    file_count = db.Column(db.Integer)
    dependency_count = db.Column(db.Integer)
    singbox_references = db.Column(db.Integer)
    error_message = db.Column(db.Text)
    created_at = db.Column(db.DateTime, default=datetime.utcnow)
    completed_at = db.Column(db.DateTime)

class Specification(db.Model):
    """Generated specifications and documentation"""
    __tablename__ = 'specifications'
    
    id = db.Column(db.Integer, primary_key=True)
    project_id = db.Column(db.Integer, db.ForeignKey('projects.id'), nullable=False)
    spec_type = db.Column(db.String(50), nullable=False)  # xray_integration, rebranding, gfw_knocker
    title = db.Column(db.String(200), nullable=False)
    content = db.Column(db.Text, nullable=False)  # JSON string of specification content
    version = db.Column(db.String(20), default='1.0.0')
    status = db.Column(db.String(20), default='draft')  # draft, review, approved, implemented
    generated_files = db.Column(db.Text)  # JSON array of generated file paths
    validation_results = db.Column(db.Text)  # JSON string of validation results
    created_at = db.Column(db.DateTime, default=datetime.utcnow)
    updated_at = db.Column(db.DateTime, default=datetime.utcnow, onupdate=datetime.utcnow)

class BuildArtifact(db.Model):
    """Build artifacts and generated code"""
    __tablename__ = 'build_artifacts'
    
    id = db.Column(db.Integer, primary_key=True)
    project_id = db.Column(db.Integer, db.ForeignKey('projects.id'), nullable=False)
    artifact_type = db.Column(db.String(50), nullable=False)  # script, skeleton_code, documentation, apk
    name = db.Column(db.String(200), nullable=False)
    file_path = db.Column(db.String(500), nullable=False)
    file_size = db.Column(db.Integer)
    content_hash = db.Column(db.String(64))  # SHA256 hash for integrity
    artifact_metadata = db.Column(db.Text)  # JSON string of additional metadata
    download_count = db.Column(db.Integer, default=0)
    created_at = db.Column(db.DateTime, default=datetime.utcnow)

class BuildMetric(db.Model):
    """Performance and build metrics tracking"""
    __tablename__ = 'build_metrics'
    
    id = db.Column(db.Integer, primary_key=True)
    project_id = db.Column(db.Integer, db.ForeignKey('projects.id'), nullable=False)
    metric_name = db.Column(db.String(100), nullable=False)
    metric_value = db.Column(db.Float, nullable=False)
    metric_unit = db.Column(db.String(20))  # seconds, bytes, percentage, count
    measurement_type = db.Column(db.String(20), nullable=False)  # baseline, current, target
    build_version = db.Column(db.String(50))
    measurement_date = db.Column(db.DateTime, default=datetime.utcnow)
    notes = db.Column(db.Text)

class CodeChange(db.Model):
    """Track specific code changes for each requirement"""
    __tablename__ = 'code_changes'
    
    id = db.Column(db.Integer, primary_key=True)
    requirement_id = db.Column(db.Integer, db.ForeignKey('requirements.id'), nullable=False)
    file_path = db.Column(db.String(500), nullable=False)
    change_type = db.Column(db.String(20), nullable=False)  # add, modify, delete, rename
    old_content = db.Column(db.Text)
    new_content = db.Column(db.Text)
    line_numbers = db.Column(db.String(100))  # e.g., "45-67" or "123"
    commit_hash = db.Column(db.String(40))
    author = db.Column(db.String(100))
    created_at = db.Column(db.DateTime, default=datetime.utcnow)

class TestResult(db.Model):
    """Test execution results"""
    __tablename__ = 'test_results'
    
    id = db.Column(db.Integer, primary_key=True)
    project_id = db.Column(db.Integer, db.ForeignKey('projects.id'), nullable=False)
    test_suite = db.Column(db.String(100), nullable=False)  # lint, detekt, unit_tests, integration_tests
    test_name = db.Column(db.String(200))
    status = db.Column(db.String(20), nullable=False)  # passed, failed, skipped, error
    execution_time = db.Column(db.Float)  # seconds
    error_message = db.Column(db.Text)
    coverage_percentage = db.Column(db.Float)
    executed_at = db.Column(db.DateTime, default=datetime.utcnow)
    build_version = db.Column(db.String(50))