# Pacote `utils`

## Objetivo
Guardar utilitários genéricos reutilizados por diferentes partes da aplicação e que não pertençam claramente a um módulo específico.

## Pode conter
- Funções auxiliares pequenas e genéricas.
- Conversões técnicas reutilizáveis.
- Constantes realmente compartilhadas.

## Não deve conter
- Regras de negócio.
- Acesso ao banco.
- Coordenação de casos de uso.
- Classes colocadas aqui apenas por falta de definição de responsabilidade.

Prefira manter a lógica no pacote funcional correspondente quando ela pertencer claramente a uma funcionalidade. Evite criar utilitários sem necessidade concreta.
