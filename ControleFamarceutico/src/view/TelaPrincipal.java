package view;

import database.DatabaseManager;
import java.awt.*;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import model.Lote;
import repository.LoteRepository;
import service.HistoricoService;
import sorting.AlgoritmosOrdenacao;

public class TelaPrincipal extends JFrame {
    private LoteRepository repositorio;
    private HistoricoService historicoService;
    private final List<Lote> estoqueLotes = new ArrayList<>();

    private DefaultTableModel modeloTabela;
    private JTable tabela;
    private JLabel lblTotalLotes, lblTotalCaixas, lblStatus, lblBadgeBD;

    private JTextField txtNome, txtQtd, txtValidade, txtFornecedor;
    private JComboBox<String> cbTipo;
    private JTextField txtBuscaBalcao;

    private PainelDashboard painelDashboard;
    private JTabbedPane abasPrincipais;

    private final DateTimeFormatter formatoData = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    public TelaPrincipal() {
        setTitle("PharmaStock • Gestão Farmacêutica");
        setSize(1280, 820);
        setMinimumSize(new Dimension(1080, 720));
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        getContentPane().setBackground(UITheme.BG_APP);
        setLayout(new BorderLayout(14, 14));

        this.repositorio = LoteRepository.obterRepositorio();
        this.historicoService = new HistoricoService(repositorio);

        criarCabecalho();
        criarSistemaAbas();
        criarRodapeStatus();

        carregarDadosIniciais();
        atualizarBadgeStatusBD();
    }

