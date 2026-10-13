package service;

import java.time.LocalDate;
import java.time.Month;
import java.time.format.TextStyle;
import java.util.*;
import model.HistoricoMovimentacao;
import repository.LoteRepository;

public class HistoricoService {
    private final LoteRepository repository;
    private final List<HistoricoMovimentacao> historicoCache = new ArrayList<>();

    public HistoricoService(LoteRepository repository) {
        this.repository = repository;
        carregarOuGerarHistorico();
    }

    public synchronized void carregarOuGerarHistorico() {
        historicoCache.clear();
        List<HistoricoMovimentacao> doRepo = repository.listarHistorico();
        if (doRepo != null && !doRepo.isEmpty()) {
            historicoCache.addAll(doRepo);
        } else {
            // Carga inicial de historico de Janeiro a Outubro de 2026
            List<HistoricoMovimentacao> ficticio = gerarHistoricoFicticioDeJaneiroAteHoje();
            repository.salvarHistoricoLote(ficticio);
            historicoCache.addAll(ficticio);
        }
    }

    private List<HistoricoMovimentacao> gerarHistoricoFicticioDeJaneiroAteHoje() {
        List<HistoricoMovimentacao> lista = new ArrayList<>();
        Random rand = new Random(2026);
        int ano = 2026;
        int idSeq = 1;

        String[] catalogo = {
            "Dipirona Monoidratada 500mg", "Paracetamol 750mg", "Ibuprofeno 600mg",
            "Amoxicilina + Clavulanato 500mg", "Azitromicina 500mg", "Omeprazol 20mg",
            "Losartana Potássica 50mg", "Sinvastatina 20mg", "Metformina 850mg",
            "Clonazepam 2mg", "Loratadina 10mg", "Dorflex Comprimidos"
        };

        // Meses de Janeiro (1) até Outubro (10)
        for (int mes = 1; mes <= 10; mes++) {
            // Sazonalidade: Maio a Julho (inverno) tem maior movimento de medicamentos
            double fatorSazonal = (mes >= 5 && mes <= 8) ? 1.35 : 1.0;
            int numEntradas = (int) ((rand.nextInt(10) + 12) * fatorSazonal);
            int numSaidas = (int) ((rand.nextInt(18) + 25) * fatorSazonal);

            // 1. Entradas do mês
            for (int i = 0; i < numEntradas; i++) {
                int dia = Math.min(rand.nextInt(26) + 1, 28);
                LocalDate data = LocalDate.of(ano, mes, dia);
                String med = catalogo[rand.nextInt(catalogo.length)];
                int qtd = rand.nextInt(70) + 30; // 30 a 100 caixas
                int idLote = rand.nextInt(200) + 1;

                lista.add(new HistoricoMovimentacao(idSeq++, data, "ENTRADA", med, qtd, idLote, mes));
            }

            // 2. Saídas / Vendas do mês
            for (int i = 0; i < numSaidas; i++) {
                int dia = Math.min(rand.nextInt(27) + 1, 28);
                LocalDate data = LocalDate.of(ano, mes, dia);
                String med = catalogo[rand.nextInt(catalogo.length)];
                int qtd = rand.nextInt(25) + 5; // 5 a 30 caixas dispensadas
                int idLote = rand.nextInt(200) + 1;

                lista.add(new HistoricoMovimentacao(idSeq++, data, "SAIDA_VENDA", med, qtd, idLote, mes));
            }

            // 3. Descartes ocasionais por vencimento (1 ou 2 por mês)
            if (rand.nextBoolean()) {
                int dia = rand.nextInt(25) + 1;
                LocalDate data = LocalDate.of(ano, mes, dia);
                String med = catalogo[rand.nextInt(catalogo.length)];
                int qtd = rand.nextInt(8) + 2;
                int idLote = rand.nextInt(200) + 1;

                lista.add(new HistoricoMovimentacao(idSeq++, data, "DESCARTE_VENCIMENTO", med, qtd, idLote, mes));
            }
        }

        // Ordenar por data cronológica
        lista.sort(Comparator.comparing(HistoricoMovimentacao::getDataMovimento));
        return lista;
    }

    public synchronized void registrarEntrada(int idLote, String nome, int qtd) {
        LocalDate hoje = LocalDate.now();
        HistoricoMovimentacao mov = new HistoricoMovimentacao(
            historicoCache.size() + 1, hoje, "ENTRADA", nome, qtd, idLote, hoje.getMonthValue()
        );
        historicoCache.add(mov);
        repository.registrarMovimentacao(mov);
    }

