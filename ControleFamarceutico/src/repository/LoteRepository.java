package repository;

import database.DatabaseManager;
import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import model.HistoricoMovimentacao;
import model.Lote;

public interface LoteRepository {
    List<Lote> listarTodos();
    void salvar(Lote lote);
    void salvarTodos(List<Lote> lotes);
    void atualizarQuantidade(int idLote, int novaQuantidade);
    void remover(int idLote);
    List<HistoricoMovimentacao> listarHistorico();
    void registrarMovimentacao(HistoricoMovimentacao mov);
    void salvarHistoricoLote(List<HistoricoMovimentacao> historicos);
    String getNomeOrigem();
    boolean isConectadoBanco();

    static LoteRepository obterRepositorio() {
        return Factory.obterRepositorio();
    }

    static LoteRepository tentarReconectar(DatabaseManager.Config cfg) {
        return Factory.tentarReconectar(cfg);
    }

    class Factory {
        private static LoteRepository instancia;

        public static synchronized LoteRepository obterRepositorio() {
            if (instancia == null) {
                DatabaseManager.Config cfg = DatabaseManager.getConfig();
                if (DatabaseManager.isDriverDisponivel() && DatabaseManager.testarConexao(cfg) && DatabaseManager.inicializarBancoDeDados()) {
                    instancia = new MySqlRepo();
                } else {
                    instancia = new InMemoryRepo();
                }
            }
            return instancia;
        }

        public static synchronized LoteRepository tentarReconectar(DatabaseManager.Config cfg) {
            DatabaseManager.setConfig(cfg);
            cfg.salvarConfiguracoes();
            if (DatabaseManager.isDriverDisponivel() && DatabaseManager.testarConexao(cfg) && DatabaseManager.inicializarBancoDeDados()) {
                instancia = new MySqlRepo();
            } else {
                instancia = new InMemoryRepo();
            }
            return instancia;
        }
    }

    class InMemoryRepo implements LoteRepository {
        private final List<Lote> estoque = new ArrayList<>();
        private final List<HistoricoMovimentacao> historico = new ArrayList<>();

        @Override
        public List<Lote> listarTodos() { return new ArrayList<>(estoque); }

        @Override
        public void salvar(Lote lote) {
            remover(lote.getIdLote());
            estoque.add(lote);
        }

        @Override
        public void salvarTodos(List<Lote> lotes) {
            estoque.clear();
            estoque.addAll(lotes);
        }

        @Override
        public void atualizarQuantidade(int idLote, int novaQuantidade) {
            for (int i = 0; i < estoque.size(); i++) {
                Lote l = estoque.get(i);
                if (l.getIdLote() == idLote) {
                    if (novaQuantidade <= 0) {
                        estoque.remove(i);
                    } else {
                        Lote atualizado = new Lote(l.getIdLote(), l.getNomeMedicamento(), 
                            l.getDataFabricacao(), l.getDataValidade(), novaQuantidade, 
                            l.getIdFornecedor(), l.getIdMedicamento());
                        estoque.set(i, atualizado);
                    }
                    break;
                }
            }
        }

        @Override
        public void remover(int idLote) {
            estoque.removeIf(l -> l.getIdLote() == idLote);
        }

        @Override
        public List<HistoricoMovimentacao> listarHistorico() { return new ArrayList<>(historico); }

        @Override
        public void registrarMovimentacao(HistoricoMovimentacao mov) { historico.add(mov); }

        @Override
        public void salvarHistoricoLote(List<HistoricoMovimentacao> historicos) {
            historico.clear();
            historico.addAll(historicos);
        }

        @Override
        public String getNomeOrigem() { return "Modo Offline (Lista Padrão em Memória)"; }

        @Override
        public boolean isConectadoBanco() { return false; }
    }

