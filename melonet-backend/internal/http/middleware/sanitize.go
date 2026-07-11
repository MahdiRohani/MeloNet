package middleware

import (
	"net/url"
	"strings"
)

var sensitiveQueryKeys = map[string]struct{}{
	"token":        {},
	"access_token": {},
	"refresh_token": {},
	"password":     {},
}

func SanitizePath(path string) string {
	if path == "" || !strings.Contains(path, "?") {
		return path
	}

	parts := strings.SplitN(path, "?", 2)
	if len(parts) != 2 {
		return path
	}

	values, err := url.ParseQuery(parts[1])
	if err != nil {
		return parts[0] + "?[redacted]"
	}

	for key := range values {
		if _, sensitive := sensitiveQueryKeys[strings.ToLower(key)]; sensitive {
			values.Set(key, "[redacted]")
		}
	}

	return parts[0] + "?" + values.Encode()
}

func SanitizeAuthHeader(header string) string {
	header = strings.TrimSpace(header)
	if header == "" {
		return ""
	}
	if strings.HasPrefix(strings.ToLower(header), "bearer ") {
		return "Bearer [redacted]"
	}
	return "[redacted]"
}
