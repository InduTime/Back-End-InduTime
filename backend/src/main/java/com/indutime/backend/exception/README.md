# Pacote `exception`

## Objetivo
Conter exceções específicas para representar erros reconhecidos pela aplicação.

## Exemplos
- Recurso não encontrado.
- Operação incompatível com o estado atual.
- Dados de negócio inválidos.
- Violação de uma regra do domínio.

## Responsabilidades
Expressar claramente o erro e permitir que a Service comunique falhas sem misturar regras de negócio com HTTP.

## Não deve conter
Endpoints, consultas ao banco ou tratamento centralizado de todas as respostas HTTP. Essa última responsabilidade pertence ao `handler`.

Evite ocultar a causa original de uma falha e não inclua segredos nas mensagens.