    class MySqlRepo implements LoteRepository {
        @Override
        public List<Lote> listarTodos() {
            List<Lote> lotes = new ArrayList<>();
            String sql = "SELECT id_lote, nome_medicamento, data_fabricacao, data_validade, quantidade, id_fornecedor, id_medicamento FROM lote";
            try (Connection conn = DatabaseManager.obterConexao();
                 Statement stmt = conn.createStatement();
                 ResultSet rs = stmt.executeQuery(sql)) {

                while (rs.next()) {
                    int id = rs.getInt("id_lote");
                    String nome = rs.getString("nome_medicamento");
                    LocalDate fab = rs.getDate("data_fabricacao").toLocalDate();
                    LocalDate val = rs.getDate("data_validade").toLocalDate();
                    int qtd = rs.getInt("quantidade");
                    int forn = rs.getInt("id_fornecedor");
                    int medId = rs.getInt("id_medicamento");
                    lotes.add(new Lote(id, nome, fab, val, qtd, forn, medId));
                }
            } catch (SQLException ignored) {}
            return lotes;
        }

        @Override
        public void salvar(Lote lote) {
            String sql = "INSERT INTO lote (id_lote, nome_medicamento, data_fabricacao, data_validade, quantidade, id_fornecedor, id_medicamento) " +
                         "VALUES (?, ?, ?, ?, ?, ?, ?) " +
                         "ON DUPLICATE KEY UPDATE nome_medicamento = VALUES(nome_medicamento), " +
                         "data_fabricacao = VALUES(data_fabricacao), data_validade = VALUES(data_validade), " +
                         "quantidade = VALUES(quantidade), id_fornecedor = VALUES(id_fornecedor)";
            try (Connection conn = DatabaseManager.obterConexao();
                 PreparedStatement ps = conn.prepareStatement(sql)) {

                ps.setInt(1, lote.getIdLote());
                ps.setString(2, lote.getNomeMedicamento());
                ps.setDate(3, Date.valueOf(lote.getDataFabricacao()));
                ps.setDate(4, Date.valueOf(lote.getDataValidade()));
                ps.setInt(5, lote.getQuantidade());
                ps.setInt(6, lote.getIdFornecedor());
                ps.setInt(7, lote.getIdMedicamento());
                ps.executeUpdate();
            } catch (SQLException ignored) {}
        }

        @Override
        public void salvarTodos(List<Lote> lotes) {
            String sql = "INSERT INTO lote (id_lote, nome_medicamento, data_fabricacao, data_validade, quantidade, id_fornecedor, id_medicamento) " +
                         "VALUES (?, ?, ?, ?, ?, ?, ?) " +
                         "ON DUPLICATE KEY UPDATE quantidade = VALUES(quantidade)";
            try (Connection conn = DatabaseManager.obterConexao();
                 PreparedStatement ps = conn.prepareStatement(sql)) {

                conn.setAutoCommit(false);
                for (Lote l : lotes) {
                    ps.setInt(1, l.getIdLote());
                    ps.setString(2, l.getNomeMedicamento());
                    ps.setDate(3, Date.valueOf(l.getDataFabricacao()));
                    ps.setDate(4, Date.valueOf(l.getDataValidade()));
                    ps.setInt(5, l.getQuantidade());
                    ps.setInt(6, l.getIdFornecedor());
                    ps.setInt(7, l.getIdMedicamento());
                    ps.addBatch();
                }
                ps.executeBatch();
                conn.commit();
                conn.setAutoCommit(true);
            } catch (SQLException ignored) {}
        }

        @Override
        public void atualizarQuantidade(int idLote, int novaQuantidade) {
            try (Connection conn = DatabaseManager.obterConexao()) {
                if (novaQuantidade <= 0) {
                    try (PreparedStatement ps = conn.prepareStatement("DELETE FROM lote WHERE id_lote = ?")) {
                        ps.setInt(1, idLote);
                        ps.executeUpdate();
                    }
                } else {
                    try (PreparedStatement ps = conn.prepareStatement("UPDATE lote SET quantidade = ? WHERE id_lote = ?")) {
                        ps.setInt(1, novaQuantidade);
                        ps.setInt(2, idLote);
                        ps.executeUpdate();
                    }
                }
            } catch (SQLException ignored) {}
        }

