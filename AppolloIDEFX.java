package org.example.kotlin.fsdsupermo;

import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.Stage;

import java.util.*;

public class AppolloIDEFX extends Application {

    private TextArea editor;
    private TextArea console;

    public static void main(String[] args) {
        launch(args);
    }

    @Override
    public void start(Stage stage) {
        stage.setTitle("Appollo IDE");

        editor = new TextArea();
        editor.setPromptText("Digite seu código Appollo aqui...");
        editor.setPrefHeight(400);

        console = new TextArea();
        console.setEditable(false);
        console.setStyle("-fx-control-inner-background:black; -fx-font-family:monospace; -fx-text-fill:green;");
        console.setPrefHeight(200);

        Button executar = new Button("Executar");
        executar.setOnAction(e -> interpretarCodigo(editor.getText()));

        VBox layout = new VBox(10, editor, executar, console);
        layout.setStyle("-fx-padding: 10;");

        Scene scene = new Scene(layout, 800, 600);
        stage.setScene(scene);
        stage.show();
    }

    private void interpretarCodigo(String codigo) {
        console.clear();
        Map<String, Integer> variaveis = new HashMap<>();
        String[] linhas = codigo.split("\n");

        for (String linha : linhas) {
            linha = linha.trim();
            if (linha.isEmpty()) continue;

            try {
                if (linha.startsWith("escreva")) {
                    String conteudo = linha.substring(8).trim();
                    if (conteudo.startsWith("\"") && conteudo.endsWith("\"")) {
                        console.appendText(conteudo.substring(1, conteudo.length() - 1) + "\n");
                    } else if (conteudo.contains("+")) {
                        String[] partes = conteudo.split("\\+");
                        String texto = partes[0].replace("\"", "").trim();
                        String expressao = partes[1].replace("(", "").replace(")", "").trim();
                        String[] termos = expressao.split("\\+");
                        int soma = 0;
                        for (String termo : termos) {
                            termo = termo.trim();
                            soma += variaveis.getOrDefault(termo, 0);
                        }
                        console.appendText(texto + soma + "\n");
                    } else {
                        console.appendText(String.valueOf(variaveis.getOrDefault(conteudo, 0)) + "\n");
                    }
                } else if (linha.startsWith("var")) {
                    String[] partes = linha.split("=");
                    String nome = partes[0].replace("var", "").trim();
                    int valor = Integer.parseInt(partes[1].trim());
                    variaveis.put(nome, valor);
                }
            } catch (Exception ex) {
                console.appendText("Erro na linha: " + linha + "\n");
            }
        }
    }
}