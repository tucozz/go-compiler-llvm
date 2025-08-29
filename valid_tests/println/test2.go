func main() {
    println("--- Teste de Operadores ---")

    resultadoAritmetico := 10 + 5 * 2 // Deve ser 20, não 30
    println("Resultado de 10 + 5 * 2 é:", resultadoAritmetico)

    resultadoLogico := resultadoAritmetico > 15 && (true || false)
    println("Resultado de (20 > 15 && true) é:", resultadoLogico)
}