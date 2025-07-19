package org.example.kotlin.fsdsupermo;


import javafx.application.Application;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.*;
import javafx.stage.Stage;

import java.util.*;

public class AppolloIDEFX extends Application {

    private TextArea editor;
    private TextArea console;
    private Map<String, Object> variaveis = new HashMap<>();
    private Queue<String> entradas = new LinkedList<>();
    private boolean esperandoEntrada = false;
    private String variavelParaLer = null;
    private List<String> linhasCodigo = new ArrayList<>();
    private int linhaAtual = 0;
    private Stack<Boolean> condicoes = new Stack<>();
    private Stack<Integer> blocosWhile = new Stack<>();
    private Stack<BlocoControle> pilhaBlocos = new Stack<>();

    public static void main(String[] args) {
        launch(args);
    }

    @Override
    public void start(Stage stage) {
        stage.setTitle("Appollo IDE");

        Label titulo = new Label("Appollo IDE");
        titulo.setStyle("-fx-font-size: 22px; -fx-text-fill: white; -fx-font-weight: bold;");
        HBox topo = new HBox(titulo);
        topo.setAlignment(Pos.CENTER);
        topo.setPadding(new Insets(10));
        topo.setStyle("-fx-background-color: #6200EE;");

        editor = new TextArea();
        editor.setPromptText("Digite seu código Appollo aqui...");
        editor.setStyle("-fx-font-family: 'Courier New'; -fx-font-size: 14px;");
        VBox.setVgrow(editor, Priority.ALWAYS);

        console = new TextArea();
        console.setEditable(true);
        console.setStyle("-fx-control-inner-background: black; -fx-text-fill: lime; -fx-font-family: 'Courier New';");
        VBox.setVgrow(console, Priority.ALWAYS);

        console.setOnKeyPressed(event -> {
            if (esperandoEntrada && event.getCode() == KeyCode.ENTER) {
                String[] linhas = console.getText().split("\n");
                String entrada = linhas[linhas.length - 1];
                entradas.add(entrada.trim());
                esperandoEntrada = false;

                String valor = entradas.poll();
                try {
                    variaveis.put(variavelParaLer, Integer.parseInt(valor));
                } catch (NumberFormatException e) {
                    variaveis.put(variavelParaLer, valor);
                }
                variavelParaLer = null;

                executarProximoBloco();
                event.consume();
            }
        });

        Button executar = new Button("Executar");
        executar.setStyle("-fx-background-color: #3700B3; -fx-text-fill: white; -fx-font-weight: bold;");
        executar.setOnAction(e -> iniciarExecucao(editor.getText()));

        HBox botoes = new HBox(executar);
        botoes.setAlignment(Pos.CENTER);
        botoes.setPadding(new Insets(10));

        VBox centro = new VBox(10, new Label("Editor"), editor, botoes, new Label("Console"), console);
        centro.setPadding(new Insets(10));
        centro.setStyle("-fx-background-color: #f0f0f0;");
        VBox.setVgrow(centro, Priority.ALWAYS);

        VBox dicionario = new VBox(10);
        dicionario.setPadding(new Insets(10));
        dicionario.setStyle("-fx-background-color: #1e1e2e;");
        dicionario.setPrefWidth(220);

        Label tituloDic = new Label("📘 DICIONÁRIO");
        tituloDic.setStyle("-fx-text-fill: white; -fx-font-weight: bold; -fx-font-size: 14px;");
        dicionario.getChildren().add(tituloDic);

        String[][] comandos = {
                {"var nome = valor", "Declara uma variável"},
                {"escreva(\"texto\")", "Imprime um texto"},
                {"leia(nome)", "Lê valor do usuário"},
                {"se(condição)", "Executa bloco se for verdadeiro"},
                {"senao", "Executa bloco se 'se' for falso"},
                {"enquanto(condição)", "Executa repetição enquanto for verdadeiro"},
                {"fim", "Finaliza um bloco de controle"},
                {"Operações: +, -, *, /", "Suporta contas matemáticas"}
        };

        for (String[] cmd : comandos) {
            Label item = new Label(cmd[0] + "\n→ " + cmd[1]);
            item.setStyle("-fx-text-fill: white; -fx-font-size: 12px;");
            dicionario.getChildren().add(item);
        }

        ScrollPane scrollPane = new ScrollPane(dicionario);
        scrollPane.setFitToWidth(true);
        scrollPane.setStyle("-fx-background: #1e1e2e;");

        BorderPane root = new BorderPane();
        root.setTop(topo);
        root.setCenter(centro);
        root.setRight(scrollPane);

        Scene scene = new Scene(root, 1000, 600);
        stage.setScene(scene);
        stage.show();
    }

