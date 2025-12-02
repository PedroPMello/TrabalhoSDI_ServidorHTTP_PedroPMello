package server;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;

public class ServerV2 {

    private ServerSocket serverSocket;
    int porta;
    Map<String, Arquivo> arquivos;

    // Mapa para armazenar usuários válidos (Chave: Username, Valor: Password)
    private final Map<String, String> usuariosValidos = new HashMap<>();
    // Mapa para armazenar tokens de sessão (Chave: Token, Valor: Username/ID)
    private Map<String, String> tokensValidos = new HashMap<>();

    private final Gson gson = new Gson();

    public ServerV2() {
        this.porta = 8080;
        arquivos = new HashMap<>();
        usuariosValidos.put("admin", "12345");
        usuariosValidos.put("user1", "senha1");
        
        // Inicialização de um arquivo de teste para GET
        String htmlContent = "<html><body><h1>Servidor Online!</h1><p>Teste de GET Básico Funcional.</p></body></html>";
        Arquivo arquivoPadrao = new Arquivo("index.html", htmlContent, "text/html");
        arquivos.put(arquivoPadrao.getNome(), arquivoPadrao);
    }

    public void connectionLoop() throws IOException {
        try {
            while (true) {
                System.out.println("Esperando conexao...");
                Socket socket = this.esperaConexao(); // Método bloqueante
                System.out.println("Conexao recebida, inciando protocolo...");
                // Tratamento em thread separada para clientes concorrentes
                new Thread(() -> clientHandle(socket)).start();
            }
        } catch (Exception e) {
            System.out.println("ERRO NO LOOP DE CONEXÃO: " + e.getMessage());
        }
    }

