# Croix Player Prime

Reproductor de música Android 12+ (`arm64-v8a` — ARMv8 / ARMv9) construido desde cero con estética Glassmorphism translúcida a 120fps, motor de reproducción YouTube Music estilo Metrolist sin anuncios, SponsorBlock, letras sincronizadas LRCLib y sincronización/algoritmo híbrido con Spotify.

## Descargar APK Directo (`v2.6.0-prime` — ARM64 Firmado)

- **Descarga directa (Último APK firmado, sin ZIP)**: [CroixPlayerPrime-arm64-v8a-signed.apk](https://github.com/SmithdelaCroix/CroixPlayerPrime/releases/download/latest-prime/CroixPlayerPrime-arm64-v8a-signed.apk)
- **Todas las versiones (GitHub Releases)**: https://github.com/SmithdelaCroix/CroixPlayerPrime/releases

## Novedades (`v2.6.0-prime`)
- **Motor de Reproducción YouTube Music estilo Metrolist**: Cadena completa de clientes 2026 (`VISIONOS`, `ANDROID_VR`, `IOS`, `IOS_MUSIC`, `IPADOS`, `ANDROID_MUSIC`, `MOBILE`, `ANDROID_CREATOR`, `TVHTML5`, `WEB_REMIX`), `visitorData`, deobfuscación `?n=` / `signatureCipher`, validación HTTP `Range: bytes=0-1` antes de reproducir, headers dinámicos por cliente en ExoPlayer y recuperación automática en caso de error.
- **Sincronización y Algoritmo de Spotify estilo Yuma**: Login WebView con soporte de ventanas emergentes OAuth (Google / Apple / Facebook) + pegado directo de `sp_dc`, auto-descubrimiento en vivo de hashes GraphQL de Spotify, mezcla configurable `0%–100%` en Inicio y Flow, e importación directa de playlists de Spotify.
- **Servidores YouTube Music + Ad-Block estricto**: Conexión Directa Sin Anuncios, Relay Piped o Servidor Personalizado, ping de latencia en vivo y contador de anuncios/patrocinios bloqueados.

## Licencia
GPL-3.0
