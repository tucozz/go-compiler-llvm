// Test 4: Loop variable scoping
func main() {
    sum := 0
    for i := 0; i < 5; i = i + 1 {
        temp := i * 2
        sum = sum + temp
    }
    result := sum
}