    private void criarCabecalho() {
        JPanel painelTopo = new JPanel(new BorderLayout());
        painelTopo.setBackground(UITheme.BG_CARD);
        painelTopo.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createMatteBorder(0, 0, 1, 0, UITheme.BORDER),
            new EmptyBorder(14, 24, 14, 24)
        ));

        JPanel painelTitulos = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        painelTitulos.setOpaque(false);

        JLabel lblTitulo = new JLabel("PharmaStock • Gestão Farmacêutica");
        lblTitulo.setFont(UITheme.FONT_TITLE);
        lblTitulo.setForeground(UITheme.TEXT_TITLE);
        painelTitulos.add(lblTitulo);

        JPanel painelCards = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        painelCards.setOpaque(false);

        JButton btnConfigBD = new BotaoAnimado("Configurar Conexão", UITheme.BTN_PRIMARY, UITheme.BTN_PRIMARY_HOVER);
        btnConfigBD.setPreferredSize(new Dimension(160, 36));
        btnConfigBD.addActionListener(e -> abrirModalConfigBD());

        lblTotalLotes = criarCardIndicador("Lotes Ativos", "0");
        lblTotalCaixas = criarCardIndicador("Unidades Totais", "0");

        painelCards.add(btnConfigBD);
        painelCards.add(lblTotalLotes);
        painelCards.add(lblTotalCaixas);

        painelTopo.add(painelTitulos, BorderLayout.WEST);
        painelTopo.add(painelCards, BorderLayout.EAST);
        add(painelTopo, BorderLayout.NORTH);
    }

    private JLabel criarCardIndicador(String titulo, String valorInicial) {
        JLabel lbl = new JLabel("<html><div style='text-align:right;'><span style='font-size:10px; color:#64748B; font-weight:600;'>" 
                + titulo.toUpperCase() + "</span><br><span style='font-size:15px; color:#0F172A; font-weight:700;'>" 
                + valorInicial + "</span></div></html>");
        lbl.setFont(UITheme.FONT_BODY);
        lbl.setBorder(BorderFactory.createCompoundBorder(
            new LineBorder(UITheme.BORDER, 1, true),
            new EmptyBorder(6, 14, 6, 14)
        ));
        lbl.setBackground(new Color(250, 250, 250));
        lbl.setOpaque(true);
        return lbl;
    }

    private void criarSistemaAbas() {
        abasPrincipais = new JTabbedPane();
        abasPrincipais.setFont(UITheme.FONT_BOLD);
        abasPrincipais.setBackground(UITheme.BG_APP);
        abasPrincipais.setForeground(UITheme.TEXT_TITLE);

        JPanel painelEstoque = new JPanel(new BorderLayout(14, 14));
        painelEstoque.setOpaque(false);
        painelEstoque.setBorder(new EmptyBorder(10, 14, 0, 14));
        painelEstoque.add(criarPainelEsquerdoCadastro(), BorderLayout.WEST);
        painelEstoque.add(criarPainelCentralTabela(), BorderLayout.CENTER);

        painelDashboard = new PainelDashboard(historicoService);
        JPanel painelBenchmark = criarPainelBenchmarkEmbutido();

        abasPrincipais.addTab("Estoque & FEFO", painelEstoque);
        abasPrincipais.addTab("Dashboard & Histórico", painelDashboard);
        abasPrincipais.addTab("Benchmark de Algoritmos", painelBenchmark);

        abasPrincipais.addChangeListener(e -> {
            if (abasPrincipais.getSelectedIndex() == 1 && painelDashboard != null) {
                painelDashboard.atualizarDados();
            }
        });

        add(abasPrincipais, BorderLayout.CENTER);
    }

    private JPanel criarPainelEsquerdoCadastro() {
        JPanel painelOeste = new JPanel();
        painelOeste.setLayout(new BoxLayout(painelOeste, BoxLayout.Y_AXIS));
        painelOeste.setOpaque(false);
        painelOeste.setPreferredSize(new Dimension(290, 0));

        JPanel form = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                UITheme.desenharCardComSombra((Graphics2D) g, 0, 0, getWidth(), getHeight(), 10);
            }
        };
        form.setOpaque(false);
        form.setLayout(new BoxLayout(form, BoxLayout.Y_AXIS));
        form.setBorder(new EmptyBorder(18, 18, 18, 18));

        JLabel lblForm = new JLabel("Recebimento de Lote");
        lblForm.setFont(UITheme.FONT_SUBTITLE);
        lblForm.setForeground(UITheme.TEXT_TITLE);
        lblForm.setAlignmentX(Component.LEFT_ALIGNMENT);
        form.add(lblForm);
        form.add(Box.createVerticalStrut(12));

        txtNome = adicionarCampoFormatado(form, "Medicamento / Substância:");
        cbTipo = new JComboBox<>(new String[]{"Analgésico", "Antibiótico", "Anti-inflamatório", "Uso Contínuo", "Controlado"});
        estilizarComboBox(cbTipo);
        adicionarComponenteFormatado(form, "Categoria Farmacológica:", cbTipo);

        txtQtd = adicionarCampoFormatado(form, "Quantidade (Caixas):");
        txtValidade = adicionarCampoFormatado(form, "Data de Validade (DD/MM/AAAA):");
        txtValidade.setText(LocalDate.now().plusMonths(12).format(formatoData));
        txtFornecedor = adicionarCampoFormatado(form, "Identificador Fornecedor:");
        txtFornecedor.setText("1");

        form.add(Box.createVerticalStrut(12));
        
        JButton btnSalvar = new BotaoAnimado("Cadastrar Lote", UITheme.BTN_PRIMARY, UITheme.BTN_PRIMARY_HOVER);
        btnSalvar.setAlignmentX(Component.LEFT_ALIGNMENT);
        btnSalvar.setMaximumSize(new Dimension(Integer.MAX_VALUE, 38));
        btnSalvar.setPreferredSize(new Dimension(240, 38));
        btnSalvar.addActionListener(e -> registrarLoteManual());
        form.add(btnSalvar);

        painelOeste.add(form);
        painelOeste.add(Box.createVerticalGlue());
        return painelOeste;
    }

    private JTextField adicionarCampoFormatado(JPanel painel, String rotulo) {
        JLabel lbl = new JLabel(rotulo);
        lbl.setFont(UITheme.FONT_BOLD);
        lbl.setForeground(UITheme.TEXT_BODY);
        lbl.setAlignmentX(Component.LEFT_ALIGNMENT);
        painel.add(lbl);
        painel.add(Box.createVerticalStrut(4));

        JTextField campo = new JTextField();
        campo.setMaximumSize(new Dimension(Integer.MAX_VALUE, 32));
        campo.setPreferredSize(new Dimension(240, 32));
        UITheme.aplicarEstiloInput(campo);
        campo.setAlignmentX(Component.LEFT_ALIGNMENT);
        painel.add(campo);
        painel.add(Box.createVerticalStrut(8));
        return campo;
    }

    private void adicionarComponenteFormatado(JPanel painel, String rotulo, JComponent comp) {
        JLabel lbl = new JLabel(rotulo);
        lbl.setFont(UITheme.FONT_BOLD);
        lbl.setForeground(UITheme.TEXT_BODY);
        lbl.setAlignmentX(Component.LEFT_ALIGNMENT);
        painel.add(lbl);
        painel.add(Box.createVerticalStrut(4));

        comp.setMaximumSize(new Dimension(Integer.MAX_VALUE, 32));
        comp.setPreferredSize(new Dimension(240, 32));
        comp.setAlignmentX(Component.LEFT_ALIGNMENT);
        painel.add(comp);
        painel.add(Box.createVerticalStrut(8));
    }

    private void estilizarComboBox(JComboBox<?> box) {
        box.setFont(UITheme.FONT_BODY);
        box.setBackground(Color.WHITE);
        box.setBorder(new LineBorder(UITheme.BORDER, 1, true));
    }

    private JPanel criarPainelCentralTabela() {
        JPanel painelCentral = new JPanel(new BorderLayout(0, 10));
        painelCentral.setOpaque(false);

        JPanel barraAcoes = new JPanel(new BorderLayout(12, 0)) {
            @Override
            protected void paintComponent(Graphics g) {
                UITheme.desenharCardComSombra((Graphics2D) g, 0, 0, getWidth(), getHeight(), 8);
            }
        };
        barraAcoes.setOpaque(false);
        barraAcoes.setBorder(new EmptyBorder(8, 12, 8, 12));

        JPanel painelBusca = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        painelBusca.setOpaque(false);
        JLabel lblBusca = new JLabel("Localizar:");
        lblBusca.setFont(UITheme.FONT_BOLD);
        lblBusca.setForeground(UITheme.TEXT_MUTED);

        txtBuscaBalcao = new JTextField(18);
        txtBuscaBalcao.setPreferredSize(new Dimension(220, 32));
        UITheme.aplicarEstiloInput(txtBuscaBalcao);
        txtBuscaBalcao.addKeyListener(new KeyAdapter() {
            @Override
            public void keyReleased(KeyEvent e) {
                filtrarTabela(txtBuscaBalcao.getText());
            }
        });

        painelBusca.add(lblBusca);
        painelBusca.add(txtBuscaBalcao);

        JPanel painelBotoes = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        painelBotoes.setOpaque(false);

        JButton btnImportar = new BotaoAnimado("Importar CSV", UITheme.BTN_PRIMARY, UITheme.BTN_PRIMARY_HOVER);
        JButton btnExportar = new BotaoAnimado("Exportar CSV", UITheme.BTN_PRIMARY, UITheme.BTN_PRIMARY_HOVER);
        JButton btnOrdenar = new BotaoAnimado("Priorizar FEFO", UITheme.BTN_PRIMARY, UITheme.BTN_PRIMARY_HOVER);

        btnImportar.addActionListener(e -> importarCSV());
        btnExportar.addActionListener(e -> exportarCSV());
        btnOrdenar.addActionListener(e -> aplicarOrdenacaoFEFO());

        painelBotoes.add(btnImportar);
        painelBotoes.add(btnExportar);
        painelBotoes.add(btnOrdenar);

        barraAcoes.add(painelBusca, BorderLayout.WEST);
        barraAcoes.add(painelBotoes, BorderLayout.EAST);
        painelCentral.add(barraAcoes, BorderLayout.NORTH);

        String[] colunas = {"ID Lote", "Medicamento", "Fabricação", "Validade (FEFO)", "Quantidade", "Fornecedor", "Status Sanitário"};
        modeloTabela = new DefaultTableModel(colunas, 0) {
            @Override
            public boolean isCellEditable(int r, int c) { return false; }
        };

        tabela = new JTable(modeloTabela);
        tabela.setRowHeight(32);
        tabela.setFont(UITheme.FONT_BODY);
        tabela.getTableHeader().setFont(UITheme.FONT_BOLD);
        tabela.getTableHeader().setBackground(new Color(248, 250, 252));
        tabela.getTableHeader().setForeground(UITheme.TEXT_BODY);
        tabela.getTableHeader().setPreferredSize(new Dimension(0, 34));
        tabela.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        tabela.setSelectionBackground(new Color(241, 245, 249));
        tabela.setSelectionForeground(Color.BLACK);
        tabela.setShowGrid(false);

        DefaultTableCellRenderer alinhamentoCentro = new DefaultTableCellRenderer();
        alinhamentoCentro.setHorizontalAlignment(SwingConstants.CENTER);
        tabela.getColumnModel().getColumn(0).setCellRenderer(alinhamentoCentro);
        tabela.getColumnModel().getColumn(2).setCellRenderer(alinhamentoCentro);
        tabela.getColumnModel().getColumn(3).setCellRenderer(alinhamentoCentro);
        tabela.getColumnModel().getColumn(4).setCellRenderer(alinhamentoCentro);
        tabela.getColumnModel().getColumn(5).setCellRenderer(alinhamentoCentro);

        tabela.getColumnModel().getColumn(6).setCellRenderer(new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int col) {
                JLabel c = (JLabel) super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, col);
                c.setHorizontalAlignment(SwingConstants.CENTER);
                c.setFont(UITheme.FONT_BOLD);
                String status = value != null ? value.toString() : "";
                
                if (status.contains("VENCIDO")) {
                    c.setForeground(UITheme.STATUS_DANGER);
                    c.setText("● Vencido");
                } else if (status.contains("breve")) {
                    c.setForeground(UITheme.STATUS_WARNING);
                    c.setText("▲ Vence em Breve");
                } else {
                    c.setForeground(UITheme.STATUS_SUCCESS);
                    c.setText("✔ Regular");
                }
                return c;
            }
        });

        JScrollPane scroll = new JScrollPane(tabela);
        scroll.setBorder(new LineBorder(UITheme.BORDER, 1, true));
        scroll.getViewport().setBackground(Color.WHITE);
        painelCentral.add(scroll, BorderLayout.CENTER);

        return painelCentral;
    }

    private JPanel criarPainelBenchmarkEmbutido() {
        JPanel painel = new JPanel(new BorderLayout(14, 14));
        painel.setOpaque(false);
        painel.setBorder(new EmptyBorder(16, 18, 16, 18));

        JPanel topo = new JPanel(new FlowLayout(FlowLayout.LEFT, 12, 10)) {
            @Override
            protected void paintComponent(Graphics g) {
                UITheme.desenharCardComSombra((Graphics2D) g, 0, 0, getWidth(), getHeight(), 8);
            }
        };
        topo.setOpaque(false);
        topo.setBorder(new EmptyBorder(8, 12, 8, 12));

        topo.add(new JLabel("Volume de Lotes Sintéticos:"));
        JComboBox<Integer> cbQtd = new JComboBox<>(new Integer[]{1000, 5000, 10000, 30000, 50000});
        estilizarComboBox(cbQtd);
        
        JButton btnExecutar = new BotaoAnimado("Executar Testes", UITheme.BTN_PRIMARY, UITheme.BTN_PRIMARY_HOVER);
        topo.add(cbQtd);
        topo.add(btnExecutar);
        painel.add(topo, BorderLayout.NORTH);

        String[] cols = {"Algoritmo", "Complexidade Assintótica", "Tempo de Execução (ms)"};
        DefaultTableModel modelBench = new DefaultTableModel(cols, 0) {
            @Override
            public boolean isCellEditable(int r, int c) { return false; }
        };

        JTable tabBench = new JTable(modelBench);
        tabBench.setRowHeight(34);
        tabBench.setFont(UITheme.FONT_BODY);
        tabBench.getTableHeader().setFont(UITheme.FONT_BOLD);
        tabBench.getTableHeader().setBackground(new Color(248, 250, 252));
        tabBench.setShowGrid(false);

        painel.add(new JScrollPane(tabBench), BorderLayout.CENTER);

        JLabel lblAviso = new JLabel("<html><i>Medição via System.nanoTime() em memória primária. QuickSort otimizado com pivô Mediana de Três.</i></html>");
        lblAviso.setBorder(new EmptyBorder(6, 12, 6, 12));
        lblAviso.setForeground(UITheme.TEXT_MUTED);
        painel.add(lblAviso, BorderLayout.SOUTH);

        btnExecutar.addActionListener(e -> {
            int n = (int) cbQtd.getSelectedItem();
            Lote[] original = gerarLotesSinteticos(n);

            Lote[] cQuick = original.clone();
            Lote[] cMerge = original.clone();
            Lote[] cInsertion = original.clone();

            long t1 = System.nanoTime();
            AlgoritmosOrdenacao.quickSort(cQuick);
            double msQuick = (System.nanoTime() - t1) / 1_000_000.0;

            long t2 = System.nanoTime();
            AlgoritmosOrdenacao.mergeSort(cMerge);
            double msMerge = (System.nanoTime() - t2) / 1_000_000.0;

            String strInsertion;
            if (n <= 10000) {
                long t3 = System.nanoTime();
                AlgoritmosOrdenacao.insertionSort(cInsertion);
                double msIns = (System.nanoTime() - t3) / 1_000_000.0;
                strInsertion = String.format("%.4f ms", msIns);
            } else {
                strInsertion = "Ignorado (Custo excessivo O(N²))";
            }

            modelBench.setRowCount(0);
            modelBench.addRow(new Object[]{"Quick Sort (Mediana de 3)", "O(N log N)", String.format("%.4f ms", msQuick)});
            modelBench.addRow(new Object[]{"Merge Sort", "O(N log N)", String.format("%.4f ms", msMerge)});
            modelBench.addRow(new Object[]{"Insertion Sort", "O(N²)", strInsertion});
        });

        return painel;
    }

    private Lote[] gerarLotesSinteticos(int total) {
        Lote[] vetor = new Lote[total];
        Random rand = new Random(42);
        LocalDate hoje = LocalDate.now();
        for (int i = 0; i < total; i++) {
            vetor[i] = new Lote(i + 1, "Item Sintético #" + (i + 1), hoje, hoje.plusDays(rand.nextInt(800)), 10, 1, 1);
        }
        return vetor;
    }

    private void criarRodapeStatus() {
        JPanel rodape = new JPanel(new BorderLayout(12, 0));
        rodape.setBackground(UITheme.BG_CARD);
        rodape.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createMatteBorder(1, 0, 0, 0, UITheme.BORDER),
            new EmptyBorder(10, 24, 10, 24)
        ));

        JButton btnBaixa = new BotaoAnimado("Registrar Venda / Saída de Caixa", UITheme.BTN_PRIMARY, UITheme.BTN_PRIMARY_HOVER);
        btnBaixa.setPreferredSize(new Dimension(250, 36));
        btnBaixa.addActionListener(e -> realizarBaixaEstoque());

        JPanel painelStatusDir = new JPanel(new FlowLayout(FlowLayout.RIGHT, 14, 0));
        painelStatusDir.setOpaque(false);

        lblStatus = new JLabel("");
        lblStatus.setFont(UITheme.FONT_BODY);
        lblStatus.setForeground(UITheme.TEXT_MUTED);

        lblBadgeBD = new JLabel();
        lblBadgeBD.setFont(UITheme.FONT_BOLD);
        lblBadgeBD.setBorder(BorderFactory.createCompoundBorder(
            new LineBorder(UITheme.BORDER, 1, true),
            new EmptyBorder(4, 10, 4, 10)
        ));

        painelStatusDir.add(lblStatus);
        painelStatusDir.add(lblBadgeBD);

        rodape.add(btnBaixa, BorderLayout.WEST);
        rodape.add(painelStatusDir, BorderLayout.EAST);
        add(rodape, BorderLayout.SOUTH);
    }

    private void atualizarBadgeStatusBD() {
        if (repositorio.isConectadoBanco()) {
            lblBadgeBD.setText("● " + repositorio.getNomeOrigem());
            lblBadgeBD.setForeground(UITheme.STATUS_SUCCESS);
            lblBadgeBD.setBackground(new Color(240, 253, 244));
        } else {
            lblBadgeBD.setText("▲ " + repositorio.getNomeOrigem());
            lblBadgeBD.setForeground(UITheme.STATUS_WARNING);
            lblBadgeBD.setBackground(new Color(254, 243, 199));
        }
        lblBadgeBD.setOpaque(true);
    }

    private void abrirModalConfigBD() {
        DialogoConexaoBD dlg = new DialogoConexaoBD(this, () -> {
            this.repositorio = LoteRepository.obterRepositorio();
            this.historicoService = new HistoricoService(repositorio);
            atualizarBadgeStatusBD();

            estoqueLotes.clear();
            estoqueLotes.addAll(repositorio.listarTodos());
            aplicarOrdenacaoFEFO();

            if (painelDashboard != null) {
                painelDashboard.atualizarDados();
            }
        });
        dlg.setVisible(true);
    }

    private void registrarLoteManual() {
        try {
            String nome = txtNome.getText().trim();
            int qtd = Integer.parseInt(txtQtd.getText().trim());
            LocalDate validade = LocalDate.parse(txtValidade.getText().trim(), formatoData);
            int fornId = Integer.parseInt(txtFornecedor.getText().trim());

            if (nome.isEmpty()) {
                JOptionPane.showMessageDialog(this, "Informe o nome do medicamento.", "Atenção", JOptionPane.WARNING_MESSAGE);
                return;
            }

            int novoId = estoqueLotes.stream().mapToInt(Lote::getIdLote).max().orElse(0) + 1;
            Lote novo = new Lote(novoId, nome, LocalDate.now(), validade, qtd, fornId, 1);
            
            repositorio.salvar(novo);
            estoqueLotes.add(novo);

            historicoService.registrarEntrada(novoId, nome, qtd);

            aplicarOrdenacaoFEFO();
            txtNome.setText("");
            txtQtd.setText("");
            lblStatus.setText("Lote #" + novoId + " registrado.");

            if (painelDashboard != null) {
                painelDashboard.atualizarDados();
            }
        } catch (DateTimeParseException ex) {
            JOptionPane.showMessageDialog(this, "Data inválida. Digite no formato DD/MM/AAAA.", "Formato Inválido", JOptionPane.ERROR_MESSAGE);
        } catch (NumberFormatException ex) {
            JOptionPane.showMessageDialog(this, "Preencha quantidade e fornecedor apenas com números.", "Erro", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void realizarBaixaEstoque() {
        int linha = tabela.getSelectedRow();
        if (linha == -1) {
            JOptionPane.showMessageDialog(this, "Selecione um lote na tabela para registrar a saída.", "Aviso", JOptionPane.WARNING_MESSAGE);
            return;
        }

        int idLote = (int) tabela.getValueAt(linha, 0);
        String entrada = JOptionPane.showInputDialog(this, "Quantidade de caixas a retirar:", "Dispensação", JOptionPane.QUESTION_MESSAGE);

        if (entrada != null && !entrada.trim().isEmpty()) {
            try {
                int qtdBaixa = Integer.parseInt(entrada.trim());
                for (int i = 0; i < estoqueLotes.size(); i++) {
                    Lote l = estoqueLotes.get(i);
                    if (l.getIdLote() == idLote) {
                        if (qtdBaixa > l.getQuantidade()) {
                            JOptionPane.showMessageDialog(this, "Quantidade excede o disponível (" + l.getQuantidade() + ").", "Saldo Insuficiente", JOptionPane.ERROR_MESSAGE);
                            return;
                        }

                        int restante = l.getQuantidade() - qtdBaixa;
                        estoqueLotes.remove(i);
                        if (restante > 0) {
                            Lote atualizado = new Lote(l.getIdLote(), l.getNomeMedicamento(), l.getDataFabricacao(), l.getDataValidade(), restante, l.getIdFornecedor(), l.getIdMedicamento());
                            estoqueLotes.add(atualizado);
                        }

                        repositorio.atualizarQuantidade(idLote, restante);
                        historicoService.registrarSaida(idLote, l.getNomeMedicamento(), qtdBaixa);
                        break;
                    }
                }

                aplicarOrdenacaoFEFO();
                lblStatus.setText("Dispensação realizada no lote #" + idLote);

                if (painelDashboard != null) {
                    painelDashboard.atualizarDados();
                }
            } catch (NumberFormatException ex) {
                JOptionPane.showMessageDialog(this, "Digite um número inteiro válido.", "Erro", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private void aplicarOrdenacaoFEFO() {
        if (estoqueLotes.isEmpty()) {
            atualizarTabela(estoqueLotes);
            return;
        }
        Lote[] arr = estoqueLotes.toArray(new Lote[estoqueLotes.size()]);
        AlgoritmosOrdenacao.quickSort(arr);
        estoqueLotes.clear();
        java.util.Collections.addAll(estoqueLotes, arr);
        atualizarTabela(estoqueLotes);
    }

    private void filtrarTabela(String termo) {
        if (termo == null || termo.trim().isEmpty()) {
            atualizarTabela(estoqueLotes);
            return;
        }
        List<Lote> filtrados = new ArrayList<>();
        for (Lote l : estoqueLotes) {
            if (l.getNomeMedicamento().toLowerCase().contains(termo.toLowerCase().trim())) {
                filtrados.add(l);
            }
        }
        atualizarTabela(filtrados);
    }

    private void atualizarTabela(List<Lote> dados) {
        modeloTabela.setRowCount(0);
        int totalCaixas = 0;
        LocalDate hoje = LocalDate.now();

        for (Lote l : dados) {
            totalCaixas += l.getQuantidade();
            String status = "Regular";
            if (l.getDataValidade().isBefore(hoje)) {
                status = "VENCIDO";
            } else if (l.getDataValidade().isBefore(hoje.plusDays(45))) {
                status = "Vence em breve";
            }

            modeloTabela.addRow(new Object[]{
                l.getIdLote(),
                l.getNomeMedicamento(),
                l.getDataFabricacao().format(formatoData),
                l.getDataValidade().format(formatoData),
                l.getQuantidade() + " un",
                "Fornecedor #" + l.getIdFornecedor(),
                status
            });
        }

        lblTotalLotes.setText("<html><div style='text-align:right;'><span style='font-size:10px; color:#64748B; font-weight:600;'>LOTES ATIVOS</span><br><span style='font-size:15px; color:#0F172A; font-weight:700;'>" 
                + estoqueLotes.size() + "</span></div></html>");
        lblTotalCaixas.setText("<html><div style='text-align:right;'><span style='font-size:10px; color:#64748B; font-weight:600;'>UNIDADES TOTAIS</span><br><span style='font-size:15px; color:#0F172A; font-weight:700;'>" 
                + totalCaixas + "</span></div></html>");
    }

    private void importarCSV() {
        JFileChooser chooser = new JFileChooser();
        chooser.setDialogTitle("Importar Planilha de Lotes (CSV)");
        if (chooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
            File arquivo = chooser.getSelectedFile();
            try (BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(arquivo), StandardCharsets.UTF_8))) {
                String linha = br.readLine();
                int carregados = 0;
                while ((linha = br.readLine()) != null) {
                    String[] c = linha.split(",");
                    if (c.length >= 6) {
                        int id = Integer.parseInt(c[0].trim());
                        String nome = c[1].trim();
                        LocalDate fab = LocalDate.parse(c[2].trim());
                        LocalDate val = LocalDate.parse(c[3].trim());
                        int qtd = Integer.parseInt(c[4].trim());
                        int forn = Integer.parseInt(c[5].trim());

                        Lote l = new Lote(id, nome, fab, val, qtd, forn, 1);
                        estoqueLotes.add(l);
                        repositorio.salvar(l);
                        carregados++;
                    }
                }
                aplicarOrdenacaoFEFO();
                JOptionPane.showMessageDialog(this, carregados + " lotes importados com sucesso!", "Sucesso", JOptionPane.INFORMATION_MESSAGE);
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this, "Erro ao carregar arquivo: " + ex.getMessage(), "Falha de Leitura", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private void exportarCSV() {
        if (estoqueLotes.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Não existem dados no estoque para exportar.", "Aviso", JOptionPane.WARNING_MESSAGE);
            return;
        }

        JFileChooser chooser = new JFileChooser();
        chooser.setDialogTitle("Salvar Arquivo de Estoque (CSV)");
        chooser.setSelectedFile(new File("estoque_farmacia.csv"));

        if (chooser.showSaveDialog(this) == JFileChooser.APPROVE_OPTION) {
            File selecionado = chooser.getSelectedFile();
            File arquivo = selecionado.getName().toLowerCase().endsWith(".csv") 
                    ? selecionado 
                    : new File(selecionado.getParentFile(), selecionado.getName() + ".csv");

            try (PrintWriter pw = new PrintWriter(new OutputStreamWriter(new FileOutputStream(arquivo), StandardCharsets.UTF_8))) {
                pw.println("id_lote,medicamento,fabricacao,validade,quantidade,id_fornecedor");

                for (Lote l : estoqueLotes) {
                    pw.printf("%d,%s,%s,%s,%d,%d\n",
                        l.getIdLote(),
                        l.getNomeMedicamento().replace(",", " "),
                        l.getDataFabricacao(),
                        l.getDataValidade(),
                        l.getQuantidade(),
                        l.getIdFornecedor()
                    );
                }
                pw.flush();
                JOptionPane.showMessageDialog(this, "Arquivo exportado com sucesso!\nSalvo em: " + arquivo.getAbsolutePath(), "Concluído", JOptionPane.INFORMATION_MESSAGE);
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this, "Erro ao gravar o arquivo: " + ex.getMessage(), "Erro", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private void carregarDadosIniciais() {
        List<Lote> salvos = repositorio.listarTodos();
        if (salvos != null && !salvos.isEmpty()) {
            estoqueLotes.addAll(salvos);
        } else {
            LocalDate hoje = LocalDate.now();
            Random rand = new Random(101);

            String[] catalogo = {
                "Dipirona Monoidratada 500mg", "Dipirona Gotas 50ml", "Paracetamol 750mg", "Paracetamol Gotas 200mg/ml",
                "Ibuprofeno 600mg", "Ibuprofeno 400mg", "Amoxicilina + Clavulanato 500mg", "Amoxicilina 500mg Suspensão",
                "Azitromicina 500mg", "Cefalexina 500mg", "Ciprofloxacino 500mg", "Omeprazol 20mg", "Pantoprazol 40mg",
                "Esomeprazol 20mg", "Losartana Potássica 50mg", "Atenolol 50mg", "Enalapril 20mg", "Hidroclorotiazida 25mg",
                "Sinvastatina 20mg", "Atorvastatina 20mg", "Rosuvastatina 10mg", "Metformina 850mg", "Gliclazida 60mg",
                "Clonazepam 2mg", "Diazepam 10mg", "Sertralina 50mg", "Fluoxetina 20mg", "Escitalopram 10mg",
                "Prednisona 20mg", "Dexametasona 4mg", "Loratadina 10mg", "Desloratadina 5mg", "Cetirizina 10mg",
                "Xarope Acebrofilina 50mg/5ml", "Xarope Ambroxol 30mg/5ml", "Salbutamol Spray 100mcg", "Budesonida Spray 50mcg",
                "Dipropionato de Betametasona Creme", "Nimesulida 100mg", "Torsilax Comprimidos", "Dorflex Comprimidos",
                "Buscopan Composto", "Neosaldina Drágeas", "Luftal Gotas 75mg/ml", "Simeticona 40mg", "Domperidona 10mg",
                "Vonau Flash 4mg", "Ondansetrona 8mg", "Complexo B Comprimidos", "Vitamina C 1g Efervescente"
            };

            for (int i = 1; i <= 200; i++) {
                String med = catalogo[rand.nextInt(catalogo.length)];
                LocalDate fabricacao = hoje.minusDays(rand.nextInt(150) + 15);
                
                int diasValidade;
                if (i % 8 == 0) {
                    diasValidade = rand.nextInt(35) + 5;
                } else if (i % 25 == 0) {
                    diasValidade = -(rand.nextInt(20) + 1);
                } else {
                    diasValidade = rand.nextInt(650) + 50;
                }

                LocalDate validade = hoje.plusDays(diasValidade);
                int quantidade = rand.nextInt(95) + 10;
                int fornecedorId = rand.nextInt(8) + 1;
                int medicamentoId = (i % catalogo.length) + 1;

                Lote novo = new Lote(i, med, fabricacao, validade, quantidade, fornecedorId, medicamentoId);
                estoqueLotes.add(novo);
            }

            repositorio.salvarTodos(estoqueLotes);
        }

        aplicarOrdenacaoFEFO();
    }
}

// ================= DESIGN SYSTEM / THEME =================
final class UITheme {
        public static final Color BG_APP = new Color(248, 250, 252);
        public static final Color BG_CARD = Color.WHITE;
        public static final Color BORDER = new Color(226, 232, 240);
        public static final Color TEXT_TITLE = new Color(15, 23, 42);
        public static final Color TEXT_BODY = new Color(51, 65, 85);
        public static final Color TEXT_MUTED = new Color(100, 116, 139);

        public static final Color BTN_PRIMARY = new Color(15, 23, 42);
        public static final Color BTN_PRIMARY_HOVER = new Color(30, 41, 59);
        public static final Color BTN_ACCENT = new Color(2, 132, 199);

        public static final Color STATUS_SUCCESS = new Color(16, 185, 129);
        public static final Color STATUS_WARNING = new Color(245, 158, 11);
        public static final Color STATUS_DANGER = new Color(239, 68, 68);

        public static final Font FONT_TITLE = new Font("Segoe UI", Font.BOLD, 19);
        public static final Font FONT_SUBTITLE = new Font("Segoe UI", Font.BOLD, 13);
        public static final Font FONT_BODY = new Font("Segoe UI", Font.PLAIN, 12);
        public static final Font FONT_BOLD = new Font("Segoe UI", Font.BOLD, 12);
        public static final Font FONT_SMALL = new Font("Segoe UI", Font.PLAIN, 11);
        public static final Font FONT_CAPTION = new Font("Segoe UI", Font.BOLD, 10);

        public static double easeOutPower2(double t) {
            t = Math.max(0.0, Math.min(1.0, t));
            return 1.0 - Math.pow(1.0 - t, 2.0);
        }

        public static Color lerpColor(Color c1, Color c2, float t) {
            t = Math.max(0f, Math.min(1f, t));
            int r = (int) (c1.getRed() + (c2.getRed() - c1.getRed()) * t);
            int g = (int) (c1.getGreen() + (c2.getGreen() - c1.getGreen()) * t);
            int b = (int) (c1.getBlue() + (c2.getBlue() - c1.getBlue()) * t);
            int a = (int) (c1.getAlpha() + (c2.getAlpha() - c1.getAlpha()) * t);
            return new Color(r, g, b, a);
        }

        public static void desenharCardComSombra(Graphics2D g2, int x, int y, int w, int h, int raio) {
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(new Color(15, 23, 42, 8));
            g2.fillRoundRect(x + 1, y + 3, w - 2, h - 2, raio, raio);
            g2.setColor(new Color(15, 23, 42, 12));
            g2.fillRoundRect(x, y + 1, w, h, raio, raio);
            g2.setColor(BG_CARD);
            g2.fillRoundRect(x, y, w, h, raio, raio);
            g2.setColor(BORDER);
            g2.drawRoundRect(x, y, w - 1, h - 1, raio, raio);
        }

        public static void aplicarEstiloInput(JTextField campo) {
            campo.setFont(FONT_BODY);
            campo.setForeground(TEXT_TITLE);
            campo.setBackground(Color.WHITE);
            campo.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(BORDER, 1, true),
                new EmptyBorder(5, 10, 5, 10)
            ));
        }
    }

    // ================= BOTÃO ANIMADO (EASING) =================
    class BotaoAnimado extends JButton {
        private final Color corBase;
        private final Color corHover;
        private Color corAtual;
        private float transicao = 0.0f;
        private Timer timerTransicao;

        public BotaoAnimado(String texto, Color bg, Color hover) {
            super(texto);
            this.corBase = bg;
            this.corHover = hover;
            this.corAtual = bg;

            setFont(UITheme.FONT_BOLD);
            setForeground(Color.WHITE);
            setContentAreaFilled(false);
            setFocusPainted(false);
            setBorderPainted(false);
            setCursor(new Cursor(Cursor.HAND_CURSOR));
            setBorder(new EmptyBorder(8, 14, 8, 14));

            addMouseListener(new MouseAdapter() {
                @Override
                public void mouseEntered(MouseEvent e) { iniciarTransicao(1.0f); }
                @Override
                public void mouseExited(MouseEvent e) { iniciarTransicao(0.0f); }
            });
        }

        private void iniciarTransicao(float alvo) {
            if (timerTransicao != null && timerTransicao.isRunning()) timerTransicao.stop();
            final float inicio = transicao;
            final long start = System.currentTimeMillis();
            final int duracao = 150;

            timerTransicao = new Timer(15, e -> {
                long tempo = System.currentTimeMillis() - start;
                double t = (double) tempo / duracao;
                if (t >= 1.0) {
                    transicao = alvo;
                    ((Timer) e.getSource()).stop();
                } else {
                    float progresso = (float) UITheme.easeOutPower2(t);
                    transicao = inicio + (alvo - inicio) * progresso;
                }
                corAtual = UITheme.lerpColor(corBase, corHover, transicao);
                repaint();
            });
            timerTransicao.start();
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(new Color(15, 23, 42, 16));
            g2.fillRoundRect(0, 2, getWidth(), getHeight() - 1, 8, 8);
            g2.setColor(corAtual);
            g2.fillRoundRect(0, 0, getWidth(), getHeight(), 8, 8);
            g2.dispose();
            super.paintComponent(g);
        }
    }

    // ================= MODAL CONFIGURAÇÃO BANCO =================
    class DialogoConexaoBD extends JDialog {
        private final JTextField txtHost, txtPorta, txtBanco, txtUsuario;
        private final JPasswordField txtSenha;
        private final JLabel lblStatusAtual;
        private final Runnable callbackAoReconectar;

        public DialogoConexaoBD(JFrame parent, Runnable callbackAoReconectar) {
            super(parent, "Configuração de Banco de Dados", true);
            this.callbackAoReconectar = callbackAoReconectar;

            setSize(480, 480);
            setLocationRelativeTo(parent);
            setResizable(false);
            setLayout(new BorderLayout(14, 14));
            getContentPane().setBackground(UITheme.BG_APP);

            DatabaseManager.Config cfg = DatabaseManager.getConfig();

            JPanel conteudo = new JPanel();
            conteudo.setLayout(new BoxLayout(conteudo, BoxLayout.Y_AXIS));
            conteudo.setOpaque(false);
            conteudo.setBorder(new EmptyBorder(20, 24, 20, 24));

            JLabel lblTit = new JLabel("Parâmetros de Conexão MySQL");
            lblTit.setFont(UITheme.FONT_TITLE);
            lblTit.setForeground(UITheme.TEXT_TITLE);
            conteudo.add(lblTit);
            conteudo.add(Box.createVerticalStrut(6));

            JLabel lblSub = new JLabel("<html>Defina os parâmetros de conexão. Caso o servidor não responda, o sistema alternará para a lista em memória.</html>");
            lblSub.setFont(UITheme.FONT_BODY);
            lblSub.setForeground(UITheme.TEXT_MUTED);
            conteudo.add(lblSub);
            conteudo.add(Box.createVerticalStrut(16));

            JPanel form = new JPanel() {
                @Override
                protected void paintComponent(Graphics g) {
                    UITheme.desenharCardComSombra((Graphics2D) g, 0, 0, getWidth(), getHeight(), 10);
                }
            };
            form.setOpaque(false);
            form.setLayout(new BoxLayout(form, BoxLayout.Y_AXIS));
            form.setBorder(new EmptyBorder(16, 16, 16, 16));

            txtHost = criarCampo(form, "Servidor Host:", cfg.getHost());
            txtPorta = criarCampo(form, "Porta:", String.valueOf(cfg.getPorta()));
            txtBanco = criarCampo(form, "Nome do Banco de Dados:", cfg.getBanco());
            txtUsuario = criarCampo(form, "Usuário:", cfg.getUsuario());
            txtSenha = criarCampoSenha(form, "Senha:", cfg.getSenha());

            conteudo.add(form);
            conteudo.add(Box.createVerticalStrut(12));

            lblStatusAtual = new JLabel("Origem Atual: " + LoteRepository.obterRepositorio().getNomeOrigem());
            lblStatusAtual.setFont(UITheme.FONT_BOLD);
            lblStatusAtual.setForeground(LoteRepository.obterRepositorio().isConectadoBanco() ? UITheme.STATUS_SUCCESS : UITheme.STATUS_WARNING);
            conteudo.add(lblStatusAtual);

            add(conteudo, BorderLayout.CENTER);

            JPanel rodape = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 14));
            rodape.setBackground(UITheme.BG_CARD);
            rodape.setBorder(BorderFactory.createMatteBorder(1, 0, 0, 0, UITheme.BORDER));

            JButton btnTestar = new JButton("Testar Conexão");
            estilizarBotaoSecundario(btnTestar);
            btnTestar.addActionListener(e -> testarConexao());

            JButton btnSalvar = new JButton("Salvar e Conectar");
            estilizarBotaoPrimario(btnSalvar);
            btnSalvar.addActionListener(e -> salvarEReconectar());

            rodape.add(btnTestar);
            rodape.add(btnSalvar);
            add(rodape, BorderLayout.SOUTH);
        }

        private JTextField criarCampo(JPanel painel, String rotulo, String valorInicial) {
            JLabel l = new JLabel(rotulo);
            l.setFont(UITheme.FONT_BOLD);
            l.setForeground(UITheme.TEXT_BODY);
            painel.add(l);
            painel.add(Box.createVerticalStrut(4));

            JTextField tf = new JTextField(valorInicial);
            tf.setMaximumSize(new Dimension(Integer.MAX_VALUE, 30));
            UITheme.aplicarEstiloInput(tf);
            painel.add(tf);
            painel.add(Box.createVerticalStrut(8));
            return tf;
        }

        private JPasswordField criarCampoSenha(JPanel painel, String rotulo, String valorInicial) {
            JLabel l = new JLabel(rotulo);
            l.setFont(UITheme.FONT_BOLD);
            l.setForeground(UITheme.TEXT_BODY);
            painel.add(l);
            painel.add(Box.createVerticalStrut(4));

            JPasswordField pf = new JPasswordField(valorInicial);
            pf.setMaximumSize(new Dimension(Integer.MAX_VALUE, 30));
            UITheme.aplicarEstiloInput(pf);
            painel.add(pf);
            return pf;
        }

        private void estilizarBotaoPrimario(JButton b) {
            b.setFont(UITheme.FONT_BOLD);
            b.setForeground(Color.WHITE);
            b.setBackground(UITheme.BTN_PRIMARY);
            b.setFocusPainted(false);
            b.setBorder(new EmptyBorder(8, 14, 8, 14));
            b.setCursor(new Cursor(Cursor.HAND_CURSOR));
        }

        private void estilizarBotaoSecundario(JButton b) {
            b.setFont(UITheme.FONT_BODY);
            b.setForeground(UITheme.TEXT_TITLE);
            b.setBackground(Color.WHITE);
            b.setFocusPainted(false);
            b.setBorder(BorderFactory.createCompoundBorder(
                new LineBorder(UITheme.BORDER, 1, true),
                new EmptyBorder(7, 12, 7, 12)
            ));
            b.setCursor(new Cursor(Cursor.HAND_CURSOR));
        }

        private DatabaseManager.Config obterConfigDoForm() {
            DatabaseManager.Config cfg = new DatabaseManager.Config();
            cfg.setHost(txtHost.getText().trim());
            try {
                cfg.setPorta(Integer.parseInt(txtPorta.getText().trim()));
            } catch (NumberFormatException e) {
                cfg.setPorta(3306);
            }
            cfg.setBanco(txtBanco.getText().trim());
            cfg.setUsuario(txtUsuario.getText().trim());
            cfg.setSenha(new String(txtSenha.getPassword()));
            return cfg;
        }

        private void testarConexao() {
            DatabaseManager.Config cfg = obterConfigDoForm();
            if (!DatabaseManager.isDriverDisponivel()) {
                JOptionPane.showMessageDialog(this, "Driver MySQL JDBC não está disponível.", "Aviso", JOptionPane.WARNING_MESSAGE);
                return;
            }

            boolean sucesso = DatabaseManager.testarConexao(cfg);
            if (sucesso) {
                JOptionPane.showMessageDialog(this, "Conexão com o servidor MySQL bem-sucedida!", "Sucesso", JOptionPane.INFORMATION_MESSAGE);
            } else {
                JOptionPane.showMessageDialog(this, "Não foi possível conectar ao servidor MySQL com as credenciais informadas.", "Falha de Conexão", JOptionPane.ERROR_MESSAGE);
            }
        }

        private void salvarEReconectar() {
            DatabaseManager.Config cfg = obterConfigDoForm();
            LoteRepository novoRepo = LoteRepository.tentarReconectar(cfg);

            if (novoRepo.isConectadoBanco()) {
                JOptionPane.showMessageDialog(this, "Conectado ao MySQL com sucesso!", "Banco Conectado", JOptionPane.INFORMATION_MESSAGE);
            } else {
                JOptionPane.showMessageDialog(this, "MySQL inacessível. Operando no modo em memória.", "Modo Offline", JOptionPane.WARNING_MESSAGE);
            }

            if (callbackAoReconectar != null) callbackAoReconectar.run();
            dispose();
        }
    }