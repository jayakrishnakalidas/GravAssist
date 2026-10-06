import http.server
import socketserver
import urllib.request
import urllib.parse
import urllib.error
import json
import os
import secrets
import subprocess
import threading
import re
import time
from datetime import datetime

# ===================================================
# Configuration
# ===================================================
PROXY_PORT = 1234
LM_STUDIO_BACKEND = "http://127.0.0.1:4567"
CUSTOM_MODEL_NAME = "GravAssist-Local-Model-v1"
DEFAULT_PHP_URL = "https://aicontroller.lovestoblog.com/ailinkupload.php"
SECRET_KEY = "6660_0D_F"

DATA_FILE = os.path.join(os.path.dirname(__file__), "proxy_data.json")

# Global Cloudflare state & process reference
active_cloudflare_url = ""
cf_process = None
cf_running = False

# ===================================================
# Cloudflare Link Uploader Helper
# ===================================================
def upload_link_to_php(tunnel_url, php_endpoint=DEFAULT_PHP_URL, secret_key=SECRET_KEY):
    if not tunnel_url:
        return False, "Tunnel URL cannot be empty"

    if not tunnel_url.startswith("http://") and not tunnel_url.startswith("https://"):
        tunnel_url = "https://" + tunnel_url

    params = urllib.parse.urlencode({
        "url": tunnel_url,
        "key": secret_key
    })

    target_full_url = f"{php_endpoint}?{params}"

    headers = {
        'User-Agent': 'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/122.0.0.0 Safari/537.36',
        'Accept': 'text/html,application/xhtml+xml,application/xml;q=0.9,image/avif,image/webp,*/*;q=0.8',
        'Accept-Language': 'en-US,en;q=0.9',
        'Connection': 'keep-alive'
    }

    try:
        req = urllib.request.Request(target_full_url, headers=headers, method='GET')
        with urllib.request.urlopen(req, timeout=10) as response:
            res_text = response.read().decode('utf-8').strip()
            print(f"[{datetime.now().strftime('%H:%M:%S')}] 🚀 Link Upload Success: {res_text}")
            return True, res_text
    except Exception as e:
        err_msg = f"Failed to upload link to PHP server: {str(e)}"
        print(f"[{datetime.now().strftime('%H:%M:%S')}] ❌ {err_msg}")
        return False, err_msg

# ===================================================
# Cloudflare Manager (Start / Stop Control)
# ===================================================
def run_cloudflare_tunnel_process():
    global cf_process, cf_running, active_cloudflare_url
    print(f"[{datetime.now().strftime('%H:%M:%S')}] 🌀 Starting Cloudflare Tunnel on port {PROXY_PORT}...")
    
    cmd = ["cloudflared", "tunnel", "--url", f"http://127.0.0.1:{PROXY_PORT}"]
    
    try:
        cf_process = subprocess.Popen(
            cmd,
            stdout=subprocess.PIPE,
            stderr=subprocess.STDOUT,
            text=True,
            bufsize=1
        )
        cf_running = True

        url_regex = re.compile(r'https://[a-zA-Z0-9-]+\.trycloudflare\.com')

        for line in cf_process.stdout:
            if not cf_running:
                break
            match = url_regex.search(line)
            if match:
                found_url = match.group(0)
                if found_url != active_cloudflare_url:
                    active_cloudflare_url = found_url
                    print(f"\n===================================================")
                    print(f"✨ Cloudflare Tunnel Active: {active_cloudflare_url}")
                    print(f"===================================================\n")
                    
                    db_data["current_tunnel_url"] = active_cloudflare_url
                    Database.save(db_data)

                    upload_link_to_php(active_cloudflare_url)

    except FileNotFoundError:
        print(f"[{datetime.now().strftime('%H:%M:%S')}] ⚠️ 'cloudflared' binary not found in system PATH.")
        cf_running = False
    except Exception as e:
        print(f"[{datetime.now().strftime('%H:%M:%S')}] ⚠️ Cloudflare Tunnel Error: {e}")
        cf_running = False

def start_cf_tunnel_thread():
    global cf_running
    if cf_running:
        return True, "Cloudflare Tunnel is already running."
    thread = threading.Thread(target=run_cloudflare_tunnel_process, daemon=True)
    thread.start()
    return True, "Cloudflare Tunnel starting..."

