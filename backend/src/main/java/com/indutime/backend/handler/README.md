# Pacote `handler`

## Objetivo
Centralizar o tratamento de exceções e padronizar respostas HTTP de erro.

## Responsabilidades
- Tratar exceções de validação e de negócio.
- Converter erros conhecidos em status HTTP apropriados.
- Manter estrutura consistente nas respostas de erro.
- Evitar tratamento repetido em cada Controller.

Em Spring MVC, pode ser implementado com `@RestControllerAdvice` e `@ExceptionHandler`, conforme o padrão do projeto.

Não coloque regras de negócio ou consultas ao banco aqui. Não exponha stack traces, credenciais ou detalhes internos nas respostas públicas.
