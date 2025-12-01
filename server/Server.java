package server;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;

public class Server {

    private ServerSocket serverSocket;
    int porta;

    public Server() {
        this.porta = 5555;
    }

    public void connectionLoop() throws IOException {
        try {
            // Espera o um pedido de conexÃ£o;
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
        try {
            input = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            output = new PrintWriter(socket.getOutputStream(), true);

            String msgCliente = input.readLine();
            System.out.println("Mensagem recebida do cliente: " + msgCliente);

            String msgResposta = "Oi";
            output.println(msgResposta);
            System.out.println("Resposta enviada ao cliente: " + msgResposta);
        } catch (IOException e) {
            System.out.println("Erro no tratamento da conexão: " + e.getMessage());
        } finally {
            // Garante o fechamento dos streams de entrada e saida ao final da conexao
            try {
                output.close();
                input.close();
                socket.close();
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
            Server server = new Server();
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
