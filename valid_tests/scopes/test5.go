// Teste 4: Shadowing - mesmo nome em escopos diferentes
var x int

func testeShadowing() {
    x = 10  // acessa a global
    
    if x > 5 {
        var x string  // OK - escopo diferente (shadowing)
        x = "local"
    }
    
    x = 20  // volta a acessar a global
}

func main() {
    testeShadowing()
    x = 30  // acessa a global
}
