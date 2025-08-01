// Test 4: Nested function calls
func multiply(a int, b int) int {
    return a * b
}

func square(x int) int {
    return multiply(x, x)
}

func main() {
    result := square(5)
    final := multiply(result, 2)
}
