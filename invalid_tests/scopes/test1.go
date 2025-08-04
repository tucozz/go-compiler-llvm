// Teste 1: Variável em escopo global e local
var globalVar int

func testeEscopo() {
    var localVar int
    localVar = 5
    globalVar = 10
}

func main() {
    // Deve conseguir acessar globalVar
    globalVar = 20
    
    // NÃO deve conseguir acessar localVar (fora de escopo)
    localVar = 30  // ERRO ESPERADO
}
