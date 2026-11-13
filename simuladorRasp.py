
import csv
import io
import json
import random
import time
from datetime import datetime
from pathlib import Path

import requests


# ============================================================
# CONFIGURAÇÕES
# ============================================================

# Enquanto o Python estiver rodando no mesmo PC do Spring Boot:
BACKEND_BASE_URL = "http://localhost:8080"

# Quando transferir para a Raspberry Pi, utilize:
# BACKEND_BASE_URL = "http://10.110.12.57:8080"

TELEMETRY_ENDPOINT = (
    f"{BACKEND_BASE_URL.rstrip('/')}/v1/cncmachine/telemetry/csv"
)

DATA_COLLECTION_INTERVAL = 20
REQUEST_TIMEOUT = 15

# Arquivos locais
CSV_FILE_NAME = Path("tornos.csv")
PENDING_FILE_NAME = Path("leituras_pendentes.json")

# Temperaturas simuladas
MIN_TEMPERATURE = 20.0
MAX_TEMPERATURE = 80.0

# Cabeçalho EXATO esperado pelo backend Java
CSV_COLUMNS = [
    "id_raspberry",
    "nome_torno",
    "numero_serie",
    "modelo",
    "fabricante",
    "localizacao",
    "data_hora",
    "status",
    "temperatura",
]

STATUS_RUNNING = "LIGADO"
STATUS_STOPPED = "DESLIGADO"


# ============================================================
# CONFIGURAÇÃO DOS DOIS TORNOS
# ============================================================

# Estes IDs são externos.
# NÃO são os IDs internos gerados pelo PostgreSQL.

MACHINES = [
    {
        "id": 1,
        "name": "Torno-01",
        "serial_number": "TORNO-001",
        "model": "HAAS_ST_10",
        "manufacturer": "HAAS",
        "location": "SETOR_USINAGEM",
    },
    {
        "id": 2,
        "name": "Torno-02",
        "serial_number": "MAZAK-TEST-002",
        "model": "MAZAK_QUICK_TURN_200",
        "manufacturer": "MAZAK",
        "location": "SETOR_PRODUCAO",
    },
]


# ============================================================
# VALIDAÇÃO DA CONFIGURAÇÃO
# ============================================================

def validate_machine_configuration():
    """Verifica se os dados dos tornos estão completos e válidos."""

    if not MACHINES:
        raise ValueError("Nenhum torno foi configurado.")

    machine_ids = set()
    serial_numbers = set()

    required_fields = [
        "name",
        "serial_number",
        "model",
        "manufacturer",
        "location",
    ]

    for machine in MACHINES:

        machine_id = machine.get("id")

        if not isinstance(machine_id, int) or isinstance(machine_id, bool):
            raise ValueError("O ID externo de cada torno precisa ser inteiro.")

        if machine_id <= 0:
            raise ValueError("O ID externo precisa ser maior que zero.")

        if machine_id in machine_ids:
            raise ValueError(f"ID externo duplicado: {machine_id}")

        machine_ids.add(machine_id)

        for field in required_fields:
            value = machine.get(field)

            if not isinstance(value, str) or not value.strip():
                raise ValueError(
                    f"Campo obrigatório ausente: {field} no torno {machine_id}"
                )

            # O parser Java atual separa as colunas pela vírgula.
            if "," in value or "\n" in value or "\r" in value:
                raise ValueError(
                    f"O campo {field} do torno {machine_id} "
                    "não pode conter vírgula ou quebra de linha."
                )

        serial = machine["serial_number"]

        if serial in serial_numbers:
            raise ValueError(f"Número de série duplicado: {serial}")

        serial_numbers.add(serial)


# ============================================================
# GERAÇÃO DAS LEITURAS
# ============================================================

def get_current_datetime():
    """Obtém a data e hora local do computador ou da Raspberry."""

    return datetime.now().strftime("%Y-%m-%d %H:%M:%S")


def generate_random_temperature():
    """Gera uma temperatura aleatória para simular o sensor."""

    return round(
        random.uniform(MIN_TEMPERATURE, MAX_TEMPERATURE),
        2
    )


def build_record(machine, date_time, status, temperature=""):
    """Cria um registro com as nove colunas do CSV."""

    return {
        "id_raspberry": machine["id"],
        "nome_torno": machine["name"],
        "numero_serie": machine["serial_number"],
        "modelo": machine["model"],
        "fabricante": machine["manufacturer"],
        "localizacao": machine["location"],
        "data_hora": date_time,
        "status": status,
        "temperatura": temperature,
    }


