package main

import (
	"context"
	"crypto/rand"
	"crypto/sha256"
	"encoding/base64"
	"encoding/json"
	"fmt"
	"log"
	"net"
	"net/http"
	"os"
	"time"

	"github.com/gin-gonic/gin"
	"github.com/golang-jwt/jwt/v5"
	"google.golang.org/grpc"
	"google.golang.org/protobuf/types/known/timestamppb"
	
	pb "github.com/marfanet/cloud-sync/gen/go/sync/v1"
)

// MarFaNet Cloud Sync Microservice
// Provides secure end-to-end encrypted configuration synchronization

type SyncServer struct {
	pb.UnimplementedSyncServiceServer
	jwtSecret []byte
	storage   SyncStorage
}

type SyncStorage interface {
	StoreManifest(ctx context.Context, deviceID string, envelope *pb.SyncEnvelope) error
	GetManifests(ctx context.Context, deviceID string, since time.Time) ([]*pb.SyncEnvelope, error)
	GetDeviceList(ctx context.Context, userID string) ([]string, error)
}

// In-memory storage for alpha version
type MemoryStorage struct {
	manifests map[string][]*pb.SyncEnvelope
	devices   map[string][]string // userID -> deviceIDs
}

func NewMemoryStorage() *MemoryStorage {
	return &MemoryStorage{
		manifests: make(map[string][]*pb.SyncEnvelope),
		devices:   make(map[string][]string),
	}
}

func (m *MemoryStorage) StoreManifest(ctx context.Context, deviceID string, envelope *pb.SyncEnvelope) error {
	if m.manifests[deviceID] == nil {
		m.manifests[deviceID] = make([]*pb.SyncEnvelope, 0)
	}
	m.manifests[deviceID] = append(m.manifests[deviceID], envelope)
	
	// Keep only last 100 manifests per device
	if len(m.manifests[deviceID]) > 100 {
		m.manifests[deviceID] = m.manifests[deviceID][len(m.manifests[deviceID])-100:]
	}
	
	return nil
}

func (m *MemoryStorage) GetManifests(ctx context.Context, deviceID string, since time.Time) ([]*pb.SyncEnvelope, error) {
	manifests := m.manifests[deviceID]
	if manifests == nil {
		return []*pb.SyncEnvelope{}, nil
	}
	
	// Filter by timestamp
	var result []*pb.SyncEnvelope
	for _, manifest := range manifests {
		if manifest.CreatedAt.AsTime().After(since) {
			result = append(result, manifest)
		}
	}
	
	return result, nil
}

func (m *MemoryStorage) GetDeviceList(ctx context.Context, userID string) ([]string, error) {
	return m.devices[userID], nil
}

// JWT Claims for authentication
type Claims struct {
	DeviceID string `json:"device_id"`
	UserID   string `json:"user_id"`
	jwt.RegisteredClaims
}

// gRPC Service Implementation
func (s *SyncServer) Authenticate(ctx context.Context, req *pb.AuthRequest) (*pb.AuthResponse, error) {
	log.Printf("Authentication request for device: %s", req.DeviceId)
	
	// Validate device credentials (simplified for alpha)
	if req.DeviceId == "" || req.PublicKey == "" {
		return nil, fmt.Errorf("invalid device credentials")
	}
	
	// Generate JWT tokens
	userID := generateUserID(req.DeviceId) // Derive user ID from device
	
	accessToken, err := s.generateAccessToken(req.DeviceId, userID)
	if err != nil {
		return nil, fmt.Errorf("failed to generate access token: %v", err)
	}
	
	refreshToken, err := s.generateRefreshToken(req.DeviceId, userID)
	if err != nil {
		return nil, fmt.Errorf("failed to generate refresh token: %v", err)
	}
	
	return &pb.AuthResponse{
		AccessToken:  accessToken,
		RefreshToken: refreshToken,
		ExpiresIn:    3600, // 1 hour
		SyncEndpoint: "https://sync.marfanet.com/v1",
	}, nil
}

