package config

import (
	"fmt"
	"os"
	"strconv"
	"time"
)

type Config struct {
	AppEnv        string
	HTTPPort      string
	DatabaseURL   string
	RedisURL      string
	JWTSecret     string
	JWTAccessTTL  time.Duration
	JWTRefreshTTL time.Duration
	PublicBaseURL string
	Storage       StorageConfig
	HTTP          HTTPConfig
	CORS          CORSConfig
}

type StorageConfig struct {
	Endpoint  string
	AccessKey string
	SecretKey string
	Bucket    string
	UseSSL    bool
	Region    string
}

type HTTPConfig struct {
	ReadTimeout     time.Duration
	WriteTimeout    time.Duration
	ShutdownTimeout time.Duration
}

type CORSConfig struct {
	AllowedOrigins []string
}

func Load() (*Config, error) {
	databaseURL, ok := os.LookupEnv("DATABASE_URL")
	if !ok {
		databaseURL = "postgres://melonet:melonet@localhost:5432/melonet?sslmode=disable"
	}
	if databaseURL == "" {
		return nil, fmt.Errorf("DATABASE_URL is required")
	}

	cfg := &Config{
		AppEnv:        getEnv("APP_ENV", "development"),
		HTTPPort:      getEnv("HTTP_PORT", "8080"),
		DatabaseURL:   databaseURL,
		RedisURL:      getEnv("REDIS_URL", "redis://localhost:6379/0"),
		JWTSecret:     getEnv("JWT_SECRET", "dev-jwt-secret-change-in-production"),
		JWTAccessTTL:  getEnvDuration("JWT_ACCESS_TTL", 15*time.Minute),
		JWTRefreshTTL: getEnvDuration("JWT_REFRESH_TTL", 7*24*time.Hour),
		PublicBaseURL: getEnv("PUBLIC_BASE_URL", "http://localhost:8080"),
		Storage: StorageConfig{
			Endpoint:  getEnv("STORAGE_ENDPOINT", "localhost:9000"),
			AccessKey: getEnv("STORAGE_ACCESS_KEY", "melonet"),
			SecretKey: getEnv("STORAGE_SECRET_KEY", "melonetsecret"),
			Bucket:    getEnv("STORAGE_BUCKET", "melonet-media"),
			UseSSL:    getEnvBool("STORAGE_USE_SSL", false),
			Region:    getEnv("STORAGE_REGION", "us-east-1"),
		},
		HTTP: HTTPConfig{
			ReadTimeout:     getEnvDuration("HTTP_READ_TIMEOUT", 15*time.Second),
			WriteTimeout:    getEnvDuration("HTTP_WRITE_TIMEOUT", 15*time.Second),
			ShutdownTimeout: getEnvDuration("HTTP_SHUTDOWN_TIMEOUT", 10*time.Second),
		},
		CORS: CORSConfig{
			AllowedOrigins: getEnvSlice("CORS_ALLOWED_ORIGINS", []string{
				"http://localhost:8080",
				"http://localhost:3000",
				"http://10.0.2.2:8080",
			}),
		},
	}

	return cfg, nil
}

func (c *Config) IsDevelopment() bool {
	return c.AppEnv == "development" || c.AppEnv == "dev"
}

func getEnv(key, fallback string) string {
	if value := os.Getenv(key); value != "" {
		return value
	}
	return fallback
}

func getEnvBool(key string, fallback bool) bool {
	value := os.Getenv(key)
	if value == "" {
		return fallback
	}
	parsed, err := strconv.ParseBool(value)
	if err != nil {
		return fallback
	}
	return parsed
}

func getEnvDuration(key string, fallback time.Duration) time.Duration {
	value := os.Getenv(key)
	if value == "" {
		return fallback
	}
	parsed, err := time.ParseDuration(value)
	if err != nil {
		return fallback
	}
	return parsed
}

func getEnvSlice(key string, fallback []string) []string {
	value := os.Getenv(key)
	if value == "" {
		return fallback
	}
	var result []string
	for _, part := range splitComma(value) {
		if part != "" {
			result = append(result, part)
		}
	}
	if len(result) == 0 {
		return fallback
	}
	return result
}

func splitComma(value string) []string {
	var parts []string
	start := 0
	for i := 0; i <= len(value); i++ {
		if i == len(value) || value[i] == ',' {
			part := trimSpace(value[start:i])
			parts = append(parts, part)
			start = i + 1
		}
	}
	return parts
}

func trimSpace(s string) string {
	for len(s) > 0 && (s[0] == ' ' || s[0] == '\t') {
		s = s[1:]
	}
	for len(s) > 0 && (s[len(s)-1] == ' ' || s[len(s)-1] == '\t') {
		s = s[:len(s)-1]
	}
	return s
}
