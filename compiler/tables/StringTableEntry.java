package compiler.tables;

public class StringTableEntry {
    private String value;
    private int id; // Um ID único para a string, útil para referenciá-la na geração de código

    public StringTableEntry(String value, int id) {
        this.value = value;
        this.id = id;
    }

    // --- Getters ---
    public String getValue() { return value; }
    public int getId() { return id; }

    @Override
    public String toString() {
        return "ID: " + id + ", Value: \"" + value + "\"";
    }
}