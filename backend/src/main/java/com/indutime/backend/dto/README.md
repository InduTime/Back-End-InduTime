# Pacote `dto`

## Objetivo
Definir os objetos usados para transportar dados entre o cliente da API e o back-end sem expor diretamente entidades JPA.

## Tipos
- DTO de entrada: dados que o cliente pode enviar.
- DTO de saída: dados que a API devolve.

## Responsabilidades
- Definir campos permitidos na entrada e na saída.
- Aplicar Bean Validation em entradas quando apropriado.
- Evitar exposição de dados internos ou sensíveis.

## Cuidados
Nunca inclua senha em DTO de resposta. Não permita que o cliente comum escolha livremente um perfil privilegiado. A Service ainda deve validar regras de negócio que não podem ser garantidas apenas pelo formato dos campos.
