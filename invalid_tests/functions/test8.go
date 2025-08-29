func multiply(a int, b int) int {
    return "error"  // Erro - retorna string em função int
}

func greet(name string) {
    return "hello"  // Erro - retorna string em função void
}

func main() {
    var x int = multiply(2, 3)
    greet("test")
}