def build_running_records():
    """Gera uma leitura para cada torno."""

    date_time = get_current_datetime()

    records = []

    for machine in MACHINES:

        temperature = generate_random_temperature()

        record = build_record(
            machine=machine,
            date_time=date_time,
            status=STATUS_RUNNING,
            temperature=f"{temperature:.2f}",
        )

        records.append(record)

    return records


def build_shutdown_records():
    """Registra o encerramento do simulador somente no CSV local."""

    date_time = get_current_datetime()

    return [
        build_record(
            machine=machine,
            date_time=date_time,
            status=STATUS_STOPPED,
        )
        for machine in MACHINES
    ]


# ============================================================
# CSV HISTÓRICO LOCAL
# ============================================================

def create_csv_file_if_missing():
    """
    Cria o CSV histórico.

    Caso exista um CSV antigo com cinco colunas,
    faz um backup e cria um novo com nove colunas.
    """

    if CSV_FILE_NAME.exists() and CSV_FILE_NAME.stat().st_size > 0:

        with CSV_FILE_NAME.open(
            "r",
            newline="",
            encoding="utf-8-sig"
        ) as csv_file:

            reader = csv.reader(csv_file)
            existing_header = next(reader, [])

        if existing_header == CSV_COLUMNS:
            return

        timestamp = datetime.now().strftime("%Y%m%d_%H%M%S")

        backup_name = (
            f"{CSV_FILE_NAME.stem}_backup_{timestamp}"
            f"{CSV_FILE_NAME.suffix}"
        )

        backup_path = CSV_FILE_NAME.with_name(backup_name)

        CSV_FILE_NAME.rename(backup_path)

        print(
            f"(CSV) Arquivo antigo preservado em: {backup_path}"
        )

    with CSV_FILE_NAME.open(
        "w",
        newline="",
        encoding="utf-8"
    ) as csv_file:

        writer = csv.DictWriter(
            csv_file,
            fieldnames=CSV_COLUMNS
        )

        writer.writeheader()

    print(f"(CSV) Histórico criado: {CSV_FILE_NAME}")


def append_records_to_csv(records):
    """Adiciona novas leituras sem apagar o histórico anterior."""

    with CSV_FILE_NAME.open(
        "a",
        newline="",
        encoding="utf-8"
    ) as csv_file:

        writer = csv.DictWriter(
            csv_file,
            fieldnames=CSV_COLUMNS,
            extrasaction="raise",
        )

        writer.writerows(records)


# ============================================================
# FILA DE LEITURAS PENDENTES
# ============================================================

def load_pending_records():
    """
    Recupera leituras que não foram enviadas anteriormente.

    A fila é salva em JSON para não perder os registros
    pendentes quando o programa for encerrado.
    """

    if not PENDING_FILE_NAME.exists():
        return []

    try:
        with PENDING_FILE_NAME.open(
            "r",
            encoding="utf-8"
        ) as file:

            records = json.load(file)

        if not isinstance(records, list):
            raise ValueError("A fila de pendências não é uma lista.")

        for record in records:
            if not isinstance(record, dict):
                raise ValueError("Registro inválido na fila de pendências.")

            if set(record.keys()) != set(CSV_COLUMNS):
                raise ValueError("Formato inválido na fila de pendências.")

        return records

    except (json.JSONDecodeError, ValueError) as error:
        raise RuntimeError(
            "Não foi possível recuperar a fila de leituras pendentes. "
            f"Verifique {PENDING_FILE_NAME}: {error}"
        ) from error


def save_pending_records(records):
    """Salva a fila de pendências de maneira segura."""

    if not records:

        if PENDING_FILE_NAME.exists():
            PENDING_FILE_NAME.unlink()

        return

    temporary_file = PENDING_FILE_NAME.with_suffix(".tmp")

    with temporary_file.open(
        "w",
        encoding="utf-8"
    ) as file:

        json.dump(
            records,
            file,
            ensure_ascii=False,
            indent=4,
        )

    temporary_file.replace(PENDING_FILE_NAME)


# ============================================================
# PREPARAÇÃO DO CSV PARA ENVIO
# ============================================================

def build_csv_content(records):
    """Monta um CSV em memória contendo somente as leituras pendentes."""

    csv_buffer = io.StringIO(newline="")

    writer = csv.DictWriter(
        csv_buffer,
        fieldnames=CSV_COLUMNS,
        lineterminator="\n",
        extrasaction="raise",
    )

    writer.writeheader()
    writer.writerows(records)

    return csv_buffer.getvalue()


