package compiler.tables;

import java.util.HashMap;
import java.util.Map;
import java.util.ArrayList;
import java.util.List;

public class SymbolTable {
    private Map<String, SymbolTableEntry> table;
    private List<SymbolTableEntry> indexedEntries;

    public SymbolTable() {
        this.table = new HashMap<>();
        this.indexedEntries = new ArrayList<>();
    }

    public boolean addEntry(String name, String type, int declarationLine) {
        if (table.containsKey(name)) {
            return false;
        }
        SymbolTableEntry newEntry = new SymbolTableEntry(name, type, declarationLine);
        table.put(name, newEntry);
        indexedEntries.add(newEntry);

        return true;
    }

    public SymbolTableEntry getEntry(String name) {
        return table.get(name);
    }

    public boolean contains(String name) {
        return table.containsKey(name);
    }
    
    public SymbolTableEntry getEntryByIndex(int index) {
        if (index >= 0 && index < indexedEntries.size()) {
            return indexedEntries.get(index);
        }
        return null;
    }

    public void printTable() {
        System.out.println("\n--- Symbol Table ---");
        if (table.isEmpty()) {
            System.out.println("No symbols declared.");
            return;
        }
        for (int i = 0; i < indexedEntries.size(); i++) {
            System.out.println("Entry " + i + " -- " + indexedEntries.get(i).toString());
        }
        System.out.println("--------------------");
    }
}