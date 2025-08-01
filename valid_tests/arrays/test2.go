// Test 2: Array with for range
func main() {
    numbers := []int{10, 20, 30, 40, 50}
    sum := 0
    for i, value := range numbers {
        sum = sum + value
        index := i
    }
}
