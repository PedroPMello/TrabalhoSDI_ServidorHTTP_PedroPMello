package server;

public class Arquivo {
    private String nome;
    private String conteudo;
    private String tipo; // Ex: "text/html", "text/plain"

    public Arquivo(String nome, String conteudo, String tipo) {
        this.nome = nome;
        this.conteudo = conteudo;
        this.tipo = tipo;
    }

    // Getters
    public String getNome() { return nome; }
    public String getConteudo() { return conteudo; }
    public String getTipo() { return tipo; }

    // Setters
    public void setNome(String nome) { this.nome = nome; }
    public void setConteudo(String conteudo) { this.conteudo = conteudo; }
    public void setTipo(String tipo) { this.tipo = tipo; }
}