/**
 * aether_strings.h — Compile-time XOR String Obfuscation
 *
 * String sensitif dienkripsi saat compile time sebagai constexpr byte array.
 * Dekripsi terjadi di runtime di native layer — string plaintext tidak pernah
 * muncul sebagai literal dalam binary.
 *
 * Cara pakai:
 *   // Di .cpp — definisikan encrypted bytes + panggil aether_decrypt()
 *   static const uint8_t kAdId[] = { 0x2D, 0x2F, ... };
 *   std::string ad_id = aether_decrypt(kAdId, sizeof(kAdId), AETHER_KEY);
 *
 * Untuk menambah string baru:
 *   1. Jalankan skrip python encrypt_string.py "string_lo"
 *   2. Copy output bytes ke sini
 *   3. Tambah JNI getter jika perlu diakses dari Kotlin
 */

#pragma once
#include <cstdint>
#include <string>

// ── XOR Key ──────────────────────────────────────────────────────────────────
// Ganti nilai ini untuk key yang berbeda per-build.
// JANGAN commit key yang sama di repo publik.
#define AETHER_KEY  0x4E

// ── Decrypt helper ───────────────────────────────────────────────────────────
/**
 * Dekripsi byte array XOR ke std::string.
 * @param data   Encrypted byte array
 * @param len    Panjang array (gunakan sizeof(array))
 * @param key    XOR key yang sama saat enkripsi
 */
static inline std::string aether_decrypt(const uint8_t* data, size_t len, uint8_t key) {
    std::string result;
    result.reserve(len);
    for (size_t i = 0; i < len; i++) {
        result += static_cast<char>(data[i] ^ key);
    }
    return result;
}

// ── Encrypted Strings ─────────────────────────────────────────────────────────
// AdMob Banner ID Production: "ca-app-pub-5043818314955328/4052266582"
static const uint8_t kAdMobBannerId[] = {
    0x2D, 0x2F, 0x63, 0x2F, 0x3E, 0x3E, 0x63, 0x3E,
    0x3B, 0x2C, 0x63, 0x7B, 0x7E, 0x7A, 0x7D, 0x76,
    0x7F, 0x76, 0x7D, 0x7F, 0x7A, 0x77, 0x7B, 0x7B,
    0x7D, 0x7C, 0x76, 0x61, 0x7A, 0x7E, 0x7B, 0x7C,
    0x7C, 0x78, 0x78, 0x7B, 0x76, 0x7C
};
