package client;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.io.Reader;
import java.net.Socket;
import java.util.HashMap;
import java.util.Map;

public class Client {

    private String nome;
    private Socket socket; // socket para comunicar com servidor

    public Client(String nome) {
        this.nome = nome;
    }

    public String getNome() {
        return nome;
    }

    public void setNome(String nome) {
        this.nome = nome;
    }

    private Socket conectaComServidor(String host, int porta) throws IOException {
        try {
            // Atribui o socket criado à variável de instância
            this.socket = new Socket(host, porta);
            return this.socket;
        } catch (IOException ex) {
            System.out.println("Erro ao conectar com o servidor: " + ex.getMessage());
            // CORREÇÃO: A exceção correta é IOException
            throw new IOException("Ocorreu um erro ao criar o socket: " + ex.getMessage());
        }
    }

    /**
     * Método de instância que encapsula toda a lógica de conexão,
     * configuração de streams e o loop de eventos/protocolo.
     */
    public void initClient() {
        try { //poderia otimizar usando um try-with-resources
            // 1. conexao com server (usa o método privado e atribui ao socket da instância)
            conectaComServidor("localhost", 80);

            // 2. streams de saida e entrada (baseados no socket da instância)
            PrintWriter output = new PrintWriter(this.socket.getOutputStream(), true);
            BufferedReader input = new BufferedReader(
                    new InputStreamReader(this.socket.getInputStream()));

            // 3. --- INÍCIO DO PROTOCOLO ---
            //lê uma mensagem (linha) da entrada do teclado e envia ao servidor
            BufferedReader teclado = new BufferedReader(new InputStreamReader(System.in));
            System.out.print("$>");
            String mensagem = teclado.readLine();
            //output é o envio ao servidor
            output.write(mensagem+" \r\n");
            output.write("\r\n");
            output.flush(); //força o esvaziamento do buffer
            
            // Recebimento da resposta do servidor
            String msg = input.readLine();
            System.out.println("Servidor respondeu: " + msg);
            // --- FIM DO PROTOCOLO ---

            // 4. fecha streams e conexão
            output.close();
            input.close();
            this.socket.close(); // Fecha o socket da instância
            System.out.println("Comunicação encerrada");

        } catch (Exception e) {
            System.out.println("Erro na comunicação: " + e.getLocalizedMessage());
        }
    }

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) {
        // O método main agora está limpo.
        // Ele apenas instancia o cliente e inicia sua lógica principal.
        Client cliente = new Client("Elder");
        cliente.initClient();
    }
    
}
