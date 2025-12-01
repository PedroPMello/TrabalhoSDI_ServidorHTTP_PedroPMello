package server;

public class Usuario {
    private String username;
    private String password; 

    // Construtor
    public Usuario(String username, String password) {
        this.username = username;
        this.password = password;
    }

    // Getters (para a lógica de autenticação)
    public String getUsername() {
        return username;
    }

    public String getPassword() {
        return password;
    }
}
