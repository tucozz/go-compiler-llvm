package compiler.tables;

import java.util.HashMap;
import java.util.Map;

public class SymbolTable {
    // Usamos um HashMap para armazenar as entradas, mapeando o nome do símbolo à sua entrada.
    private Map<String, SymbolTableEntry> table;

    public SymbolTable() {
        this.table = new HashMap<>();
    }

    /**
     * Adiciona uma nova entrada à tabela de símbolos.
     * @param name O nome do identificador (variável, constante, função).
     * @param type O tipo do identificador (String, pois é flexível).
     * @param declarationLine O número da linha onde o identificador foi declarado.
     * @return true se o identificador foi adicionado com sucesso, false se já existia.
     */
    public boolean addEntry(String name, String type, int declarationLine) {
        if (table.containsKey(name)) {
            return false; // Símbolo já declarado neste escopo
        }
        table.put(name, new SymbolTableEntry(name, type, declarationLine));
        return true;
    }

    /**
     * Busca uma entrada na tabela de símbolos.
     * @param name O nome do identificador a ser buscado.
     * @return A SymbolTableEntry se encontrada, ou null caso contrário.
     */
    public SymbolTableEntry getEntry(String name) {
        return table.get(name);
    }

    /**
     * Verifica se a tabela contém um identificador específico.
     * @param name O nome do identificador.
     * @return true se contiver, false caso contrário.
     */
    public boolean contains(String name) {
        return table.containsKey(name);
    }

    /**
     * Imprime o conteúdo da tabela de símbolos.
     */
    public void printTable() {
        System.out.println("\n--- Symbol Table ---");
        if (table.isEmpty()) {
            System.out.println("No symbols declared.");
            return;
        }
        table.values().forEach(System.out::println);
        System.out.println("--------------------");
    }
}