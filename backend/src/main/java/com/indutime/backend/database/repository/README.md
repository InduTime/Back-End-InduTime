# Pacote `database.repository`

## Objetivo
Conter interfaces responsáveis pelo acesso e pelas consultas aos dados.

## Responsabilidades
- Estender `JpaRepository` ou outra abstração aprovada.
- Declarar consultas derivadas quando forem adequadas.
- Declarar consultas explícitas quando necessário.
- Retornar dados de persistência para a Service.

## Não deve conter
Endpoints, respostas HTTP, autorização ou fluxos completos de negócio.

## Boas práticas
Os nomes de consultas derivadas devem corresponder às propriedades Java da entidade. JPQL usa nomes de entidades e propriedades; SQL nativo usa nomes físicos do banco. Evite métodos sem necessidade identificada.
