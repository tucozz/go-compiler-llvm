// Função simples
func multiply(a int, b int) int {
    return a * b
}

// Sem retorno
func printHello() {
    println("Hello")
}

// Múltiplos parâmetros do mesmo tipo
func calculate(x int, y int, z int) int {
    return x + y * z
}

// Como tipo de função
var operation func(int, int) int = multiply