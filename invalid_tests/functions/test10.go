func add(x int, y int) int {
    return x + y
}

func greet(name string, age int) {
    // função de exemplo
}

func main() {
    var result int
    result = add(1)           // Erro - poucos argumentos
    result = add(1, 2, 3)     // Erro - muitos argumentos
    greet(30, "Maria")        // Erro - tipos incorretos
}
