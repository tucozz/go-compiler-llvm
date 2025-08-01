// Test 2: Function parameter scoping
func calculate(x int, y int) int {
    result := x + y
    return result
}

func main() {
    x := 5
    y := 10
    sum := calculate(x, y)
}
