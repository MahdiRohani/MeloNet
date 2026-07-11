package main

import (
	"fmt"
	"log"
	"os"
	"path/filepath"

	"melonet-backend/internal/config"
	"melonet-backend/internal/migrate"
)

func main() {
	if len(os.Args) < 2 {
		log.Fatal("usage: migrate [up|down]")
	}

	cfg, err := config.Load()
	if err != nil {
		log.Fatal(err)
	}

	migrationsPath, err := resolveMigrationsPath()
	if err != nil {
		log.Fatal(err)
	}

	switch os.Args[1] {
	case "up":
		if err := migrate.Up(cfg.DatabaseURL, migrationsPath); err != nil {
			log.Fatal(err)
		}
		fmt.Println("migrations applied")
	case "down":
		if err := migrate.Down(cfg.DatabaseURL, migrationsPath); err != nil {
			log.Fatal(err)
		}
		fmt.Println("migrations rolled back")
	default:
		log.Fatal("usage: migrate [up|down]")
	}
}

func resolveMigrationsPath() (string, error) {
	if envPath := os.Getenv("MIGRATIONS_PATH"); envPath != "" {
		return envPath, nil
	}

	wd, err := os.Getwd()
	if err != nil {
		return "", err
	}

	candidate := filepath.Join(wd, "migrations")
	if info, err := os.Stat(candidate); err == nil && info.IsDir() {
		return candidate, nil
	}

	return "", fmt.Errorf("migrations directory not found")
}
