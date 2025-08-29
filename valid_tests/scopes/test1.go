// Test 1: Basic block scoping
func main() {
    x := 10
    {
        y := 20
        z := x + y
    }
    w := x + 5
}
