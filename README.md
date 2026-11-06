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

As versões e dependências efetivamente utilizadas devem ser consultadas no arquivo `pom.xml`.

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

- **`config`:** configurações compartilhadas da aplicação.
- **`controller`:** recebe requisições HTTP e retorna respostas da API.
- **`database/model`:** entidades JPA que representam os dados persistidos.
- **`database/repository`:** interfaces de acesso e consulta aos dados.
- **`dto`:** objetos utilizados para entrada e saída de dados da API.
- **`exception`:** exceções específicas da aplicação.
- **`handler`:** tratamento centralizado de erros e exceções.
- **`service`:** regras de negócio e coordenação das operações.
- **`utils`:** utilitários genéricos reutilizáveis.

A classe `SpringBootEssentialsApplication` é responsável por iniciar a aplicação Spring Boot. Confira o nome e o pacote da classe inicial no código-fonte, caso tenham sido alterados.

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

O modelo de dados descrito pela equipe contempla cinco tabelas principais:

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

As entidades JPA devem respeitar o modelo de dados aprovado pela equipe, incluindo nomes de tabelas e colunas, restrições e relacionamentos definidos. Em caso de divergência entre o código e o modelo aprovado, alinhe a alteração com a equipe antes de modificar o banco.

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
- PostgreSQL instalado localmente ou acessível em outro ambiente.
- Git.
- Maven Wrapper incluído no projeto (`mvnw` e `mvnw.cmd`). Não é necessário instalar o Maven globalmente quando o wrapper está disponível.

### 1. Clonar o repositório

```bash
git clone <URL_DO_REPOSITORIO>
cd <DIRETORIO_DO_REPOSITORIO>
```

Substitua os marcadores pelos dados reais do repositório. Os comandos abaixo devem ser executados na pasta `backend`, onde está o arquivo `pom.xml`.

### 2. Preparar o banco de dados

Antes de iniciar a aplicação, confirme com a equipe o nome do banco e a forma oficial de preparar o schema. O banco deve existir e estar acessível; a configuração da aplicação não cria automaticamente o banco PostgreSQL.

O nome `InduTime` usado nos exemplos é ilustrativo. Utilize exatamente o nome criado para o seu ambiente.

### 3. Configurar as variáveis de ambiente

A aplicação lê estas variáveis:

| Variável | Finalidade | Exemplo |
|---|---|---|
| `DATABASE_URL` | URL JDBC do PostgreSQL | `jdbc:postgresql://localhost:5432/InduTime` |
| `DATABASE_USERNAME` | Usuário do banco | `<usuario_do_banco>` |
| `DATABASE_PASSWORD` | Senha do banco | Não inserir uma senha real no README |

A propriedade `DATABASE_URL` possui um valor padrão local no `application.properties`. Se o banco estiver em outro servidor ou usar outro nome, configure a URL correta. As variáveis de usuário e senha devem ser definidas no ambiente de execução.

#### Windows — PowerShell

Execute no terminal em que a aplicação será iniciada:

```powershell
$env:DATABASE_URL="jdbc:postgresql://localhost:5432/InduTime"
$env:DATABASE_USERNAME="<usuario_do_banco>"
$env:DATABASE_PASSWORD="<senha_do_banco>"
```

Substitua os marcadores pelos valores fornecidos para o seu ambiente. Essas variáveis permanecem disponíveis nessa sessão do PowerShell.

#### Windows — Prompt de Comando (CMD)

```bat
set "DATABASE_URL=jdbc:postgresql://localhost:5432/InduTime"
set "DATABASE_USERNAME=<usuario_do_banco>"
set "DATABASE_PASSWORD=<senha_do_banco>"
```

Essas variáveis permanecem disponíveis nessa janela do CMD. Não compartilhe capturas de tela ou logs que revelem senhas.

#### Linux ou macOS

```bash
export DATABASE_URL="jdbc:postgresql://localhost:5432/InduTime"
export DATABASE_USERNAME="<usuario_do_banco>"
export DATABASE_PASSWORD="<senha_do_banco>"
```

Não versione credenciais reais. Caso use arquivos locais para guardar configurações, confira se estão protegidos pelo `.gitignore`. O Spring Boot não carrega automaticamente um arquivo `.env` apenas por ele existir.

### 4. Estratégia do schema

A configuração do `application.properties` utiliza:

```properties
spring.jpa.hibernate.ddl-auto=update
```

Com `update`, o Hibernate tenta ajustar o schema existente às entidades JPA, incluindo a criação de tabelas ou colunas ausentes identificadas pelo mapeamento. Isso pode facilitar o desenvolvimento enquanto novas entidades e tabelas são adicionadas.

Essa opção não substitui uma ferramenta de migrações e não garante que toda alteração estrutural seja aplicada como a equipe espera. Por exemplo, renomear ou remover atributos de entidades exige atenção: não se deve contar com `update` para renomear ou remover automaticamente colunas antigas. O banco PostgreSQL também precisa existir antes de a aplicação iniciar; essa propriedade não cria o próprio banco.

Utilize `update` apenas nos ambientes em que a equipe concordou com esse comportamento. Não use `create` ou `create-drop` em um banco com dados que precisam ser preservados. Antes de alterações estruturais importantes ou em ambientes compartilhados, siga o procedimento acordado pela equipe e mantenha backups quando necessário.

### 5. Executar a aplicação

Na pasta `backend`, execute o Maven Wrapper.

Windows:

```powershell
.\mvnw.cmd spring-boot:run
```

Linux ou macOS:

```bash
./mvnw spring-boot:run
```

A inicialização depende de o PostgreSQL estar acessível, de as credenciais serem válidas e de o schema ser compatível com as entidades. Se o banco não estiver disponível ou a validação falhar, confira a mensagem de erro no console.

## Docker

O suporte a Docker está planejado para facilitar a execução da API e do PostgreSQL em um ambiente reproduzível.

A configuração deverá incluir:

- Um `Dockerfile` para construir a imagem da aplicação.
- Um `compose.yaml` para executar a API e o PostgreSQL.
- Configuração de variáveis de ambiente.
- Persistência dos dados do banco por meio de volume.

Até esses arquivos estarem implementados e validados, a execução com Docker deve ser considerada planejada, não um procedimento disponível.
