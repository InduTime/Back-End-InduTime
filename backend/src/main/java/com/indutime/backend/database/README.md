# Pacote `database`

## Objetivo
Agrupar componentes relacionados ao modelo persistido e ao acesso aos dados.

## Subpacotes
- `model`: entidades JPA.
- `repository`: interfaces de persistência e consulta.

As entidades ficam em `model` e as interfaces de acesso aos dados em `repository`. DTOs, regras de negócio e endpoints pertencem aos seus próprios pacotes. Preserve o schema aprovado pela equipe.
