# Pacote `controller`

## Objetivo
Receber requisições HTTP, encaminhar o trabalho para a Service e devolver respostas com status HTTP apropriados.

## Responsabilidades
- Definir endpoints com `@RestController` e mappings HTTP.
- Receber parâmetros, query parameters e corpos de requisição.
- Receber DTOs de entrada e aplicar `@Valid` quando aplicável.
- Chamar a Service responsável.
- Retornar DTOs de saída e status HTTP coerentes.

## Não deve conter
- Regras de negócio complexas.
- Acesso direto ao banco por repositories.
- Lógica repetida de tratamento de erros que pertença ao handler.

## Fluxo
`HTTP request → Controller → Service → Repository (se necessário) → Service → Controller → HTTP response`

Não confie em dados enviados pelo cliente para conceder privilégios. Evite expor entidades JPA diretamente quando houver DTOs definidos.
