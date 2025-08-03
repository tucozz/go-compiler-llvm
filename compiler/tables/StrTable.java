package compiler.tables;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Tabela de strings para gerenciar literais string no programa.
 * Evita duplicação e gera IDs únicos para cada string literal.
 */
public class StrTable {
    private Map<String, StrEntry> table;
    private AtomicInteger nextId;

    public StrTable() {
        this.table = new HashMap<>();
        this.nextId = new AtomicInteger(0);
    }

    /**
     * Adiciona uma string literal à tabela.
     * Se já existir, retorna a entrada existente.
     */
    public StrEntry addString(String value) {
        if (table.containsKey(value)) {
            return table.get(value);
        }
        
        StrEntry newEntry = new StrEntry(value, nextId.getAndIncrement());
        table.put(value, newEntry);
        return newEntry;
    }

    /**
     * Busca uma string na tabela
     */
    public StrEntry getString(String value) {
        return table.get(value);
    }

    /**
     * Verifica se uma string existe na tabela
     */
    public boolean contains(String value) {
        return table.containsKey(value);
    }

    /**
     * Retorna o número de strings na tabela
     */
    public int size() {
        return table.size();
    }

    /**
     * Retorna todas as entradas da tabela
     */
    public Map<String, StrEntry> getAllEntries() {
        return new HashMap<>(table);
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("String Table:\n");
        for (StrEntry entry : table.values()) {
            sb.append(entry.toString()).append("\n");
        }
        return sb.toString();
    }

    /**
     * Classe interna para representar uma entrada da tabela de strings
     */
    public static class StrEntry {
        private String value;
        private int id;
        
        public StrEntry(String value, int id) {
            this.value = value;
            this.id = id;
        }
        
        public String getValue() { return value; }
        public int getId() { return id; }
        
        @Override
        public String toString() {
            return String.format("ID: %d, Value: \"%s\"", id, value);
        }
        
        @Override
        public boolean equals(Object obj) {
            if (this == obj) return true;
            if (obj == null || getClass() != obj.getClass()) return false;
            StrEntry strEntry = (StrEntry) obj;
            return id == strEntry.id && value.equals(strEntry.value);
        }
        
        @Override
        public int hashCode() {
            return value.hashCode();
        }
    }
}
