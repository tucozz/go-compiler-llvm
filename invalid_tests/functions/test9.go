func add(x int, y int) int {
    return x + y
}

func greet(name string, age int) {
    // função de exemplo
}

func main() {
    var a int = 10
    var name string = "João"
    var age int = 25
    
    var result int
    // Erros de tipo
    result = add(name, age)    // Erro - string no lugar de int
    greet(a, name)             // Erro - int no lugar de string, string no lugar de int
}
