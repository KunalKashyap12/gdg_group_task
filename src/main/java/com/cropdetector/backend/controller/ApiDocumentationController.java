package com.cropdetector.backend.controller;

import java.util.Map;

import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/docs")
public class ApiDocumentationController {

    @GetMapping(value = "/openapi.json", produces = MediaType.APPLICATION_JSON_VALUE)
    public Map<String, Object> getOpenApiSpec() {
        return Map.of(
            "openapi", "3.0.3",
            "info", Map.of(
                "title", "Local Crop Disease Detector API",
                "version", "1.0.0",
                "description", "RESTful backend service for leaf image disease detection, farmer guidance, authentication, and history."
            ),
            "servers", java.util.List.of(
                Map.of("url", "http://localhost:8080", "description", "Local Development Server")
            ),
            "paths", Map.of(
                "/api/v1/predictions", Map.of(
                    "post", Map.of(
                        "summary", "Upload leaf image and predict disease",
                        "description", "Accepts JPEG/PNG leaf image up to 5MB and optional crop name. Returns disease class, confidence, and general guidance.",
                        "requestBody", Map.of(
                            "required", true,
                            "content", Map.of(
                                "multipart/form-data", Map.of(
                                    "schema", Map.of(
                                        "type", "object",
                                        "required", java.util.List.of("image"),
                                        "properties", Map.of(
                                            "image", Map.of("type", "string", "format", "binary", "description", "Leaf image file (JPEG or PNG, max 5MB)"),
                                            "crop", Map.of("type", "string", "example", "tomato", "description", "Optional crop identifier (tomato, potato, apple, corn)")
                                        )
                                    )
                                )
                            )
                        ),
                        "responses", Map.of(
                            "201", Map.of("description", "Prediction completed successfully"),
                            "400", Map.of("description", "Invalid image format, dimensions, or empty file"),
                            "413", Map.of("description", "Image file exceeds 5MB limit"),
                            "502", Map.of("description", "ML service failure or invalid ML output"),
                            "504", Map.of("description", "ML service connection timed out")
                        )
                    ),
                    "get", Map.of(
                        "summary", "Get user prediction history",
                        "description", "Requires Bearer token. Returns paginated history of predictions for the authenticated user.",
                        "parameters", java.util.List.of(
                            Map.of("name", "page", "in", "query", "schema", Map.of("type", "integer", "default", 0)),
                            Map.of("name", "size", "in", "query", "schema", Map.of("type", "integer", "default", 20))
                        ),
                        "responses", Map.of("200", Map.of("description", "Paginated history"))
                    )
                ),
                "/api/v1/crops", Map.of(
                    "get", Map.of(
                        "summary", "List all supported crops",
                        "description", "Returns supported crops, known disease classes, photographic capture tips, and preventive management tips.",
                        "responses", Map.of("200", Map.of("description", "List of crops"))
                    )
                ),
                "/api/v1/auth/register", Map.of(
                    "post", Map.of(
                        "summary", "Register new farmer/user account",
                        "responses", Map.of("201", Map.of("description", "User registered, returns JWT token"))
                    )
                ),
                "/api/v1/auth/login", Map.of(
                    "post", Map.of(
                        "summary", "Authenticate and obtain JWT token",
                        "responses", Map.of("200", Map.of("description", "JWT token and user info"))
                    )
                ),
                "/api/v1/users/me", Map.of(
                    "get", Map.of(
                        "summary", "Get current user profile",
                        "responses", Map.of("200", Map.of("description", "User profile"))
                    )
                )
            )
        );
    }

