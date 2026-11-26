# Croix Player Prime (`v6.0.0-prime`)

Reproductor de música Android 12+ (`arm64-v8a` — ARMv8 / ARMv9) construido desde cero **100% en español**, con **algoritmo inteligente estilo Spotify con grafo real de artistas**, **compatibilidad profunda con Spotify (Mixes propios, Daily Mix, Discover Weekly, Daylist, reproducción directa y Me gusta)**, **colas masivas de 150–220+ canciones en TODAS las reproducciones con regeneración infinita automática**, **cambio real de cuentas y perfiles/canales de YouTube Music**, **Estudio de Apariencia Pro** y firma digital permanente.

## Descargar APK Directo (`v6.0.0-prime` — ARM64 Firma Permanente)

- **Descarga directa (`v6.0.0-prime`, sin ZIP)**: [CroixPlayerPrime-arm64-v8a-signed.apk](https://github.com/SmithdelaCroix/CroixPlayerPrime/releases/download/v6.0.0-prime/CroixPlayerPrime-arm64-v8a-signed.apk)
- **Enlace permanente siempre actualizado (`latest-prime`)**: [CroixPlayerPrime-arm64-v8a-signed.apk](https://github.com/SmithdelaCroix/CroixPlayerPrime/releases/download/latest-prime/CroixPlayerPrime-arm64-v8a-signed.apk)
- **Todas las versiones (GitHub Releases)**: https://github.com/SmithdelaCroix/CroixPlayerPrime/releases

## Novedades Principales (`v6.0.0-prime`)
- **Cambio de Cuenta y Perfiles / Canales de YouTube Music Arreglado**:
  - Consulta limpia `accounts_list` + extracción automática de `pageId`, `datasyncIdToken`, `authUser` y `signinUrl` con verificación real de identidad (`verifyYtMusicAccount`).
  - Flujo dedicado para **Añadir cuenta** (`AddSession`), **Cambiar perfil/canal en YouTube Music** (preservando cookies activas para elegir cualquier canal bajo el mismo Gmail + botón **«Listo»**) y cambio en 1 toque entre cuentas guardadas sin sobrescribirse entre sí.
- **Algoritmo Estilo Spotify (Grafo Real de Artistas + Filtro Anti-Ruido)**:
  - Elimina recomendaciones genéricas o irrelevantes filtrando spam (*slowed, reverb, 8D, karaoke, nightcore, covers, compilaciones de 1 hora*).
  - Expande recomendaciones usando el **grafo real de artistas similares** (YouTube Music `relatedArtists` + Spotify Pathfinder `queryArtistOverview` + afinidad reciente/personal) con **separación inteligente de artistas** (*de-clumping*) para no repetir el mismo artista seguido.
- **Compatibilidad Profunda con Spotify**:
  - Motor Pathfinder GraphQL + Web API con autorreparación de hashes, respaldo de sesión HTML y sincronización automática al iniciar.
  - Sincroniza **Mixes reales de tu Spotify** (*Daily Mixes, Descubrimiento Semanal, Radar de Novedades, Daylist*), artistas recientes (*short-term* y *medium-term*), artistas relacionados, más de 100 *Me gusta* paginados y tus playlists (hasta 120 canciones).
  - **Reproducción directa en 1 toque** de cualquier Mix o Playlist de Spotify desde Inicio o Configuración (inicio rápido en ~1.5s + carga progresiva del resto) + botón para importar tus *Me gusta* de Spotify a Favoritos.
- **Colas Masivas (150–220+ Canciones) en TODAS las Colas + Regeneración Infinita**:
  - Cada canción, playlist, mix, búsqueda o favorito que reproduzcas expande la cola progresivamente en 3 oleadas hasta **160–220+ canciones coherentes** en segundo plano, visible en vivo desde el panel de Cola.
  - Cuando te acercas al final de cualquier cola (`≤ 35` canciones restantes), el motor genera e inyecta automáticamente **+115 canciones nuevas coherentes** para que la música nunca se corte.

## Licencia
GPL-3.0
