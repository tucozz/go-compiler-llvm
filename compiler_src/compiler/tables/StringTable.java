package compiler.tables;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger; // Para gerar IDs únicos de forma segura

public class StringTable {
    // Usamos um HashMap para armazenar as strings, mapeando o valor da string à sua entrada.
    private Map<String, StringTableEntry> table;
    // AtomicInteger para gerar IDs sequenciais e únicos para cada string adicionada.
    private AtomicInteger nextId;

    public StringTable() {
        this.table = new HashMap<>();
        this.nextId = new AtomicInteger(0); // Começa a numeração dos IDs a partir de 0
    }

    /**
     * Adiciona uma string à tabela de strings. Se a string já existe, retorna a entrada existente.
     * Caso contrário, cria uma nova entrada com um ID único.
     * @param value O valor literal da string (sem as aspas externas).
     * @return A StringTableEntry correspondente à string.
     */
    public StringTableEntry addString(String value) {
        if (table.containsKey(value)) {
            return table.get(value);
        }
        StringTableEntry newEntry = new StringTableEntry(value, nextId.getAndIncrement());
        table.put(value, newEntry);
        return newEntry;
    }

    /**
     * Imprime o conteúdo da tabela de strings.
     */
    public void printTable() {
        System.out.println("\n--- String Table ---");
        if (table.isEmpty()) {
            System.out.println("No strings found.");
            return;
        }
        table.values().forEach(System.out::println);
        System.out.println("--------------------");
    }
}