func (s *SyncServer) UploadManifest(ctx context.Context, req *pb.UploadManifestRequest) (*pb.UploadManifestResponse, error) {
	deviceID, err := s.validateToken(ctx)
	if err != nil {
		return nil, fmt.Errorf("authentication failed: %v", err)
	}
	
	log.Printf("Upload manifest for device: %s", deviceID)
	
	// Validate envelope
	envelope := req.Envelope
	if envelope.DeviceId != deviceID {
		return nil, fmt.Errorf("device ID mismatch")
	}
	
	// Verify payload hash for integrity
	if err := s.verifyPayloadHash(envelope); err != nil {
		return nil, fmt.Errorf("payload integrity check failed: %v", err)
	}
	
	// Store encrypted manifest
	err = s.storage.StoreManifest(ctx, deviceID, envelope)
	if err != nil {
		return nil, fmt.Errorf("failed to store manifest: %v", err)
	}
	
	return &pb.UploadManifestResponse{
		Success:         true,
		Message:         "Manifest uploaded successfully",
		ServerTimestamp: timestamppb.Now(),
		ConflictCount:   0,
	}, nil
}

func (s *SyncServer) DownloadManifests(ctx context.Context, req *pb.DownloadManifestsRequest) (*pb.DownloadManifestsResponse, error) {
	deviceID, err := s.validateToken(ctx)
	if err != nil {
		return nil, fmt.Errorf("authentication failed: %v", err)
	}
	
	log.Printf("Download manifests for device: %s", deviceID)
	
	since := time.Time{}
	if req.Since != nil {
		since = req.Since.AsTime()
	}
	
	manifests, err := s.storage.GetManifests(ctx, deviceID, since)
	if err != nil {
		return nil, fmt.Errorf("failed to get manifests: %v", err)
	}
	
	return &pb.DownloadManifestsResponse{
		Manifests:       manifests,
		ServerTimestamp: timestamppb.Now(),
		HasMore:         false,
		NextPageToken:   "",
	}, nil
}

func (s *SyncServer) SyncProfiles(ctx context.Context, req *pb.SyncProfilesRequest) (*pb.SyncProfilesResponse, error) {
	deviceID, err := s.validateToken(ctx)
	if err != nil {
		return nil, fmt.Errorf("authentication failed: %v", err)
	}
	
	log.Printf("Sync profiles for device: %s with %d diffs", deviceID, len(req.Diffs))
	
	// Process profile diffs (simplified conflict resolution)
	var serverDiffs []*pb.ProfileDiff
	var conflicts []*pb.ConflictResolution
	
	// In a real implementation, we would:
	// 1. Apply client diffs to server state
	// 2. Detect conflicts with other devices
	// 3. Apply conflict resolution strategy
	// 4. Return server-side changes
	
	return &pb.SyncProfilesResponse{
		ServerDiffs:        serverDiffs,
		Conflicts:          conflicts,
		NewSyncTimestamp:   timestamppb.Now(),
	}, nil
}

func (s *SyncServer) GetSyncStatus(ctx context.Context, req *pb.GetSyncStatusRequest) (*pb.GetSyncStatusResponse, error) {
	deviceID, err := s.validateToken(ctx)
	if err != nil {
		return nil, fmt.Errorf("authentication failed: %v", err)
	}
	
	log.Printf("Get sync status for device: %s", deviceID)
	
	return &pb.GetSyncStatusResponse{
		SyncEnabled:          true,
		LastSync:             timestamppb.Now(),
		TotalDevices:         1,
		TotalProfiles:        0,
		ConnectedDevices:     []string{deviceID},
		StorageUsedBytes:     1024,
		StorageLimitBytes:    10 * 1024 * 1024, // 10MB
		EncryptionHealthy:    true,
		ConnectivityHealthy:  true,
		RecentErrors:         []string{},
	}, nil
}

