// Test 3: Function with different types
func calculate(x float64, name string, count int) float64 {
    if count > 0 {
        return x * float64(count)
    }
    return 0.0
}

func main() {
    result := calculate(3.14, "test", 2)
}
