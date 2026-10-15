# Pacote `service`

## Objetivo
Conter regras de negócio e coordenar as operações de cada funcionalidade.

## Responsabilidades
- Validar regras de negócio.
- Chamar repositories para consultar ou persistir dados.
- Coordenar mudanças de estado.
- Garantir consistência entre etapas de um caso de uso.
- Lançar exceções apropriadas quando uma regra não for atendida.

## Exemplos INDUTIME
- Verificar se um torno existe antes de associá-lo a uma operação.
- Impedir duas pausas abertas para a mesma operação.
- Validar transições de status.
- Consultar operações e pausas para calcular indicadores.

Esses exemplos são orientações; regras definitivas devem ser confirmadas pela equipe. Avalie transações quando um caso de uso exigir que várias alterações sejam consistentes. Priorize testes unitários de regras e testes de integração para persistência.