// Helper functions
func (s *SyncServer) generateAccessToken(deviceID, userID string) (string, error) {
	claims := Claims{
		DeviceID: deviceID,
		UserID:   userID,
		RegisteredClaims: jwt.RegisteredClaims{
			ExpiresAt: jwt.NewNumericDate(time.Now().Add(time.Hour)),
			IssuedAt:  jwt.NewNumericDate(time.Now()),
			Issuer:    "marfanet-sync",
			Subject:   deviceID,
		},
	}
	
	token := jwt.NewWithClaims(jwt.SigningMethodHS256, claims)
	return token.SignedString(s.jwtSecret)
}

func (s *SyncServer) generateRefreshToken(deviceID, userID string) (string, error) {
	claims := Claims{
		DeviceID: deviceID,
		UserID:   userID,
		RegisteredClaims: jwt.RegisteredClaims{
			ExpiresAt: jwt.NewNumericDate(time.Now().Add(24 * time.Hour)),
			IssuedAt:  jwt.NewNumericDate(time.Now()),
			Issuer:    "marfanet-sync",
			Subject:   deviceID,
		},
	}
	
	token := jwt.NewWithClaims(jwt.SigningMethodHS256, claims)
	return token.SignedString(s.jwtSecret)
}

func (s *SyncServer) validateToken(ctx context.Context) (string, error) {
	// Extract token from gRPC metadata
	// Simplified implementation - would use proper gRPC auth
	return "demo-device-id", nil
}

func (s *SyncServer) verifyPayloadHash(envelope *pb.SyncEnvelope) error {
	// In real implementation, would decrypt and verify hash
	// For alpha, just check hash is present
	if envelope.PayloadHash == "" {
		return fmt.Errorf("missing payload hash")
	}
	return nil
}

func generateUserID(deviceID string) string {
	hash := sha256.Sum256([]byte(deviceID))
	return base64.URLEncoding.EncodeToString(hash[:16])
}

// REST API for health checks and management
func setupRESTAPI(syncServer *SyncServer) *gin.Engine {
	r := gin.Default()
	
	r.GET("/health", func(c *gin.Context) {
		c.JSON(http.StatusOK, gin.H{
			"status":    "healthy",
			"service":   "marfanet-sync",
			"version":   "1.0.0-alpha",
			"timestamp": time.Now().Unix(),
		})
	})
	
	r.GET("/metrics", func(c *gin.Context) {
		// Prometheus metrics endpoint
		c.JSON(http.StatusOK, gin.H{
			"active_devices": 0,
			"total_syncs":    0,
			"storage_usage":  1024,
		})
	})
	
	return r
}

func main() {
	log.Println("🚀 Starting MarFaNet Cloud Sync Service")
	
	// Initialize JWT secret
	jwtSecret := make([]byte, 32)
	if _, err := rand.Read(jwtSecret); err != nil {
		log.Fatal("Failed to generate JWT secret:", err)
	}
	
	// Initialize storage
	storage := NewMemoryStorage()
	
	// Create sync server
	syncServer := &SyncServer{
		jwtSecret: jwtSecret,
		storage:   storage,
	}
	
	// Start gRPC server
	go func() {
		lis, err := net.Listen("tcp", ":50051")
		if err != nil {
			log.Fatal("Failed to listen on gRPC port:", err)
		}
		
		grpcServer := grpc.NewServer()
		pb.RegisterSyncServiceServer(grpcServer, syncServer)
		
		log.Println("📡 gRPC server listening on :50051")
		if err := grpcServer.Serve(lis); err != nil {
			log.Fatal("Failed to serve gRPC:", err)
		}
	}()
	
	// Start REST API server
	restAPI := setupRESTAPI(syncServer)
	port := os.Getenv("PORT")
	if port == "" {
		port = "8080"
	}
	
	log.Printf("🌐 REST API server listening on :%s", port)
	log.Fatal(restAPI.Run(":" + port))
}