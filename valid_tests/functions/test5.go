func calculate(a int, b int) int {
    var sum int = a + b
    var product int = a * b
    
    if sum > product {
        return sum
    } else {
        return product
    }
}

func main() {
    // Declarações básicas
    var count int = 0
    var numbers []int
    var names []string
    var found bool = false
    
    // Arrays com assignments
    numbers[0] = 10
    numbers[1] = 20
    names[0] = "first"
    names[1] = "second"
    
    // Loop com break e continue
    for count < 10 {
        if count == 3 {
            count = count + 1
            continue
        }
        
        if count == 7 {
            break
        }
        
        // Operações com arrays
        var current int = numbers[count]
        if current > 15 {
            found = true
        }
        
        count = count + 1
    }
    
    // Função auxiliar
    var result int = calculate(5, 3)
}

