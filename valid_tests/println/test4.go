func main() {
    println("--- Teste de Escopos ---")
    
    x := 100
    println("Valor de x no escopo externo (antes do bloco):", x)
    
    {
        x := 200 // Declara um NOVO 'x' no escopo interno
        println("Valor de x no escopo interno:", x)
    }
    
    println("Valor de x no escopo externo (depois do bloco):", x)
}