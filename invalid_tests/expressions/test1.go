func main() {
    var x int = 5
    var s string = "hello"
    var f float64 = 3.14
    
    // Operações inválidas que devem ser detectadas
    var invalid1 string = x + s    // Erro - int + string (não permitido)
    var invalid2 float64 = x + f   // Erro - int + float64 (requer conversão)
    var invalid3 bool = x && f     // Erro - operador lógico com tipos não-bool
}
