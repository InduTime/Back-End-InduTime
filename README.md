# INDUTIME — Back-end

API REST para o sistema INDUTIME, voltado ao monitoramento de operações CNC e ao gerenciamento de informações operacionais das máquinas.

O projeto utiliza Java e Spring Boot, com PostgreSQL para persistência dos dados e uma arquitetura em camadas para facilitar a organização, a manutenção e a evolução do sistema.

## Sobre o projeto

A INDUTIME tem como objetivo centralizar informações relacionadas aos tornos CNC, às operações realizadas e aos períodos de pausa, permitindo acompanhar o histórico operacional e consultar indicadores relevantes.

O sistema também prevê integração com uma Raspberry Pi responsável pela coleta e pelo envio de dados relacionados às máquinas. Os detalhes do contrato de comunicação serão definidos conforme a integração técnica disponível.

## Objetivos

- Desenvolver uma API REST organizada e de fácil manutenção.
- Gerenciar usuários e seus perfis de acesso.
- Cadastrar e consultar tornos CNC.
- Registrar e consultar operações.
- Controlar pausas associadas às operações.
- Consultar o histórico operacional por período.
- Calcular indicadores de utilização das máquinas.
- Gerar relatórios operacionais em PDF.
- Aplicar validações, tratamento de erros e testes automatizados.

## Tecnologias

| Tecnologia | Finalidade |
|---|---|
| Java | Linguagem principal |
| Spring Boot | Estrutura da aplicação |
| Spring Web | Desenvolvimento da API REST |
| Spring Data JPA | Persistência e consultas |
| Hibernate / JPA | Mapeamento objeto-relacional |
| PostgreSQL | Banco de dados relacional |
| Lombok | Redução de código repetitivo |
| SpringDoc OpenAPI | Documentação da API, quando configurada |
| Spring Security | Autenticação e autorização, conforme a implementação |

As versões e dependências efetivamente utilizadas devem ser consultadas no arquivo de configuração do projeto.

## Arquitetura

O back-end está organizado em camadas, separando a comunicação HTTP, as regras de negócio, os objetos de transferência de dados e o acesso à persistência.

```text
src/main/java/.../
├── config/
├── controller/
├── database/
│   ├── model/
│   └── repository/
├── dto/
├── exception/
├── handler/
├── service/
├── utils/
└── SpringBootEssentialsApplication.java
```

### Responsabilidades dos pacotes

- **config:** configurações compartilhadas da aplicação.
- **controller:** recebe requisições HTTP e retorna respostas da API.
- **database/model:** entidades JPA que representam os dados persistidos.
- **database/repository:** interfaces de acesso e consulta aos dados.
- **dto:** objetos utilizados para entrada e saída de dados da API.
- **exception:** exceções específicas da aplicação.
- **handler:** tratamento centralizado de erros e exceções.
- **service:** regras de negócio e coordenação das operações.
- **utils:** utilitários genéricos reutilizáveis.

A classe `SpringBootEssentialsApplication` é responsável por iniciar a aplicação Spring Boot.

### Fluxo de uma requisição

```text
Cliente
   |
   v
Controller
   |
   v
Service
   |
   v
Repository
   |
   v
PostgreSQL
```

O resultado retorna pelas camadas até o Controller, que produz a resposta HTTP. Os DTOs definem os dados expostos pela API, enquanto as entidades representam o modelo persistido.

## Modelo de dados

O banco de dados da INDUTIME possui cinco tabelas principais:

| Tabela | Responsabilidade |
|---|---|
| `usuarios` | Dados dos usuários e seus perfis. |
| `tornos` | Informações cadastrais das máquinas CNC. |
| `operacoes` | Histórico de trabalho dos tornos. |
| `pausas_operacao` | Registro das pausas associadas às operações. |
| `relatorios` | Registro dos parâmetros dos relatórios solicitados. |

### Relacionamentos principais

- Uma operação está associada a um torno.
- Uma operação pode estar associada a um usuário.
- Uma operação pode possuir várias pausas.
- Um relatório está associado a um usuário e pode referenciar um torno.

As entidades JPA devem respeitar o modelo de dados aprovado pela equipe, incluindo os nomes das colunas, as restrições e os relacionamentos definidos.

## Planejamento do desenvolvimento

O trabalho está organizado em cinco milestones no GitHub Project:

| Milestone | Escopo |
|---|---|
| M0 — Estrutura e banco de dados | Configuração do ambiente, conexão com PostgreSQL e documentação. |
| M1 — Usuários e segurança | Gerenciamento de usuários, autenticação e autorização. |
| M2 — Tornos e operações | Cadastro de máquinas e registro de operações. |
| M3 — Pausas e histórico operacional | Registro, consulta e encerramento de pausas. |
| M4 — Relatórios | Consultas por período, indicadores e exportação para PDF. |

Consulte as Issues do repositório para acompanhar o estado real de cada funcionalidade.

## Como executar o projeto

### Pré-requisitos

- JDK compatível com a versão utilizada pelo projeto.
- PostgreSQL instalado ou acessível.
- Git.
- Maven ou Gradle, conforme o sistema de build configurado no repositório.

### 1. Clonar o repositório

```bash
git clone <URL_DO_REPOSITORIO>
cd <DIRETORIO_DO_PROJETO>
```

Substitua os valores pelos dados reais do repositório.

### 2. Configurar o banco de dados

Configure o acesso ao PostgreSQL utilizando as propriedades adotadas pelo projeto ou variáveis de ambiente.

Exemplo ilustrativo:

```properties
spring.datasource.url=${DB_URL}
spring.datasource.username=${DB_USERNAME}
spring.datasource.password=${DB_PASSWORD}
```

Os nomes das variáveis devem corresponder à configuração efetiva da aplicação.

**Importante:** não armazene senhas ou credenciais reais no repositório. A estratégia de criação e atualização do schema deve preservar os dados existentes e evitar operações destrutivas acidentais.

### 3. Executar a aplicação

Se o projeto utilizar Maven Wrapper, os comandos normalmente serão:

Linux ou macOS:

```bash
./mvnw spring-boot:run
```

Windows:

```powershell
.\mvnw.cmd spring-boot:run
```

Se o projeto utilizar Gradle, consulte os comandos correspondentes à configuração existente.

## Docker

O suporte a Docker está planejado para facilitar a execução da API e do PostgreSQL em um ambiente reproduzível.

A configuração deverá incluir:

- Um `Dockerfile` para construir a imagem da aplicação.
- Um `compose.yaml` para executar a API e o PostgreSQL.
- Configuração de variáveis de ambiente.
- Persistência dos dados do banco por meio de volume.
