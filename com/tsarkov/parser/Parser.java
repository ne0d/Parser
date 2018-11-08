package com.tsarkov.parser;

import java.io.*;
import java.sql.*;
import java.util.EmptyStackException;
import java.util.Stack;
import java.util.regex.Matcher;


public class Parser implements RegExpPattern {
    private String inText = "";
    private int idNode = 0;
    private Node threeNode = new Node(idNode, null, "", "");

    public static void main(String[] args){
        Parser parser = new Parser();
        // Передача в парсер файлов для чтения, записи и путь к БД
        parser.readFile("/home/x7/raw");
        parser.writeFile("/home/x7/result");
        parser.writeDB("jdbc:postgresql://127.0.0.1:5432/parser");
    }

    private void readFile(String pathReadFile ) {
        try (BufferedReader rd = new BufferedReader(new FileReader(pathReadFile))) {
            // Чтение всего файла в строковую переменную
            while (rd.ready()) {
                inText += rd.readLine() + " ";
            }
            if (inText.isEmpty()) throw new IOException();
            // Разбор прочитанного текста и создание дерева согласно его иерархии
            parseString(inText, threeNode);
        } catch (IllegalArgumentException | EmptyStackException e) {
            System.out.println("Неверный формат данных");
        } catch (IOException e) {
            System.out.println("Файл пуст либо не найден");
            e.printStackTrace();
        }
    }
    void parseString(String inText, Node rootNode) {
        int indexStart = 0;
        int indexEnd = 0;
        String buffer = "";
        Stack<Integer> stack = new Stack<>();
        // Поиск открывающих скобок и закрывающих скобок, обозначающих границы следующего рекурсивного вызова
        for (int i = 0; i < inText.length(); i++) {
            if (inText.charAt(i) == '{') {
                if (stack.size() == 0) {
                    indexStart = i;
                    // Создание строки-буфера для поиска узлов в текущем методе рекурсивного дерева
                    buffer = inText.substring(indexEnd, i);
                }
                // При положительном поиске добавление в стек
                stack.push(i);
            }
            if (inText.charAt(i) == '}') {
                // Поиск конца области для запуска ее в рекурсию
                if (stack.size() == 1) {
                    indexEnd = i;
                    // Рекурсионный вызов  с передачей в него найденной области, поиск узлов дерева из буфера
                    parseString(inText.substring(indexStart + 1, indexEnd), createNode(rootNode, buffer));
                    indexEnd++;
                }
                // Удаление из стека
                stack.pop();
            }
        }
        // Проверка контракта на соотношения открывающихся и закрывающихся символов
        if (!stack.empty()) throw new IllegalArgumentException();
        // Проверка условия конца рекурсионного спуска, парсинг узлов
        if (indexEnd == 0) {
            createNode(rootNode, inText);
        } else {
            if (indexEnd != inText.length() && !inText.substring(indexEnd, inText.length()).matches("^\\s+$")) {
                createNode(rootNode, inText.substring(indexEnd, inText.length()));
            }
        }
    }

    Node createNode(Node rootNode, String inBuffer) {
        // Один узел в строке
        Matcher m = regexpBranchRec.matcher(inBuffer);
        if (m.matches()) {
            // Парсинг узла
            m = regexpListNode.matcher(inBuffer);
            m.find();
            // Обнаружения root узла
            if (idNode == 0) {
                rootNode.name = inBuffer.substring(m.start(), m.end());
                rootNode.id = ++idNode;
                return rootNode;
            } else {
                Node node = new Node(++idNode, rootNode, inBuffer.substring(m.start(), m.end()), "");
                rootNode.children.add(node);
                return node;
            }
        }
        // Несколько узлов в строке
        m = regexpBranchRecLong.matcher(inBuffer);
        if (m.matches()) {
            // Парсинг узлов
            m = regexpChildrenNode.matcher(inBuffer);
            while (m.find()) {
                String[] arrInBuffer = inBuffer.trim().split("\\s");
                Node node = new Node(++idNode, rootNode, arrInBuffer[0], arrInBuffer[2].replaceAll("[\"\\“\\”]", ""));
                rootNode.children.add(node);
            }
            // Парсинг узла без значения
            m = regexpListNodeLong.matcher(inBuffer);
            m.find();
            Node node = new Node(++idNode, rootNode, inBuffer.substring(m.start(), m.end()), "");
            rootNode.children.add(node);
            return node;
        }
        // Поиск узлов в конце рекурсивного спуска
        m = regexpInnerEndNode.matcher(inBuffer);
        if (m.matches()) {
            m = regexpChildrenNode.matcher(inBuffer);
            while (m.find()) {
                String[] arrInBuffer = inBuffer.trim().split("\\s");
                Node node = new Node(++idNode, rootNode, arrInBuffer[0], arrInBuffer[2].replaceAll("[\"\\“\\”]", ""));
                rootNode.children.add(node);
            }
            return null;
        }
        throw new IllegalArgumentException();
    }
    private  void writeDB(String pathWriteDB) {
        try {
            //
            Class.forName("org.postgresql.Driver");
            String url = "jdbc:postgresql://localhost:5432/parser";
            String login = "pars";
            String password = "qwe123";
            // Подключение к БД
            Connection connectionDB = DriverManager.getConnection(url, login, password);
            try {
                String createTable =  "create table tree_table (" +
                        "node_id int not null primary key," +
                        "parent_id int references tree_table( node_id )," +
                        "node_name varchar(100)," +
                        "node_value varchar(1000) )";
                Statement statement = connectionDB.createStatement();
                // Создание таблицы
                statement.execute(createTable);
                // Создание нулевой строки
                statement.executeUpdate("INSERT INTO tree_table (node_id, parent_id, node_name, node_value) VALUES ('0','0','','');");
                // Добавление информации узлов в БД
                readNodeTree(threeNode,statement);
                statement.close();
            } finally {
                connectionDB.close();
            }
        } catch (SQLException e) {
            System.out.println("Ошибка при работе с базой данных");
            e.printStackTrace();
        } catch (ClassNotFoundException e){
            System.out.println("Не найден дравер базы данных");
            e.printStackTrace();
        }
    }
    private void writeFile(String pathWriteFile){
        try ( BufferedWriter writer = new BufferedWriter(new FileWriter(pathWriteFile, false))) {
            readNodeTree(threeNode, writer);
        } catch (IOException e){
            System.out.println("Файл пуст либо не найден");
            e.printStackTrace();
        }
    }
    void readNodeTree(Node node, Writer writer) throws IOException {
        writer.write("[ " + node.id + ", " + (node.parent == null ? 0 : node.parent.id) + ", " + node.name + ", " + node.value + " ]\n");
        if (node.children.size() != 0) {
            for (Node nd : node.children) {
                readNodeTree(nd, writer);
            }
        }
    }
    void readNodeTree(Node node, Statement statement) throws SQLException {
        String loadFieldNode = "INSERT INTO tree_table (node_id, parent_id, node_name, node_value) VALUES ('" +
                node.id + "','" + (node.parent == null ? 0 : node.parent.id) + "','" + node.name + "','" + node.value + "');";
        statement.executeUpdate(loadFieldNode);
        if (node.children.size() != 0) {
            for (Node nd : node.children) {
                readNodeTree(nd, statement);
            }
        }
    }
}
