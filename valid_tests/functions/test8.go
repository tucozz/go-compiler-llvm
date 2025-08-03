func add(x int, y int) int {
    return x + y    // OK - retorna int
}

func sayHello() {
    return          // OK - return void em função void
}

func calculate(a int, b int) int {
    if a > b {
        return a    // OK - retorna int
    } else {
        return b    // OK - retorna int
    }
}

func main() {
    var result int = add(5, 3)
    sayHello()
}
