// Teste 3: Redeclaração no mesmo escopo
func testeRedeclaracao() {
    var x int
    x = 5
    
    // Tentar redeclarar x no mesmo escopo - ERRO ESPERADO
    var x string  // ERRO ESPERADO
}

func main() {
    testeRedeclaracao()
}
