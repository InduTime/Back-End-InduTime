# Pacote `database.model`

## Objetivo
Conter entidades JPA que representam os dados persistidos.

## Entidades previstas
- `Usuario` — `usuarios`.
- `Torno` — `tornos`.
- `Operacao` — `operacoes`.
- `PausaOperacao` — `pausas_operacao`.
- `Relatorio` — `relatorios`.

## Responsabilidades
- Mapear entidades, tabelas, chaves, colunas e restrições com JPA.
- Representar os relacionamentos aprovados.
- Usar tipos Java coerentes com os dados persistidos.

## Relacionamentos conhecidos
- `operacoes` referencia `tornos` e pode referenciar `usuarios`.
- `pausas_operacao` referencia `operacoes`.
- `relatorios` referencia `usuarios` e pode referenciar `tornos`.

A nulabilidade e a cardinalidade devem seguir o schema aprovado. Não torne `id_usuario` obrigatório em `operacoes` se o modelo permitir `NULL`.

## Não deve conter
Endpoints, DTOs, consultas personalizadas ou regras completas de negócio. Não altere o schema sem discutir com a equipe.
