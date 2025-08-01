// Test 1: Nested if statements
func main() {
    x := 10
    y := 20
    
    if x > 0 {
        if y > x {
            result := y - x
        } else {
            result := x - y
        }
    }
}
