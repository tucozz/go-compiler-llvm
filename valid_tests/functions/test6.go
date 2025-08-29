// Compilador Go - Teste Final de Funcionalidades
// Demonstra todas as capacidades implementadas

func fibonacci(n int) int {
    if n <= 1 {
        return n
    } else {
        return fibonacci(n - 1) + fibonacci(n - 2)
    }
}

func processArray(arr []int, size int) bool {
    var i int = 0
    var found bool = false
    
    for i < size {
        if arr[i] == 42 {
            found = true
            break
        }
        
        if arr[i] < 0 {
            i = i + 1
            continue
        }
        
        arr[i] = arr[i] * 2
        i = i + 1
    }
    
    return found
}

func main() {
    // Tipos básicos
    var count int = 10
    var pi float64 = 3.14
    var message string = "Hello Go Compiler!"
    var active bool = true
    
    // Arrays (tipo composto)
    var numbers []int
    var words []string
    var flags []bool
    
    // Inicialização de arrays
    numbers[0] = 1
    numbers[1] = 2
    numbers[2] = 42
    
    words[0] = "first"
    words[1] = "second"
    
    flags[0] = true
    flags[1] = false
    
    // Chamadas de função
    var fib int = fibonacci(count)
    var result bool = processArray(numbers, 3)
    
    // Estruturas de controle
    if result == true {
        message = "Found target!"
    } else {
        message = "Target not found"
    }
    
    // Loop com break/continue
    var j int = 0
    for j < count {
        if j == 5 {
            j = j + 1
            continue
        }
        
        if j >= 8 {
            break
        }
        
        // Operações aritméticas e de comparação
        var temp int = j * 2 + 1
        if temp > fib {
            flags[0] = false
        }
        
        j = j + 1
    }
    
    // Teste final
    var final_result int = fib + count
}
