package view;

import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import model.HistoricoMovimentacao;
import service.HistoricoService;

public class PainelDashboard extends JPanel {
    private final HistoricoService historicoService;

    private JLabel lblTotalDispensado, lblTaxaFefo, lblTotalRecebido, lblPicoMes;
    private DefaultTableModel modeloTabelaHist;
    private JComboBox<String> cbFiltroMes;
    private GraficoBarrasMensais graficoMensal;
    private GraficoRankingMedicamentos graficoRanking;

    public PainelDashboard(HistoricoService service) {
        this.historicoService = service;
        setLayout(new BorderLayout(14, 14));
        setBackground(UITheme.BG_APP);
        setBorder(new EmptyBorder(16, 20, 16, 20));

        construirInterface();
        atualizarDados();
    }

    private void construirInterface() {
        JPanel painelCards = new JPanel(new GridLayout(1, 4, 14, 0));
        painelCards.setOpaque(false);

        lblTotalDispensado = new JLabel();
        lblTaxaFefo = new JLabel();
        lblTotalRecebido = new JLabel();
        lblPicoMes = new JLabel();

        painelCards.add(criarCardKpi("DISPENSAÇÃO TOTAL", lblTotalDispensado, "Caixas vendidas no período", UITheme.STATUS_SUCCESS));
        painelCards.add(criarCardKpi("CONFORMIDADE FEFO", lblTaxaFefo, "Saída por menor validade", UITheme.BTN_ACCENT));
        painelCards.add(criarCardKpi("TOTAL RECEBIDO", lblTotalRecebido, "Entradas integradas", UITheme.TEXT_TITLE));
        painelCards.add(criarCardKpi("PICO OPERACIONAL", lblPicoMes, "Mês de maior demanda", UITheme.STATUS_WARNING));

        JPanel painelGraficos = new JPanel(new GridLayout(1, 2, 14, 0));
        painelGraficos.setOpaque(false);

        graficoMensal = new GraficoBarrasMensais();
        graficoRanking = new GraficoRankingMedicamentos();

        painelGraficos.add(criarContainerCard("Movimentação Mensal (Jan a Out)", graficoMensal));
        painelGraficos.add(criarContainerCard("Top 5 Medicamentos Dispensados", graficoRanking));

        JPanel painelTabela = criarPainelTabelaAuditoria();

        JPanel painelConteudo = new JPanel();
        painelConteudo.setLayout(new BoxLayout(painelConteudo, BoxLayout.Y_AXIS));
        painelConteudo.setOpaque(false);

        painelCards.setMaximumSize(new Dimension(Integer.MAX_VALUE, 92));
        painelCards.setPreferredSize(new Dimension(1000, 92));
        painelGraficos.setPreferredSize(new Dimension(1000, 260));
        painelTabela.setPreferredSize(new Dimension(1000, 220));

        painelConteudo.add(painelCards);
        painelConteudo.add(Box.createVerticalStrut(14));
        painelConteudo.add(painelGraficos);
        painelConteudo.add(Box.createVerticalStrut(14));
        painelConteudo.add(painelTabela);

        JScrollPane scrollGeral = new JScrollPane(painelConteudo);
        scrollGeral.setBorder(null);
        scrollGeral.setOpaque(false);
        scrollGeral.getViewport().setOpaque(false);
        scrollGeral.getVerticalScrollBar().setUnitIncrement(16);

        add(scrollGeral, BorderLayout.CENTER);
    }

    private JPanel criarCardKpi(String rotulo, JLabel valorLabel, String descricao, Color corDestaque) {
        JPanel card = new JPanel(new BorderLayout(4, 4)) {
            @Override
            protected void paintComponent(Graphics g) {
                UITheme.desenharCardComSombra((Graphics2D) g, 0, 0, getWidth(), getHeight(), 10);
            }
        };
        card.setOpaque(false);
        card.setBorder(new EmptyBorder(12, 16, 12, 16));

        JLabel lblRotulo = new JLabel(rotulo);
        lblRotulo.setFont(UITheme.FONT_CAPTION);
        lblRotulo.setForeground(UITheme.TEXT_MUTED);

        valorLabel.setFont(new Font("Segoe UI", Font.BOLD, 21));
        valorLabel.setForeground(corDestaque);

        JLabel lblDesc = new JLabel(descricao);
        lblDesc.setFont(UITheme.FONT_SMALL);
        lblDesc.setForeground(UITheme.TEXT_MUTED);

        card.add(lblRotulo, BorderLayout.NORTH);
        card.add(valorLabel, BorderLayout.CENTER);
        card.add(lblDesc, BorderLayout.SOUTH);
        return card;
    }

