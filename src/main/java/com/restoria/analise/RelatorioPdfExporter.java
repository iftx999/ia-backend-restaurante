package com.restoria.analise;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDFont;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.springframework.stereotype.Component;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.math.BigDecimal;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * Exporta um {@link Relatorio} em PDF simples (texto corrido), para o
 * gerente imprimir ou anexar em email (RF-14).
 */
@Component
class RelatorioPdfExporter {

    private static final DateTimeFormatter FORMATO_DATA = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
    private static final float MARGEM = 50f;
    private static final float TAMANHO_FONTE_TITULO = 16f;
    private static final float TAMANHO_FONTE_TEXTO = 11f;
    private static final float ALTURA_LINHA = 15f;

    byte[] exportar(Relatorio relatorio) {
        try (PDDocument document = new PDDocument()) {
            PDFont fonteNormal = new PDType1Font(Standard14Fonts.FontName.HELVETICA);
            PDFont fonteNegrito = new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD);

            EscritorPdf escritor = new EscritorPdf(document, fonteNormal, fonteNegrito);

            escritor.escreverTitulo("Relatorio RestorIA");
            escritor.escreverTexto("Gerado em " + relatorio.getGeradoEm().format(FORMATO_DATA));
            escritor.pularLinha();

            escritor.escreverSubtitulo("CMV calculado");
            escritor.escreverTexto(relatorio.getCmvCalculado() == null
                    ? "nao calculavel (faltam dados de vendas ou de estoque)"
                    : formatarPercentual(relatorio.getCmvCalculado()));
            escritor.pularLinha();

            if (!relatorio.getIndicadoresPrato().isEmpty()) {
                escritor.escreverSubtitulo("Indicadores por prato");
                for (IndicadorPrato indicador : relatorio.getIndicadoresPrato()) {
                    String linha = "- %s: margem %s%s".formatted(
                            indicador.getNomePrato(),
                            formatarPercentual(indicador.getMargemCalculada()),
                            indicador.isAlerta() ? " [ALERTA]" : "");
                    escritor.escreverTexto(linha);
                }
                escritor.pularLinha();
            }

            escritor.escreverSubtitulo("Analise");
            for (String paragrafo : relatorio.getConteudoTextoIA().split("\n")) {
                if (paragrafo.isBlank()) {
                    escritor.pularLinha();
                } else {
                    escritor.escreverTexto(paragrafo);
                }
            }

            escritor.finalizar();

            ByteArrayOutputStream out = new ByteArrayOutputStream();
            document.save(out);
            return out.toByteArray();
        } catch (IOException e) {
            throw new UncheckedIOException("Falha ao gerar PDF do relatorio", e);
        }
    }

    private String formatarPercentual(BigDecimal valor) {
        return valor.multiply(BigDecimal.valueOf(100)) + "%";
    }

    /**
     * Escreve texto com quebra de linha automatica (por largura) e paginacao
     * automatica (quando o cursor Y chega perto do rodape). PDFBox nao tem
     * suporte nativo a texto corrido multi-pagina — isso e feito na mao.
     */
    private static final class EscritorPdf {
        private final PDDocument document;
        private final PDFont fonteNormal;
        private final PDFont fonteNegrito;
        private final float larguraUtil;

        private PDPage paginaAtual;
        private PDPageContentStream stream;
        private float cursorY;

        EscritorPdf(PDDocument document, PDFont fonteNormal, PDFont fonteNegrito) throws IOException {
            this.document = document;
            this.fonteNormal = fonteNormal;
            this.fonteNegrito = fonteNegrito;
            this.larguraUtil = PDRectangle.A4.getWidth() - 2 * MARGEM;
            novaPagina();
        }

        void escreverTitulo(String texto) throws IOException {
            escreverComFonte(texto, fonteNegrito, TAMANHO_FONTE_TITULO);
        }

        void escreverSubtitulo(String texto) throws IOException {
            escreverComFonte(texto, fonteNegrito, TAMANHO_FONTE_TEXTO + 1);
        }

        void escreverTexto(String texto) throws IOException {
            escreverComFonte(texto, fonteNormal, TAMANHO_FONTE_TEXTO);
        }

        void pularLinha() {
            cursorY -= ALTURA_LINHA;
        }

        void finalizar() throws IOException {
            stream.close();
        }

        private void escreverComFonte(String texto, PDFont fonte, float tamanho) throws IOException {
            for (String linha : quebrarLinhas(texto, fonte, tamanho)) {
                if (cursorY < MARGEM) {
                    novaPagina();
                }
                stream.beginText();
                stream.setFont(fonte, tamanho);
                stream.newLineAtOffset(MARGEM, cursorY);
                stream.showText(linha);
                stream.endText();
                cursorY -= ALTURA_LINHA;
            }
        }

        private List<String> quebrarLinhas(String texto, PDFont fonte, float tamanho) throws IOException {
            List<String> linhas = new ArrayList<>();
            StringBuilder linhaAtual = new StringBuilder();

            for (String palavra : texto.split(" ")) {
                String tentativa = linhaAtual.isEmpty() ? palavra : linhaAtual + " " + palavra;
                if (largura(tentativa, fonte, tamanho) > larguraUtil && !linhaAtual.isEmpty()) {
                    linhas.add(linhaAtual.toString());
                    linhaAtual = new StringBuilder(palavra);
                } else {
                    linhaAtual = new StringBuilder(tentativa);
                }
            }
            if (!linhaAtual.isEmpty()) {
                linhas.add(linhaAtual.toString());
            }
            return linhas;
        }

        private float largura(String texto, PDFont fonte, float tamanho) throws IOException {
            return fonte.getStringWidth(texto) / 1000 * tamanho;
        }

        private void novaPagina() throws IOException {
            if (stream != null) {
                stream.close();
            }
            paginaAtual = new PDPage(PDRectangle.A4);
            document.addPage(paginaAtual);
            stream = new PDPageContentStream(document, paginaAtual);
            cursorY = PDRectangle.A4.getHeight() - MARGEM;
        }
    }
}