    private void iniciarExecucao(String codigo) {
        console.clear();
        variaveis.clear();
        entradas.clear();
        condicoes.clear();
        blocosWhile.clear();
        pilhaBlocos.clear();
        linhasCodigo = Arrays.asList(codigo.split("\n"));
        linhaAtual = 0;
        executarProximoBloco();
    }

    private void executarProximoBloco() {
        while (linhaAtual < linhasCodigo.size()) {
            String linha = linhasCodigo.get(linhaAtual).trim();
            linhaAtual++;
            if (linha.isEmpty()) continue;

            try {
                if (linha.startsWith("se")) {
                    String cond = extrairCondicao(linha);
                    boolean resultado = avaliarCondicao(cond);
                    pilhaBlocos.push(new BlocoControle("SE", resultado, false));
                    condicoes.push(resultado);
                } else if (linha.equals("senao")) {
                    if (!pilhaBlocos.isEmpty() && pilhaBlocos.peek().tipo.equals("SE")) {
                        BlocoControle blocoAnterior = pilhaBlocos.pop();
                        boolean executarSenao = !blocoAnterior.condicao && !blocoAnterior.senaoExecutado;
                        pilhaBlocos.push(new BlocoControle("SENAO", executarSenao, true));
                        condicoes.pop();
                        condicoes.push(executarSenao);
                    } else {
                        console.appendText("Erro: 'senao' sem 'se'.\n");
                        return;
                    }
                } else if (linha.startsWith("enquanto")) {
                    String cond = extrairCondicao(linha);
                    boolean resultado = avaliarCondicao(cond);
                    condicoes.push(resultado);
                    if (resultado) {
                        blocosWhile.push(linhaAtual - 1);
                    }
                } else if (linha.equals("fim")) {
                    if (!condicoes.isEmpty()) {
                        boolean resultado = condicoes.pop();
                        if (!blocosWhile.isEmpty() && resultado) {
                            linhaAtual = blocosWhile.peek();
                        } else if (!blocosWhile.isEmpty()) {
                            blocosWhile.pop();
                        }
                    }
                    if (!pilhaBlocos.isEmpty()) {
                        pilhaBlocos.pop();
                    }
                } else if (deveExecutarBlocoAtual()) {
                    if (linha.startsWith("escreva")) {
                        String conteudo = linha.substring(7).trim();
                        if (conteudo.startsWith("(") && conteudo.endsWith(")")) {
                            conteudo = conteudo.substring(1, conteudo.length() - 1);
                        }
                        if (conteudo.contains("+")) {
                            String[] partes = conteudo.split("\\+");
                            StringBuilder resultado = new StringBuilder();
                            for (String parte : partes) {
                                parte = parte.trim();
                                if (parte.startsWith("\"") && parte.endsWith("\"")) {
                                    resultado.append(parte.substring(1, parte.length() - 1));
                                } else {
                                    resultado.append(avaliarValor(parte));
                                }
                            }
                            console.appendText(resultado + "\n");
                        } else if (conteudo.startsWith("\"") && conteudo.endsWith("\"")) {
                            console.appendText(conteudo.substring(1, conteudo.length() - 1) + "\n");
                        } else {
                            console.appendText(avaliarValor(conteudo) + "\n");
                        }
                    } else if (linha.startsWith("var")) {
                        String[] partes = linha.substring(3).split("=", 2);
                        String nome = partes[0].trim();
                        String valorRaw = partes[1].trim();
                        if (valorRaw.startsWith("\"") && valorRaw.endsWith("\"")) {
                            variaveis.put(nome, valorRaw.substring(1, valorRaw.length() - 1));
                        } else {
                            variaveis.put(nome, avaliarValor(valorRaw));
                        }
                    } else if (linha.startsWith("leia")) {
                        String nome = linha.substring(5, linha.length() - 1).trim();
                        if (entradas.isEmpty()) {
                            esperandoEntrada = true;
                            variavelParaLer = nome;
                            console.appendText("> ");
                            return;
                        }
                    }
                }
            } catch (Exception ex) {
                console.appendText("Erro na linha " + linhaAtual + ": " + linha + "\n");
                return;
            }
        }
    }

