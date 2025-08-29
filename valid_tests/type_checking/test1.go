func main() {
    var x int = 5
    var y int = 3
    var s1 string = "hello"
    var s2 string = "world"
    var f float64 = 3.14
    var b bool = true
    
    // Operações válidas em Go
    var sum_int int = x + y           // OK - int + int = int
    var concat string = s1 + s2       // OK - string + string = string
    var sum_float float64 = f + 2.5   // OK - float64 + float64 = float64
    var compare bool = x < y          // OK - int < int = bool
    var str_compare bool = s1 == s2   // OK - string == string = bool
    var logic bool = b && true        // OK - bool && bool = bool
    
    // Operações inválidas que agora devem ser detectadas
    // var invalid1 string = x + s1   // Erro - int + string (não permitido)
    // var invalid2 float64 = x + f   // Erro - int + float64 (requer conversão)
    // var invalid3 bool = x && y     // Erro - operador lógico com int
}