        @Override
        public void remover(int idLote) {
            try (Connection conn = DatabaseManager.obterConexao();
                 PreparedStatement ps = conn.prepareStatement("DELETE FROM lote WHERE id_lote = ?")) {
                ps.setInt(1, idLote);
                ps.executeUpdate();
            } catch (SQLException ignored) {}
        }

        @Override
        public List<HistoricoMovimentacao> listarHistorico() {
            List<HistoricoMovimentacao> lista = new ArrayList<>();
            String sql = "SELECT id, data_movimento, tipo, nome_medicamento, quantidade, id_lote, mes_referencia FROM historico_movimentacao ORDER BY data_movimento ASC";
            try (Connection conn = DatabaseManager.obterConexao();
                 Statement stmt = conn.createStatement();
                 ResultSet rs = stmt.executeQuery(sql)) {

                while (rs.next()) {
                    int id = rs.getInt("id");
                    LocalDate data = rs.getDate("data_movimento").toLocalDate();
                    String tipo = rs.getString("tipo");
                    String nome = rs.getString("nome_medicamento");
                    int qtd = rs.getInt("quantidade");
                    int idLote = rs.getInt("id_lote");
                    int mes = rs.getInt("mes_referencia");
                    lista.add(new HistoricoMovimentacao(id, data, tipo, nome, qtd, idLote, mes));
                }
            } catch (SQLException ignored) {}
            return lista;
        }

        @Override
        public void registrarMovimentacao(HistoricoMovimentacao mov) {
            String sql = "INSERT INTO historico_movimentacao (data_movimento, tipo, nome_medicamento, quantidade, id_lote, mes_referencia) VALUES (?, ?, ?, ?, ?, ?)";
            try (Connection conn = DatabaseManager.obterConexao();
                 PreparedStatement ps = conn.prepareStatement(sql)) {

                ps.setDate(1, Date.valueOf(mov.getDataMovimento()));
                ps.setString(2, mov.getTipo());
                ps.setString(3, mov.getNomeMedicamento());
                ps.setInt(4, mov.getQuantidade());
                ps.setInt(5, mov.getIdLote());
                ps.setInt(6, mov.getMesReferencia());
                ps.executeUpdate();
            } catch (SQLException ignored) {}
        }

        @Override
        public void salvarHistoricoLote(List<HistoricoMovimentacao> historicos) {
            String sql = "INSERT INTO historico_movimentacao (data_movimento, tipo, nome_medicamento, quantidade, id_lote, mes_referencia) VALUES (?, ?, ?, ?, ?, ?)";
            try (Connection conn = DatabaseManager.obterConexao();
                 PreparedStatement ps = conn.prepareStatement(sql)) {

                conn.setAutoCommit(false);
                for (HistoricoMovimentacao mov : historicos) {
                    ps.setDate(1, Date.valueOf(mov.getDataMovimento()));
                    ps.setString(2, mov.getTipo());
                    ps.setString(3, mov.getNomeMedicamento());
                    ps.setInt(4, mov.getQuantidade());
                    ps.setInt(5, mov.getIdLote());
                    ps.setInt(6, mov.getMesReferencia());
                    ps.addBatch();
                }
                ps.executeBatch();
                conn.commit();
                conn.setAutoCommit(true);
            } catch (SQLException ignored) {}
        }

        @Override
        public String getNomeOrigem() {
            return "MySQL (" + DatabaseManager.getConfig().getHost() + ":" + DatabaseManager.getConfig().getPorta() + "/" + DatabaseManager.getConfig().getBanco() + ")";
        }

        @Override
        public boolean isConectadoBanco() { return true; }
    }
}
