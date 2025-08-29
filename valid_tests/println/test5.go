func soma(a int, b int) int {
    resultadoSoma := a + b
    return resultadoSoma
}

func apresentaResultado(operacao string, valor int) {
    println("O resultado da", operacao, "é:", valor)
}

func main() {
    println("--- Teste de Funções Simples ---")

    x := 15
    y := 7

    // Chama uma função que retorna um valor
    total := soma(x, y)

    // Usa o resultado como argumento para outra função
    apresentaResultado("soma", total)
}
