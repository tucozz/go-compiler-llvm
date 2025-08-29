// Test 3: Nested scopes with same variable names
func main() {
    x := 1
    {
        x := 2
        {
            x := 3
            y := x
        }
        z := x
    }
    w := x
}
