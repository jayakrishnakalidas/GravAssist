<?xml version="1.0" encoding="utf-8"?>
<?php
// ===================================================
// Configuration
// ===================================================
$data_file = __DIR__ . '/cloudflared_url.txt';
$secret_key = '6660_0D_F'; // Passcode authorization key

// Enable CORS so Android app can read link without origin restrictions
header("Access-Control-Allow-Origin: *");
header("Access-Control-Allow-Methods: GET, POST, OPTIONS");
header("Access-Control-Allow-Headers: Content-Type");

// Preflight OPTIONS handler
if ($_SERVER['REQUEST_METHOD'] === 'OPTIONS') {
    http_response_code(200);
    exit();
}

// ===================================================
// UPDATE LINK METHOD (Triggered by Python proxy_controller.py)
// ===================================================
if (isset($_REQUEST['url']) && isset($_REQUEST['key'])) {
    $provided_key = trim($_REQUEST['key']);
    $new_url = trim($_REQUEST['url']);

    if ($provided_key !== $secret_key) {
        http_response_code(403);
        echo "ERROR: Invalid Authorization Key";
        exit();
    }

    if (empty($new_url)) {
        http_response_code(400);
        echo "ERROR: URL cannot be empty";
        exit();
    }

    // Ensure URL has protocol
    if (!preg_match("~^(?:f|ht)tps?://~i", $new_url)) {
        $new_url = "https://" . $new_url;
    }

    // Save URL to text file
    file_put_contents($data_file, $new_url);
    echo "SUCCESS: Link updated to " . $new_url;
    exit();
}

// ===================================================
// GET LINK METHOD (Triggered by GravAssist Android App)
// ===================================================
if ($_SERVER['REQUEST_METHOD'] === 'GET') {
    if (file_exists($data_file)) {
        $url = trim(file_get_contents($data_file));
        if (!empty($url)) {
            header("Content-Type: text/plain");
            echo $url;
            exit();
        }
    }
    
    header("Content-Type: text/plain");
    echo "https://offline.cloudflared.local";
    exit();
}
?>
