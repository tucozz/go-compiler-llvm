// Teste 5: Escopo de parâmetros de função
func testeParametros(param int) {
    param = 10  // OK - param está no escopo da função
    var local int
    local = param  // OK
}

func outraFuncao() {
    // NÃO deve conseguir acessar param
    param = 20  // ERRO ESPERADO
}

func main() {
    testeParametros(5)
    outraFuncao()
}
