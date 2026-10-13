package model;

import java.time.LocalDate;

public class HistoricoMovimentacao {
    private int id;
    private LocalDate dataMovimento;
    private String tipo; // "ENTRADA", "SAIDA_VENDA", "DESCARTE_VENCIMENTO"
    private String nomeMedicamento;
    private int quantidade;
    private int idLote;
    private int mesReferencia; // 1 = Jan, 2 = Fev, etc.

    public HistoricoMovimentacao(int id, LocalDate dataMovimento, String tipo, 
                                 String nomeMedicamento, int quantidade, int idLote, int mesReferencia) {
        this.id = id;
        this.dataMovimento = dataMovimento;
        this.tipo = tipo;
        this.nomeMedicamento = nomeMedicamento;
        this.quantidade = quantidade;
        this.idLote = idLote;
        this.mesReferencia = mesReferencia;
    }

    public int getId() { return id; }
    public LocalDate getDataMovimento() { return dataMovimento; }
    public String getTipo() { return tipo; }
    public String getNomeMedicamento() { return nomeMedicamento; }
    public int getQuantidade() { return quantidade; }
    public int getIdLote() { return idLote; }
    public int getMesReferencia() { return mesReferencia; }
}