    private void clientHandle(Socket socket) {
        BufferedReader is = null;
        OutputStream os = null;
        HttpParser parser = new HttpParser();

        try {
            is = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            os = socket.getOutputStream();
            String linha;
            StringBuilder requestData = new StringBuilder();

            // Ler a linha da requisição (método, path, versão)
            linha = is.readLine();
            if (linha == null || linha.trim().isEmpty()) {
                System.out.println("Recebido: null. Conexão fechada pelo cliente ou requisição vazia.");
                return;
            }

            // Parsear a request line
            String[] tokens = linha.split(" ");
            if (tokens.length >= 3) {
                parser.setMethod(tokens[0]);
                parser.setPath(tokens[1]);
                parser.setHttpVersion(tokens[2]);
            }
            requestData.append(linha).append("\n");

            // Ler cabeçalhos
            int contentLength = 0;
            while ((linha = is.readLine()) != null && !linha.trim().isEmpty()) {
                requestData.append(linha).append("\n");
                String[] headerTokens = linha.split(":", 2);
                if (headerTokens.length == 2) {
                    String key = headerTokens[0].trim().toLowerCase();
                    String value = headerTokens[1].trim();
                    parser.setHeader(key, value);
                    if (key.equals("content-length")) {
                        try {
                            contentLength = Integer.parseInt(value);
                        } catch (NumberFormatException e) {
                            System.out.println("Erro ao parsear Content-Length.");
                        }
                    }
                }
            }

            // Ler corpo (body) se houver
            if (contentLength > 0) {
                char[] bodyChars = new char[contentLength];
                int charsRead = is.read(bodyChars, 0, contentLength);
                if (charsRead != -1) {
                    String body = new String(bodyChars, 0, charsRead);
                    parser.setBody(body);
                    requestData.append("\n").append(body);
                }
            }

            // Tratar roteamento e protocolo
            String method = parser.getMethod();
            String path = parser.getPath();
            String httpResponse = "";

            System.out.println("Processando: " + method + " " + path);

            // Roteamento GET (leitura)
            if ("GET".equalsIgnoreCase(method)) {

                // Rota 1: GET /arquivos/{nome}
                if (path != null && path.trim().startsWith("/arquivos/")) {
                    String nomeArquivo = path.trim().replaceFirst("/arquivos/", "");
                    Arquivo arquivoEncontrado = arquivos.get(nomeArquivo);

                    if (arquivoEncontrado != null) {
                        // 200 OK
                        String conteudo = arquivoEncontrado.getConteudo();
                        String tipo = arquivoEncontrado.getTipo();

                        int contentLengthResponse = conteudo.getBytes(StandardCharsets.UTF_8).length;

                        httpResponse = "HTTP/1.1 200 OK\r\n" +
                                       "Content-Type: " + tipo + "\r\n" +
                                       "Content-Length: " + contentLengthResponse + "\r\n" +
                                       "Connection: close\r\n" +
                                       "\r\n" +
                                       conteudo;
                    } else {
                        // 404 Not Found
                        String erroContent = "<h1>404 - Arquivo Nao Encontrado</h1>";
                        int contentLengthResponse = erroContent.getBytes(StandardCharsets.UTF_8).length;

                        httpResponse = "HTTP/1.1 404 Not Found\r\n" +
                                       "Content-Type: text/html\r\n" +
                                       "Content-Length: " + contentLengthResponse + "\r\n" +
                                       "Connection: close\r\n" +
                                       "\r\n" +
                                       erroContent;
                    }
                }
                // Rota 2: GET / Rota raiz
                else if (path != null && path.trim().equalsIgnoreCase("/")) {
                    String conteudoPadrao = "<html><body><h1>Servidor HTTP Customizado Online!</h1><p>Acesse /arquivos/{nome} ou use o Postman para Login/CRUD.</p></body></html>";
                    String tipoPadrao = "text/html";
                    int contentLengthResponse = conteudoPadrao.getBytes(StandardCharsets.UTF_8).length;

                    httpResponse = "HTTP/1.1 200 OK\r\n" +
                                   "Content-Type: " + tipoPadrao + "\r\n" +
                                   "Content-Length: " + contentLengthResponse + "\r\n" +
                                   "Connection: close\r\n" +
                                   "\r\n" +
                                   conteudoPadrao;
                }
                // FALLBACK: 501 Not Implemented (Qualquer outra rota GET)
                else {
                    String erroContent = "<h1>501 - Rota GET Nao Implementada</h1>";
                    int contentLengthResponse = erroContent.getBytes(StandardCharsets.UTF_8).length;

                    httpResponse = "HTTP/1.1 501 Not Implemented\r\n" +
                                   "Content-Type: text/html\r\n" +
                                   "Content-Length: " + contentLengthResponse + "\r\n" +
                                   "Connection: close\r\n" +
                                   "\r\n" +
                                   erroContent;
                }
            }

            // Roteamento POST (Criação e Login)
            else if ("POST".equalsIgnoreCase(method)) {
                
                // Rota 1: POST /login
                if ("/login".equalsIgnoreCase(path)) {
                    try {
                        Usuario user = gson.fromJson(parser.getBody(), Usuario.class);

                        if (usuariosValidos.containsKey(user.getUsername()) && 
                            usuariosValidos.get(user.getUsername()).equals(user.getPassword())) {
                            
                            // Sucesso: Gera o token e armazena
                            String token = UUID.randomUUID().toString();
                            tokensValidos.put(token, user.getUsername());

                            String jsonResponse = gson.toJson(Map.of("status", "Login bem-sucedido", "token", token));
                            int contentLengthResponse = jsonResponse.getBytes(StandardCharsets.UTF_8).length;

                            httpResponse = "HTTP/1.1 200 OK\r\n" +
                                           "Content-Type: application/json\r\n" +
                                           "Content-Length: " + contentLengthResponse + "\r\n" +
                                           "\r\n" +
                                           jsonResponse;
                        } else {
                            // 401 Unauthorized
                            String jsonResponse = gson.toJson(Map.of("status", "Credenciais invalidas"));
                            int contentLengthResponse = jsonResponse.getBytes(StandardCharsets.UTF_8).length;

                            httpResponse = "HTTP/1.1 401 Unauthorized\r\n" +
                                           "Content-Type: application/json\r\n" +
                                           "Content-Length: " + contentLengthResponse + "\r\n" +
                                           "\r\n" +
                                           jsonResponse;
                        }
                    } catch (JsonSyntaxException e) {
                        // 400 Bad Request
                        String jsonResponse = gson.toJson(Map.of("status", "Erro de formato JSON para login."));
                        int contentLengthResponse = jsonResponse.getBytes(StandardCharsets.UTF_8).length;

                        httpResponse = "HTTP/1.1 400 Bad Request\r\n" +
                                       "Content-Type: application/json\r\n" +
                                       "Content-Length: " + contentLengthResponse + "\r\n" +
                                       "\r\n" +
                                       jsonResponse;
                    }
                }
                // Rota 2: POST /arquivos
                else if ("/arquivos".equalsIgnoreCase(path)) {
                    if (!isAuthenticated(parser)) {
                        httpResponse = buildUnauthorizedResponse();
                    } else {
                        try {
                            String body = parser.getBody();
                            Arquivo novoArquivo = gson.fromJson(body, Arquivo.class);

                            // Validação
                            if (novoArquivo == null || 
                                novoArquivo.getNome() == null || novoArquivo.getNome().trim().isEmpty() ||
                                novoArquivo.getConteudo() == null || novoArquivo.getConteudo().trim().isEmpty() ||
                                novoArquivo.getTipo() == null || novoArquivo.getTipo().trim().isEmpty()) 
                            {
                                throw new IllegalArgumentException("Dados incompletos no JSON: 'nome', 'conteudo' e 'tipo' são obrigatórios e não podem ser vazios.");
                            }
                            
                            if (arquivos.containsKey(novoArquivo.getNome())) {
                                throw new IllegalArgumentException("O arquivo '" + novoArquivo.getNome() + "' ja existe.");
                            }

                            arquivos.put(novoArquivo.getNome(), novoArquivo);

                            String jsonResponse = gson.toJson(Map.of("status", "Criado com sucesso", "nome", novoArquivo.getNome()));
                            int contentLengthResponse = jsonResponse.getBytes(StandardCharsets.UTF_8).length;

                            httpResponse = "HTTP/1.1 201 Created\r\n" +
                                           "Content-Type: application/json\r\n" +
                                           "Content-Length: " + contentLengthResponse + "\r\n" +
                                           "\r\n" +
                                           jsonResponse;
                        } catch (JsonSyntaxException | IllegalArgumentException e) {
                            // 400 Bad Request
                            String jsonResponse = gson.toJson(Map.of("status", "Erro ao criar arquivo", "detalhes", e.getMessage()));
                            int contentLengthResponse = jsonResponse.getBytes(StandardCharsets.UTF_8).length;

                            httpResponse = "HTTP/1.1 400 Bad Request\r\n" +
                                           "Content-Type: application/json\r\n" +
                                           "Content-Length: " + contentLengthResponse + "\r\n" +
                                           "\r\n" +
                                           jsonResponse;
                        }
                    }
                }
                // FALLBACK: 501 Not Implemented
                else {
                    httpResponse = buildNotImplementedResponse(method);
                }
            }

            // Roteamento PUT (atualização)
            else if ("PUT".equalsIgnoreCase(method)) {
                if (!isAuthenticated(parser)) {
                    httpResponse = buildUnauthorizedResponse();
                } else {
                    if (path != null && path.trim().startsWith("/arquivos/")) {
                        String nomeArquivo = path.trim().replaceFirst("/arquivos/", "");

                        if (!arquivos.containsKey(nomeArquivo)) {
                            httpResponse = buildNotFoundResponse(nomeArquivo);
                        } else {
                            try {
                                String body = parser.getBody();
                                // Desserializa para um objeto temporário
                                Arquivo updateData = gson.fromJson(body, Arquivo.class);
                                Arquivo arquivoExistente = arquivos.get(nomeArquivo);
                                
                                boolean updated = false;
                                
                                // Lógica de Atualização
                                if (updateData != null) {
                                    if (updateData.getConteudo() != null && !updateData.getConteudo().trim().isEmpty()) {
                                        arquivoExistente.setConteudo(updateData.getConteudo());
                                        updated = true;
                                    }
                                    if (updateData.getTipo() != null && !updateData.getTipo().trim().isEmpty()) {
                                        arquivoExistente.setTipo(updateData.getTipo());
                                        updated = true;
                                    }
                                }

                                if (!updated) {
                                    throw new IllegalArgumentException("Nenhum campo válido ('conteudo' ou 'tipo') foi fornecido para atualização.");
                                }

                                String jsonResponse = gson.toJson(Map.of("status", "Atualizado com sucesso", "nome", nomeArquivo));
                                int contentLengthResponse = jsonResponse.getBytes(StandardCharsets.UTF_8).length;

                                httpResponse = "HTTP/1.1 200 OK\r\n" +
                                               "Content-Type: application/json\r\n" +
                                               "Content-Length: " + contentLengthResponse + "\r\n" +
                                               "\r\n" +
                                               jsonResponse;
                            } catch (JsonSyntaxException | IllegalArgumentException e) {
                                // 400 Bad Request
                                String jsonResponse = gson.toJson(Map.of("status", "Erro na atualização", "detalhes", e.getMessage()));
                                int contentLengthResponse = jsonResponse.getBytes(StandardCharsets.UTF_8).length;

                                httpResponse = "HTTP/1.1 400 Bad Request\r\n" +
                                               "Content-Type: application/json\r\n" +
                                               "Content-Length: " + contentLengthResponse + "\r\n" +
                                               "\r\n" +
                                               jsonResponse;
                            }
                        }
                    } else {
                        httpResponse = buildNotImplementedResponse(method);
                    }
                }
            }

            // Roteamento DELETE (exclusão)
            else if ("DELETE".equalsIgnoreCase(method)) {
                if (!isAuthenticated(parser)) {
                    httpResponse = buildUnauthorizedResponse();
                } else {
                    if (path != null && path.trim().startsWith("/arquivos/")) {
                        String nomeArquivo = path.trim().replaceFirst("/arquivos/", "");

                        if (arquivos.containsKey(nomeArquivo)) {
                            arquivos.remove(nomeArquivo);
                            // 204 No Content (Padrão para exclusão bem-sucedida sem corpo)
                            httpResponse = "HTTP/1.1 204 No Content\r\n" +
                                           "Connection: close\r\n" +
                                           "\r\n";
                        } else {
                            httpResponse = buildNotFoundResponse(nomeArquivo);
                        }
                    } else {
                        httpResponse = buildNotImplementedResponse(method);
                    }
                }
            }

            // Fallback para métodos não suportados
            else {
                httpResponse = buildNotImplementedResponse(method);
            }

            // Enviar resposta final
            System.out.println("Enviando resposta: " + httpResponse.split("\r\n")[0]);
            os.write(httpResponse.getBytes(StandardCharsets.UTF_8));
            os.flush();

        } catch (IOException e) {
            System.out.println("Erro na comunicação com o cliente: " + e.getMessage());
        } finally {
            try {
                if (os != null) os.close();
                if (is != null) is.close();
                if (socket != null && !socket.isClosed()) socket.close();
                System.out.println("Conexão fechada.");
            } catch (IOException e) {
                System.out.println("Erro no fechamento de conexão: " + e.getMessage());
            }
        }
    }

