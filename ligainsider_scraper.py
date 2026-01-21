import requests
from bs4 import BeautifulSoup
import json
import os
import sys
import concurrent.futures
import re

# Einstiegs-URL: Die Übersicht des aktuellen Spieltags
OVERVIEW_URL = "https://www.ligainsider.de/bundesliga/spieltage/"
OUTPUT_FILE = "ligainsider_lineups.json"

def clean_text(text):
    if not text: return ""
    return text.strip().replace('\n', ' ').replace('\t', '')

def get_headers():
    return {
        'User-Agent': 'Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/91.0.4472.114 Safari/537.36'
    }

def fetch_team_lineup(team_url):
    """
    Besucht die Team-Detailseite eines Spiels und extrahiert die voraussichtliche Aufstellung.
    """
    print(f"Lade Aufstellung von: {team_url}")
    try:
        response = requests.get(team_url, headers=get_headers(), timeout=10)
        response.raise_for_status()
        soup = BeautifulSoup(response.content, 'html.parser')
        
        players = []
        
        # Suche nach dem Bereich "Voraussichtliche Aufstellung"
        # Die Struktur ist oft: H2 Header -> DIV Container -> Spieler Links
        # Wir suchen nach Links, die auf ein Spielerprofil verweisen und im Content-Bereich liegen.
        
        # Strategie: Suche nach dem Header und nimm die nachfolgenden Spielernamen
        headers = soup.find_all(string=re.compile("VORAUSSICHTLICHE AUFSTELLUNG"))
        
        for header in headers:
            # Wir suchen den Container, der diesen Header enthält
            container = header.find_parent('div') or header.find_parent('section')
            if not container: continue
            
            # Manchmal ist der Container höher im Baum
            lineup_box = container.find_parent('div', class_='content_box') or container.find_parent('div')
            
            if lineup_box:
                # Sammle alle Links zu Spielern in diesem Bereich
                # Ligainsider Spieler Links haben format: /vorname-nachname_id/
                player_links = lineup_box.find_all('a', href=re.compile(r'/[a-zA-Z0-9-]+_\d+/'))
                
                # Filter: Wir wollen nur die ersten 11 eindeutigen Spieler
                # Oft werden Ersatzspieler oder verletzte auch gelistet, aber die S11 steht meist als Block oben oder grafisch.
                # Ein starkes Indiz ist oft, dass S11 Spieler fett gedruckt sind oder in einer Aufstellungsgrafik stehen.
                # Vereinfacht nehmen wir die ersten 11 gefundenen Namen im relevanten Bereich.
                
                start_eleven_candidates = []
                seen = set()
                
                for link in player_links:
                    name = clean_text(link.text)
                    if not name or name in seen: continue
                    
                    # Ignoriere Links, die "News" oder ähnliches im Text haben, falls falsch gematcht
                    if len(name) < 3: continue 
                    
                    seen.add(name)
                    start_eleven_candidates.append(name)
                    
                    if len(start_eleven_candidates) >= 11:
                        break
                
                if start_eleven_candidates:
                    players = start_eleven_candidates
                    break # Gefunden
        
        return players

    except Exception as e:
        print(f"Fehler bei {team_url}: {e}")
        return []

def fetch_lineups():
    print(f"Starte Abruf des Spieltags von {OVERVIEW_URL}...")
    
    try:
        response = requests.get(OVERVIEW_URL, headers=get_headers())
        response.raise_for_status()
        soup = BeautifulSoup(response.content, 'html.parser')
        
        matches = []
        
        # Finde alle Spielpaarungen auf der Übersichtsseite.
        # Strategie: Suche nach Links, die '/bundesliga/team/' und '/saison-' enthalten.
        # Diese treten paarweise auf (Heim, Gast).
        
        team_links = soup.find_all('a', href=re.compile(r'/bundesliga/team/.*/saison-'))
        
        # Wir gruppieren die Links immer in 2er Paaren (Heim vs Gast)
        # Dies ist eine Heuristik, die davon ausgeht, dass die Links in der Reihenfolge Heim, Gast im HTML stehen.
        
        match_pairs = []
        current_pair = []
        
        for link in team_links:
            url = link['href']
            if not url.startswith('http'):
                url = "https://www.ligainsider.de" + url
            
            name = clean_text(link.text)
            # Bereinige Namen (manchmal steht "FC Bayern München FC Bayern München" drin wegen hidden text)
            if len(name) > 40 and name[:len(name)//2].strip() == name[len(name)//2:].strip():
                 name = name[:len(name)//2].strip()

            current_pair.append({'name': name, 'url': url})
            
            if len(current_pair) == 2:
                match_pairs.append(current_pair)
                current_pair = []

        print(f"Gefundene Spiele: {len(match_pairs)}")

        # Parallelisierung: Wir laden alle Team-Seiten gleichzeitig
        with concurrent.futures.ThreadPoolExecutor(max_workers=10) as executor:
            # Erstelle Liste von Aufgaben
            future_to_team = {}
            for pair in match_pairs:
                home = pair[0]
                away = pair[1]
                
                # Starte Task für Heim
                future_home = executor.submit(fetch_team_lineup, home['url'])
                future_to_team[future_home] = (home, 'home')
                
                # Starte Task für Gast
                future_away = executor.submit(fetch_team_lineup, away['url'])
                future_to_team[future_away] = (away, 'away')
            
            # Ergebnisse sammeln
            results = {} # Key: URL, Value: Lineup List
            for future in concurrent.futures.as_completed(future_to_team):
                team_info, side = future_to_team[future]
                try:
                    lineup = future.result()
                    results[team_info['url']] = lineup
                except Exception as exc:
                    print(f"Task generated an exception: {exc}")
                    results[team_info['url']] = []

        # Matches zusammenbauen
        for pair in match_pairs:
            home = pair[0]
            away = pair[1]
            
            home_lineup = results.get(home['url'], [])
            away_lineup = results.get(away['url'], [])
            
            # Fallback falls leer (optional: leere Liste lassen)
            if not home_lineup: home_lineup = ["Keine Daten"]
            if not away_lineup: away_lineup = ["Keine Daten"]

            matches.append({
                "homeTeam": home['name'],
                "awayTeam": away['name'],
                "homeLineup": home_lineup,
                "awayLineup": away_lineup,
                "url": home['url'] # Link zur Heimseite als Referenz
            })

        # JSON speichern
        script_dir = os.path.dirname(os.path.abspath(__file__))
        file_path = os.path.join(script_dir, OUTPUT_FILE)
        
        with open(file_path, 'w', encoding='utf-8') as f:
            json.dump(matches, f, ensure_ascii=False, indent=4)
            
        print(f"Erfolgreich {len(matches)} Spiele gespeichert in {file_path}.")
        return True

    except Exception as e:
        print(f"Haupt-Fehler: {e}")
        return False

if __name__ == "__main__":
    fetch_lineups()
