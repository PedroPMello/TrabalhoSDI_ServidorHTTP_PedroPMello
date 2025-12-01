package server;
/*
 * ... (imports)
 */

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.CharBuffer;
import java.util.HashMap;
import java.util.Map;
import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.util.UUID;

public class ServerV2 {

    private ServerSocket serverSocket;
    int porta;
    Map<String, Arquivo> arquivos;

    // Mapa para armazenar usuários válidos (Chave: Username, Valor: Password)
    private final Map<String, String> usuariosValidos = new HashMap<>();
    private Map<String, String> tokensValidos = new HashMap<>();

    private final Gson gson = new Gson();

    public ServerV2() {
        this.porta = 8080;
        arquivos = new HashMap<>();
        usuariosValidos.put("admin", "12345");
        usuariosValidos.put("user1", "senha1");
        
        // Definição do conteúdo
        String htmlContent = "<html><body><h1>Bem-vindo!</h1><p>Teste de GET Básico Funcional.</p></body></html>";
        
        // Criação do objeto Arquivo: new Arquivo(nome, conteudo, tipo)
        Arquivo arquivoInicial = new Arquivo("index.html", htmlContent, "text/html");
        
        // Inserção no Map: put(nomeDoArquivo, objetoArquivo)
        arquivos.put("index.html", arquivoInicial);
        
        // Exemplo de arquivo de texto (também deve ser um objeto Arquivo)
        Arquivo arquivoTexto = new Arquivo("info.txt", "Conteúdo em texto simples.", "text/plain");
        arquivos.put("info.txt", arquivoTexto);
    }

