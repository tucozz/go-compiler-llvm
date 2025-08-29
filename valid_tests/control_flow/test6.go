func main() {
    var i int = 0
    var condition bool = true
    var limit int = 10
    
    // For com condição válida
    for i < limit {       // OK - comparação retorna bool
        i = i + 1
    }
    
    // For com condição booleana
    for condition {       // OK - variável bool
        condition = false
    }
    
    // For infinito (válido em Go)
    for {                 // OK - sem condição
        if i > 20 {
            break
        }
        i = i + 1
    }
}
