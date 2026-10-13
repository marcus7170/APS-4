package database;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Properties;

public class DatabaseManager {

    public static class Config {
        private String host = "127.0.0.1";
        private int porta = 3306;
        private String banco = "controle_farmaceutico";
        private String usuario = "root";
        private String senha = "";

        public Config() {
            carregarConfiguracoes();
        }

        public void carregarConfiguracoes() {
            File file = new File("db.properties");
            if (file.exists()) {
                Properties prop = new Properties();
                try (FileInputStream fis = new FileInputStream(file)) {
                    prop.load(fis);
                    this.host = prop.getProperty("db.host", "127.0.0.1");
                    this.porta = Integer.parseInt(prop.getProperty("db.porta", "3306"));
                    this.banco = prop.getProperty("db.banco", "controle_farmaceutico");
                    this.usuario = prop.getProperty("db.usuario", "root");
                    this.senha = prop.getProperty("db.senha", "");
                } catch (Exception ignored) {}
            }
        }

        public void salvarConfiguracoes() {
            Properties prop = new Properties();
            prop.setProperty("db.host", this.host);
            prop.setProperty("db.porta", String.valueOf(this.porta));
            prop.setProperty("db.banco", this.banco);
            prop.setProperty("db.usuario", this.usuario);
            prop.setProperty("db.senha", this.senha);

            try (FileOutputStream fos = new FileOutputStream("db.properties")) {
                prop.store(fos, "Configuracoes - PharmaStock");
            } catch (IOException ignored) {}
        }

        public String getHost() { return host; }
        public void setHost(String host) { this.host = host; }
        public int getPorta() { return porta; }
        public void setPorta(int porta) { this.porta = porta; }
        public String getBanco() { return banco; }
        public void setBanco(String banco) { this.banco = banco; }
        public String getUsuario() { return usuario; }
        public void setUsuario(String usuario) { this.usuario = usuario; }
        public String getSenha() { return senha; }
        public void setSenha(String senha) { this.senha = senha; }

        public String getUrlSemBanco() {
            return "jdbc:mysql://" + host + ":" + porta + "/?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC&connectTimeout=3000";
        }

        public String getUrlComBanco() {
            return "jdbc:mysql://" + host + ":" + porta + "/" + banco + "?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC&connectTimeout=3000";
        }
    }

    private static Config config = new Config();
    private static boolean driverDisponivel = false;

    static {
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
            driverDisponivel = true;
        } catch (ClassNotFoundException e) {
            driverDisponivel = false;
        }
    }

    public static boolean isDriverDisponivel() {
        return driverDisponivel;
    }

    public static Config getConfig() {
        return config;
    }

    public static void setConfig(Config novaConfig) {
        config = novaConfig;
    }

    public static boolean testarConexao(Config cfg) {
        if (!driverDisponivel) return false;
        try (Connection conn = DriverManager.getConnection(cfg.getUrlSemBanco(), cfg.getUsuario(), cfg.getSenha())) {
            return conn != null && !conn.isClosed();
        } catch (SQLException e) {
            return false;
        }
    }

    public static Connection obterConexao() throws SQLException {
        if (!driverDisponivel) {
            throw new SQLException("Driver MySQL nao disponivel.");
        }
        return DriverManager.getConnection(config.getUrlComBanco(), config.getUsuario(), config.getSenha());
    }

    public static boolean inicializarBancoDeDados() {
        if (!driverDisponivel) return false;

        try (Connection connServidor = DriverManager.getConnection(config.getUrlSemBanco(), config.getUsuario(), config.getSenha());
             Statement stmt = connServidor.createStatement()) {

            stmt.executeUpdate("CREATE DATABASE IF NOT EXISTS " + config.getBanco() 
                + " CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci");
        } catch (SQLException e) {
            return false;
        }

        try (Connection connBanco = obterConexao();
             Statement stmt = connBanco.createStatement()) {

            stmt.executeUpdate("CREATE TABLE IF NOT EXISTS fornecedor (" +
                "id_fornecedor INT PRIMARY KEY, " +
                "cnpj VARCHAR(25), " +
                "nome VARCHAR(120), " +
                "telefone VARCHAR(30), " +
                "email VARCHAR(100), " +
                "endereco VARCHAR(200)" +
                ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4");

            stmt.executeUpdate("CREATE TABLE IF NOT EXISTS medicamento (" +
                "id_medicamento INT PRIMARY KEY, " +
                "nome VARCHAR(150), " +
                "preco DOUBLE, " +
                "quantidade_estoque INT, " +
                "tipo_medicamento VARCHAR(50)" +
                ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4");

            stmt.executeUpdate("CREATE TABLE IF NOT EXISTS lote (" +
                "id_lote INT PRIMARY KEY, " +
                "nome_medicamento VARCHAR(150), " +
                "data_fabricacao DATE, " +
                "data_validade DATE, " +
                "quantidade INT, " +
                "id_fornecedor INT, " +
                "id_medicamento INT" +
                ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4");

            stmt.executeUpdate("CREATE TABLE IF NOT EXISTS historico_movimentacao (" +
                "id INT AUTO_INCREMENT PRIMARY KEY, " +
                "data_movimento DATE, " +
                "tipo VARCHAR(30), " +
                "nome_medicamento VARCHAR(150), " +
                "quantidade INT, " +
                "id_lote INT, " +
                "mes_referencia INT" +
                ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4");

            return true;
        } catch (SQLException e) {
            return false;
        }
    }

    public static int contarLotesBanco() {
        try (Connection conn = obterConexao();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT COUNT(*) FROM lote")) {
            if (rs.next()) return rs.getInt(1);
        } catch (SQLException ignored) {}
        return 0;
    }
}