    public void connectionLoop() throws IOException {
        try {
            // Esperar o um pedido de conexão;
            while (true) {
                System.out.println("Esperando conexao...");
                Socket socket = this.esperaConexao(); // Método bloqueante
                System.out.println("Conexao recebida, inciando protocolo...");
                // Objeto socket representa a conexão com o servidor
                clientHandle(socket);// Chama método para tratar mensagem do cliente

            }

        } catch (Exception e) {
            System.out.println("Erro na main do ServerSocket " + e.getMessage());
            System.exit(0);
        } finally {
            System.out.println("Servidor finalizado.");
        }

    }
    
private void clientHandle(Socket socket) {
    BufferedReader input = null;
    PrintWriter output = null;
    String httpResponse = "";
    
    // O bloco try principal deve englobar toda a lógica que pode gerar exceção
    try {
        // Cria streams de entrada e saída
        OutputStream os = socket.getOutputStream(); 
        
        input = new BufferedReader(new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8));
        output = new PrintWriter(new OutputStreamWriter(os, StandardCharsets.UTF_8), true); 

        // Trata a conversação entre cliente e servidor (tratar protocolo);
        HttpParser parser = new HttpParser();
        String linha;
        Integer contentLength = 0;
        
        // Leitura e Processamento da Linha de Status
        linha = input.readLine();
        System.out.println("Recebido: " + linha);

        if (linha == null || linha.isEmpty()) {
            System.out.println("Conexão fechada pelo cliente ou requisição vazia.");
            return; 
        }
        
        String[] partes = linha.split(" ");
        if (partes.length >= 3) {
            parser.setMethod(partes[0].trim());
            parser.setPath(partes[1].trim());
            parser.setHttpVersion(partes[2].trim());
        }

        // Loop para ler os Headers
        while ( (linha = input.readLine()) != null && !linha.isEmpty() ) { 
            System.out.println("Recebido: " + linha);
            
            String[] headerParts = linha.split(":", 2);
            if(headerParts.length == 2){
                parser.setHeader(headerParts[0].trim(), headerParts[1].trim());
                
                if(headerParts[0].trim().equalsIgnoreCase("Content-Length")){
                    try {
                        contentLength = Integer.parseInt(headerParts[1].trim());
                    } catch (NumberFormatException nfe) {
                         System.err.println("Erro ao parsear Content-Length: " + nfe.getMessage());
                         contentLength = 0;
                    }
                }
            }
        } 
        
        // Leitura do Corpo (Body)
        if(contentLength > 0){
            CharBuffer bodyBuffer = CharBuffer.allocate(contentLength);
            input.read(bodyBuffer); 
            bodyBuffer.flip();
            parser.setBody(bodyBuffer.toString());
        }
        
        System.out.println("Requisição completa recebida:\n" + parser.toString());
        
        // Roteador (lógica de negócio)
        
        String method = parser.getMethod();
        String path = parser.getPath();

        // Roteamento GET (Leitura)
        if ("GET".equalsIgnoreCase(method)) {
            
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
                                   
                    // Envio imediato da resposta 200
                    System.out.println("Enviando resposta (Status Line e Headers): " + httpResponse.split("\r\n")[0]);
                    os.write(httpResponse.getBytes(StandardCharsets.UTF_8)); 
                    os.flush();
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
                                   
                    // Envio imediato da resposta 404
                    System.out.println("Enviando resposta (Status Line e Headers): " + httpResponse.split("\r\n")[0]);
                    os.write(httpResponse.getBytes(StandardCharsets.UTF_8)); 
                    os.flush();
                }
            } else {
                // 501 Not Implemented (Outras rotas GET)
                String erroContent = "<h1>501 - Rota GET Nao Implementada</h1>";
                int contentLengthResponse = erroContent.getBytes(StandardCharsets.UTF_8).length;
                
                httpResponse = "HTTP/1.1 501 Not Implemented\r\n" + 
                               "Content-Type: text/html\r\n" +
                               "Content-Length: " + contentLengthResponse + "\r\n" +
                               "Connection: close\r\n" +
                               "\r\n" +
                               erroContent;

                // Envio imediato da resposta 501
                System.out.println("Enviando resposta (Status Line e Headers): " + httpResponse.split("\r\n")[0]);
                os.write(httpResponse.getBytes(StandardCharsets.UTF_8)); 
                os.flush();
            }
        } 

        // Roteamento POST (Login e Criação)
        else if ("POST".equalsIgnoreCase(method)) {
            
            // Rota de autenticação: POST /login
            if (path != null && path.trim().equalsIgnoreCase("/login")) {
                
                String body = parser.getBody();

                try {
                    Usuario usuario = gson.fromJson(body, Usuario.class);
                    String senhaEsperada = usuariosValidos.get(usuario.getUsername());

                    if (senhaEsperada != null && senhaEsperada.equals(usuario.getPassword())) {
                        
                        // 1. Geração e armazenamento de token dinâmico
                        String token = UUID.randomUUID().toString(); 
                        tokensValidos.put(token, usuario.getUsername());
                        
                        // 2. 200 OK - Sucesso
                        String contentResponse = "{\"status\": \"Login bem-sucedido\", \"token\": \"" + token + "\"}";
                        int contentLengthResponse = contentResponse.getBytes(StandardCharsets.UTF_8).length;
                        
                        httpResponse = "HTTP/1.1 200 OK\r\n" + 
                                       "Content-Type: application/json; charset=UTF-8\r\n" +
                                       "Content-Length: " + contentLengthResponse + "\r\n" +
                                       "Connection: close\r\n" +
                                       "\r\n" + 
                                       contentResponse;
                        
                        // Envio imediato da resposta 200
                        System.out.println("Enviando resposta (Status Line e Headers): " + httpResponse.split("\r\n")[0]);
                        os.write(httpResponse.getBytes(StandardCharsets.UTF_8)); 
                        os.flush();
                                               
                    } else {
                        // 403 Forbidden
                        String erroContent = "{\"erro\": \"403 - Forbidden\", \"mensagem\": \"Credenciais invalidas.\"}";
                        int contentLengthResponse = erroContent.getBytes(StandardCharsets.UTF_8).length;

                        httpResponse = "HTTP/1.1 403 Forbidden\r\n" + 
                                       "Content-Type: application/json; charset=UTF-8\r\n" +
                                       "Content-Length: " + contentLengthResponse + "\r\n" +
                                       "Connection: close\r\n" +
                                       "\r\n" +
                                       erroContent;

                        // Envio imediato da resposta 403
                        System.out.println("Enviando resposta (Status Line e Headers): " + httpResponse.split("\r\n")[0]);
                        os.write(httpResponse.getBytes(StandardCharsets.UTF_8)); 
                        os.flush();
                    }

                } catch (JsonSyntaxException | NullPointerException e) {
                    // 400 Bad Request
                    System.err.println("Erro 400 no /login: " + e.getMessage());
                    String erroContent = "{\"erro\": \"400 - Requisicao Invalida\", \"mensagem\": \"JSON mal formatado para login.\"}";
                    int contentLengthResponse = erroContent.getBytes(StandardCharsets.UTF_8).length;

                    httpResponse = "HTTP/1.1 400 Bad Request\r\n" + 
                                   "Content-Type: application/json; charset=UTF-8\r\n" +
                                   "Content-Length: " + contentLengthResponse + "\r\n" +
                                   "Connection: close\r\n" +
                                   "\r\n" +
                                   erroContent;
                    
                    // Envio imediato da resposta 400
                    System.out.println("Enviando resposta (Status Line e Headers): " + httpResponse.split("\r\n")[0]);
                    os.write(httpResponse.getBytes(StandardCharsets.UTF_8)); 
                    os.flush();
                }
            } 
            // Rota de criação de arquivo: POST /arquivos (com filtro de sessão)
            else if (path != null && path.trim().equalsIgnoreCase("/arquivos")) {
                
                // Filtro de segurança: Checa a validade do Token de Sessão
                String tokenRecebido = parser.getHeader("Authorization");
                
                // Verifica se o token está ausente ou se não está no Map de tokens válidos
                if (tokenRecebido == null || !tokensValidos.containsKey(tokenRecebido)) {
                    // 401 Unauthorized
                    String erroContent = "{\"erro\": \"401 - Nao Autorizado\", \"mensagem\": \"Token de acesso ausente ou invalido.\"}";
                    int contentLengthResponse = erroContent.getBytes(StandardCharsets.UTF_8).length;

                    httpResponse = "HTTP/1.1 401 Unauthorized\r\n" + 
                                   "Content-Type: application/json; charset=UTF-8\r\n" +
                                   "Content-Length: " + contentLengthResponse + "\r\n" +
                                   "Connection: close\r\n" +
                                   "\r\n" +
                                   erroContent;

                    os.write(httpResponse.getBytes(StandardCharsets.UTF_8)); 
                    os.flush();
                    return; 
                }

                // Token válido: Processamento continua
                String body = parser.getBody();

                try {
                    Arquivo novoArquivo = gson.fromJson(body, Arquivo.class);

                    if (novoArquivo == null || novoArquivo.getNome() == null || novoArquivo.getConteudo() == null || novoArquivo.getTipo() == null) {
                        throw new IllegalArgumentException("Dados incompletos no JSON.");
                    }
                    
                    arquivos.put(novoArquivo.getNome(), novoArquivo);
                    
                    // 201 Created
                    String contentResponse = "{\"status\": \"Criado com sucesso\", \"nome\": \"" + novoArquivo.getNome() + "\"}";
                    int contentLengthResponse = contentResponse.getBytes(StandardCharsets.UTF_8).length;
                    
                    httpResponse = "HTTP/1.1 201 Created\r\n" + 
                                 "Content-Type: application/json; charset=UTF-8\r\n" +
                                 "Content-Length: " + contentLengthResponse + "\r\n" +
                                 "Connection: close\r\n" +
                                 "\r\n" + 
                                 contentResponse;

                    // Envio imediato da resposta 201
                    System.out.println("Enviando resposta (Status Line e Headers): " + httpResponse.split("\r\n")[0]);
                    os.write(httpResponse.getBytes(StandardCharsets.UTF_8)); 
                    os.flush();
                                 
                } catch (JsonSyntaxException | IllegalArgumentException e) {
                    // 400 Bad Request
                    String erroContent = "{\"erro\": \"400 - Requisicao Invalida\", \"mensagem\": \"JSON mal formatado ou campos faltando.\"}";
                    int contentLengthResponse = erroContent.getBytes(StandardCharsets.UTF_8).length;

                    httpResponse = "HTTP/1.1 400 Bad Request\r\n" + 
                                 "Content-Type: application/json; charset=UTF-8\r\n" +
                                 "Content-Length: " + contentLengthResponse + "\r\n" +
                                 "Connection: close\r\n" +
                                 "\r\n" +
                                 erroContent;

                    // Envio imediato da resposta 400
                    System.out.println("Enviando resposta (Status Line e Headers): " + httpResponse.split("\r\n")[0]);
                    os.write(httpResponse.getBytes(StandardCharsets.UTF_8)); 
                    os.flush();
                }
            }
            // POST Fallback
            else {
                // 501 Not Implemented (Rotas POST diferentes de /arquivos e /login)
                String erroContent = "<h1>501 - Rota POST Nao Implementada</h1>";
                int contentLengthResponse = erroContent.getBytes(StandardCharsets.UTF_8).length;
                
                httpResponse = "HTTP/1.1 501 Not Implemented\r\n" + 
                               "Content-Type: text/html\r\n" +
                               "Content-Length: " + contentLengthResponse + "\r\n" +
                               "Connection: close\r\n" +
                               "\r\n" +
                               erroContent;

                // Envio imediato da resposta 501
                System.out.println("Enviando resposta (Status Line e Headers): " + httpResponse.split("\r\n")[0]);
                os.write(httpResponse.getBytes(StandardCharsets.UTF_8)); 
                os.flush();
            }
        } 

        // Roteamento PUT (Atualização)
        else if ("PUT".equalsIgnoreCase(method) && path != null && path.trim().startsWith("/arquivos/")) {
            
            // 1. Filtro de segurança: Checa a validade do Token de Sessão
            String tokenRecebido = parser.getHeader("Authorization");
            
            if (tokenRecebido == null || !tokensValidos.containsKey(tokenRecebido)) {
                // 401 Unauthorized
                String erroContent = "{\"erro\": \"401 - Nao Autorizado\", \"mensagem\": \"Token de acesso ausente ou invalido.\"}";
                int contentLengthResponse = erroContent.getBytes(StandardCharsets.UTF_8).length;

                httpResponse = "HTTP/1.1 401 Unauthorized\r\n" + 
                               "Content-Type: application/json; charset=UTF-8\r\n" +
                               "Content-Length: " + contentLengthResponse + "\r\n" +
                               "Connection: close\r\n" +
                               "\r\n" +
                               erroContent;

                os.write(httpResponse.getBytes(StandardCharsets.UTF_8)); 
                os.flush();
                return; 
            }
            
            String nomeArquivo = path.trim().replaceFirst("/arquivos/", "");
            String body = parser.getBody();

            // 2. Verifica se o arquivo existe
            if (!arquivos.containsKey(nomeArquivo)) {
                // 404 Not Found
                String erroContent = "<h1>404 - Arquivo Nao Encontrado (PUT)</h1>";
                int contentLengthResponse = erroContent.getBytes(StandardCharsets.UTF_8).length;

                httpResponse = "HTTP/1.1 404 Not Found\r\n" + 
                               "Content-Type: text/html\r\n" +
                               "Content-Length: " + contentLengthResponse + "\r\n" +
                               "Connection: close\r\n" +
                               "\r\n" +
                               erroContent;

                os.write(httpResponse.getBytes(StandardCharsets.UTF_8)); 
                os.flush();
                return;
            }

            try {
                Arquivo arquivoAtualizado = gson.fromJson(body, Arquivo.class);

                // No PUT, só é necesspario conteúdo e tipo.
                if (arquivoAtualizado == null || arquivoAtualizado.getConteudo() == null || arquivoAtualizado.getTipo() == null) {
                    throw new IllegalArgumentException("Dados incompletos no JSON para atualização.");
                }
                
                // Define o nome vindo da URL
                arquivoAtualizado.setNome(nomeArquivo); 
                arquivos.put(nomeArquivo, arquivoAtualizado); // Atualiza no mapa
                
                // 200 OK
                String contentResponse = "{\"status\": \"Atualizado com sucesso\", \"nome\": \"" + nomeArquivo + "\"}";
                int contentLengthResponse = contentResponse.getBytes(StandardCharsets.UTF_8).length;
                
                httpResponse = "HTTP/1.1 200 OK\r\n" + 
                             "Content-Type: application/json; charset=UTF-8\r\n" +
                             "Content-Length: " + contentLengthResponse + "\r\n" +
                             "Connection: close\r\n" +
                             "\r\n" + 
                             contentResponse;

                // Envio imediato da resposta 200
                System.out.println("Enviando resposta (Status Line e Headers): " + httpResponse.split("\r\n")[0]);
                os.write(httpResponse.getBytes(StandardCharsets.UTF_8)); 
                os.flush();
                             
            } catch (JsonSyntaxException | IllegalArgumentException e) {
                // 400 Bad Request
                String erroContent = "{\"erro\": \"400 - Requisicao Invalida\", \"mensagem\": \"JSON mal formatado ou campos faltando (PUT).\"}";
                int contentLengthResponse = erroContent.getBytes(StandardCharsets.UTF_8).length;

                httpResponse = "HTTP/1.1 400 Bad Request\r\n" + 
                             "Content-Type: application/json; charset=UTF-8\r\n" +
                             "Content-Length: " + contentLengthResponse + "\r\n" +
                             "Connection: close\r\n" +
                             "\r\n" +
                             erroContent;

                // Envio imediato da resposta 400
                System.out.println("Enviando resposta (Status Line e Headers): " + httpResponse.split("\r\n")[0]);
                os.write(httpResponse.getBytes(StandardCharsets.UTF_8)); 
                os.flush();
            }
        }

        // Roteamento DELETE (Exclusão)
        else if ("DELETE".equalsIgnoreCase(method) && path != null && path.trim().startsWith("/arquivos/")) {
            
            // Filtro de segurança: Checa a validade do Token de Sessão
            String tokenRecebido = parser.getHeader("Authorization");
            
            if (tokenRecebido == null || !tokensValidos.containsKey(tokenRecebido)) {
                // 401 Unauthorized
                String erroContent = "{\"erro\": \"401 - Nao Autorizado\", \"mensagem\": \"Token de acesso ausente ou invalido.\"}";
                int contentLengthResponse = erroContent.getBytes(StandardCharsets.UTF_8).length;

                httpResponse = "HTTP/1.1 401 Unauthorized\r\n" + 
                               "Content-Type: application/json; charset=UTF-8\r\n" +
                               "Content-Length: " + contentLengthResponse + "\r\n" +
                               "Connection: close\r\n" +
                               "\r\n" +
                               erroContent;

                os.write(httpResponse.getBytes(StandardCharsets.UTF_8)); 
                os.flush();
                return; 
            }
            
            String nomeArquivo = path.trim().replaceFirst("/arquivos/", "");

            // 2. Tenta remover o arquivo do mapa.
            if (arquivos.remove(nomeArquivo) != null) {
                // 204 No Content - Sucesso na exclusão (sem corpo na resposta)
                httpResponse = "HTTP/1.1 204 No Content\r\n" + 
                               "Connection: close\r\n" +
                               "\r\n";
                
                // Envio imediato da resposta 204
                System.out.println("Enviando resposta (Status Line e Headers): HTTP/1.1 204 No Content");
                os.write(httpResponse.getBytes(StandardCharsets.UTF_8)); 
                os.flush();

            } else {
                // 404 Not Found
                String erroContent = "<h1>404 - Arquivo Nao Encontrado (DELETE)</h1>";
                int contentLengthResponse = erroContent.getBytes(StandardCharsets.UTF_8).length;

                httpResponse = "HTTP/1.1 404 Not Found\r\n" + 
                               "Content-Type: text/html\r\n" +
                               "Content-Length: " + contentLengthResponse + "\r\n" +
                               "Connection: close\r\n" +
                               "\r\n" +
                               erroContent;

                os.write(httpResponse.getBytes(StandardCharsets.UTF_8)); 
                os.flush();
            }
        }

        // Roteamento FALLBACK (Outros Métodos)
        else { 
            // 501 Not Implemented (Outros métodos HTTP)
            String erroContent = "<h1>501 - Metodo Nao Implementado</h1>";
            int contentLengthResponse = erroContent.getBytes(StandardCharsets.UTF_8).length;
            
            httpResponse = "HTTP/1.1 501 Not Implemented\r\n" + 
                           "Content-Type: text/html\r\n" +
                           "Content-Length: " + contentLengthResponse + "\r\n" +
                           "Connection: close\r\n" +
                           "\r\n" +
                           erroContent;

            // Envio imediato da resposta 501
            System.out.println("Enviando resposta (Status Line e Headers): " + httpResponse.split("\r\n")[0]);
            os.write(httpResponse.getBytes(StandardCharsets.UTF_8)); 
            os.flush();
        }
        // Fim do roteador
        
    } catch (IOException e) {
        System.out.println("Erro na conexão: " + e.getMessage());
    } catch (Exception e) {
        System.err.println("Erro inesperado durante o processamento: " + e.getMessage());
        e.printStackTrace(); 

        // Tenta enviar 500 Internal Server Error 
        try {
            String erroContent = "<h1>500 - Erro Interno do Servidor!</h1>";
            httpResponse = "HTTP/1.1 500 Internal Server Error\r\n" +
                           "Content-Type: text/html\r\n" +
                           "Content-Length: " + erroContent.getBytes(StandardCharsets.UTF_8).length + "\r\n" +
                           "Connection: close\r\n" + 
                           "\r\n" +
                           erroContent;
            OutputStream fallbackOs = socket.getOutputStream();
            if (fallbackOs != null) {
                 fallbackOs.write(httpResponse.getBytes(StandardCharsets.UTF_8));
                 fallbackOs.flush();
            }
        } catch (Exception e2) { 
            System.err.println("Falha ao enviar 500 ao cliente: " + e2.getMessage());
        }
    } finally {
        try {
            if (socket != null) socket.close();
        } catch (IOException e) {
            System.out.println("Erro no fechamento de conexão: " + e.getMessage());
        }
    }
}
    /**
     * @param args the command line arguments

     */
    public static void main(String[] args) {
        try {
            ServerV2 server = new ServerV2();
            // Cria o servidor de conexões
            server.criarServerSocket(server.porta);
            // Inicia o looping de espera de conexoes
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