def stop_cf_tunnel():
    global cf_process, cf_running, active_cloudflare_url
    if not cf_running and cf_process is None:
        return False, "Cloudflare Tunnel is not running."
    
    cf_running = False
    try:
        if cf_process:
            cf_process.terminate()
            cf_process.kill()
            cf_process = None
        active_cloudflare_url = ""
        db_data["current_tunnel_url"] = ""
        Database.save(db_data)
        print(f"[{datetime.now().strftime('%H:%M:%S')}] 🛑 Cloudflare Tunnel Stopped.")
        return True, "Cloudflare Tunnel stopped successfully."
    except Exception as e:
        return False, f"Failed to stop Cloudflare Tunnel: {str(e)}"

# ===================================================
# Database Manager
# ===================================================
class Database:
    @staticmethod
    def load():
        if os.path.exists(DATA_FILE):
            try:
                with open(DATA_FILE, "r") as f:
                    return json.load(f)
            except Exception:
                pass
        
        default_key = f"sk-{secrets.token_hex(12)}"
        data = {
            "current_tunnel_url": "",
            "php_endpoint_url": DEFAULT_PHP_URL,
            "api_keys": {
                default_key: {
                    "user_name": "Admin User",
                    "created_at": datetime.now().strftime("%Y-%m-%d %H:%M:%S"),
                    "active": True
                }
            },
            "usage": {
                default_key: {
                    "requests": 0,
                    "prompt_tokens": 0,
                    "completion_tokens": 0,
                    "last_seen": "Never"
                }
            }
        }
        Database.save(data)
        return data

    @staticmethod
    def save(data):
        with open(DATA_FILE, "w") as f:
            json.dump(data, f, indent=4)

db_data = Database.load()

