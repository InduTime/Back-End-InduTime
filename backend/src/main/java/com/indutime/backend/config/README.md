# Pacote `config`

## Objetivo
Manter configurações compartilhadas da aplicação e classes que declaram beans do Spring quando necessário.

## Pode conter
- Classes `@Configuration`.
- Beans compartilhados que não sejam configurados automaticamente.
- Configurações técnicas aprovadas pela equipe.

## Não deve conter
- Regras de negócio.
- Endpoints HTTP.
- Consultas ao banco.
- Senhas, tokens ou credenciais escritos diretamente no código.

## Boas práticas
Verifique se o Spring Boot já configura o recurso antes de criar uma classe. Mantenha segredos em variáveis de ambiente ou configuração externa e documente configurações não óbvias.
