# Classe `SpringBootEssentialsApplication`

## Objetivo
Classe de entrada da aplicação Spring Boot. Inicializa o contexto do Spring e inicia a aplicação.

## Responsabilidades
- Conter o método `main`.
- Acionar a inicialização do Spring Boot, normalmente com `SpringApplication.run(...)`.
- Permanecer em um pacote raiz que permita ao component scan encontrar os componentes da aplicação.

## Não deve conter
Regras de negócio, endpoints, consultas ao banco ou configurações específicas que deveriam estar em classes próprias.

Mantenha a classe simples. O documento explica a classe existente; não é necessário renomeá-la nem criar uma pasta nova para ela.
