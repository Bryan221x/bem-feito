"""Prepara snapshot oficial e SQL offline; nunca conecta ao banco ou roda no Spring."""
import argparse
import csv
import hashlib
import io
import json
from collections import Counter
from datetime import datetime, timezone
from pathlib import Path
from urllib.request import urlopen

BASE = Path(__file__).resolve().parents[1]
DATA = BASE / "ibge"
SOURCE = "https://servicodados.ibge.gov.br/api/v1/localidades/municipios?orderBy=id"
SNAPSHOT = DATA / "municipalities.csv"
MANIFEST = DATA / "manifest.json"
OUTPUT = BASE / "cities.sql"


def validate(rows):
    if not rows or len({r["ibge_id"] for r in rows}) != len(rows):
        raise ValueError("Snapshot vazio ou códigos IBGE duplicados")
    if len({r["uf"] for r in rows}) != 27:
        raise ValueError("A carga deve abranger as 27 UFs")
    if len({(r["uf"], r["name"]) for r in rows}) != len(rows):
        raise ValueError("Municípios duplicados por UF/nome")
    for row in rows:
        if not (len(row["ibge_id"]) == 7 and row["ibge_id"].isdigit()
                and len(row["uf"]) == 2 and row["uf"].isascii() and row["uf"].isupper()
                and 0 < len(row["name"]) <= 150
                and not any(ord(c) < 32 or c == "\ufffd" for c in row["name"])):
            raise ValueError(f"Município inválido: {row!r}")


def refresh(raw=None):
    if raw is None:
        with urlopen(SOURCE, timeout=120) as response:
            raw = response.read()
    entries = json.loads(raw)
    rows = []
    for entry in entries:
        uf = entry["regiao-imediata"]["regiao-intermediaria"]["UF"]
        if str(entry["id"])[:2] != str(uf["id"]):
            raise ValueError("Código do município incompatível com sua UF")
        rows.append({"ibge_id": str(entry["id"]), "uf": uf["sigla"], "name": entry["nome"]})
    rows.sort(key=lambda r: int(r["ibge_id"]))
    validate(rows)
    buffer = io.StringIO(newline="")
    writer = csv.DictWriter(buffer, fieldnames=["ibge_id", "uf", "name"], lineterminator="\n")
    writer.writeheader()
    writer.writerows(rows)
    content = buffer.getvalue().encode("utf-8")
    manifest = {
        "source": SOURCE,
        "documentation": "https://servicodados.ibge.gov.br/api/docs/localidades",
        "retrieved_at": datetime.now(timezone.utc).isoformat(),
        "count": len(rows),
        "counts_by_uf": dict(sorted(Counter(r["uf"] for r in rows).items())),
        "raw_sha256": hashlib.sha256(raw).hexdigest(),
        "snapshot_sha256": hashlib.sha256(content).hexdigest(),
    }
    DATA.mkdir(parents=True, exist_ok=True)
    SNAPSHOT.write_bytes(content)
    MANIFEST.write_text(json.dumps(manifest, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")


def generate():
    content = SNAPSHOT.read_bytes()
    manifest = json.loads(MANIFEST.read_text(encoding="utf-8"))
    if hashlib.sha256(content).hexdigest() != manifest["snapshot_sha256"]:
        raise ValueError("Checksum do snapshot não confere")
    rows = list(csv.DictReader(io.StringIO(content.decode("utf-8"))))
    validate(rows)
    counts = dict(sorted(Counter(r["uf"] for r in rows).items()))
    if len(rows) != manifest["count"] or counts != manifest["counts_by_uf"]:
        raise ValueError("Contagens do manifesto não conferem")

    # Hex UTF-8 evita dependência de NO_BACKSLASH_ESCAPES e preserva apóstrofos.
    def literal(value):
        return "CONVERT(X'" + value.encode("utf-8").hex() + "' USING utf8mb4)"

    sql = [
        "-- Gerado por database/scripts/prepare_cities.py; não editar manualmente.",
        f"-- IBGE: {manifest['count']} entradas; snapshot SHA-256: {manifest['snapshot_sha256']}",
        "-- Executar após schema.sql e reference-data.sql; NÃO usar mysql --force.",
        "SET NAMES utf8mb4;",
        "SET @ibge_previous_sql_mode = @@SESSION.sql_mode;",
        "SET SESSION sql_mode = CONCAT_WS(',', @@SESSION.sql_mode, 'STRICT_ALL_TABLES');",
        "USE bem_feito;",
        "DROP TEMPORARY TABLE IF EXISTS ibge_cities_stage;",
        "DROP TEMPORARY TABLE IF EXISTS ibge_states_guard;",
        "CREATE TEMPORARY TABLE ibge_cities_stage (uf CHAR(2) NOT NULL, name VARCHAR(150) NOT NULL, PRIMARY KEY (uf, name)) ENGINE=InnoDB;",
        "CREATE TEMPORARY TABLE ibge_states_guard (uf CHAR(2) PRIMARY KEY, state_id BIGINT UNSIGNED NOT NULL) ENGINE=InnoDB;",
        "START TRANSACTION;",
    ]
    for offset in range(0, len(rows), 500):
        values = ["(" + literal(r["uf"]) + ", " + literal(r["name"]) + ")" for r in rows[offset:offset + 500]]
        sql.append("INSERT INTO ibge_cities_stage (uf, name) VALUES\n" + ",\n".join(values) + ";")
    sql += [
        "-- Falha antes de alterar cities se uma UF não existir (state_id NOT NULL).",
        "INSERT INTO ibge_states_guard (uf, state_id) SELECT DISTINCT c.uf, s.id FROM ibge_cities_stage c LEFT JOIN states s ON s.uf = c.uf;",
        "INSERT INTO cities (state_id, name) SELECT g.state_id, c.name FROM ibge_cities_stage c JOIN ibge_states_guard g ON g.uf = c.uf ORDER BY c.uf, c.name ON DUPLICATE KEY UPDATE id = cities.id;",
        "COMMIT;",
        "SELECT COUNT(*) AS snapshot_entries_found FROM cities c JOIN states s ON s.id = c.state_id JOIN ibge_cities_stage i ON i.uf = s.uf AND i.name = c.name;",
        "DROP TEMPORARY TABLE ibge_cities_stage;",
        "DROP TEMPORARY TABLE ibge_states_guard;",
        "SET SESSION sql_mode = @ibge_previous_sql_mode;",
    ]
    OUTPUT.write_text("\n".join(sql) + "\n", encoding="utf-8")
    print(f"Preparadas {len(rows)} entradas em {OUTPUT}")


if __name__ == "__main__":
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--refresh", action="store_true", help="Baixa novamente do IBGE; revisar diff antes de importar")
    args = parser.parse_args()
    if args.refresh:
        refresh()
    generate()
