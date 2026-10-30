# Kryn´s Waypoints

Rein clientseitige Fabric-Mod für Wegpunkte (Minecraft 26.3+).

- **B** (änderbar unter Steuerung): Wegpunkt an der aktuellen Position erstellen – Name, Koordinaten, 64 Symbole und 8x8-Farbwahl
- **Escape-Menü → „Optionen“**: Wechselentfernung zur XP-Leiste (50–2000), Markergröße, 50% Transparenz
- **Escape-Menü → „Waypointliste“** (oben rechts): Wegpunkte bearbeiten, ein-/ausblenden, löschen
- Bis zur eingestellten Entfernung (Standard 300): Symbol + Name + Entfernung in der Welt (durch Wände sichtbar), weiter weg: Symbol auf der XP-Leiste in Blickrichtung
- Gespeichert pro Welt/Server und Dimension in `config/kryns_waypoints.json`

## Bauen

```
./gradlew build
```

Die fertige Jar liegt in `build/libs/`. Alternativ baut GitHub Actions bei jedem Push und stellt die Jar als Artefakt bereit.

## Release

Jeder Push auf `main` baut die Mod und legt die Jar im Release `v<mod_version>` ab (Version in `gradle.properties`). Für ein neues Release `mod_version` erhöhen.
