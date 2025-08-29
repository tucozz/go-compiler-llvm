func main() {
    println("--- Teste de Controle de Fluxo ---")
    
    soma := 0
    for i := 0; i < 5; i = i + 1 {
        if i % 2 == 0 {
            println(i, "é par")
            soma = soma + i
        } else {
            println(i, "é ímpar")
        }
    }
    println("A soma dos números pares de 0 a 4 é:", soma)
}