    @GetMapping(produces = MediaType.TEXT_HTML_VALUE)
    public String getDocsHtml() {
        return """
            <!DOCTYPE html>
            <html lang="en">
            <head>
                <meta charset="UTF-8">
                <meta name="viewport" content="width=device-width, initial-scale=1.0">
                <title>Crop Disease Detector API Documentation</title>
                <link rel="preconnect" href="https://fonts.googleapis.com">
                <link href="https://fonts.googleapis.com/css2?family=Outfit:wght@300;400;600;700&family=JetBrains+Mono:wght@400;600&display=swap" rel="stylesheet">
                <style>
                    :root {
                        --bg: #0d131a;
                        --card-bg: #151e28;
                        --accent: #10b981;
                        --accent-hover: #059669;
                        --text: #e2e8f0;
                        --text-muted: #94a3b8;
                        --border: #1e293b;
                        --badge-post: #0284c7;
                        --badge-get: #10b981;
                        --badge-patch: #f59e0b;
                        --badge-delete: #ef4444;
                    }
                    * { box-sizing: border-box; margin: 0; padding: 0; }
                    body {
                        font-family: 'Outfit', sans-serif;
                        background: var(--bg);
                        color: var(--text);
                        line-height: 1.6;
                        padding: 2rem 1rem;
                    }
                    .container { max-width: 1000px; margin: 0 auto; }
                    header {
                        padding-bottom: 2rem;
                        border-bottom: 1px solid var(--border);
                        margin-bottom: 2rem;
                    }
                    h1 { font-size: 2.2rem; color: #fff; margin-bottom: 0.5rem; }
                    .subtitle { color: var(--text-muted); font-size: 1.1rem; }
                    .badge {
                        display: inline-block;
                        padding: 0.25rem 0.6rem;
                        border-radius: 4px;
                        font-family: 'JetBrains Mono', monospace;
                        font-size: 0.8rem;
                        font-weight: 700;
                        color: #fff;
                        margin-right: 0.5rem;
                    }
                    .badge-get { background: var(--badge-get); }
                    .badge-post { background: var(--badge-post); }
                    .badge-patch { background: var(--badge-patch); }
                    .badge-delete { background: var(--badge-delete); }
                    .endpoint-card {
                        background: var(--card-bg);
                        border: 1px solid var(--border);
                        border-radius: 8px;
                        margin-bottom: 1.25rem;
                        padding: 1.25rem;
                    }
                    .endpoint-header {
                        display: flex;
                        align-items: center;
                        margin-bottom: 0.75rem;
                        font-family: 'JetBrains Mono', monospace;
                        font-size: 1rem;
                    }
                    .endpoint-desc { color: var(--text-muted); margin-bottom: 0.75rem; }
                    pre {
                        background: #090d12;
                        padding: 0.85rem;
                        border-radius: 6px;
                        font-family: 'JetBrains Mono', monospace;
                        font-size: 0.85rem;
                        overflow-x: auto;
                        color: #a7f3d0;
                    }
                    .meta-grid {
                        display: grid;
                        grid-template-columns: repeat(auto-fit, minmax(220px, 1fr));
                        gap: 1rem;
                        margin-bottom: 2rem;
                    }
                    .meta-box {
                        background: var(--card-bg);
                        border: 1px solid var(--border);
                        border-radius: 8px;
                        padding: 1rem;
                    }
                    .meta-box h4 { color: var(--accent); margin-bottom: 0.35rem; }
                </style>
            </head>
            <body>
                <div class="container">
                    <header>
                        <h1>🌱 Crop Disease Detector API</h1>
                        <p class="subtitle">Secure REST API connecting farmers, mobile/web clients, and ML inference service.</p>
                        <p style="margin-top: 0.5rem;"><a href="/api/v1/docs/openapi.json" style="color: var(--accent);">View Raw OpenAPI 3.0 JSON Specification &rarr;</a></p>
                    </header>

                    <div class="meta-grid">
                        <div class="meta-box">
                            <h4>Base URL</h4>
                            <code>http://localhost:8080/api/v1</code>
                        </div>
                        <div class="meta-box">
                            <h4>Auth Strategy</h4>
                            <p>JWT Bearer Token in <code>Authorization: Bearer &lt;token&gt;</code></p>
                        </div>
                        <div class="meta-box">
                            <h4>Image Constraints</h4>
                            <p>JPEG / PNG only, Max 5MB, Max 6000x6000px</p>
                        </div>
                        <div class="meta-box">
                            <h4>Confidence Guardrail</h4>
                            <p>Scores &lt; 0.70 flagged as <code>UNCERTAIN</code></p>
                        </div>
                    </div>

                    <h2 style="margin-bottom: 1rem; color: #fff;">Core Endpoints</h2>

                    <div class="endpoint-card">
                        <div class="endpoint-header">
                            <span class="badge badge-post">POST</span>
                            <span>/api/v1/predictions</span>
                        </div>
                        <p class="endpoint-desc">Upload leaf image (multipart/form-data) for disease classification. Publicly accessible or saves to user account when authenticated.</p>
                        <pre>curl -X POST http://localhost:8080/api/v1/predictions \\
  -F "image=@leaf.jpg" \\
  -F "crop=tomato"</pre>
                    </div>

                    <div class="endpoint-card">
                        <div class="endpoint-header">
                            <span class="badge badge-get">GET</span>
                            <span>/api/v1/predictions</span>
                        </div>
                        <p class="endpoint-desc">Retrieve authenticated user's prediction history with pagination (<code>?page=0&size=20</code>).</p>
                    </div>

                    <div class="endpoint-card">
                        <div class="endpoint-header">
                            <span class="badge badge-get">GET</span>
                            <span>/api/v1/crops</span>
                        </div>
                        <p class="endpoint-desc">List supported crops (Tomato, Potato, Apple, Corn), disease classes, photography tips, and preventive management guidance.</p>
                    </div>

                    <div class="endpoint-card">
                        <div class="endpoint-header">
                            <span class="badge badge-post">POST</span>
                            <span>/api/v1/auth/register</span>
                        </div>
                        <p class="endpoint-desc">Register a new farmer user account. Returns JWT access token.</p>
                    </div>

                    <div class="endpoint-card">
                        <div class="endpoint-header">
                            <span class="badge badge-post">POST</span>
                            <span>/api/v1/auth/login</span>
                        </div>
                        <p class="endpoint-desc">Authenticate existing user with email and password to receive a JWT access token.</p>
                    </div>

                    <div class="endpoint-card">
                        <div class="endpoint-header">
                            <span class="badge badge-get">GET</span>
                            <span>/api/v1/users/me</span>
                        </div>
                        <p class="endpoint-desc">Fetch authenticated user's profile and preferences (language, notification settings).</p>
                    </div>
                </div>
            </body>
            </html>
            """;
    }
}