# ===================================================
# HTTP Proxy Handler
# ===================================================
class ProxyHandler(http.server.BaseHTTPRequestHandler):

    def log_message(self, format, *args):
        print(f"[{datetime.now().strftime('%H:%M:%S')}] {self.address_string()} - {format % args}")

    def do_OPTIONS(self):
        self.send_response(200)
        self.send_cors_headers()
        self.end_headers()

    def send_cors_headers(self):
        self.send_header("Access-Control-Allow-Origin", "*")
        self.send_header("Access-Control-Allow-Methods", "GET, POST, OPTIONS, DELETE, PUT")
        self.send_header("Access-Control-Allow-Headers", "Authorization, Content-Type")

    def send_json(self, status_code, data):
        self.send_response(status_code)
        self.send_cors_headers()
        self.send_header("Content-Type", "application/json")
        body = json.dumps(data).encode("utf-8")
        self.send_header("Content-Length", str(len(body)))
        self.end_headers()
        self.wfile.write(body)

    def extract_bearer_token(self):
        auth_header = self.headers.get("Authorization", "")
        if auth_header.startswith("Bearer "):
            return auth_header[7:].strip()
        return None

    def validate_api_key(self):
        token = self.extract_bearer_token()
        if not token:
            return None, "Missing Authorization Header"
        
        keys = db_data.get("api_keys", {})
        if token in keys and keys[token].get("active", False):
            return token, None
        return None, "Invalid or Inactive API Key"

    def do_GET(self):
        url_path = self.path.split("?")[0]

        if url_path in ["/dashboard", "/admin"]:
            self.serve_dashboard()
            return
        
        if url_path == "/api/admin/data":
            response = dict(db_data)
            response["cf_running"] = cf_running
            response["current_tunnel_url"] = active_cloudflare_url
            self.send_json(200, response)
            return

        if url_path == "/v1/models":
            self.send_json(200, {
                "object": "list",
                "data": [
                    {
                        "id": CUSTOM_MODEL_NAME,
                        "object": "model",
                        "created": 1700000000,
                        "owned_by": "gravassist"
                    }
                ]
            })
            return

        if url_path == "/api/v1/models/load":
            self.send_json(403, {"status": "error", "message": "no permission"})
            return

        if url_path == "/api/v1/models/download":
            self.send_json(200, {"status": "restricted", "message": "contact @ASSISTANTofDF_Bot in telegram"})
            return

        self.proxy_request("GET")

    def do_POST(self):
        url_path = self.path.split("?")[0]

        # Cloudflare Tunnel Start / Stop Control
        if url_path == "/api/admin/start-tunnel":
            success, msg = start_cf_tunnel_thread()
            self.send_json(200 if success else 500, {"status": "success" if success else "error", "message": msg})
            return

        if url_path == "/api/admin/stop-tunnel":
            success, msg = stop_cf_tunnel()
            self.send_json(200 if success else 500, {"status": "success" if success else "error", "message": msg})
            return

        if url_path == "/api/admin/upload-link":
            content_length = int(self.headers.get("Content-Length", 0))
            body_bytes = self.rfile.read(content_length)
            payload = json.loads(body_bytes.decode("utf-8")) if body_bytes else {}
            
            tunnel_url = payload.get("tunnel_url", "").strip()
            php_endpoint = payload.get("php_endpoint", db_data.get("php_endpoint_url", DEFAULT_PHP_URL)).strip()
            
            db_data["current_tunnel_url"] = tunnel_url
            db_data["php_endpoint_url"] = php_endpoint
            Database.save(db_data)

            success, msg = upload_link_to_php(tunnel_url, php_endpoint)
            if success:
                self.send_json(200, {"status": "success", "message": msg})
            else:
                self.send_json(500, {"status": "error", "message": msg})
            return

        if url_path == "/api/admin/create-key":
            content_length = int(self.headers.get("Content-Length", 0))
            body_bytes = self.rfile.read(content_length)
            payload = json.loads(body_bytes.decode("utf-8")) if body_bytes else {}
            
            user_name = payload.get("user_name", "User").strip()
            new_key = f"sk-{secrets.token_hex(12)}"
            
            db_data["api_keys"][new_key] = {
                "user_name": user_name,
                "created_at": datetime.now().strftime("%Y-%m-%d %H:%M:%S"),
                "active": True
            }
            db_data["usage"][new_key] = {
                "requests": 0,
                "prompt_tokens": 0,
                "completion_tokens": 0,
                "last_seen": "Never"
            }
            Database.save(db_data)
            self.send_json(200, {"status": "success", "key": new_key, "user_name": user_name})
            return

        if url_path == "/api/admin/toggle-key":
            content_length = int(self.headers.get("Content-Length", 0))
            body_bytes = self.rfile.read(content_length)
            payload = json.loads(body_bytes.decode("utf-8")) if body_bytes else {}
            
            target_key = payload.get("key")
            if target_key in db_data["api_keys"]:
                current_state = db_data["api_keys"][target_key]["active"]
                db_data["api_keys"][target_key]["active"] = not current_state
                Database.save(db_data)
                self.send_json(200, {"status": "success", "active": not current_state})
            else:
                self.send_json(404, {"status": "error", "message": "Key not found"})
            return

        if url_path == "/api/admin/delete-key":
            content_length = int(self.headers.get("Content-Length", 0))
            body_bytes = self.rfile.read(content_length)
            payload = json.loads(body_bytes.decode("utf-8")) if body_bytes else {}
            
            target_key = payload.get("key")
            if target_key in db_data["api_keys"]:
                del db_data["api_keys"][target_key]
                if target_key in db_data["usage"]:
                    del db_data["usage"][target_key]
                Database.save(db_data)
                self.send_json(200, {"status": "success"})
            else:
                self.send_json(404, {"status": "error", "message": "Key not found"})
            return

        if url_path == "/api/v1/models/load":
            self.send_json(403, {"status": "error", "message": "no permission"})
            return

        if url_path == "/api/v1/models/download":
            self.send_json(200, {"status": "restricted", "message": "contact @ASSISTANTofDF_Bot in telegram"})
            return

        api_key, err = self.validate_api_key()
        if err:
            self.send_json(401, {"error": "Unauthorized", "message": err})
            return

        self.proxy_request("POST", api_key=api_key)

    def proxy_request(self, method, api_key=None):
        content_length = int(self.headers.get("Content-Length", 0))
        body = self.rfile.read(content_length) if content_length > 0 else None

        target_url = f"{LM_STUDIO_BACKEND}{self.path}"
        req = urllib.request.Request(target_url, data=body, method=method)

        for k, v in self.headers.items():
            if k.lower() != "host":
                req.add_header(k, v)

        try:
            with urllib.request.urlopen(req) as resp:
                resp_body = resp.read()
                
                if api_key and "/v1/chat/completions" in self.path:
                    try:
                        resp_json = json.loads(resp_body.decode("utf-8"))
                        usage = resp_json.get("usage", {})
                        p_tokens = usage.get("prompt_tokens", 0)
                        c_tokens = usage.get("completion_tokens", 0)

                        if api_key in db_data["usage"]:
                            u_stats = db_data["usage"][api_key]
                            u_stats["requests"] += 1
                            u_stats["prompt_tokens"] += p_tokens
                            u_stats["completion_tokens"] += c_tokens
                            u_stats["last_seen"] = datetime.now().strftime("%Y-%m-%d %H:%M:%S")
                            Database.save(db_data)
                    except Exception:
                        pass

                self.send_response(resp.status)
                self.send_cors_headers()
                for k, v in resp.headers.items():
                    if k.lower() not in ["content-encoding", "transfer-encoding", "content-length"]:
                        self.send_header(k, v)
                self.send_header("Content-Length", str(len(resp_body)))
                self.end_headers()
                self.wfile.write(resp_body)

        except urllib.error.HTTPError as e:
            err_body = e.read()
            self.send_response(e.code)
            self.send_cors_headers()
            self.send_header("Content-Type", "application/json")
            self.send_header("Content-Length", str(len(err_body)))
            self.end_headers()
            self.wfile.write(err_body)

        except Exception as e:
            self.send_json(502, {
                "error": "Backend Connection Failed",
                "details": f"Failed to connect to LM Studio at {LM_STUDIO_BACKEND}: {str(e)}"
            })

    def serve_dashboard(self):
        html = """<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>GravAssist AI Proxy Admin</title>
    <style>
        :root { --bg: #0F0F1A; --surface: #1A1A2E; --card: #16213E; --primary: #6C5CE7; --accent: #00CEC9; --text: #F1F2F6; --muted: #A4B0BE; --danger: #FF7675; --success: #00B894; }
        body { font-family: 'Segoe UI', Tahoma, Geneva, Verdana, sans-serif; background: var(--bg); color: var(--text); margin: 0; padding: 30px; }
        .container { max-width: 1100px; margin: 0 auto; }
        .header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 30px; border-bottom: 1px solid #222; padding-bottom: 20px; }
        h1 { color: var(--primary); margin: 0; font-size: 28px; }
        .grid { display: grid; grid-template-columns: repeat(auto-fit, minmax(220px, 1fr)); gap: 20px; margin-bottom: 30px; }
        .card { background: var(--surface); padding: 20px; border-radius: 12px; border: 1px solid #2a2a4a; }
        .card h3 { margin: 0 0 10px; color: var(--muted); font-size: 14px; text-transform: uppercase; }
        .card .value { font-size: 24px; font-weight: bold; color: var(--accent); }
        .section { background: var(--surface); padding: 24px; border-radius: 12px; margin-bottom: 30px; border: 1px solid #2a2a4a; }
        .section h2 { margin-top: 0; color: var(--text); font-size: 20px; }
        table { width: 100%; border-collapse: collapse; margin-top: 15px; }
        th, td { padding: 12px; text-align: left; border-bottom: 1px solid #2a2a4a; font-size: 14px; }
        th { color: var(--muted); background: var(--card); }
        .badge { padding: 4px 8px; border-radius: 6px; font-size: 12px; font-weight: bold; }
        .badge-active { background: rgba(0,184,148,0.2); color: var(--success); }
        .badge-disabled { background: rgba(255,118,117,0.2); color: var(--danger); }
        .btn { padding: 8px 14px; border-radius: 6px; border: none; cursor: pointer; font-weight: bold; transition: 0.2s; }
        .btn-primary { background: var(--primary); color: #fff; }
        .btn-primary:hover { background: #5a4bcf; }
        .btn-success { background: var(--success); color: #fff; }
        .btn-danger { background: var(--danger); color: #fff; }
        .btn-toggle { background: #333; color: var(--text); }
        input[type="text"] { padding: 10px; border-radius: 6px; border: 1px solid #333; background: var(--bg); color: #fff; width: 280px; margin-right: 10px; }
        .key-text { font-family: monospace; color: var(--accent); }
        .status-msg { margin-top: 10px; font-size: 14px; font-weight: bold; }
        .cf-bar { display: flex; align-items: center; justify-content: space-between; background: var(--card); padding: 16px; border-radius: 8px; margin-bottom: 16px; }
    </style>
</head>
<body>
    <div class="container">
        <div class="header">
            <h1>🤖 GravAssist Proxy Dashboard</h1>
            <div>Port: <strong>1234</strong> | LM Studio: <strong>4567</strong></div>
        </div>

        <div class="section">
            <h2>🌀 Cloudflare Tunnel Controller &amp; PHP Auto-Uploader</h2>
            
            <div class="cf-bar">
                <div>
                    Status: <span id="cf-status-badge" class="badge badge-disabled">OFFLINE</span>
                    <span id="cf-url-display" style="margin-left: 10px; font-family: monospace; color: var(--accent);"></span>
                </div>
                <div>
                    <button id="btn-start-cf" class="btn btn-success" onclick="startTunnel()">Start Cloudflare Tunnel</button>
                    <button id="btn-stop-cf" class="btn btn-danger" onclick="stopTunnel()" style="margin-left: 8px;">Turn Off Cloudflare Tunnel</button>
                </div>
            </div>

            <p style="color: var(--muted); font-size: 14px; margin-top: 20px;">Manual PHP Upload Endpoint (<code>ailinkupload.php</code>):</p>
            <div style="display: flex; align-items: center; flex-wrap: wrap; gap: 10px;">
                <input type="text" id="tunnel-url-input" style="width: 350px;" placeholder="https://xxxx.trycloudflare.com">
                <button class="btn btn-primary" onclick="uploadTunnelLink()">Upload Link to PHP Server</button>
            </div>
            <div id="upload-status" class="status-msg"></div>
        </div>

        <div class="grid">
            <div class="card">
                <h3>Total API Keys</h3>
                <div class="value" id="stat-keys">0</div>
            </div>
            <div class="card">
                <h3>Total Requests</h3>
                <div class="value" id="stat-requests">0</div>
            </div>
            <div class="card">
                <h3>Prompt Tokens</h3>
                <div class="value" id="stat-prompt">0</div>
            </div>
            <div class="card">
                <h3>Completion Tokens</h3>
                <div class="value" id="stat-completion">0</div>
            </div>
        </div>

        <div class="section">
            <h2>➕ Create New API Key</h2>
            <div style="display: flex; align-items: center;">
                <input type="text" id="new-user-name" placeholder="User / Device Name">
                <button class="btn btn-primary" onclick="createKey()">Generate API Key</button>
            </div>
        </div>

        <div class="section">
            <h2>🔑 API Keys &amp; User Usage</h2>
            <table>
                <thead>
                    <tr>
                        <th>User Name</th>
                        <th>API Key</th>
                        <th>Status</th>
                        <th>Requests</th>
                        <th>Total Tokens</th>
                        <th>Last Active</th>
                        <th>Actions</th>
                    </tr>
                </thead>
                <tbody id="keys-table">
                    <tr><td colspan="7">Loading data...</td></tr>
                </tbody>
            </table>
        </div>
    </div>

    <script>
        async function loadData() {
            const res = await fetch('/api/admin/data');
            const data = await res.json();
            
            const cfRunning = data.cf_running;
            const currentUrl = data.current_tunnel_url || '';

            const cfBadge = document.getElementById('cf-status-badge');
            const cfUrlDisp = document.getElementById('cf-url-display');
            
            if (cfRunning) {
                cfBadge.className = 'badge badge-active';
                cfBadge.innerText = 'ONLINE';
                cfUrlDisp.innerText = currentUrl;
                if (currentUrl) document.getElementById('tunnel-url-input').value = currentUrl;
            } else {
                cfBadge.className = 'badge badge-disabled';
                cfBadge.innerText = 'OFFLINE';
                cfUrlDisp.innerText = '';
            }

            const keys = data.api_keys || {};
            const usage = data.usage || {};

            let totalRequests = 0;
            let totalPrompt = 0;
            let totalCompletion = 0;

            const tbody = document.getElementById('keys-table');
            tbody.innerHTML = '';

            const keyList = Object.keys(keys);
            document.getElementById('stat-keys').innerText = keyList.length;

            keyList.forEach(key => {
                const info = keys[key];
                const stats = usage[key] || { requests: 0, prompt_tokens: 0, completion_tokens: 0, last_seen: 'Never' };

                totalRequests += stats.requests;
                totalPrompt += stats.prompt_tokens;
                totalCompletion += stats.completion_tokens;

                const tr = document.createElement('tr');
                tr.innerHTML = `
                    <td><strong>${info.user_name}</strong></td>
                    <td class="key-text">${key}</td>
                    <td><span class="badge ${info.active ? 'badge-active' : 'badge-disabled'}">${info.active ? 'ACTIVE' : 'DISABLED'}</span></td>
                    <td>${stats.requests}</td>
                    <td>${stats.prompt_tokens + stats.completion_tokens} (${stats.prompt_tokens}p / ${stats.completion_tokens}c)</td>
                    <td>${stats.last_seen}</td>
                    <td>
                        <button class="btn btn-toggle" onclick="toggleKey('${key}')">${info.active ? 'Disable' : 'Enable'}</button>
                        <button class="btn btn-danger" onclick="deleteKey('${key}')">Delete</button>
                    </td>
                `;
                tbody.appendChild(tr);
            });

            document.getElementById('stat-requests').innerText = totalRequests;
            document.getElementById('stat-prompt').innerText = totalPrompt;
            document.getElementById('stat-completion').innerText = totalCompletion;
        }

        async function startTunnel() {
            const statusDiv = document.getElementById('upload-status');
            statusDiv.style.color = '#00CEC9';
            statusDiv.innerText = 'Starting Cloudflare Tunnel...';
            await fetch('/api/admin/start-tunnel', { method: 'POST' });
            setTimeout(loadData, 2000);
        }

        async function stopTunnel() {
            const statusDiv = document.getElementById('upload-status');
            statusDiv.style.color = '#FF7675';
            statusDiv.innerText = 'Stopping Cloudflare Tunnel...';
            await fetch('/api/admin/stop-tunnel', { method: 'POST' });
            loadData();
        }

        async function uploadTunnelLink() {
            const url = document.getElementById('tunnel-url-input').value.trim();
            const statusDiv = document.getElementById('upload-status');
            if (!url) return alert('Please enter a valid Cloudflare Tunnel URL');
            
            statusDiv.style.color = '#00CEC9';
            statusDiv.innerText = 'Uploading link to PHP server...';

            try {
                const res = await fetch('/api/admin/upload-link', {
                    method: 'POST',
                    headers: { 'Content-Type': 'application/json' },
                    body: JSON.stringify({ tunnel_url: url })
                });
                const resp = await res.json();
                if (res.ok) {
                    statusDiv.style.color = '#00B894';
                    statusDiv.innerText = '✅ ' + resp.message;
                } else {
                    statusDiv.style.color = '#FF7675';
                    statusDiv.innerText = '❌ Error: ' + resp.message;
                }
            } catch (e) {
                statusDiv.style.color = '#FF7675';
                statusDiv.innerText = '❌ Network Error: ' + e.message;
            }
        }

        async function createKey() {
            val = document.getElementById('new-user-name').value.trim();
            if (!val) return alert('Please enter a user name');
            await fetch('/api/admin/create-key', {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify({ user_name: val })
            });
            document.getElementById('new-user-name').value = '';
            loadData();
        }

        async function toggleKey(key) {
            await fetch('/api/admin/toggle-key', {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify({ key: key })
            });
            loadData();
        }

        async function deleteKey(key) {
            if (!confirm('Are you sure you want to delete this key?')) return;
            await fetch('/api/admin/delete-key', {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify({ key: key })
            });
            loadData();
        }

        setInterval(loadData, 5000);
        loadData();
    </script>
</body>
</html>
"""
        self.send_response(200)
        self.send_cors_headers()
        self.send_header("Content-Type", "text/html; charset=utf-8")
        body = html.encode("utf-8")
        self.send_header("Content-Length", str(len(body)))
        self.end_headers()
        self.wfile.write(body)

# ===================================================
# Main Entry Point
# ===================================================
if __name__ == "__main__":
    print(f"===================================================")
    print(f"🤖 GravAssist Proxy Server (Pure Python, Auto-Cloudflare)")
    print(f"===================================================")
    print(f"📡 Listening Port   : http://0.0.0.0:{PROXY_PORT}")
    print(f"🖥️ Admin Dashboard  : http://127.0.0.1:{PROXY_PORT}/dashboard")
    print(f"🔗 LM Studio Target : {LM_STUDIO_BACKEND}")
    print(f"🌐 Remote PHP Target: {DEFAULT_PHP_URL}")
    print(f"===================================================")

    start_cf_tunnel_thread()

    server = socketserver.TCPServer(("0.0.0.0", PROXY_PORT), ProxyHandler)
    try:
        server.serve_forever()
    except KeyboardInterrupt:
        print("\nStopping proxy server...")
        stop_cf_tunnel()
        server.server_close()
