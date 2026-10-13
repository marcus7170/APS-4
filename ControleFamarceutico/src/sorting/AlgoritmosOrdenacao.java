package sorting;

import model.Lote;

public final class AlgoritmosOrdenacao {

    private AlgoritmosOrdenacao() {}

    public static void quickSort(Lote[] vetor) {
        if (vetor == null || vetor.length < 2) return;
        ordenarQuick(vetor, 0, vetor.length - 1);
    }

    private static void ordenarQuick(Lote[] vetor, int baixo, int alto) {
        if (baixo < alto) {
            int pivo = particionar(vetor, baixo, alto);
            ordenarQuick(vetor, baixo, pivo - 1);
            ordenarQuick(vetor, pivo + 1, alto);
        }
    }

    private static int particionar(Lote[] vetor, int baixo, int alto) {
        int meio = baixo + (alto - baixo) / 2;
        if (vetor[baixo].compareTo(vetor[meio]) > 0) trocar(vetor, baixo, meio);
        if (vetor[baixo].compareTo(vetor[alto]) > 0) trocar(vetor, baixo, alto);
        if (vetor[meio].compareTo(vetor[alto]) > 0) trocar(vetor, meio, alto);
        trocar(vetor, meio, alto);

        Lote pivo = vetor[alto];
        int i = baixo - 1;
        for (int j = baixo; j < alto; j++) {
            if (vetor[j].compareTo(pivo) <= 0) {
                i++;
                trocar(vetor, i, j);
            }
        }
        trocar(vetor, i + 1, alto);
        return i + 1;
    }

    public static void mergeSort(Lote[] vetor) {
        if (vetor == null || vetor.length < 2) return;
        ordenarMerge(vetor, 0, vetor.length - 1);
    }

    private static void ordenarMerge(Lote[] vetor, int inicio, int fim) {
        if (inicio < fim) {
            int meio = inicio + (fim - inicio) / 2;
            ordenarMerge(vetor, inicio, meio);
            ordenarMerge(vetor, meio + 1, fim);
            intercalar(vetor, inicio, meio, fim);
        }
    }

    private static void intercalar(Lote[] vetor, int inicio, int meio, int fim) {
        int n1 = meio - inicio + 1;
        int n2 = fim - meio;

        Lote[] esq = new Lote[n1];
        Lote[] dir = new Lote[n2];

        for (int i = 0; i < n1; i++) esq[i] = vetor[inicio + i];
        for (int j = 0; j < n2; j++) dir[j] = vetor[meio + 1 + j];

        int i = 0, j = 0, k = inicio;
        while (i < n1 && j < n2) {
            if (esq[i].compareTo(dir[j]) <= 0) {
                vetor[k++] = esq[i++];
            } else {
                vetor[k++] = dir[j++];
            }
        }
        while (i < n1) vetor[k++] = esq[i++];
        while (j < n2) vetor[k++] = dir[j++];
    }

    public static void insertionSort(Lote[] vetor) {
        if (vetor == null || vetor.length < 2) return;
        for (int i = 1; i < vetor.length; i++) {
            Lote chave = vetor[i];
            int j = i - 1;
            while (j >= 0 && vetor[j].compareTo(chave) > 0) {
                vetor[j + 1] = vetor[j];
                j--;
            }
            vetor[j + 1] = chave;
        }
    }

    private static void trocar(Lote[] vetor, int a, int b) {
        Lote temp = vetor[a];
        vetor[a] = vetor[b];
        vetor[b] = temp;
    }

    // Classes de compatibilidade
    public static class QuickSort {
        public static void ordenar(Lote[] vetor) { AlgoritmosOrdenacao.quickSort(vetor); }
    }
    public static class MergeSort {
        public static void ordenar(Lote[] vetor) { AlgoritmosOrdenacao.mergeSort(vetor); }
    }
    public static class InsertionSort {
        public static void ordenar(Lote[] vetor) { AlgoritmosOrdenacao.insertionSort(vetor); }
    }
}