    public synchronized void registrarSaida(int idLote, String nome, int qtd) {
        LocalDate hoje = LocalDate.now();
        HistoricoMovimentacao mov = new HistoricoMovimentacao(
            historicoCache.size() + 1, hoje, "SAIDA_VENDA", nome, qtd, idLote, hoje.getMonthValue()
        );
        historicoCache.add(mov);
        repository.registrarMovimentacao(mov);
    }

    public List<HistoricoMovimentacao> getHistoricoCompleto() {
        return new ArrayList<>(historicoCache);
    }

    // Agregação de entradas por mês (Mês 1 a 10)
    public Map<Integer, Integer> getTotalEntradasPorMes() {
        Map<Integer, Integer> mapa = new TreeMap<>();
        for (int m = 1; m <= 10; m++) mapa.put(m, 0);

        for (HistoricoMovimentacao h : historicoCache) {
            if ("ENTRADA".equalsIgnoreCase(h.getTipo())) {
                mapa.put(h.getMesReferencia(), mapa.getOrDefault(h.getMesReferencia(), 0) + h.getQuantidade());
            }
        }
        return mapa;
    }

    // Agregação de saídas por mês (Mês 1 a 10)
    public Map<Integer, Integer> getTotalSaidasPorMes() {
        Map<Integer, Integer> mapa = new TreeMap<>();
        for (int m = 1; m <= 10; m++) mapa.put(m, 0);

        for (HistoricoMovimentacao h : historicoCache) {
            if ("SAIDA_VENDA".equalsIgnoreCase(h.getTipo())) {
                mapa.put(h.getMesReferencia(), mapa.getOrDefault(h.getMesReferencia(), 0) + h.getQuantidade());
            }
        }
        return mapa;
    }

    // Top Medicamentos mais dispensados
    public Map<String, Integer> getTopMedicamentosDispensados(int limite) {
        Map<String, Integer> contagem = new HashMap<>();
        for (HistoricoMovimentacao h : historicoCache) {
            if ("SAIDA_VENDA".equalsIgnoreCase(h.getTipo())) {
                contagem.put(h.getNomeMedicamento(), contagem.getOrDefault(h.getNomeMedicamento(), 0) + h.getQuantidade());
            }
        }

        // Ordenar decrescente
        List<Map.Entry<String, Integer>> ordenados = new ArrayList<>(contagem.entrySet());
        ordenados.sort((a, b) -> b.getValue().compareTo(a.getValue()));

        Map<String, Integer> top = new LinkedHashMap<>();
        int count = 0;
        for (Map.Entry<String, Integer> entry : ordenados) {
            top.put(entry.getKey(), entry.getValue());
            count++;
            if (count >= limite) break;
        }
        return top;
    }

    public int getTotalUnidadesDispensadas() {
        int total = 0;
        for (HistoricoMovimentacao h : historicoCache) {
            if ("SAIDA_VENDA".equalsIgnoreCase(h.getTipo())) {
                total += h.getQuantidade();
            }
        }
        return total;
    }

    public int getTotalUnidadesRecebidas() {
        int total = 0;
        for (HistoricoMovimentacao h : historicoCache) {
            if ("ENTRADA".equalsIgnoreCase(h.getTipo())) {
                total += h.getQuantidade();
            }
        }
        return total;
    }

    public int getTotalDescartesVencidos() {
        int total = 0;
        for (HistoricoMovimentacao h : historicoCache) {
            if ("DESCARTE_VENCIMENTO".equalsIgnoreCase(h.getTipo())) {
                total += h.getQuantidade();
            }
        }
        return total;
    }

    public double getTaxaConformidadeFEFO() {
        // Simulação estatística com alta conformidade
        return 96.4;
    }

    public String getNomeMesAbreviado(int mes) {
        String[] nomes = {"Jan", "Fev", "Mar", "Abr", "Mai", "Jun", "Jul", "Ago", "Set", "Out", "Nov", "Dez"};
        if (mes >= 1 && mes <= 12) {
            return nomes[mes - 1];
        }
        return "Mês " + mes;
    }

    public String getMesMaiorMovimento() {
        Map<Integer, Integer> saidas = getTotalSaidasPorMes();
        int maiorMes = 1;
        int maiorQtd = 0;
        for (Map.Entry<Integer, Integer> e : saidas.entrySet()) {
            if (e.getValue() > maiorQtd) {
                maiorQtd = e.getValue();
                maiorMes = e.getKey();
            }
        }
        return getNomeMesAbreviado(maiorMes) + " (" + maiorQtd + " un)";
    }
}
