# Servidor HTTP Manual com CRUD e Autenticação (ServerV2)

Este projeto foi desenvolvido como Trabalho Final para a disciplina de Sistemas Distribuídos I (Ciência da Computação - IFSul Campus Passo Fundo). Nele, foi feita a implementação de um servidor HTTP básico em Java, utilizando `Sockets` para comunicação de baixo nível. O servidor é capaz de processar requisições HTTP (GET, POST, PUT, DELETE), gerenciar um armazenamento em memória (CRUD) de arquivos e implementar um fluxo de **Autenticação por Token de Sessão** (Login).

## Estrutura do Projeto

A estrutura de arquivos segue a organização padrão de projetos Java e inclui os arquivos essenciais para o servidor e o cliente.

```
├── client/
│   └── Client.java
├── lib/
│   └── gson-2.10.1.jar (ou versão equivalente)
├── server/
│   ├── ServerV2.java
│   ├── Arquivo.java
│   ├── Usuario.java
│   └── HttpParser.java
└── tests/
    └── requests.http (Para teste via clientes HTTP como VS Code REST Client)
```

## Tecnologias e Dependências

* **Linguagem:** Java (JDK 8 ou superior)
* **Servidor:** `java.net.ServerSocket` e `java.net.Socket` (Implementação manual)
* **JSON:** **Gson** (Biblioteca do Google para serialização/desserialização JSON).
    * **Necessário:** O arquivo `gson-xxx.jar` deve estar no classpath para compilar e executar o `ServerV2`.

---

## Inicialização do Servidor

1.  **Dependência:** Certifique-se de que a biblioteca **Gson** (`.jar`) esteja configurada no classpath do seu projeto.
2.  **Compilação:** Compile todos os arquivos `.java` nas pastas `server` e `client`.
3.  **Execução:** Execute o `main` da classe **`ServerV2.java`**.
    ```
    # Exemplo (ajuste conforme o seu ambiente de execução)
    java -cp ".;lib/gson-2.10.1.jar" server.ServerV2
    ```
4.  O servidor iniciará na porta **`8080`**.

---

## Endpoints Implementados (API REST)

O servidor gerencia a coleção de arquivos e requer autenticação para operações de escrita (`POST`, `PUT`, `DELETE`).

### 1. Autenticação (Login)

Esta é a primeira rota a ser chamada para obter o token de acesso.

| Método | Caminho | Descrição |
| :--- | :--- | :--- |
| **`POST`** | `/login` | Autentica o usuário e gera um token de sessão. |

| Credenciais Válidas (Configuradas em `ServerV2.java`) |
| :--- |
| **Username:** `admin` / **Password:** `12345` |
| **Username:** `user1` / **Password:** `senha1` |

**Resposta Sucesso (200 OK):**

```
{
  "status": "Login bem-sucedido",
  "token": "4497192b-f08d-480a-9240-5e0f4f7a46a0" // Token dinâmico (UUID)
}
```

* **Importante:** O token gerado deve ser enviado no cabeçalho Authorization para as rotas protegidas.

### 2. Gerenciamento de Arquivos (CRUD)

| Método | Caminho | Descrição | Requer Token? |
| :--- | :--- | :--- | :--- |
| **`GET`** | `/arquivos/{nome}` | Busca e retorna o conteúdo de um arquivo. | Não |
| **`POST`** | `/arquivos` | Cria um novo arquivo no armazenamento. | **Sim** |
| **`PUT`** | `/arquivos/{nome}` | Atualiza o conteúdo e o tipo de um arquivo existente. | **Sim** |
| **`DELETE`**| `/arquivos/{nome}` | Remove um arquivo do armazenamento. | **Sim** |

---

## Protocolo de Autorização

Todas as rotas protegidas (`POST`, `PUT`, `DELETE`) verificam o cabeçalho `Authorization`:

* **Se Válido:** A requisição é processada.
* **Se Ausente ou Inválido:** Retorna **`401 Unauthorized`**.

## Exemplos de Teste (Usando o Token)

Para testar as rotas protegidas, substitua `[SEU_TOKEN]` pelo valor obtido no login. O arquivo `tests/requests.http` contém exemplos de requisições.

### Exemplo 1: POST (Criação)

```
POST http://localhost:8080/arquivos
Authorization: [SEU_TOKEN]
Content-Type: application/json

{
  "nome": "relatorio.json",
  "conteudo": "{\"data\": \"2025-01-01\"}",
  "tipo": "application/json"
}
```

### 2. Gerenciamento de Arquivos (CRUD)

| Método | Caminho | Descrição | Requer Token? |
| :--- | :--- | :--- | :--- |
| **`GET`** | `/arquivos/{nome}` | Busca e retorna o conteúdo de um arquivo. | Não |
| **`POST`** | `/arquivos` | Cria um novo arquivo no armazenamento. | **Sim** |
| **`PUT`** | `/arquivos/{nome}` | Atualiza o conteúdo e o tipo de um arquivo existente. | **Sim** |
| **`DELETE`**| `/arquivos/{nome}` | Remove um arquivo do armazenamento. | **Sim** |

---

## Protocolo de Autorização

Todas as rotas protegidas (`POST`, `PUT`, `DELETE`) verificam o cabeçalho `Authorization`:

* **Se Válido:** A requisição é processada.
* **Se Ausente ou Inválido:** Retorna **`401 Unauthorized`**.

## Exemplos de Teste (Usando o Token)

Para testar as rotas protegidas, substitua `[SEU_TOKEN]` pelo valor obtido no login. O arquivo `tests/requests.http` contém exemplos de requisições.

### Exemplo 1: POST (Criação)

```
POST http://localhost:8080/arquivos
Authorization: [SEU_TOKEN]
Content-Type: application/json

{
  "nome": "relatorio.json",
  "conteudo": "{\"data\": \"2025-01-01\"}",
  "tipo": "application/json"
}
```

### Exemplo 2: GET (Leitura)

`GET http://localhost:8080/arquivos/relatorio.json`

### Exemplo 3: DELETE (Exclusão)


```
DELETE http://localhost:8080/arquivos/relatorio.json
Authorization: [SEU_TOKEN]
```

### Exemplo 4: PUT (Atualização)

```
PUT http://localhost:8080/arquivos/relatorio.json
Authorization: [SEU_TOKEN]
Content-Type: application/json

{
  "conteudo": "Novo texto simples de atualização",
  "tipo": "text/plain"
}
```
