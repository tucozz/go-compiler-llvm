func main() {
    var x int
    x = 0
    
    // For com condição simples
    for x < 10 {
        x = x + 1
    }
    
    var running bool
    running = true
    
    // For com variável booleana
    for running {
        x = x + 1
        if x > 15 {
            running = false
        }
    }
}
