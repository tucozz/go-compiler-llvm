func main() {
    var x int = 10
    var y string = "hello"
    
    // Assignments inválidos
    x = "invalid"        // Erro - string para int
    y = 42               // Erro - int para string
    
    // Assignment para constante (erro)
    const PI int = 3
    PI = 4               // Erro - assignment para constante
}
