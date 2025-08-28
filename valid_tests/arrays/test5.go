// Test: Arrays as function parameters
func modifyArray(arr []int, index int, value int) {
    arr[index] = value
}

func printArray(arr []int) {
    println(arr[0])
    println(arr[1])
    println(arr[2])
}

func main() {
    var numbers []int
    numbers[0] = 1
    numbers[1] = 2
    numbers[2] = 3
    
    printArray(numbers)
    
    modifyArray(numbers, 1, 99)
    
    printArray(numbers)
}
