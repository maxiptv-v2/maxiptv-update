<?php
function env_log(string $tag, $payload): void {
    if (!is_string($payload)) {
        $payload = json_encode($payload, JSON_UNESCAPED_UNICODE | JSON_UNESCAPED_SLASHES);
    }
    error_log('[' . $tag . '] ' . $payload);
}
?>
