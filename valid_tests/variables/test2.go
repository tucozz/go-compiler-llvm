func add(x int, y int) int {
    return x + y
}

func greet(name string, age int) {
    // função de exemplo
}

func main() {
    var a int = 10
    var b int = 20
    name := "João"
    age := 25
    score := 95
    message := "Hello"

    var result int
    result = add(a, b)         // OK - variáveis int
    result = add(a, 5)         // OK - variável + literal
    greet(name, age)           // OK - variáveis string e int
    greet("Maria", age)        // OK - literal + variável
}