    // Métodos auxiliares
    private boolean isAuthenticated(HttpParser parser) {
        String authHeader = parser.getHeader("authorization");
        if (authHeader != null && authHeader.trim().length() > 0) {
            String token = authHeader.trim();
            return tokensValidos.containsKey(token);
        }
        return false;
    }

    private String buildUnauthorizedResponse() {
        String jsonResponse = gson.toJson(Map.of("status", "Token de autenticacao ausente ou invalido."));
        int contentLengthResponse = jsonResponse.getBytes(StandardCharsets.UTF_8).length;

        return "HTTP/1.1 401 Unauthorized\r\n" +
               "Content-Type: application/json\r\n" +
               "Content-Length: " + contentLengthResponse + "\r\n" +
               "\r\n" +
               jsonResponse;
    }
    
    private String buildNotFoundResponse(String resource) {
        String erroContent = "<h1>404 - Recurso Nao Encontrado</h1><p>O arquivo " + resource + " nao existe.</p>";
        int contentLengthResponse = erroContent.getBytes(StandardCharsets.UTF_8).length;

        return "HTTP/1.1 404 Not Found\r\n" +
               "Content-Type: text/html\r\n" +
               "Content-Length: " + contentLengthResponse + "\r\n" +
               "\r\n" +
               erroContent;
    }
    
    private String buildNotImplementedResponse(String method) {
        String erroContent = "<h1>501 - Rota " + method + " Nao Implementada</h1>";
        int contentLengthResponse = erroContent.getBytes(StandardCharsets.UTF_8).length;

        return "HTTP/1.1 501 Not Implemented\r\n" +
               "Content-Type: text/html\r\n" +
               "Content-Length: " + contentLengthResponse + "\r\n" +
               "\r\n" +
               erroContent;
    }
    
    // Métodos de conexão   
    public static void main(String[] args) {
        try {
            ServerV2 server = new ServerV2();
            server.criarServerSocket(server.porta);
            server.connectionLoop();

        } catch (Exception e) {
            System.out.println("ERRO NO MAIN: " + e);
        }
    }

    private ServerSocket criarServerSocket(int porta) {
        try {
            this.serverSocket = new ServerSocket(porta);
        } catch (Exception e) {
            System.out.println("Erro na Criação do server Socket " + e.getMessage());
        }
        return serverSocket;
    }

    private Socket esperaConexao() {
        try {
            return this.serverSocket.accept();
        } catch (IOException ex) {
            System.out.println("Erro ao criar socket do cliente " + ex.getMessage());
            return null;
        }
    }
}