package model;

import java.time.LocalDate;

public class Lote implements Comparable<Lote> {
    private int idLote;
    private String nomeMedicamento;
    private LocalDate dataFabricacao;
    private LocalDate dataValidade;
    private int quantidade;
    private int idFornecedor;
    private int idMedicamento;

    public Lote(int idLote, String nomeMedicamento, LocalDate dataFabricacao, 
                LocalDate dataValidade, int quantidade, int idFornecedor, int idMedicamento) {
        this.idLote = idLote;
        this.nomeMedicamento = nomeMedicamento;
        this.dataFabricacao = dataFabricacao;
        this.dataValidade = dataValidade;
        this.quantidade = quantidade;
        this.idFornecedor = idFornecedor;
        this.idMedicamento = idMedicamento;
    }

    public int getIdLote() { return idLote; }
    public String getNomeMedicamento() { return nomeMedicamento; }
    public LocalDate getDataFabricacao() { return dataFabricacao; }
    public LocalDate getDataValidade() { return dataValidade; }
    public int getQuantidade() { return quantidade; }
    public int getIdFornecedor() { return idFornecedor; }
    public int getIdMedicamento() { return idMedicamento; }

    @Override
    public int compareTo(Lote outro) {
        int compData = this.dataValidade.compareTo(outro.dataValidade);
        if (compData != 0) {
            return compData;
        }
        return Integer.compare(this.idLote, outro.idLote);
    }

    public static class Medicamento {
        private int id;
        private String nome;
        private double preco;
        private int estoque;
        private String tipo;

        public Medicamento(int id, String nome, double preco, int estoque, String tipo) {
            this.id = id;
            this.nome = nome;
            this.preco = preco;
            this.estoque = estoque;
            this.tipo = tipo;
        }
        public int getId() { return id; }
        public String getNome() { return nome; }
        public double getPreco() { return preco; }
        public int getEstoque() { return estoque; }
        public String getTipo() { return tipo; }
    }

    public static class Fornecedor {
        private int id;
        private String cnpj, nome, telefone, email, endereco;

        public Fornecedor(int id, String cnpj, String nome, String telefone, String email, String endereco) {
            this.id = id;
            this.cnpj = cnpj;
            this.nome = nome;
            this.telefone = telefone;
            this.email = email;
            this.endereco = endereco;
        }
        public int getId() { return id; }
        public String getCnpj() { return cnpj; }
        public String getNome() { return nome; }
        public String getTelefone() { return telefone; }
        public String getEmail() { return email; }
        public String getEndereco() { return endereco; }
    }
}