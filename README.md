# Croix Player Prime (`v4.0.1-prime`)

Reproductor de música Android 12+ (`arm64-v8a` — ARMv8 / ARMv9) construido desde cero **100% en español**, con estética editorial + Glassmorphism translúcido a 120fps, motor multi-cliente sin anuncios, algoritmo híbrido con Spotify, reconocimiento acústico tipo Shazam, Audio Espacial 8D y firma digital permanente para actualizaciones directas sin conflicto de paquetes.

## Descargar APK Directo (`v4.0.1-prime` — ARM64 Firma Permanente)

- **Descarga directa (`v4.0.1-prime`, sin ZIP)**: [CroixPlayerPrime-arm64-v8a-signed.apk](https://github.com/SmithdelaCroix/CroixPlayerPrime/releases/download/v4.0.1-prime/CroixPlayerPrime-arm64-v8a-signed.apk)
- **Enlace permanente siempre actualizado (`latest-prime`)**: [CroixPlayerPrime-arm64-v8a-signed.apk](https://github.com/SmithdelaCroix/CroixPlayerPrime/releases/download/latest-prime/CroixPlayerPrime-arm64-v8a-signed.apk)
- **Todas las versiones (GitHub Releases)**: https://github.com/SmithdelaCroix/CroixPlayerPrime/releases

> **Actualización directa sin pérdida de datos**: Firmado con clave RSA-4096 permanente y migración de base de datos Room `v3 → v4`. Si ya tienes instalada la `v3.0.0-prime`, instala `v4.0.0-prime` encima y conservarás todos tus favoritos, playlists, descargas e historial.

## Novedades (`v4.0.0-prime`)
- **Reconocimiento de música tipo Shazam**: Huella acústica FFT en vivo desde el micrófono (`16 kHz`) con radar animado en **Inicio** y **Buscar** que identifica y reproduce la canción al instante.
- **Letras Multi-fuente + Traducción + Romanización + Tarjeta PNG**: Cascada **LRCLib → Spotify Color-Lyrics → YouTube Music**, traducción al español en vivo, romanización offline (Coreano, Japonés y Ruso), ajuste de desfase (`±0.5s`) y exportación de **Tarjetas de Letras Glassmorphism en PNG**.
- **Caché Inteligente de Audio LRU (`SimpleCache`)**: Guarda canciones escuchadas por `videoId` para reproducción instantánea sin volver a gastar datos (`Off / 256 MB / 512 MB / 1 GB / 2 GB` + vaciado en 1 toque).
- **Audio Espacial 8D real + Reverb + AutoEq + Perfiles por Salida**: Procesador PCM estéreo 8D orbital en Media3, Reverb por hardware, perfiles **AutoEq** para audífonos (*AirPods Pro, Sony WH/WF-1000XM, Galaxy Buds Pro, JBL, Bose QC, KZ / IEM Estudio*) y cambio automático según la salida conectada (*Bluetooth / Cable-USB / Altavoz*).
- **Discord Rich Presence + Scrobbling (Last.fm / ListenBrainz)**: Presencia en vivo por WebSocket Gateway v10 y scrobbling automático (pausados en Sesión Privada).
- **Recibo Musical PNG + Estadísticas por hora y día**: Gráficas por hora del día (`00h–23h`) y día de la semana (`Lun–Dom`) + tarjeta **CROIX RECEIPT** compartible en PNG.
- **Historial por fechas + Bloqueo de Artistas**: Pestaña **Historial** (*Hoy / Ayer / Esta semana / Anteriores*) y bloqueo de artistas desde `⋮` o su perfil para excluirlos de Inicio, Mixes, Flow y Radar.
- **Cola Pro**: Reordenar pistas arriba/abajo, eliminar duplicados y guardar la cola como playlist.
- **Radar de Joyas Ocultas**: Modo descubrimiento puro en Inicio excluyendo el 100% de temas ya escuchados o en favoritos.
- **Inspector Técnico de Audio en vivo**: Toca el códec/bitrate en el reproductor para inspeccionar códec, MIME, bitrate, `loudnessDb`, origen (*Caché / Offline / Directo / Relay*), cliente motor, salida y DSP activo.
- **Canvas animado + Carátula 3D Parallax (giroscopio) + 4 Íconos de App + Sesión Privada (Incógnito)**.

## Licencia
GPL-3.0
