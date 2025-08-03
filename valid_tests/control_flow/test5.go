func main() {
    var x int = 10
    var y string = "hello"
    var condition bool = true
    
    // Assignments válidos
    x = 20               // OK - int para int
    condition = false    // OK - bool para bool
    
    // If-else válidos
    if condition == true {
        x = x + 1
    } else {
        x = x - 1
    }
    
    // For loop válido
    var i int = 0
    for i < 5 {
        x = x + i
        i = i + 1
    }
}