    private JPanel criarContainerCard(String titulo, JComponent conteudo) {
        JPanel container = new JPanel(new BorderLayout(0, 10)) {
            @Override
            protected void paintComponent(Graphics g) {
                UITheme.desenharCardComSombra((Graphics2D) g, 0, 0, getWidth(), getHeight(), 10);
            }
        };
        container.setOpaque(false);
        container.setBorder(new EmptyBorder(14, 16, 14, 16));

        JLabel lblTitulo = new JLabel(titulo);
        lblTitulo.setFont(UITheme.FONT_SUBTITLE);
        lblTitulo.setForeground(UITheme.TEXT_TITLE);

        container.add(lblTitulo, BorderLayout.NORTH);
        container.add(conteudo, BorderLayout.CENTER);
        return container;
    }

    private JPanel criarPainelTabelaAuditoria() {
        JPanel painel = new JPanel(new BorderLayout(0, 10)) {
            @Override
            protected void paintComponent(Graphics g) {
                UITheme.desenharCardComSombra((Graphics2D) g, 0, 0, getWidth(), getHeight(), 10);
            }
        };
        painel.setOpaque(false);
        painel.setBorder(new EmptyBorder(14, 16, 14, 16));

        JPanel topo = new JPanel(new BorderLayout());
        topo.setOpaque(false);

        JLabel lblTit = new JLabel("Histórico de Movimentações");
        lblTit.setFont(UITheme.FONT_SUBTITLE);
        lblTit.setForeground(UITheme.TEXT_TITLE);

        JPanel filtro = new JPanel(new FlowLayout(FlowLayout.RIGHT, 6, 0));
        filtro.setOpaque(false);
        JLabel lblFiltro = new JLabel("Filtrar por Mês:");
        lblFiltro.setFont(UITheme.FONT_BOLD);
        lblFiltro.setForeground(UITheme.TEXT_MUTED);

        cbFiltroMes = new JComboBox<>(new String[]{
            "Todos os Meses", "Janeiro", "Fevereiro", "Março", "Abril", "Maio", 
            "Junho", "Julho", "Agosto", "Setembro", "Outubro"
        });
        cbFiltroMes.setFont(UITheme.FONT_BODY);
        cbFiltroMes.setBackground(Color.WHITE);
        cbFiltroMes.addActionListener(e -> filtrarTabelaHistorico());

        filtro.add(lblFiltro);
        filtro.add(cbFiltroMes);

        topo.add(lblTit, BorderLayout.WEST);
        topo.add(filtro, BorderLayout.EAST);
        painel.add(topo, BorderLayout.NORTH);

        String[] cols = {"Data", "Operação", "Medicamento", "Quantidade", "ID Lote"};
        modeloTabelaHist = new DefaultTableModel(cols, 0) {
            @Override
            public boolean isCellEditable(int r, int c) { return false; }
        };

        JTable tabHist = new JTable(modeloTabelaHist);
        tabHist.setRowHeight(28);
        tabHist.setFont(UITheme.FONT_BODY);
        tabHist.getTableHeader().setFont(UITheme.FONT_BOLD);
        tabHist.getTableHeader().setBackground(new Color(248, 250, 252));
        tabHist.setShowGrid(false);

        DefaultTableCellRenderer centro = new DefaultTableCellRenderer();
        centro.setHorizontalAlignment(SwingConstants.CENTER);
        tabHist.getColumnModel().getColumn(0).setCellRenderer(centro);
        tabHist.getColumnModel().getColumn(1).setCellRenderer(new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int col) {
                JLabel c = (JLabel) super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, col);
                c.setHorizontalAlignment(SwingConstants.CENTER);
                c.setFont(UITheme.FONT_BOLD);
                String v = value != null ? value.toString() : "";
                if (v.contains("SAIDA")) {
                    c.setForeground(UITheme.STATUS_SUCCESS);
                    c.setText("↓ Saída / Venda");
                } else if (v.contains("ENTRADA")) {
                    c.setForeground(UITheme.TEXT_TITLE);
                    c.setText("↑ Recebimento");
                } else {
                    c.setForeground(UITheme.STATUS_DANGER);
                    c.setText("✕ Descarte");
                }
                return c;
            }
        });
        tabHist.getColumnModel().getColumn(3).setCellRenderer(centro);
        tabHist.getColumnModel().getColumn(4).setCellRenderer(centro);

        JScrollPane scroll = new JScrollPane(tabHist);
        scroll.setBorder(new LineBorder(UITheme.BORDER, 1, true));
        scroll.getViewport().setBackground(Color.WHITE);
        painel.add(scroll, BorderLayout.CENTER);

        return painel;
    }

    public void atualizarDados() {
        historicoService.carregarOuGerarHistorico();

        lblTotalDispensado.setText(String.format("%,d un", historicoService.getTotalUnidadesDispensadas()));
        lblTaxaFefo.setText(String.format("%.1f%%", historicoService.getTaxaConformidadeFEFO()));
        lblTotalRecebido.setText(String.format("%,d un", historicoService.getTotalUnidadesRecebidas()));
        lblPicoMes.setText(historicoService.getMesMaiorMovimento());

        filtrarTabelaHistorico();
        if (graficoMensal != null) graficoMensal.iniciarAnimacao();
        if (graficoRanking != null) graficoRanking.iniciarAnimacao();
        repaint();
    }

    private void filtrarTabelaHistorico() {
        modeloTabelaHist.setRowCount(0);
        int idx = cbFiltroMes.getSelectedIndex();
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("dd/MM/yyyy");

        List<HistoricoMovimentacao> lista = historicoService.getHistoricoCompleto();
        for (HistoricoMovimentacao h : lista) {
            if (idx == 0 || h.getMesReferencia() == idx) {
                modeloTabelaHist.addRow(new Object[]{
                    h.getDataMovimento().format(fmt),
                    h.getTipo(),
                    h.getNomeMedicamento(),
                    h.getQuantidade() + " un",
                    "#" + h.getIdLote()
                });
            }
        }
    }

    // ================= GRÁFICO DE BARRAS COM ANIMAÇÃO GSAP POWER2.OUT =================
    private class GraficoBarrasMensais extends JPanel {
        private double progressoAnimacao = 0.0;
        private Timer timerAnimacao;

        public GraficoBarrasMensais() {
            setOpaque(false);
            setPreferredSize(new Dimension(460, 210));
            iniciarAnimacao();
        }

        public void iniciarAnimacao() {
            if (timerAnimacao != null && timerAnimacao.isRunning()) {
                timerAnimacao.stop();
            }
            progressoAnimacao = 0.0;
            final long inicio = System.currentTimeMillis();
            final int duracaoMs = 400; // Curva rápida e responsiva de 400ms

            timerAnimacao = new Timer(16, e -> {
                long decorrido = System.currentTimeMillis() - inicio;
                double linear = (double) decorrido / duracaoMs;
                if (linear >= 1.0) {
                    progressoAnimacao = 1.0;
                    ((Timer) e.getSource()).stop();
                } else {
                    progressoAnimacao = UITheme.easeOutPower2(linear);
                }
                repaint();
            });
            timerAnimacao.start();
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

            int w = getWidth();
            int h = getHeight();
            int margemEsq = 45;
            int margemDir = 15;
            int margemInf = 35;
            int margemSup = 30;

            Map<Integer, Integer> entradas = historicoService.getTotalEntradasPorMes();
            Map<Integer, Integer> saidas = historicoService.getTotalSaidasPorMes();

            int maxVal = 100;
            for (int v : entradas.values()) if (v > maxVal) maxVal = v;
            for (int v : saidas.values()) if (v > maxVal) maxVal = v;
            maxVal = (int) (Math.ceil(maxVal / 100.0) * 100);

            // Grade horizontal
            g2.setFont(UITheme.FONT_CAPTION);
            int numLinhas = 4;
            for (int i = 0; i <= numLinhas; i++) {
                int y = h - margemInf - (i * (h - margemInf - margemSup) / numLinhas);
                g2.setColor(new Color(241, 245, 249));
                g2.drawLine(margemEsq, y, w - margemDir, y);
                int valorLinha = (maxVal * i) / numLinhas;
                g2.setColor(UITheme.TEXT_MUTED);
                g2.drawString(String.valueOf(valorLinha), 12, y + 4);
            }

            // Legenda
            g2.setFont(UITheme.FONT_BOLD);
            g2.setColor(UITheme.TEXT_TITLE);
            g2.fillRect(w - 180, 8, 10, 10);
            g2.setColor(UITheme.TEXT_MUTED);
            g2.drawString("Entradas", w - 165, 17);

            g2.setColor(UITheme.STATUS_SUCCESS);
            g2.fillRect(w - 95, 8, 10, 10);
            g2.setColor(UITheme.TEXT_MUTED);
            g2.drawString("Dispensação", w - 80, 17);

            // Barras com altura escalada por progressoAnimacao
            int areaGraficoW = (w - margemEsq - margemDir);
            int larguraSlot = areaGraficoW / 10;
            int larguraBarra = Math.max(6, (larguraSlot - 10) / 2);

            for (int mes = 1; mes <= 10; mes++) {
                int xBase = margemEsq + (mes - 1) * larguraSlot + (larguraSlot / 2);

                int valEntrada = entradas.getOrDefault(mes, 0);
                int valSaida = saidas.getOrDefault(mes, 0);

                int alturaEntrada = (int) ((((double) valEntrada / maxVal) * (h - margemInf - margemSup)) * progressoAnimacao);
                int alturaSaida = (int) ((((double) valSaida / maxVal) * (h - margemInf - margemSup)) * progressoAnimacao);

                int yEntrada = h - margemInf - alturaEntrada;
                int ySaida = h - margemInf - alturaSaida;

                // Barra Entrada
                g2.setColor(UITheme.TEXT_TITLE);
                g2.fillRoundRect(xBase - larguraBarra - 1, yEntrada, larguraBarra, alturaEntrada, 4, 4);

                // Barra Saída
                g2.setColor(UITheme.STATUS_SUCCESS);
                g2.fillRoundRect(xBase + 1, ySaida, larguraBarra, alturaSaida, 4, 4);

                // Rótulo
                g2.setColor(UITheme.TEXT_MUTED);
                g2.setFont(UITheme.FONT_CAPTION);
                String nomeMes = historicoService.getNomeMesAbreviado(mes);
                int strW = g2.getFontMetrics().stringWidth(nomeMes);
                g2.drawString(nomeMes, xBase - (strW / 2), h - margemInf + 16);
            }

            g2.dispose();
        }
    }

    // ================= RANKING TOP 5 ANIMADO =================
    private class GraficoRankingMedicamentos extends JPanel {
        private double progressoAnimacao = 0.0;
        private Timer timerAnimacao;

        public GraficoRankingMedicamentos() {
            setOpaque(false);
            setPreferredSize(new Dimension(460, 210));
            iniciarAnimacao();
        }

        public void iniciarAnimacao() {
            if (timerAnimacao != null && timerAnimacao.isRunning()) {
                timerAnimacao.stop();
            }
            progressoAnimacao = 0.0;
            final long inicio = System.currentTimeMillis();
            final int duracaoMs = 400;

            timerAnimacao = new Timer(16, e -> {
                long decorrido = System.currentTimeMillis() - inicio;
                double linear = (double) decorrido / duracaoMs;
                if (linear >= 1.0) {
                    progressoAnimacao = 1.0;
                    ((Timer) e.getSource()).stop();
                } else {
                    progressoAnimacao = UITheme.easeOutPower2(linear);
                }
                repaint();
            });
            timerAnimacao.start();
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

            int w = getWidth();
            int h = getHeight();

            Map<String, Integer> top = historicoService.getTopMedicamentosDispensados(5);
            if (top.isEmpty()) {
                g2.dispose();
                return;
            }

            int maxQtd = top.values().stream().max(Integer::compareTo).orElse(1);
            int y = 24;
            int alturaSlot = (h - 30) / Math.max(1, top.size());

            int idx = 1;
            for (Map.Entry<String, Integer> entry : top.entrySet()) {
                String nome = entry.getKey();
                int qtd = entry.getValue();

                g2.setFont(UITheme.FONT_BOLD);
                g2.setColor(UITheme.TEXT_TITLE);
                String labelPos = "#" + idx + " " + nome;
                g2.drawString(labelPos, 14, y);

                g2.setFont(UITheme.FONT_SMALL);
                g2.setColor(UITheme.TEXT_MUTED);
                String labelQtd = qtd + " caixas";
                int qtdW = g2.getFontMetrics().stringWidth(labelQtd);
                g2.drawString(labelQtd, w - qtdW - 14, y);

                int barraY = y + 6;
                int barraMaxW = w - 28;
                int barraW = (int) ((((double) qtd / maxQtd) * barraMaxW) * progressoAnimacao);

                g2.setColor(new Color(241, 245, 249));
                g2.fillRoundRect(14, barraY, barraMaxW, 10, 6, 6);

                g2.setColor(UITheme.STATUS_SUCCESS);
                g2.fillRoundRect(14, barraY, Math.max(6, barraW), 10, 6, 6);

                y += alturaSlot;
                idx++;
            }

            g2.dispose();
        }
    }
}
