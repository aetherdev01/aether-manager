# Changelog

## v1.2.2 (2026-04-11)
- Exec Tweak now Use aether-utility
- Fixes Long Loading System Information & Lag after Apply Performance Profile
- Now Exec aether-utility is smarter, directly detects SOC according to your device

## v1.2.1
- Fix PID Stopped: service.sh tulis PID file, get-info cek PID file + fallback scan /proc
- Fix tweak tidak ter-apply: saveTweak apply tweak realtime via apply-tweak command
- Fix Battery profile lag: ganti matiin core ke cap frekuensi 60% via scaling_max_freq
- Tambah command apply-tweak untuk apply satu tweak secara individual