    private boolean deveExecutarBlocoAtual(){
        for (Boolean cond : condicoes) {
            if (!cond) return false;
        }
        return true;
    }

    private String extrairCondicao(String linha) throws Exception {
        int ini = linha.indexOf("(");
        int fim = linha.lastIndexOf(")");
        if (ini == -1 || fim == -1 || fim < ini) {
            throw new Exception("Erro de sintaxe: condição mal formada.");
        }
        return linha.substring(ini + 1, fim).trim();
    }

    private Object avaliarValor(String expr) {
        expr = substituirVariaveis(expr);
        try {
            if (expr.matches("-?\\d+(\\s*[-+*/]\\s*-?\\d+)*")) {
                return calcular(expr);
            }
            return expr;
        } catch (Exception e) {
            return expr;
        }
    }


    private boolean avaliarCondicao(String cond) {
        cond = substituirVariaveis(cond);
        try {
            if (cond.contains("==")) {
                String[] partes = cond.split("==");
                return avaliarValor(partes[0].trim()).toString().equals(avaliarValor(partes[1].trim()).toString());
            } else if (cond.contains("!=")) {
                String[] partes = cond.split("!=");
                return !avaliarValor(partes[0].trim()).toString().equals(avaliarValor(partes[1].trim()).toString());
            } else if (cond.contains(">=")) {
                String[] partes = cond.split(">=");
                return calcular(partes[0].trim()) >= calcular(partes[1].trim());
            } else if (cond.contains("<=")) {
                String[] partes = cond.split("<=");
                return calcular(partes[0].trim()) <= calcular(partes[1].trim());
            } else if (cond.contains(">")) {
                String[] partes = cond.split(">");
                return calcular(partes[0].trim()) > calcular(partes[1].trim());
            } else if (cond.contains("<")) {
                String[] partes = cond.split("<");
                return calcular(partes[0].trim()) < calcular(partes[1].trim());
            } else {
                console.appendText("Condição inválida: " + cond + "\n");
                return false;
            }
        } catch (Exception e) {
            console.appendText("Erro ao avaliar condição: " + cond + "\n");
            return false;
        }
    }

    private String substituirVariaveis(String expr) {
        for (Map.Entry<String, Object> entry : variaveis.entrySet()) {
            if (entry.getValue() instanceof String) continue; // evita substituir strings em expressões matemáticas
            expr = expr.replaceAll("\\b" + entry.getKey() + "\\b", entry.getValue().toString());
        }
        return expr;
    }


    private int calcular(String expr) {
        Stack<Integer> valores = new Stack<>();
        Stack<Character> operadores = new Stack<>();

        for (int i = 0; i < expr.length(); i++) {
            char c = expr.charAt(i);

            if (Character.isWhitespace(c)) continue;

            if (Character.isDigit(c)) {
                int numero = 0;
                while (i < expr.length() && Character.isDigit(expr.charAt(i))) {
                    numero = numero * 10 + (expr.charAt(i) - '0');
                    i++;
                }
                valores.push(numero);
                i--;
            } else if (c == '+' || c == '-' || c == '*' || c == '/') {
                while (!operadores.isEmpty() && precedencia(c) <= precedencia(operadores.peek())) {
                    int b = valores.pop();
                    int a = valores.pop();
                    valores.push(aplicarOperador(operadores.pop(), a, b));
                }
                operadores.push(c);
            }
        }

        while (!operadores.isEmpty()) {
            int b = valores.pop();
            int a = valores.pop();
            valores.push(aplicarOperador(operadores.pop(), a, b));
        }

        return valores.pop();
    }

    private int aplicarOperador(char operador, int a, int b) {
        return switch (operador) {
            case '+' -> a + b;
            case '-' -> a - b;
            case '*' -> a * b;
            case '/' -> b != 0 ? a / b : 0;
            default -> 0;
        };
    }

    private int precedencia(char operador) {
        return (operador == '+' || operador == '-') ? 1 : 2;
    }
    private static class BlocoControle {
        String tipo;
        boolean condicao;
        boolean senaoExecutado;

        public BlocoControle(String tipo, boolean condicao, boolean senaoExecutado) {
            this.tipo = tipo;
            this.condicao = condicao;
            this.senaoExecutado = senaoExecutado;
        }
    }
}
