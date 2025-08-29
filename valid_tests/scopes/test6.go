// Teste 6: Caso válido - escopo correto
var global int

func funcaoValida(param int) {
    var local int
    
    // Todas essas atribuições devem ser válidas
    global = 10
    param = 20
    local = 30
    
    if param > 10 {
        var blocal string
        blocal = "teste"
        local = param  // OK - local está no escopo externo
    }
}

func main() {
    global = 100
    funcaoValida(50)
}
