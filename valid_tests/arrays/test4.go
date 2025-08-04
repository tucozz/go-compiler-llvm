func main() {
    // Declaração de arrays
    var numbers []int
    var names []string
    var flags []bool
    
    // Acesso válido a arrays (assumindo que existem)
    var first int = numbers[0]
    var name string = names[1]
    var flag bool = flags[2]
    
    // Expressão com acesso a array
    var sum int = numbers[0] + numbers[1]
    var comparison bool = numbers[0] < numbers[1]
    
    // Assignment para array
    numbers[0] = 42
    names[1] = "test"
    flags[2] = true
}