# ============================================================
# ENVIO PARA O BACKEND SPRING BOOT
# ============================================================

def send_csv_file(records):
    """Envia as leituras através do endpoint multipart/form-data."""

    if not records:
        return True

    csv_content = build_csv_content(records)
    csv_bytes = csv_content.encode("utf-8")

    try:

        response = requests.post(
            TELEMETRY_ENDPOINT,
            files={
                "file": (
                    "tornos.csv",
                    csv_bytes,
                    "text/csv",
                )
            },
            timeout=REQUEST_TIMEOUT,
        )

        response.raise_for_status()

        print(
            f"(backend) HTTP {response.status_code}: "
            f"{response.text.strip()}"
        )

        return True

    except requests.exceptions.Timeout:

        print(
            "(backend) Tempo limite excedido. "
            "As leituras continuarão pendentes."
        )

    except requests.exceptions.ConnectionError:

        print(
            "(backend) Falha de conexão. "
            "Verifique o endereço do backend, a porta 8080 "
            "e se o Spring Boot está em execução."
        )

    except requests.exceptions.HTTPError as error:

        status_code = (
            error.response.status_code
            if error.response is not None
            else "desconhecido"
        )

        response_text = (
            error.response.text
            if error.response is not None
            else str(error)
        )

        print(
            f"(backend) Erro HTTP {status_code}: "
            f"{response_text}"
        )

    except requests.exceptions.RequestException as error:

        print(f"(backend) Erro durante o envio: {error}")

    return False


# ============================================================
# EXIBIÇÃO NO TERMINAL
# ============================================================

def display_records(records):
    """Exibe as leituras geradas."""

    for record in records:

        message = (
            f"[{record['data_hora']}] "
            f"{record['nome_torno']} "
            f"{record['status'].lower()}"
        )

        if record["temperatura"] != "":
            message += f" - {record['temperatura']} °C"

        print(message)


# ============================================================
# CICLO DE MONITORAMENTO
# ============================================================

def run_monitoring_cycle(pending_records):
    """
    Executa um ciclo:

    1. Gera temperatura para os dois tornos.
    2. Salva o histórico local.
    3. Adiciona as leituras à fila de pendências.
    4. Tenta enviar todas as leituras pendentes.
    5. Limpa a fila somente após HTTP de sucesso.
    """

    records = build_running_records()

    # Guarda as novas leituras na fila antes de tentar enviar.
    pending_records.extend(records)
    save_pending_records(pending_records)

    # Guarda o histórico local.
    append_records_to_csv(records)

    display_records(records)

    if send_csv_file(pending_records):

        pending_records.clear()
        save_pending_records(pending_records)

        print(
            "(backend) Todas as leituras pendentes "
            "foram enviadas com sucesso."
        )

    else:

        print(
            f"(backend) {len(pending_records)} leitura(s) "
            "aguardando nova tentativa."
        )


# ============================================================
# ENCERRAMENTO
# ============================================================

def register_simulator_shutdown():
    """
    Registra o encerramento no histórico local.

    Não envia DESLIGADO ao backend e não altera CNCStatus.
    """

    records = build_shutdown_records()

    append_records_to_csv(records)
    display_records(records)

    print(
        "(simulador) Encerramento registrado no CSV local."
    )


# ============================================================
# EXECUÇÃO PRINCIPAL
# ============================================================

def main():

    validate_machine_configuration()
    create_csv_file_if_missing()

    pending_records = load_pending_records()

    print()
    print("=" * 60)
    print("INDUTIME - SIMULADOR DE TELEMETRIA CNC")
    print("=" * 60)

    print(f"Quantidade de tornos: {len(MACHINES)}")
    print(f"Intervalo de coleta: {DATA_COLLECTION_INTERVAL} segundos")
    print(f"Endpoint: {TELEMETRY_ENDPOINT}")
    print(f"Histórico CSV: {CSV_FILE_NAME.resolve()}")
    print(f"Fila pendente: {len(pending_records)} leitura(s)")

    print("=" * 60)
    print("Pressione CTRL + C para encerrar.")
    print()

    try:

        while True:

            run_monitoring_cycle(pending_records)

            print()
            time.sleep(DATA_COLLECTION_INTERVAL)

    except KeyboardInterrupt:

        print()
        print("(simulador) Encerrando...")

        register_simulator_shutdown()

        print("(simulador) Finalizado.")


if __name__ == "__main__":
    main()
