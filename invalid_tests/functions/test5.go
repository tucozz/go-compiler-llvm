// Test: Function redeclaration
func add(a int, b int) int {
    return a + b
}

func add(x int, y int) int {
    return x - y
}
