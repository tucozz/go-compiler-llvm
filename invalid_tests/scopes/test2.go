// Teste 2: Escopo de blocos (if, for, etc)
func testeEscopoBloco() {
    var x int
    x = 5
    
    if x > 0 {
        var y int
        y = 10
        x = y  // OK - x está no escopo externo
    }
    
    // y NÃO deve estar acessível aqui
    y = 15  // ERRO ESPERADO
}

func main() {
    testeEscopoBloco()
}
