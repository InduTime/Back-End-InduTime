# Telemetria e histórico de operações CNC

Os horários deste contrato usam ISO-8601 com offset, por exemplo `2026-11-19T08:20:00-03:00`. O back-end persiste o instante em uma coluna PostgreSQL `TIMESTAMP WITH TIME ZONE` e mantém o offset original em uma coluna separada para devolver a data local correta.

## `POST /v1/operations/telemetry`

Recebe uma leitura operacional. `raspberryMachineId` é o identificador externo configurado na Raspberry; ele não é o ID interno do registro da máquina.

Campos obrigatórios: `eventId` (UUID), `raspberryMachineId` (inteiro positivo), `operationStartedAt`, `running` (boolean), `temperature` (decimal) e `measuredAt`. Os três horários aceitam offset ISO-8601. `actualEndAt` é obrigatório quando `running` é `false` e deve ser nulo enquanto a operação estiver em andamento. `scheduledEndAt` é opcional e nunca substitui o horário real de encerramento.

Exemplo de início e continuidade:

```json
{
  "eventId": "d739e601-33dd-4a50-a012-fc3f114293d0",
  "raspberryMachineId": 1,
  "operationStartedAt": "2026-11-19T08:00:00-03:00",
  "running": true,
  "actualEndAt": null,
  "scheduledEndAt": "2026-11-19T12:00:00-03:00",
  "temperature": 63.45,
  "measuredAt": "2026-11-19T08:20:00-03:00"
}
```

Cada evento precisa de um `eventId` novo. Em novas tentativas, reutilize o mesmo ID e o mesmo conteúdo. O primeiro evento de uma operação retorna `201 Created`; continuidade e repetição idempotente retornam `200 OK`. Máquina desconhecida retorna `404`, payload inválido `400`, e máquina desativada ou estado inconsistente `409`.

Exemplo de encerramento:

```json
{
  "eventId": "c3937c0d-c2fe-4686-a00f-cc1f2cd66e7a",
  "raspberryMachineId": 1,
  "operationStartedAt": "2026-11-19T08:00:00-03:00",
  "running": false,
  "actualEndAt": "2026-11-19T12:03:00-03:00",
  "scheduledEndAt": "2026-11-19T12:00:00-03:00",
  "temperature": 59.12,
  "measuredAt": "2026-11-19T12:03:00-03:00"
}
```

O início original é mantido durante a operação. `actualEndAt` fecha a operação e é usado no cálculo do tempo operacional; `scheduledEndAt` permanece como previsão. `operationDate` nas respostas é derivada da data local de `operationStartedAt`.

## `GET /v1/operations`

Retorna resumos em ordem decrescente de início. Filtros opcionais: `raspberryMachineId`, `startDate`, `endDate` e `status` (`EM_ANDAMENTO`, `CONCLUIDA` ou `PAUSADA`). As datas filtram inclusivamente o horário de início e devem usar timestamps ISO-8601 com offset.

Exemplo:

```text
GET /v1/operations?raspberryMachineId=1&startDate=2026-11-19T00:00:00-03:00&endDate=2026-11-20T00:00:00-03:00&status=CONCLUIDA
```

Exemplo de resumo:

```json
{
  "id": 12,
  "machineId": 4,
  "raspberryMachineId": 1,
  "machineName": "Torno CNC Principal",
  "operationDate": "2026-11-19",
  "operationStartedAt": "2026-11-19T08:00:00-03:00",
  "actualEndAt": "2026-11-19T12:03:00-03:00",
  "scheduledEndAt": "2026-11-19T12:00:00-03:00",
  "status": "CONCLUIDA"
}
```

## `GET /v1/operations/{operationId}`

Retorna o resumo, as informações da máquina e as leituras de temperatura em ordem cronológica. Uma operação inexistente retorna `404`.

```json
{
  "id": 12,
  "machineId": 4,
  "raspberryMachineId": 1,
  "machineName": "Torno CNC Principal",
  "operationDate": "2026-11-19",
  "operationStartedAt": "2026-11-19T08:00:00-03:00",
  "actualEndAt": "2026-11-19T12:03:00-03:00",
  "scheduledEndAt": "2026-11-19T12:00:00-03:00",
  "status": "CONCLUIDA",
  "temperatureReadings": [
    {
      "id": 35,
      "eventId": "d739e601-33dd-4a50-a012-fc3f114293d0",
      "temperature": 63.45,
      "measuredAt": "2026-11-19T08:20:00-03:00",
      "running": true
    }
  ]
}
```

## Exemplo de envio em Python

```python
from datetime import datetime
from uuid import uuid4

import requests

payload = {
    "eventId": str(uuid4()),
    "raspberryMachineId": 1,
    "operationStartedAt": "2026-11-19T08:00:00-03:00",
    "running": True,
    "actualEndAt": None,
    "scheduledEndAt": "2026-11-19T12:00:00-03:00",
    "temperature": 63.45,
    "measuredAt": datetime.now().astimezone().isoformat(timespec="seconds"),
}

response = requests.post(
    "http://localhost:8081/v1/operations/telemetry",
    json=payload,
    timeout=15,
)
response.raise_for_status()
print(response.status_code, response.json())
```

Persistir o payload (incluindo `eventId`) antes da requisição e reutilizá-lo se houver timeout ou erro de conexão.

## Migração PostgreSQL

O projeto não possui uma ferramenta de migração de banco configurada. Antes de implantar a alteração em um banco existente, revise e execute [`20261119_operation_timestamps.sql`](./migrations/20261119_operation_timestamps.sql) uma vez. A migração interpreta timestamps antigos sem fuso como horário local de `America/Sao_Paulo`; confirme que essa era a zona usada ao gravar esses dados. Ela também cria um índice único parcial que impede mais de uma operação aberta por máquina.

O endpoint CSV atual permanece em `POST /v1/cncmachine/telemetry/csv`. As transições CSV agora passam pelo mesmo serviço de operações do endpoint JSON. O formato CSV não possui offset no simulador atual, embora o parser Java já exija um offset ISO-8601; ajuste de relógio/offset nesse cliente é uma pendência independente e o arquivo do simulador não foi alterado.
